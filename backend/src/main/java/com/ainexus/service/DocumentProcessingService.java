package com.ainexus.service;

import com.ainexus.dto.TextChunk;
import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractionService textExtractionService;
    private final DocumentTextCleaningService textCleaningService;
    private final DocumentTextChunkingService textChunkingService;

    public DocumentProcessingService(DocumentRepository documentRepository,
                                     FileStorageService fileStorageService,
                                     DocumentTextExtractionService textExtractionService,
                                     DocumentTextCleaningService textCleaningService,
                                     DocumentTextChunkingService textChunkingService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
        this.textCleaningService = textCleaningService;
        this.textChunkingService = textChunkingService;
    }

    @Async("documentProcessingExecutor")
    public void processDocumentAsync(Long documentId) {
        logger.info("Starting asynchronous processing for document id: {}", documentId);

        Optional<Document> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            logger.error("Document with id {} not found for asynchronous processing", documentId);
            return;
        }

        Document document = documentOpt.get();

        // Prevent duplicate simultaneous processing
        if (document.getStatus() == DocumentStatus.PROCESSING) {
            logger.warn("Document id {} is already in PROCESSING state. Skipping duplicate trigger.", documentId);
            return;
        }

        // Update state to PROCESSING in a fresh transaction
        updateStatus(documentId, DocumentStatus.PROCESSING);

        try {
            // 1. Resolve stored file
            Path filePath = fileStorageService.getRootLocation().resolve(document.getStoragePath()).normalize();

            // 2. Text Extraction
            logger.debug("Extracting text for document id: {}", documentId);
            String rawText = textExtractionService.extractTextFromFile(filePath);
            logger.debug("Extraction completed for document id: {} (raw character count: {})", documentId, rawText.length());

            // 3. Text Cleaning & Normalization
            logger.debug("Cleaning text for document id: {}", documentId);
            String cleanedText = textCleaningService.cleanText(rawText);
            logger.debug("Cleaning completed for document id: {} (cleaned character count: {})", documentId, cleanedText.length());

            // 4. Text Chunking
            logger.debug("Chunking text for document id: {}", documentId);
            List<TextChunk> chunks = textChunkingService.chunkText(cleanedText);
            logger.info("Chunking completed for document id: {}. Generated {} chunks.", documentId, chunks.size());

            // 5. Update state to INDEXED
            updateStatus(documentId, DocumentStatus.INDEXED);
            logger.info("Document id {} successfully processed and marked as INDEXED", documentId);

        } catch (Exception e) {
            logger.error("Document processing failed for document id: {}. Reason: {}", documentId, e.getMessage(), e);
            updateStatus(documentId, DocumentStatus.FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long documentId, DocumentStatus status) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(status);
            documentRepository.saveAndFlush(doc);
        });
    }
}
