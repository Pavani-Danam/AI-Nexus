package com.ainexus.repository;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByWorkspace(Workspace workspace);

    List<Document> findByWorkspace_Id(Long workspaceId);

    Page<Document> findByWorkspace(Workspace workspace, Pageable pageable);

    List<Document> findByUser(User user);

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByWorkspaceAndStatus(Workspace workspace, DocumentStatus status);

    @Query("SELECT d FROM Document d WHERE d.workspace = :workspace AND (:search IS NULL OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY d.createdAt DESC")
    List<Document> findByWorkspaceAndSearch(@Param("workspace") Workspace workspace, @Param("search") String search);
}
