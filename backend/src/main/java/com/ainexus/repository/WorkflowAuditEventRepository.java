package com.ainexus.repository;

import com.ainexus.entity.WorkflowAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowAuditEventRepository extends JpaRepository<WorkflowAuditEvent, Long> {

    Page<WorkflowAuditEvent> findByWorkspaceIdOrderByTimestampDesc(Long workspaceId, Pageable pageable);

    List<WorkflowAuditEvent> findByExecutionIdOrderByTimestampAsc(Long executionId);

    Page<WorkflowAuditEvent> findByWorkflowIdOrderByTimestampDesc(Long workflowId, Pageable pageable);
}
