package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentChunk;
import com.ainexus.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;

    public DocumentChunkService(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    public DocumentChunk saveChunk(DocumentChunk chunk) {
        if (chunk.getContent() == null || chunk.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Chunk content cannot be empty");
        }
        return documentChunkRepository.save(chunk);
    }

    public List<DocumentChunk> saveAllChunks(List<DocumentChunk> chunks) {
        return documentChunkRepository.saveAll(chunks);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> getChunksByDocument(Document document) {
        return documentChunkRepository.findByDocumentOrderByChunkIndexAsc(document);
    }

    public void deleteChunksByDocument(Document document) {
        documentChunkRepository.deleteByDocument(document);
    }
}
