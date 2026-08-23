package com.ainexus.dto;

import com.ainexus.entity.ScheduleType;
import com.ainexus.entity.WorkflowSchedule;
import java.time.LocalDateTime;

public record WorkflowScheduleResponse(
        Long id,
        Long workflowId,
        String workflowName,
        Long workspaceId,
        ScheduleType scheduleType,
        String cronExpression,
        Long intervalSeconds,
        String timezone,
        boolean enabled,
        LocalDateTime nextExecutionAt,
        LocalDateTime lastExecutionAt,
        String inputQuery,
        Long createdById,
        String createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorkflowScheduleResponse fromEntity(WorkflowSchedule schedule) {
        if (schedule == null) return null;
        return new WorkflowScheduleResponse(
                schedule.getId(),
                schedule.getWorkflow() != null ? schedule.getWorkflow().getId() : null,
                schedule.getWorkflow() != null ? schedule.getWorkflow().getName() : null,
                schedule.getWorkspace() != null ? schedule.getWorkspace().getId() : null,
                schedule.getScheduleType(),
                schedule.getCronExpression(),
                schedule.getIntervalSeconds(),
                schedule.getTimezone(),
                schedule.isEnabled(),
                schedule.getNextExecutionAt(),
                schedule.getLastExecutionAt(),
                schedule.getInputQuery(),
                schedule.getCreatedBy() != null ? schedule.getCreatedBy().getId() : null,
                schedule.getCreatedBy() != null ? schedule.getCreatedBy().getUsername() : null,
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
