package com.ainexus.service.impl;

import com.ainexus.dto.AuditLogResponse;
import com.ainexus.dto.AuditLogSearchRequest;
import com.ainexus.entity.AuditActionType;
import com.ainexus.entity.EnterpriseAuditLog;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.EnterpriseAuditLogRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.service.EnterpriseAuditService;
import com.ainexus.util.AuditDataSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class EnterpriseAuditServiceImpl implements EnterpriseAuditService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseAuditServiceImpl.class);

    private final EnterpriseAuditLogRepository auditLogRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public EnterpriseAuditServiceImpl(
            EnterpriseAuditLogRepository auditLogRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.auditLogRepository = auditLogRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) return false;
        return user.getRole().name().contains("ADMIN");
    }

    @Override
    public EnterpriseAuditLog logEvent(
            AuditActionType actionType,
            String actorUsername,
            Long workspaceId,
            String resourceType,
            String resourceId,
            String result,
            String details) {

        String safeDetails = AuditDataSanitizer.sanitize(details);

        EnterpriseAuditLog auditLog = EnterpriseAuditLog.builder()
                .actionType(actionType != null ? actionType : AuditActionType.SECURITY_ALERT)
                .actorUsername(actorUsername != null && !actorUsername.isBlank() ? actorUsername : "system")
                .workspaceId(workspaceId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .result(result != null ? result : "SUCCESS")
                .safeDetails(safeDetails)
                .build();

        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(AuditLogSearchRequest filter, Pageable pageable, User requester) {
        Objects.requireNonNull(requester, "Requester must not be null");

        boolean userIsAdmin = isAdmin(requester);

        Long effectiveWorkspaceId = filter != null ? filter.workspaceId() : null;

        if (!userIsAdmin) {
            if (effectiveWorkspaceId == null) {
                logger.warn("[SECURITY] Non-admin user '{}' attempted global audit log search.", requester.getUsername());
                throw new UnauthorizedAccessException("Forbidden: Global audit log inspection requires administrator privileges.");
            }
            boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(effectiveWorkspaceId, requester.getId());
            if (!isMember) {
                logger.warn("[SECURITY] User '{}' attempted audit log access for unauthorized workspace ID {}", requester.getUsername(), effectiveWorkspaceId);
                throw new UnauthorizedAccessException("Forbidden: You are not authorized to view audit logs for workspace ID " + effectiveWorkspaceId);
            }
        }

        AuditActionType actionType = filter != null ? filter.actionType() : null;
        String actorUsername = filter != null ? filter.actorUsername() : null;
        var startDate = filter != null ? filter.startDate() : null;
        var endDate = filter != null ? filter.endDate() : null;

        Page<EnterpriseAuditLog> pageResult = auditLogRepository.searchAuditLogs(
                effectiveWorkspaceId,
                actionType,
                actorUsername,
                startDate,
                endDate,
                pageable
        );

        return pageResult.map(AuditLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogDetails(Long auditId, User requester) {
        Objects.requireNonNull(requester, "Requester must not be null");

        EnterpriseAuditLog log = auditLogRepository.findById(auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit record not found with ID: " + auditId));

        if (!isAdmin(requester)) {
            if (log.getWorkspaceId() == null) {
                throw new UnauthorizedAccessException("Forbidden: Administrator privileges required to view system-level audit events.");
            }
            boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(log.getWorkspaceId(), requester.getId());
            if (!isMember) {
                throw new UnauthorizedAccessException("Forbidden: Cross-workspace audit event access denied.");
            }
        }

        return AuditLogResponse.fromEntity(log);
    }
}
