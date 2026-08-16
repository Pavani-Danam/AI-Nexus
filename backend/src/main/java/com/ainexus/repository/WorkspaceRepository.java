package com.ainexus.repository;

import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.owner = :owner")
    List<Workspace> findByOwner(@Param("owner") User owner);

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.id = :id")
    Optional<Workspace> findByIdWithDetails(@Param("id") Long id);
}
