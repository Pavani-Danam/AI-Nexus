package com.ainexus.repository;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentOrderByChunkIndexAsc(Document document);
    void deleteByDocument(Document document);
}
