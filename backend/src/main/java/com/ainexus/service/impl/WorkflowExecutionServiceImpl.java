package com.ainexus.service.impl;

import com.ainexus.agent.model.*;
import com.ainexus.dto.*;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowExecutionRepository;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.PlanExecutionService;
import com.ainexus.service.WorkflowExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionServiceImpl.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PlanExecutionService planExecutionService;

    public WorkflowExecutionServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository workflowExecutionRepository,
            WorkspaceRepository workspaceRepository,
            PlanExecutionService planExecutionService) {
        this.workflowRepository = workflowRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.workspaceRepository = workspaceRepository;
        this.planExecutionService = planExecutionService;
    }

    @Override
    public WorkflowExecutionResponse executeWorkflow(Long workflowId, WorkflowExecutionRequest request, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace().getId(), user);

        List<WorkflowStep> enabledSteps = workflow.getSteps().stream()
                .filter(WorkflowStep::isEnabled)
                .sorted(Comparator.comparingInt(WorkflowStep::getExecutionOrder))
                .toList();

        if (enabledSteps.isEmpty()) {
            throw new IllegalArgumentException("Cannot execute workflow without enabled steps.");
        }

        // Initialize execution state
        WorkflowExecution execution = new WorkflowExecution(workflow, workflow.getWorkspace(), user);
        execution.setStatus(WorkflowExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());

        Map<String, WorkflowStepExecution> stepExecutionMap = new HashMap<>();
        for (WorkflowStep step : enabledSteps) {
            WorkflowStepExecution stepExec = new WorkflowStepExecution(
                    step.getStepKey(),
                    step.getName(),
                    step.getType(),
                    step.getExecutionOrder()
            );
            stepExec.setStatus(WorkflowExecutionStatus.RUNNING);
            stepExec.setStartTime(LocalDateTime.now());
            execution.addStepExecution(stepExec);
            stepExecutionMap.put(step.getStepKey(), stepExec);
        }

        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Map Workflow Steps to AgentTasks for PlanExecutionService
        List<AgentTask> agentTasks = new ArrayList<>();
        String effectiveQuery = (request != null && request.inputQuery() != null && !request.inputQuery().isBlank())
                ? request.inputQuery().trim()
                : workflow.getName();

        for (WorkflowStep step : enabledSteps) {
            AgentTaskType taskType = mapStepTypeToAgentTaskType(step.getType());
            List<String> deps = step.getDependencies() != null ? step.getDependencies() : List.of();
            Map<String, Object> taskParams = new HashMap<>();
            taskParams.put("stepKey", step.getStepKey());
            taskParams.put("workflowId", workflow.getId());
            taskParams.put("configuration", step.getConfiguration());
            if (request != null && request.inputVariables() != null) {
                taskParams.putAll(request.inputVariables());
            }

            AgentTask task = new AgentTask(
                    step.getStepKey(),
                    taskType,
                    step.getName(),
                    effectiveQuery,
                    deps,
                    taskParams,
                    step.getExecutionOrder()
            );
            agentTasks.add(task);
        }

        AgentPlan agentPlan = new AgentPlan(
                "wf-plan-" + savedExecution.getId(),
                workflow.getWorkspace().getId(),
                effectiveQuery,
                agentTasks
        );

        LocalDateTime execStartTime = LocalDateTime.now();
        PlanExecutionResult planResult = null;
        try {
            logger.info("Executing workflow id: {} (execution id: {}) with {} steps for user: {}",
                    workflow.getId(), savedExecution.getId(), enabledSteps.size(), user.getUsername());

            planResult = planExecutionService.executePlan(agentPlan, user);

            LocalDateTime execEndTime = LocalDateTime.now();
            savedExecution.setEndTime(execEndTime);
            savedExecution.setDurationMs(Duration.between(execStartTime, execEndTime).toMillis());

            // Collect step results
            if (planResult != null) {
                Map<String, TaskExecutionResult> taskResults = planResult.taskResults();
                for (Map.Entry<String, WorkflowStepExecution> entry : stepExecutionMap.entrySet()) {
                    String stepKey = entry.getKey();
                    WorkflowStepExecution stepExec = entry.getValue();
                    TaskExecutionResult taskRes = taskResults != null ? taskResults.get(stepKey) : null;

                    if (taskRes != null) {
                        stepExec.setStatus(mapTaskStatusToWorkflowStatus(taskRes.status()));
                        stepExec.setOutput(taskRes.output());
                        stepExec.setErrorMessage(taskRes.errorMessage());
                    } else {
                        stepExec.setStatus(WorkflowExecutionStatus.FAILED);
                        stepExec.setErrorMessage("Task result was missing after plan execution.");
                    }
                    stepExec.setEndTime(execEndTime);
                }

                if (planResult.status() == ExecutionStatus.COMPLETED) {
                    savedExecution.setStatus(WorkflowExecutionStatus.COMPLETED);
                    savedExecution.setFinalOutput(planResult.finalSynthesis() != null ? planResult.finalSynthesis() : "Workflow executed successfully.");
                } else {
                    savedExecution.setStatus(WorkflowExecutionStatus.FAILED);
                    savedExecution.setErrorMessage(planResult.errorMessage() != null ? planResult.errorMessage() : "One or more workflow steps failed.");
                }
            } else {
                savedExecution.setStatus(WorkflowExecutionStatus.FAILED);
                savedExecution.setErrorMessage("Workflow execution yielded no result.");
            }

        } catch (Exception ex) {
            logger.error("Error executing workflow id: {}", workflowId, ex);
            LocalDateTime execEndTime = LocalDateTime.now();
            savedExecution.setStatus(WorkflowExecutionStatus.FAILED);
            savedExecution.setEndTime(execEndTime);
            savedExecution.setDurationMs(Duration.between(execStartTime, execEndTime).toMillis());
            savedExecution.setErrorMessage("Execution failed: " + ex.getMessage());

            for (WorkflowStepExecution stepExec : savedExecution.getStepExecutions()) {
                if (stepExec.getStatus() == WorkflowExecutionStatus.RUNNING || stepExec.getStatus() == WorkflowExecutionStatus.PENDING) {
                    stepExec.setStatus(WorkflowExecutionStatus.FAILED);
                    stepExec.setEndTime(execEndTime);
                    stepExec.setErrorMessage("Aborted due to workflow error: " + ex.getMessage());
                }
            }
        }

        WorkflowExecution completed = workflowExecutionRepository.save(savedExecution);
        return WorkflowExecutionResponse.fromEntity(completed);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowExecutionResponse getExecutionById(Long executionId, User user) {
        Objects.requireNonNull(executionId, "Execution ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowExecution execution = workflowExecutionRepository.findByIdWithSteps(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow execution not found with ID: " + executionId));

        authorizeWorkspaceAccess(execution.getWorkspace().getId(), user);
        return WorkflowExecutionResponse.fromEntity(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> getExecutionsByWorkflow(Long workflowId, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace().getId(), user);
        List<WorkflowExecution> executions = workflowExecutionRepository.findByWorkflowIdOrderByStartTimeDesc(workflowId);
        return executions.stream().map(WorkflowExecutionResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> getExecutionsByWorkspace(Long workspaceId, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        authorizeWorkspaceAccess(workspaceId, user);
        List<WorkflowExecution> executions = workflowExecutionRepository.findByWorkspaceIdOrderByStartTimeDesc(workspaceId);
        return executions.stream().map(WorkflowExecutionResponse::fromEntity).toList();
    }

    private Workspace authorizeWorkspaceAccess(Long workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner) {
            logger.warn("User {} is unauthorized for workspace {}", user.getUsername(), workspaceId);
            throw new UnauthorizedAccessException("You are not authorized to access workspace ID: " + workspaceId);
        }
        return workspace;
    }

    private AgentTaskType mapStepTypeToAgentTaskType(WorkflowStepType stepType) {
        if (stepType == null) return AgentTaskType.SEARCH;
        return switch (stepType) {
            case SEARCH -> AgentTaskType.SEARCH;
            case ANALYZE -> AgentTaskType.ANALYZE;
            case KNOWLEDGE -> AgentTaskType.KNOWLEDGE;
            case SYNTHESIZE -> AgentTaskType.SYNTHESIZE;
            case NOTIFICATION -> AgentTaskType.NOTIFY;
        };
    }

    private WorkflowExecutionStatus mapTaskStatusToWorkflowStatus(TaskExecutionStatus status) {
        if (status == null) return WorkflowExecutionStatus.FAILED;
        return switch (status) {
            case SUCCESS -> WorkflowExecutionStatus.COMPLETED;
            case FAILED -> WorkflowExecutionStatus.FAILED;
            case SKIPPED -> WorkflowExecutionStatus.CANCELLED;
            case IN_PROGRESS -> WorkflowExecutionStatus.RUNNING;
            case PENDING -> WorkflowExecutionStatus.PENDING;
        };
    }
}
