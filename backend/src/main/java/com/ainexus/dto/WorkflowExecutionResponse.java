package com.ainexus.dto;

import com.ainexus.entity.WorkflowExecution;
import com.ainexus.entity.WorkflowExecutionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowExecutionResponse(
        Long id,
        Long workflowId,
        String workflowName,
        Long workspaceId,
        Long triggeredById,
        String triggeredByName,
        WorkflowExecutionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs,
        String finalOutput,
        String errorMessage,
        List<WorkflowStepExecutionResponse> stepExecutions
) {
    public static WorkflowExecutionResponse fromEntity(WorkflowExecution execution) {
        if (execution == null) return null;
        return new WorkflowExecutionResponse(
                execution.getId(),
                execution.getWorkflow() != null ? execution.getWorkflow().getId() : null,
                execution.getWorkflow() != null ? execution.getWorkflow().getName() : null,
                execution.getWorkspace() != null ? execution.getWorkspace().getId() : null,
                execution.getTriggeredBy() != null ? execution.getTriggeredBy().getId() : null,
                execution.getTriggeredBy() != null ? execution.getTriggeredBy().getUsername() : null,
                execution.getStatus(),
                execution.getStartTime(),
                execution.getEndTime(),
                execution.getDurationMs(),
                execution.getFinalOutput(),
                execution.getErrorMessage(),
                execution.getStepExecutions() != null
                        ? execution.getStepExecutions().stream().map(WorkflowStepExecutionResponse::fromEntity).toList()
                        : List.of()
        );
    }
}
