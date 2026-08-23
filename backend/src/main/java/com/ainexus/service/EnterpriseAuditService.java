package com.ainexus.service;

import com.ainexus.dto.AuditLogResponse;
import com.ainexus.dto.AuditLogSearchRequest;
import com.ainexus.entity.AuditActionType;
import com.ainexus.entity.EnterpriseAuditLog;
import com.ainexus.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnterpriseAuditService {

    EnterpriseAuditLog logEvent(
            AuditActionType actionType,
            String actorUsername,
            Long workspaceId,
            String resourceType,
            String resourceId,
            String result,
            String details
    );

    Page<AuditLogResponse> searchAuditLogs(AuditLogSearchRequest filter, Pageable pageable, User requester);

    AuditLogResponse getAuditLogDetails(Long auditId, User requester);
}
