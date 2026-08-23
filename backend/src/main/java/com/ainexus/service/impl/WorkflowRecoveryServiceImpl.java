package com.ainexus.service.impl;

import com.ainexus.dto.*;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowApprovalRepository;
import com.ainexus.repository.WorkflowExecutionRepository;
import com.ainexus.service.AgentReplanningService;
import com.ainexus.service.PlanExecutionService;
import com.ainexus.service.WorkflowRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowRecoveryServiceImpl implements WorkflowRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRecoveryServiceImpl.class);

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowApprovalRepository approvalRepository;
    private final PlanExecutionService planExecutionService;
    private final AgentReplanningService replanningService;

    public WorkflowRecoveryServiceImpl(
            WorkflowExecutionRepository executionRepository,
            WorkflowApprovalRepository approvalRepository,
            PlanExecutionService planExecutionService,
            AgentReplanningService replanningService) {
        this.executionRepository = executionRepository;
        this.approvalRepository = approvalRepository;
        this.planExecutionService = planExecutionService;
        this.replanningService = replanningService;
    }

    @Override
    public WorkflowExecutionResponse recoverExecution(Long executionId, User user) {
        Objects.requireNonNull(executionId, "Execution ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow execution not found with ID: " + executionId));

        authorizeWorkspaceAccess(execution.getWorkspace(), user);

        // Check if waiting for human approval - NEVER bypass pending approval
        List<WorkflowApproval> pendingApprovals = approvalRepository.findByExecutionIdOrderByCreatedAtAsc(executionId)
                .stream()
                .filter(a -> a.getStatus() == WorkflowApprovalStatus.PENDING)
                .toList();

        if (!pendingApprovals.isEmpty()) {
            logger.warn("Cannot auto-recover execution id: {} because it is waiting for human approval.", executionId);
            throw new IllegalStateException("Execution has pending human approvals and cannot be auto-recovered without approval.");
        }

        if (execution.getStatus() != WorkflowExecutionStatus.FAILED) {
            throw new IllegalStateException("Only FAILED workflow executions can be recovered. Current status: " + execution.getStatus());
        }

        // Classify failure
        String errorMsg = execution.getErrorMessage() != null ? execution.getErrorMessage() : "Unknown error";
        WorkflowFailureType failureType = classifyFailure(new RuntimeException(errorMsg));

        if (!failureType.isRecoverable()) {
            logger.warn("[SECURITY] Execution id: {} failed with non-recoverable error: {}. Recovery aborted.", executionId, failureType);
            throw new IllegalStateException("Cannot recover non-recoverable failure type: " + failureType + " (" + errorMsg + ")");
        }

        logger.info("Attempting recovery for execution id: {} (Classified as: {})", executionId, failureType);

        // Fetch completed steps to enforce IDEMPOTENCY
        List<WorkflowStepExecution> existingStepExecs = execution.getStepExecutions() != null
                ? execution.getStepExecutions()
                : List.of();

        Set<String> completedStepKeys = existingStepExecs.stream()
                .filter(se -> se.getStatus() == WorkflowExecutionStatus.COMPLETED)
                .map(WorkflowStepExecution::getStepKey)
                .collect(Collectors.toSet());

        Workflow workflow = execution.getWorkflow();
        List<WorkflowStep> remainingSteps = (workflow.getSteps() != null ? workflow.getSteps() : List.<WorkflowStep>of()).stream()
                .filter(s -> !completedStepKeys.contains(s.getStepKey()))
                .sorted(Comparator.comparingInt(s -> s.getExecutionOrder() != null ? s.getExecutionOrder() : 0))
                .toList();

        if (remainingSteps.isEmpty()) {
            execution.setStatus(WorkflowExecutionStatus.COMPLETED);
            execution.setErrorMessage(null);
            execution.setEndTime(LocalDateTime.now());
            WorkflowExecution saved = executionRepository.save(execution);
            return WorkflowExecutionResponse.fromEntity(saved);
        }

        // Create recovery tasks for uncompleted steps only
        List<AgentTask> recoveryTasks = remainingSteps.stream()
                .map(s -> new AgentTask(
                        s.getStepKey(),
                        mapStepTypeToAgentTaskType(s.getType()),
                        s.getName(),
                        s.getDependencies() != null ? s.getDependencies() : List.of(),
                        AgentTaskStatus.PENDING
                ))
                .toList();

        AgentPlan recoveryPlan = new AgentPlan(
                "recovery-plan-" + execution.getId(),
                "Recovered workflow plan for execution " + execution.getId(),
                execution.getWorkspace().getId(),
                recoveryTasks
        );

        // Trigger execution of the remaining idempotent tasks
        execution.setStatus(WorkflowExecutionStatus.RUNNING);
        execution.setErrorMessage(null);
        executionRepository.save(execution);

        try {
            AgentExecutionResult result = planExecutionService.executePlan(recoveryPlan, user);
            if (result != null && result.status() == PlanExecutionStatus.COMPLETED) {
                execution.setStatus(WorkflowExecutionStatus.COMPLETED);
                execution.setFinalOutput(result.finalOutput() != null ? result.finalOutput() : "Recovered and completed successfully.");
            } else {
                // Replan attempt on partial execution failure
                ConversationMemory memory = new ConversationMemory(execution.getId(), execution.getWorkspace().getId(), List.of(), "Recovery Memory", 0);
                AgentPlan replanned = replanningService.generateCorrectedPlan(recoveryPlan, result, 1, memory, user);
                AgentExecutionResult replannedResult = planExecutionService.executePlan(replanned, user);

                if (replannedResult != null && replannedResult.status() == PlanExecutionStatus.COMPLETED) {
                    execution.setStatus(WorkflowExecutionStatus.COMPLETED);
                    execution.setFinalOutput(replannedResult.finalOutput());
                } else {
                    execution.setStatus(WorkflowExecutionStatus.FAILED);
                    execution.setErrorMessage("Replanning recovery attempt also failed.");
                }
            }
        } catch (Exception ex) {
            logger.error("Exception during recovery execution for id: {}", executionId, ex);
            execution.setStatus(WorkflowExecutionStatus.FAILED);
            execution.setErrorMessage("Recovery execution failed: " + ex.getMessage());
        }

        execution.setEndTime(LocalDateTime.now());
        if (execution.getStartTime() != null) {
            execution.setDurationMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        }

        WorkflowExecution saved = executionRepository.save(execution);
        return WorkflowExecutionResponse.fromEntity(saved);
    }

    @Override
    public WorkflowFailureType classifyFailure(Throwable throwable) {
        return WorkflowFailureType.classify(throwable);
    }

    @Override
    public boolean isRecoverable(WorkflowFailureType failureType) {
        return failureType != null && failureType.isRecoverable();
    }

    private AgentTaskType mapStepTypeToAgentTaskType(WorkflowStepType stepType) {
        if (stepType == null) return AgentTaskType.SYNTHESIZE;
        return switch (stepType) {
            case SEARCH -> AgentTaskType.SEARCH;
            case ANALYZE -> AgentTaskType.ANALYZE;
            case KNOWLEDGE -> AgentTaskType.KNOWLEDGE;
            case SYNTHESIZE, NOTIFICATION -> AgentTaskType.SYNTHESIZE;
        };
    }

    private void authorizeWorkspaceAccess(Workspace workspace, User user) {
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner) {
            logger.warn("[SECURITY] Recovery denied. User {} unauthorized for workspace {}", user.getUsername(), workspace.getId());
            throw new UnauthorizedAccessException("You are not authorized to recover executions in workspace ID: " + workspace.getId());
        }
    }
}
