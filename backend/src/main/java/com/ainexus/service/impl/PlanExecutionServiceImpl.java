package com.ainexus.service.impl;

import com.ainexus.agent.*;
import com.ainexus.dto.*;
import com.ainexus.entity.User;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.AgentPlanningService;
import com.ainexus.service.PlanExecutionService;
import com.ainexus.service.RAGGenerationService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class PlanExecutionServiceImpl implements PlanExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(PlanExecutionServiceImpl.class);

    private final AgentPlanningService planningService;
    private final WorkspaceRepository workspaceRepository;
    private final SearchAgent searchAgent;
    private final AnalysisAgent analysisAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final RAGGenerationService ragGenerationService;

    @Value("${app.agent.execution.max-concurrency:4}")
    private int maxConcurrency = 4;

    @Value("${app.agent.execution.task-timeout-seconds:30}")
    private int taskTimeoutSeconds = 30;

    private final ExecutorService executorService;

    @Autowired
    public PlanExecutionServiceImpl(
            AgentPlanningService planningService,
            WorkspaceRepository workspaceRepository,
            @Autowired(required = false) SearchAgent searchAgent,
            @Autowired(required = false) AnalysisAgent analysisAgent,
            @Autowired(required = false) KnowledgeAgent knowledgeAgent,
            @Autowired(required = false) RAGGenerationService ragGenerationService) {
        this.planningService = planningService;
        this.workspaceRepository = workspaceRepository;
        this.searchAgent = searchAgent;
        this.analysisAgent = analysisAgent;
        this.knowledgeAgent = knowledgeAgent;
        this.ragGenerationService = ragGenerationService;
        this.executorService = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public void setTaskTimeoutSeconds(int taskTimeoutSeconds) {
        this.taskTimeoutSeconds = taskTimeoutSeconds;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down PlanExecutionServiceImpl executor pool");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public AgentExecutionResult executePlan(AgentPlan plan, User authenticatedUser) {
        return executePlan(plan, null, authenticatedUser);
    }

    @Override
    public AgentExecutionResult executePlan(AgentPlan plan, ConversationMemory memory, User authenticatedUser) {
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null for execution.");
        }
        if (authenticatedUser == null) {
            throw new UnauthorizedAccessException("Authenticated user is required to execute an agent plan.");
        }

        // 1. Validate plan structure
        if (!planningService.validatePlan(plan)) {
            logger.error("Plan {} failed validation before execution.", plan.planId());
            throw new IllegalArgumentException("Invalid plan structure: cannot execute plan " + plan.planId());
        }

        // 2. Authorize workspace access
        Long workspaceId = plan.workspaceId();
        if (workspaceRepository != null && workspaceId != null) {
            boolean hasAccess = workspaceRepository.findById(workspaceId)
                    .map(ws -> ws.getOwner() != null && ws.getOwner().getId().equals(authenticatedUser.getId()))
                    .orElse(false);
            if (!hasAccess) {
                logger.warn("User {} is unauthorized for workspace {}", authenticatedUser.getUsername(), workspaceId);
                throw new UnauthorizedAccessException("User does not have access to workspace " + workspaceId);
            }
        }

        String executionId = "exec-" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Starting parallel execution {} for plan {}", executionId, plan.planId());

        ConcurrentMap<String, String> outputsByTaskId = new ConcurrentHashMap<>();
        ConcurrentMap<String, AgentTaskStatus> taskStatusMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, AgentTaskResult> taskResultsMap = new ConcurrentHashMap<>();

        Set<String> remainingTaskIds = plan.tasks().stream().map(AgentTask::id).collect(Collectors.toSet());
        Map<String, AgentTask> taskLookup = plan.tasks().stream().collect(Collectors.toMap(AgentTask::id, t -> t));

        Semaphore concurrencyLimiter = new Semaphore(Math.max(1, maxConcurrency));

        while (!remainingTaskIds.isEmpty()) {
            List<AgentTask> readyTasks = new ArrayList<>();
            List<AgentTask> skippedTasks = new ArrayList<>();

            for (String taskId : remainingTaskIds) {
                AgentTask task = taskLookup.get(taskId);
                boolean allDepsSatisfied = true;
                boolean anyDepFailed = false;

                for (String depId : task.dependsOn()) {
                    AgentTaskStatus depStatus = taskStatusMap.getOrDefault(depId, AgentTaskStatus.PENDING);
                    if (depStatus == AgentTaskStatus.FAILED || depStatus == AgentTaskStatus.SKIPPED) {
                        anyDepFailed = true;
                        break;
                    }
                    if (depStatus != AgentTaskStatus.COMPLETED) {
                        allDepsSatisfied = false;
                    }
                }

                if (anyDepFailed) {
                    skippedTasks.add(task);
                } else if (allDepsSatisfied) {
                    readyTasks.add(task);
                }
            }

            // Mark skipped tasks immediately
            for (AgentTask skipped : skippedTasks) {
                logger.warn("Task {} skipped due to upstream dependency failure.", skipped.id());
                taskStatusMap.put(skipped.id(), AgentTaskStatus.SKIPPED);
                taskResultsMap.put(skipped.id(), AgentTaskResult.skipped(skipped.id(), skipped.type(), "Dependency not completed successfully."));
                remainingTaskIds.remove(skipped.id());
            }

            if (readyTasks.isEmpty() && !skippedTasks.isEmpty()) {
                continue;
            }

            if (readyTasks.isEmpty() && !remainingTaskIds.isEmpty()) {
                logger.error("Deadlock or unresolved dependencies detected for tasks: {}", remainingTaskIds);
                for (String unresolvableId : remainingTaskIds) {
                    AgentTask t = taskLookup.get(unresolvableId);
                    taskStatusMap.put(t.id(), AgentTaskStatus.SKIPPED);
                    taskResultsMap.put(t.id(), AgentTaskResult.skipped(t.id(), t.type(), "Unresolvable dependency graph"));
                }
                remainingTaskIds.clear();
                break;
            }

            // Execute ready tasks in parallel
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (AgentTask task : readyTasks) {
                StringBuilder dependencyContext = new StringBuilder();
                for (String depId : task.dependsOn()) {
                    String depOut = outputsByTaskId.get(depId);
                    if (depOut != null && !depOut.isBlank()) {
                        dependencyContext.append("\n[Context from ").append(depId).append("]:\n").append(depOut).append("\n");
                    }
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        concurrencyLimiter.acquire();
                        taskStatusMap.put(task.id(), AgentTaskStatus.RUNNING);
                        logger.info("Executing task {} ({}) in thread {}", task.id(), task.type(), Thread.currentThread().getName());
                        String output = executeSingleTask(task, plan, dependencyContext.toString(), memory, authenticatedUser);
                        outputsByTaskId.put(task.id(), output);
                        taskStatusMap.put(task.id(), AgentTaskStatus.COMPLETED);
                        taskResultsMap.put(task.id(), AgentTaskResult.success(task.id(), task.type(), output));
                    } catch (Exception e) {
                        logger.error("Task {} failed during execution: {}", task.id(), e.getMessage());
                        taskStatusMap.put(task.id(), AgentTaskStatus.FAILED);
                        taskResultsMap.put(task.id(), AgentTaskResult.failure(task.id(), task.type(), e.getMessage()));
                    } finally {
                        concurrencyLimiter.release();
                    }
                }, executorService);

                futures.add(future);
                remainingTaskIds.remove(task.id());
            }

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(taskTimeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                logger.error("Parallel execution timed out after {} seconds", taskTimeoutSeconds);
                for (AgentTask task : readyTasks) {
                    if (taskStatusMap.get(task.id()) == AgentTaskStatus.RUNNING) {
                        taskStatusMap.put(task.id(), AgentTaskStatus.FAILED);
                        taskResultsMap.put(task.id(), AgentTaskResult.failure(task.id(), task.type(), "Task timed out"));
                    }
                }
            } catch (Exception e) {
                logger.error("Error awaiting parallel task futures: {}", e.getMessage());
            }
        }

        // Assemble ordered results according to original plan order
        List<AgentTaskResult> orderedResults = new ArrayList<>();
        Map<String, String> orderedOutputs = new LinkedHashMap<>();

        boolean anyFailed = false;
        boolean allSuccess = true;

        for (AgentTask task : plan.tasks()) {
            AgentTaskResult r = taskResultsMap.get(task.id());
            if (r != null) {
                orderedResults.add(r);
                if (r.status() == AgentTaskStatus.COMPLETED && r.output() != null) {
                    orderedOutputs.put(task.id(), r.output());
                } else if (r.status() == AgentTaskStatus.FAILED || r.status() == AgentTaskStatus.SKIPPED) {
                    allSuccess = false;
                    if (r.status() == AgentTaskStatus.FAILED) {
                        anyFailed = true;
                    }
                }
            }
        }

        PlanExecutionStatus executionStatus;
        if (allSuccess && !orderedResults.isEmpty()) {
            executionStatus = PlanExecutionStatus.COMPLETED;
        } else if (anyFailed && !orderedOutputs.isEmpty()) {
            executionStatus = PlanExecutionStatus.PARTIALLY_COMPLETED;
        } else if (anyFailed) {
            executionStatus = PlanExecutionStatus.FAILED;
        } else {
            executionStatus = PlanExecutionStatus.PARTIALLY_COMPLETED;
        }

        String finalOutput = deriveFinalOutput(plan, orderedOutputs, orderedResults);

        return new AgentExecutionResult(
                executionId,
                plan.planId(),
                executionStatus,
                finalOutput,
                orderedResults,
                orderedOutputs
        );
    }

    private String executeSingleTask(AgentTask task, AgentPlan plan, String dependencyContext, ConversationMemory memory, User authenticatedUser) {
        String enrichedQuery = task.description();
        if (!dependencyContext.isBlank()) {
            enrichedQuery += "\n\nAdditional Context:\n" + dependencyContext;
        }

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        AgentContext agentContext = new AgentContext(
                traceId,
                plan.workspaceId(),
                authenticatedUser
        );

        AgentRequest request = new AgentRequest(
                enrichedQuery,
                mapToAgentType(task.type()),
                plan.workspaceId(),
                authenticatedUser,
                traceId,
                Map.of("taskId", task.id(), "originalQuery", plan.originalQuery())
        );

        switch (task.type()) {
            case SEARCH:
                if (searchAgent != null) {
                    AgentResult result = searchAgent.execute(request, agentContext);
                    return result != null && result.output() != null ? result.output() : "Search completed with no results.";
                } else if (ragGenerationService != null) {
                    RAGResponse response = ragGenerationService.generateAnswer(enrichedQuery, plan.workspaceId(), 5, authenticatedUser);
                    return response != null ? response.answer() : "RAG search produced empty answer.";
                }
                return "Search agent executed for: " + task.description();

            case ANALYZE:
                if (analysisAgent != null) {
                    AgentResult result = analysisAgent.execute(request, agentContext);
                    return result != null && result.output() != null ? result.output() : "Analysis completed with no findings.";
                }
                return "Analysis executed for: " + task.description();

            case KNOWLEDGE:
                if (knowledgeAgent != null) {
                    AgentResult result = knowledgeAgent.execute(request, agentContext);
                    return result != null && result.output() != null ? result.output() : "Knowledge retrieval completed.";
                }
                return "Knowledge task executed for: " + task.description();

            case SYNTHESIZE:
                if (ragGenerationService != null) {
                    RAGResponse response = ragGenerationService.generateAnswer(enrichedQuery, plan.workspaceId(), 5, authenticatedUser);
                    return response != null ? response.answer() : "Synthesis produced empty answer.";
                } else if (analysisAgent != null) {
                    AgentResult result = analysisAgent.execute(request, agentContext);
                    return result != null && result.output() != null ? result.output() : "Synthesis completed.";
                }
                return "Synthesized result for plan: " + plan.originalQuery();

            default:
                throw new UnsupportedOperationException("Unsupported task type: " + task.type());
        }
    }

    private AgentType mapToAgentType(AgentTaskType taskType) {
        if (taskType == null) {
            return AgentType.SEARCH;
        }
        return switch (taskType) {
            case SEARCH -> AgentType.SEARCH;
            case ANALYZE, SYNTHESIZE -> AgentType.ANALYSIS;
            case KNOWLEDGE -> AgentType.KNOWLEDGE;
        };
    }

    private String deriveFinalOutput(AgentPlan plan, Map<String, String> outputsByTaskId, List<AgentTaskResult> taskResults) {
        if (outputsByTaskId.isEmpty()) {
            return "No task outputs produced during plan execution.";
        }
        if (!plan.tasks().isEmpty()) {
            String lastTaskId = plan.tasks().get(plan.tasks().size() - 1).id();
            if (outputsByTaskId.containsKey(lastTaskId)) {
                return outputsByTaskId.get(lastTaskId);
            }
        }
        StringBuilder sb = new StringBuilder();
        outputsByTaskId.forEach((k, v) -> sb.append("[").append(k).append("]: ").append(v).append("\n\n"));
        return sb.toString().trim();
    }
}
