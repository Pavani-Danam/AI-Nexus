package com.ainexus.repository;

import com.ainexus.entity.WorkflowApproval;
import com.ainexus.entity.WorkflowApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkflowApprovalRepository extends JpaRepository<WorkflowApproval, Long> {

    List<WorkflowApproval> findByExecutionIdOrderByCreatedAtAsc(Long executionId);

    List<WorkflowApproval> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<WorkflowApproval> findByWorkspaceIdAndStatus(Long workspaceId, WorkflowApprovalStatus status);

    List<WorkflowApproval> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, WorkflowApprovalStatus status);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.status = 'PENDING' AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    List<WorkflowApproval> findExpiredApprovals(@Param("now") LocalDateTime now);
}
