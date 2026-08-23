package com.ainexus.repository;

import com.ainexus.entity.WorkflowSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkflowScheduleRepository extends JpaRepository<WorkflowSchedule, Long> {

    List<WorkflowSchedule> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<WorkflowSchedule> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    @Query("SELECT s FROM WorkflowSchedule s WHERE s.enabled = true AND s.nextExecutionAt IS NOT NULL AND s.nextExecutionAt <= :now")
    List<WorkflowSchedule> findDueSchedules(@Param("now") LocalDateTime now);
}
