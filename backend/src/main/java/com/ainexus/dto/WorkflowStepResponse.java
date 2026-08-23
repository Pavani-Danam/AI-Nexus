package com.ainexus.dto;

import com.ainexus.entity.WorkflowStep;
import com.ainexus.entity.WorkflowStepType;
import java.util.List;

public record WorkflowStepResponse(
        Long id,
        String stepKey,
        String name,
        WorkflowStepType type,
        String configuration,
        Integer executionOrder,
        List<String> dependencies,
        boolean enabled
) {
    public static WorkflowStepResponse fromEntity(WorkflowStep step) {
        if (step == null) return null;
        return new WorkflowStepResponse(
                step.getId(),
                step.getStepKey(),
                step.getName(),
                step.getType(),
                step.getConfiguration(),
                step.getExecutionOrder(),
                step.getDependencies() != null ? List.copyOf(step.getDependencies()) : List.of(),
                step.isEnabled()
        );
    }
}
