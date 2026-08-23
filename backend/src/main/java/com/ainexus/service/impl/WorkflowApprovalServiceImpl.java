package com.ainexus.service.impl;

import com.ainexus.dto.*;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowApprovalRepository;
import com.ainexus.repository.WorkflowExecutionRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.PlanExecutionService;
import com.ainexus.service.WorkflowApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class WorkflowApprovalServiceImpl implements WorkflowApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowApprovalServiceImpl.class);

    private final WorkflowApprovalRepository approvalRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PlanExecutionService planExecutionService;

    public WorkflowApprovalServiceImpl(
            WorkflowApprovalRepository approvalRepository,
            WorkflowExecutionRepository executionRepository,
            WorkspaceRepository workspaceRepository,
            PlanExecutionService planExecutionService) {
        this.approvalRepository = approvalRepository;
        this.executionRepository = executionRepository;
        this.workspaceRepository = workspaceRepository;
        this.planExecutionService = planExecutionService;
    }

    @Override
    public WorkflowApprovalResponse requestApproval(WorkflowExecution execution, WorkflowStep step,
                                                    User requestedBy, String reason, LocalDateTime expiresAt) {
        Objects.requireNonNull(execution, "Execution must not be null");
        Objects.requireNonNull(step, "Workflow step must not be null");
        Objects.requireNonNull(requestedBy, "Requested by user must not be null");

        execution.setStatus(WorkflowExecutionStatus.WAITING_FOR_APPROVAL);
        executionRepository.save(execution);

        WorkflowApproval approval = new WorkflowApproval(
                execution,
                execution.getWorkspace(),
                step.getStepKey(),
                step.getName(),
                requestedBy,
                reason != null ? reason : "Human approval required for step: " + step.getName(),
                expiresAt
        );

        WorkflowApproval saved = approvalRepository.save(approval);
        logger.info("[AUDIT] Approval requested id: {} for execution id: {}, step: '{}' by user: {}",
                saved.getId(), execution.getId(), step.getStepKey(), requestedBy.getUsername());

        return WorkflowApprovalResponse.fromEntity(saved);
    }

    @Override
    public WorkflowExecutionResponse approveStep(Long approvalId, WorkflowApprovalDecisionRequest request, User approver) {
        Objects.requireNonNull(approvalId, "Approval ID must not be null");
        Objects.requireNonNull(approver, "Approver must not be null");

        WorkflowApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow approval not found with ID: " + approvalId));

        validatePendingAndNotExpired(approval);
        authorizeApprover(approval.getWorkspace(), approver);

        approval.setStatus(WorkflowApprovalStatus.APPROVED);
        approval.setApprover(approver);
        approval.setResolvedAt(LocalDateTime.now());
        approval.setResolutionComment(request != null ? request.comment() : null);
        approvalRepository.save(approval);

        logger.info("[AUDIT] Approval id: {} APPROVED for execution id: {} by user: {}",
                approval.getId(), approval.getExecution().getId(), approver.getUsername());

        // Resume workflow execution
        WorkflowExecution execution = approval.getExecution();
        execution.setStatus(WorkflowExecutionStatus.RUNNING);

        // Resume remaining steps via plan execution
        AgentTask resumeTask = new AgentTask(
                approval.getStepKey(),
                AgentTaskType.SYNTHESIZE,
                approval.getStepName(),
                List.of(),
                AgentTaskStatus.PENDING
        );

        AgentPlan resumePlan = new AgentPlan(
                "wf-resume-plan-" + execution.getId(),
                "Approved execution resume",
                execution.getWorkspace().getId(),
                List.of(resumeTask)
        );

        try {
            AgentExecutionResult planResult = planExecutionService.executePlan(resumePlan, approver);
            if (planResult != null && planResult.status() == PlanExecutionStatus.COMPLETED) {
                execution.setStatus(WorkflowExecutionStatus.COMPLETED);
                execution.setFinalOutput(planResult.finalOutput() != null ? planResult.finalOutput() : "Workflow completed after approval.");
            } else {
                execution.setStatus(WorkflowExecutionStatus.FAILED);
                execution.setErrorMessage("Execution failed during post-approval task resumption.");
            }
        } catch (Exception ex) {
            logger.error("Error resuming workflow execution id: {}", execution.getId(), ex);
            execution.setStatus(WorkflowExecutionStatus.FAILED);
            execution.setErrorMessage("Resume failed: " + ex.getMessage());
        }

        execution.setEndTime(LocalDateTime.now());
        if (execution.getStartTime() != null) {
            execution.setDurationMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        }

        WorkflowExecution saved = executionRepository.save(execution);
        return WorkflowExecutionResponse.fromEntity(saved);
    }

    @Override
    public WorkflowExecutionResponse rejectStep(Long approvalId, WorkflowApprovalDecisionRequest request, User approver) {
        Objects.requireNonNull(approvalId, "Approval ID must not be null");
        Objects.requireNonNull(approver, "Approver must not be null");

        WorkflowApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow approval not found with ID: " + approvalId));

        validatePendingAndNotExpired(approval);
        authorizeApprover(approval.getWorkspace(), approver);

        approval.setStatus(WorkflowApprovalStatus.REJECTED);
        approval.setApprover(approver);
        approval.setResolvedAt(LocalDateTime.now());
        approval.setResolutionComment(request != null ? request.comment() : "Step rejected by approver");
        approvalRepository.save(approval);

        logger.info("[AUDIT] Approval id: {} REJECTED for execution id: {} by user: {}",
                approval.getId(), approval.getExecution().getId(), approver.getUsername());

        // Safely stop workflow execution
        WorkflowExecution execution = approval.getExecution();
        execution.setStatus(WorkflowExecutionStatus.CANCELLED);
        execution.setErrorMessage("Workflow stopped safely due to approval rejection: " + approval.getResolutionComment());
        execution.setEndTime(LocalDateTime.now());
        if (execution.getStartTime() != null) {
            execution.setDurationMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        }

        WorkflowExecution saved = executionRepository.save(execution);
        return WorkflowExecutionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowApprovalResponse getApprovalById(Long approvalId, User user) {
        Objects.requireNonNull(approvalId, "Approval ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow approval not found with ID: " + approvalId));

        authorizeWorkspaceAccess(approval.getWorkspace(), user);
        return WorkflowApprovalResponse.fromEntity(approval);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowApprovalResponse> getPendingApprovalsByWorkspace(Long workspaceId, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        authorizeWorkspaceAccess(workspace, user);
        List<WorkflowApproval> approvals = approvalRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(
                workspaceId, WorkflowApprovalStatus.PENDING);
        return approvals.stream().map(WorkflowApprovalResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowApprovalResponse> getApprovalsByExecution(Long executionId, User user) {
        Objects.requireNonNull(executionId, "Execution ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow execution not found with ID: " + executionId));

        authorizeWorkspaceAccess(execution.getWorkspace(), user);
        List<WorkflowApproval> approvals = approvalRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);
        return approvals.stream().map(WorkflowApprovalResponse::fromEntity).toList();
    }

    @Override
    public void processExpiredApprovals() {
        List<WorkflowApproval> expiredList = approvalRepository.findExpiredApprovals(LocalDateTime.now());
        for (WorkflowApproval approval : expiredList) {
            approval.setStatus(WorkflowApprovalStatus.EXPIRED);
            approval.setResolvedAt(LocalDateTime.now());
            approval.setResolutionComment("Approval gate expired automatically.");
            approvalRepository.save(approval);

            WorkflowExecution execution = approval.getExecution();
            if (execution.getStatus() == WorkflowExecutionStatus.WAITING_FOR_APPROVAL) {
                execution.setStatus(WorkflowExecutionStatus.CANCELLED);
                execution.setErrorMessage("Workflow cancelled due to expired approval request.");
                execution.setEndTime(LocalDateTime.now());
                executionRepository.save(execution);
            }

            logger.info("[AUDIT] Approval id: {} marked EXPIRED for execution id: {}",
                    approval.getId(), execution.getId());
        }
    }

    private void validatePendingAndNotExpired(WorkflowApproval approval) {
        if (approval.getStatus() != WorkflowApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval is already resolved with status: " + approval.getStatus());
        }
        if (approval.getExpiresAt() != null && approval.getExpiresAt().isBefore(LocalDateTime.now())) {
            approval.setStatus(WorkflowApprovalStatus.EXPIRED);
            approval.setResolvedAt(LocalDateTime.now());
            approvalRepository.save(approval);
            throw new IllegalStateException("Approval request has expired.");
        }
    }

    private void authorizeApprover(Workspace workspace, User user) {
        authorizeWorkspaceAccess(workspace, user);
        boolean isOwnerOrAdmin = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwnerOrAdmin) {
            logger.warn("[SECURITY] User {} is not authorized to approve in workspace {}", user.getUsername(), workspace.getId());
            throw new UnauthorizedAccessException("Only workspace owners or administrators can grant approvals.");
        }
    }

    private void  authorizeWorkspaceAccess(Workspace workspace, User user) {
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner) {
            logger.warn("[SECURITY] Workspace isolation: User {} denied access to workspace {}", user.getUsername(), workspace.getId());
            throw new UnauthorizedAccessException("You are not authorized to access workspace ID: " + workspace.getId());
        }
    }
}
