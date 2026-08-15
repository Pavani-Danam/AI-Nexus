package com.ainexus.service;

import com.ainexus.entity.AuditLog;
import com.ainexus.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog logAction(Long userId, String action, String entityName, Long entityId, String details) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .build();
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }
}
