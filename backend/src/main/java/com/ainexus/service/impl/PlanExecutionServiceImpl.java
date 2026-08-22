package com.ainexus.service.impl;

import com.ainexus.agent.*;
import com.ainexus.dto.*;
import com.ainexus.entity.User;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.AgentPlanningService;
import com.ainexus.service.PlanExecutionService;
import com.ainexus.service.RAGGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlanExecutionServiceImpl implements PlanExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(PlanExecutionServiceImpl.class);

    private final AgentPlanningService planningService;
    private final WorkspaceRepository workspaceRepository;
    private final SearchAgent searchAgent;
    private final AnalysisAgent analysisAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final RAGGenerationService ragGenerationService;

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
        logger.info("Starting execution {} for plan {}", executionId, plan.planId());

        Map<String, String> outputsByTaskId = new LinkedHashMap<>();
        Map<String, AgentTaskStatus> taskStatusMap = new HashMap<>();
        List<AgentTaskResult> taskResults = new ArrayList<>();

        boolean anyFailed = false;
        boolean allSuccess = true;

        for (AgentTask task : plan.tasks()) {
            // Check if dependencies succeeded
            boolean dependenciesSatisfied = true;
            StringBuilder dependencyContext = new StringBuilder();

            for (String depId : task.dependsOn()) {
                AgentTaskStatus depStatus = taskStatusMap.getOrDefault(depId, AgentTaskStatus.PENDING);
                if (depStatus != AgentTaskStatus.COMPLETED) {
                    dependenciesSatisfied = false;
                    break;
                }
                String depOutput = outputsByTaskId.get(depId);
                if (depOutput != null && !depOutput.isBlank()) {
                    dependencyContext.append("\n[Context from ").append(depId).append("]:\n").append(depOutput).append("\n");
                }
            }

            if (!dependenciesSatisfied) {
                logger.warn("Task {} skipped due to missing/failed dependencies.", task.id());
                taskStatusMap.put(task.id(), AgentTaskStatus.SKIPPED);
                taskResults.add(AgentTaskResult.skipped(task.id(), task.type(), "Dependency not completed successfully."));
                allSuccess = false;
                continue;
            }

            // Execute Task
            try {
                logger.info("Executing task {} ({})", task.id(), task.type());
                String taskOutput = executeSingleTask(task, plan, dependencyContext.toString(), memory, authenticatedUser);
                outputsByTaskId.put(task.id(), taskOutput);
                taskStatusMap.put(task.id(), AgentTaskStatus.COMPLETED);
                taskResults.add(AgentTaskResult.success(task.id(), task.type(), taskOutput));
            } catch (Exception e) {
                logger.error("Task {} failed during execution: {}", task.id(), e.getMessage());
                taskStatusMap.put(task.id(), AgentTaskStatus.FAILED);
                taskResults.add(AgentTaskResult.failure(task.id(), task.type(), e.getMessage()));
                anyFailed = true;
                allSuccess = false;
            }
        }

        PlanExecutionStatus executionStatus;
        if (allSuccess) {
            executionStatus = PlanExecutionStatus.COMPLETED;
        } else if (anyFailed && !outputsByTaskId.isEmpty()) {
            executionStatus = PlanExecutionStatus.PARTIALLY_COMPLETED;
        } else if (anyFailed) {
            executionStatus = PlanExecutionStatus.FAILED;
        } else {
            executionStatus = PlanExecutionStatus.PARTIALLY_COMPLETED;
        }

        String finalOutput = deriveFinalOutput(plan, outputsByTaskId, taskResults);

        return new AgentExecutionResult(
                executionId,
                plan.planId(),
                executionStatus,
                finalOutput,
                taskResults,
                outputsByTaskId
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
