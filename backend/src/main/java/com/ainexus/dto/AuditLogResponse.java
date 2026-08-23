package com.ainexus.dto;

import com.ainexus.entity.AuditActionType;
import com.ainexus.entity.EnterpriseAuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        AuditActionType actionType,
        String actorUsername,
        Long workspaceId,
        String resourceType,
        String resourceId,
        String result,
        String safeDetails,
        LocalDateTime createdAt
) {
    public static AuditLogResponse fromEntity(EnterpriseAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActionType(),
                log.getActorUsername(),
                log.getWorkspaceId(),
                log.getResourceType(),
                log.getResourceId(),
                log.getResult(),
                log.getSafeDetails(),
                log.getCreatedAt()
        );
    }
}
