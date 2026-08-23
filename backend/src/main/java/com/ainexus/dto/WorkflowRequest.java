package com.ainexus.dto;

import com.ainexus.entity.WorkflowStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WorkflowRequest(
        @NotBlank(message = "Workflow name is required")
        @Size(max = 120, message = "Workflow name cannot exceed 120 characters")
        String name,

        @Size(max = 1000, message = "Workflow description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Workspace ID is required")
        Long workspaceId,

        WorkflowStatus status,

        @Valid
        List<WorkflowStepRequest> steps
) {}
