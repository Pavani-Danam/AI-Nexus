package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.User;
import com.ainexus.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document saveDocument(Document document) {
        if (document.getFileName() == null || document.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Document filename cannot be empty");
        }
        if (document.getStoragePath() == null || document.getStoragePath().trim().isEmpty()) {
            throw new IllegalArgumentException("Document storage path cannot be empty");
        }
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByUser(User user) {
        return documentRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }

    public Document updateDocumentStatus(Long id, DocumentStatus status) {
        return documentRepository.findById(id)
                .map(doc -> {
                    doc.setStatus(status);
                    return documentRepository.save(doc);
                })
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));
    }

    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Document not found with id: " + id);
        }
        documentRepository.deleteById(id);
    }
}
