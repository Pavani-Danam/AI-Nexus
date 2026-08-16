package com.ainexus.repository;

import com.ainexus.entity.Document;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByWorkspace(Workspace workspace);
    Page<Document> findByWorkspace(Workspace workspace, Pageable pageable);
    List<Document> findByWorkspace_Id(Long workspaceId);
    List<Document> findByUser(User user);
}
