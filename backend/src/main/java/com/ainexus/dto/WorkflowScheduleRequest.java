package com.ainexus.dto;

import com.ainexus.entity.ScheduleType;
import java.time.LocalDateTime;

public record WorkflowScheduleRequest(
        ScheduleType scheduleType,
        String cronExpression,
        Long intervalSeconds,
        String timezone,
        String inputQuery,
        LocalDateTime oneTimeExecutionAt
) {
}
