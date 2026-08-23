package com.ainexus.repository;

import com.ainexus.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    @Query("SELECT DISTINCT we FROM WorkflowExecution we LEFT JOIN FETCH we.stepExecutions WHERE we.id = :id")
    Optional<WorkflowExecution> findByIdWithSteps(@Param("id") Long id);

    @Query("SELECT DISTINCT we FROM WorkflowExecution we LEFT JOIN FETCH we.stepExecutions WHERE we.workflow.id = :workflowId ORDER BY we.startTime DESC")
    List<WorkflowExecution> findByWorkflowIdOrderByStartTimeDesc(@Param("workflowId") Long workflowId);

    @Query("SELECT DISTINCT we FROM WorkflowExecution we LEFT JOIN FETCH we.stepExecutions WHERE we.workspace.id = :workspaceId ORDER BY we.startTime DESC")
    List<WorkflowExecution> findByWorkspaceIdOrderByStartTimeDesc(@Param("workspaceId") Long workspaceId);
}
