package com.ainexus.repository;

import com.ainexus.entity.WorkflowApproval;
import com.ainexus.entity.WorkflowApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowApprovalRepository extends JpaRepository<WorkflowApproval, Long> {

    List<WorkflowApproval> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, WorkflowApprovalStatus status);

    List<WorkflowApproval> findByExecutionIdOrderByCreatedAtAsc(Long executionId);

    Optional<WorkflowApproval> findByExecutionIdAndStepKeyAndStatus(Long executionId, String stepKey, WorkflowApprovalStatus status);

    @Query("SELECT a FROM WorkflowApproval a WHERE a.status = 'PENDING' AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    List<WorkflowApproval> findExpiredApprovals(@Param("now") LocalDateTime now);
}
