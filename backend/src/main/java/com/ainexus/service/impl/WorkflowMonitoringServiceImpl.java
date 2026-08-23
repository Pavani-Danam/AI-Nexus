package com.ainexus.service.impl;

import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.dto.WorkflowMonitoringSummaryResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import com.ainexus.service.WorkflowMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class WorkflowMonitoringServiceImpl implements WorkflowMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowMonitoringServiceImpl.class);

    private final WorkflowAuditEventRepository auditRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowApprovalRepository approvalRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkflowMonitoringServiceImpl(
            WorkflowAuditEventRepository auditRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowApprovalRepository approvalRepository,
            WorkspaceRepository workspaceRepository) {
        this.auditRepository = auditRepository;
        this.executionRepository = executionRepository;
        this.approvalRepository = approvalRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void recordAuditEvent(WorkflowAuditEventType eventType, Long workflowId, Long workspaceId,
                                  Long executionId, String actorUsername, String description) {
        if (workspaceId == null || eventType == null) {
            return;
        }
        WorkflowAuditEvent audit = new WorkflowAuditEvent(
                eventType, workflowId, workspaceId, executionId, actorUsername, description
        );
        auditRepository.save(audit);
        logger.info("[AUDIT] Event: {} | Workspace: {} | Workflow: {} | Execution: {} | Actor: {}",
                eventType, workspaceId, workflowId, executionId, actorUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> getExecutionHistory(Long workspaceId, Long workflowId,
                                                               WorkflowExecutionStatus status,
                                                               Pageable pageable, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));
        authorizeWorkspaceAccess(workspace, user);

        List<WorkflowExecution> list = executionRepository.findByWorkspaceIdOrderByStartTimeDesc(workspaceId);

        List<WorkflowExecutionResponse> filtered = list.stream()
                .filter(e -> workflowId == null || (e.getWorkflow() != null && e.getWorkflow().getId().equals(workflowId)))
                .filter(e -> status == null || e.getStatus() == status)
                .map(WorkflowExecutionResponse::fromEntity)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<WorkflowExecutionResponse> pageContent = (start <= end && start < filtered.size())
                ? filtered.subList(start, end)
                : List.of();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowExecutionResponse getExecutionDetails(Long executionId, User user) {
        Objects.requireNonNull(executionId, "Execution ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow execution not found with ID: " + executionId));
        authorizeWorkspaceAccess(execution.getWorkspace(), user);

        return WorkflowExecutionResponse.fromEntity(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAuditEvent> getAuditEventsByExecution(Long executionId, User user) {
        Objects.requireNonNull(executionId, "Execution ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow execution not found with ID: " + executionId));
        authorizeWorkspaceAccess(execution.getWorkspace(), user);

        return auditRepository.findByExecutionIdOrderByTimestampAsc(executionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowAuditEvent> getAuditEventsByWorkspace(Long workspaceId, Pageable pageable, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));
        authorizeWorkspaceAccess(workspace, user);

        return auditRepository.findByWorkspaceIdOrderByTimestampDesc(workspaceId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowMonitoringSummaryResponse getWorkspaceMonitoringSummary(Long workspaceId, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));
        authorizeWorkspaceAccess(workspace, user);

        List<WorkflowExecution> executions = executionRepository.findByWorkspaceIdOrderByStartTimeDesc(workspaceId);
        long total = executions.size();
        long successful = executions.stream().filter(e -> e.getStatus() == WorkflowExecutionStatus.COMPLETED).count();
        long failed = executions.stream().filter(e -> e.getStatus() == WorkflowExecutionStatus.FAILED).count();

        double avgDuration = executions.stream()
                .filter(e -> e.getDurationMs() != null)
                .mapToLong(WorkflowExecution::getDurationMs)
                .average()
                .orElse(0.0);

        List<WorkflowApproval> pendingApprovals = approvalRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .filter(a -> a.getStatus() == WorkflowApprovalStatus.PENDING)
                .toList();

        List<WorkflowExecutionResponse> recent = executions.stream()
                .limit(10)
                .map(WorkflowExecutionResponse::fromEntity)
                .toList();

        return new WorkflowMonitoringSummaryResponse(
                total,
                successful,
                failed,
                pendingApprovals.size(),
                avgDuration,
                recent
        );
    }

    private void authorizeWorkspaceAccess(Workspace workspace, User user) {
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner) {
            logger.warn("[SECURITY] Monitoring access denied for user {} in workspace {}", user.getUsername(), workspace.getId());
            throw new UnauthorizedAccessException("You are not authorized to view monitoring metrics for workspace ID: " + workspace.getId());
        }
    }
}
