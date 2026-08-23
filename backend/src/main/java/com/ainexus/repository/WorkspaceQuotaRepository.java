package com.ainexus.repository;

import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceQuotaRepository extends JpaRepository<WorkspaceQuota, Long> {

    Optional<WorkspaceQuota> findByWorkspace(Workspace workspace);

    Optional<WorkspaceQuota> findByWorkspace_Id(Long workspaceId);

    boolean existsByWorkspace_Id(Long workspaceId);
}
