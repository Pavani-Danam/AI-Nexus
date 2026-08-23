package com.ainexus.dto;

import com.ainexus.entity.AuditActionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record AuditLogSearchRequest(
        Long workspaceId,
        AuditActionType actionType,
        String actorUsername,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate
) {
}
