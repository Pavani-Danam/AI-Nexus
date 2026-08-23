package com.ainexus.dto;

import com.ainexus.entity.Workflow;
import com.ainexus.entity.WorkflowStatus;
import java.time.LocalDateTime;
import java.util.List;

public record WorkflowResponse(
        Long id,
        String name,
        String description,
        Integer version,
        WorkflowStatus status,
        Long workspaceId,
        String workspaceName,
        Long createdById,
        String createdByName,
        List<WorkflowStepResponse> steps,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorkflowResponse fromEntity(Workflow workflow) {
        if (workflow == null) return null;
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getVersion(),
                workflow.getStatus(),
                workflow.getWorkspace() != null ? workflow.getWorkspace().getId() : null,
                workflow.getWorkspace() != null ? workflow.getWorkspace().getName() : null,
                workflow.getCreatedBy() != null ? workflow.getCreatedBy().getId() : null,
                workflow.getCreatedBy() != null ? workflow.getCreatedBy().getUsername() : null,
                workflow.getSteps() != null ? workflow.getSteps().stream().map(WorkflowStepResponse::fromEntity).toList() : List.of(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }
}
