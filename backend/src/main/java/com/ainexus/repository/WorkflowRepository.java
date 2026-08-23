package com.ainexus.repository;

import com.ainexus.entity.Workflow;
import com.ainexus.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    List<Workflow> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<Workflow> findByWorkspaceIdAndStatus(Long workspaceId, WorkflowStatus status);

    boolean existsByWorkspaceIdAndNameIgnoreCase(Long workspaceId, String name);

    @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.steps WHERE w.id = :id")
    Optional<Workflow> findByIdWithSteps(@Param("id") Long id);

    @Query("SELECT w FROM Workflow w WHERE w.workspace.id = :workspaceId AND w.workspace.owner.id = :userId")
    List<Workflow> findAllByWorkspaceIdAndOwnerId(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);
}
