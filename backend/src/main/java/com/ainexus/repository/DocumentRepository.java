package com.ainexus.repository;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Find by User (Uploader)
    List<Document> findByUser(User user);
    Page<Document> findByUser(User user, Pageable pageable);
    List<Document> findByUserId(Long userId);
    Page<Document> findByUserId(Long userId, Pageable pageable);

    // Find by Workspace (Workspace Isolation)
    List<Document> findByWorkspace(Workspace workspace);
    Page<Document> findByWorkspace(Workspace workspace, Pageable pageable);
    List<Document> findByWorkspaceId(Long workspaceId);
    Page<Document> findByWorkspaceId(Long workspaceId, Pageable pageable);

    // Workspace-isolated search by filename (case-insensitive partial matching)
    Page<Document> findByWorkspaceIdAndFileNameContainingIgnoreCase(Long workspaceId, String fileName, Pageable pageable);

    // Global / User search by filename (case-insensitive partial matching)
    Page<Document> findByUserIdAndFileNameContainingIgnoreCase(Long userId, String fileName, Pageable pageable);
    Page<Document> findByFileNameContainingIgnoreCase(String fileName, Pageable pageable);

    // Status filtering
    List<Document> findByStatus(DocumentStatus status);
    Page<Document> findByWorkspaceIdAndStatus(Long workspaceId, DocumentStatus status, Pageable pageable);

    // Secure document lookup respecting workspace boundaries
    Optional<Document> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
