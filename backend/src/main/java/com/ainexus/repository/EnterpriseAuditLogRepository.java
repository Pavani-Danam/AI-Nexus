package com.ainexus.repository;

import com.ainexus.entity.AuditActionType;
import com.ainexus.entity.EnterpriseAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EnterpriseAuditLogRepository extends JpaRepository<EnterpriseAuditLog, Long> {

    @Query("SELECT a FROM EnterpriseAuditLog a WHERE " +
           "(:workspaceId IS NULL OR a.workspaceId = :workspaceId) AND " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:actorUsername IS NULL OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :actorUsername, '%'))) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<EnterpriseAuditLog> searchAuditLogs(
            @Param("workspaceId") Long workspaceId,
            @Param("actionType") AuditActionType actionType,
            @Param("actorUsername") String actorUsername,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
