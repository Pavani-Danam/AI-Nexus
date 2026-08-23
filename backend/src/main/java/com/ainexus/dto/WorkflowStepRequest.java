package com.ainexus.dto;

import com.ainexus.entity.WorkflowStepType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WorkflowStepRequest(
        @NotBlank(message = "Step key is required")
        String stepKey,

        @NotBlank(message = "Step name is required")
        String name,

        @NotNull(message = "Step type is required")
        WorkflowStepType type,

        String configuration,

        Integer executionOrder,

        List<String> dependencies,

        Boolean enabled
) {}
