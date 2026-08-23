package com.ainexus.dto;

import com.ainexus.entity.WorkflowExecutionStatus;
import com.ainexus.entity.WorkflowStepExecution;
import com.ainexus.entity.WorkflowStepType;

import java.time.LocalDateTime;

public record WorkflowStepExecutionResponse(
        Long id,
        String stepKey,
        String stepName,
        WorkflowStepType stepType,
        WorkflowExecutionStatus status,
        String output,
        String errorMessage,
        Integer executionOrder,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static WorkflowStepExecutionResponse fromEntity(WorkflowStepExecution stepExec) {
        if (stepExec == null) return null;
        return new WorkflowStepExecutionResponse(
                stepExec.getId(),
                stepExec.getStepKey(),
                stepExec.getStepName(),
                stepExec.getStepType(),
                stepExec.getStatus(),
                stepExec.getOutput(),
                stepExec.getErrorMessage(),
                stepExec.getExecutionOrder(),
                stepExec.getStartTime(),
                stepExec.getEndTime()
        );
    }
}
