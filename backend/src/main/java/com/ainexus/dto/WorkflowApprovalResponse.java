package com.ainexus.dto;

import com.ainexus.entity.WorkflowApproval;
import com.ainexus.entity.WorkflowApprovalStatus;

import java.time.LocalDateTime;

public record WorkflowApprovalResponse(
        Long id,
        Long executionId,
        Long workspaceId,
        String stepKey,
        String stepName,
        Long requestedById,
        String requestedByUsername,
        Long approverId,
        String approverUsername,
        WorkflowApprovalStatus status,
        String reason,
        String resolutionComment,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        LocalDateTime expiresAt
) {
    public static WorkflowApprovalResponse fromEntity(WorkflowApproval approval) {
        if (approval == null) return null;
        return new WorkflowApprovalResponse(
                approval.getId(),
                approval.getExecution() != null ? approval.getExecution().getId() : null,
                approval.getWorkspace() != null ? approval.getWorkspace().getId() : null,
                approval.getStepKey(),
                approval.getStepName(),
                approval.getRequestedBy() != null ? approval.getRequestedBy().getId() : null,
                approval.getRequestedBy() != null ? approval.getRequestedBy().getUsername() : null,
                approval.getApprover() != null ? approval.getApprover().getId() : null,
                approval.getApprover() != null ? approval.getApprover().getUsername() : null,
                approval.getStatus(),
                approval.getReason(),
                approval.getResolutionComment(),
                approval.getCreatedAt(),
                approval.getResolvedAt(),
                approval.getExpiresAt()
        );
    }
}
