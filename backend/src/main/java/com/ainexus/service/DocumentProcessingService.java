package com.ainexus.service;

import com.ainexus.dto.TextChunk;
import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.exception.EmbeddingException;
import com.ainexus.exception.VectorStoreException;
import com.ainexus.model.vector.VectorRecord;
import com.ainexus.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;

@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractionService textExtractionService;
    private final DocumentTextCleaningService textCleaningService;
    private final DocumentTextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public DocumentProcessingService(DocumentRepository documentRepository,
                                     FileStorageService fileStorageService,
                                     DocumentTextExtractionService textExtractionService,
                                     DocumentTextCleaningService textCleaningService,
                                     DocumentTextChunkingService textChunkingService,
                                     EmbeddingService embeddingService,
                                     VectorStoreService vectorStoreService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
        this.textCleaningService = textCleaningService;
        this.textChunkingService = textChunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    @Async("documentProcessingExecutor")
    public void processDocumentAsync(Long documentId) {
        logger.info("Starting asynchronous indexing pipeline for document id: {}", documentId);

        Optional<Document> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            logger.error("Document with id {} not found for asynchronous processing", documentId);
            return;
        }

        Document document = documentOpt.get();

        if (document.getStatus() == DocumentStatus.PROCESSING) {
            logger.warn("Document id {} is already in PROCESSING state. Skipping duplicate trigger.", documentId);
            return;
        }

        updateStatus(documentId, DocumentStatus.PROCESSING);

        Long workspaceId = document.getWorkspace() != null ? document.getWorkspace().getId() : null;

        try {
            if (workspaceId == null) {
                throw new IllegalStateException("Document id " + documentId + " has no associated workspace context.");
            }

            // 1. Resolve stored file
            Path filePath = fileStorageService.getRootLocation().resolve(document.getStoragePath()).normalize();

            // 2. Text Extraction
            logger.debug("Extracting text for document id: {}", documentId);
            String rawText = textExtractionService.extractTextFromFile(filePath);
            if (rawText == null || rawText.trim().isEmpty()) {
                throw new IllegalArgumentException("Text extraction produced empty content for document id: " + documentId);
            }
            logger.debug("Extraction completed for document id: {} (raw length: {})", documentId, rawText.length());

            // 3. Text Cleaning & Normalization
            logger.debug("Cleaning text for document id: {}", documentId);
            String cleanedText = textCleaningService.cleanText(rawText);
            if (cleanedText == null || cleanedText.trim().isEmpty()) {
                throw new IllegalArgumentException("Text cleaning produced empty content for document id: " + documentId);
            }
            logger.debug("Cleaning completed for document id: {} (cleaned length: {})", documentId, cleanedText.length());

            // 4. Text Chunking
            logger.debug("Chunking text for document id: {}", documentId);
            List<TextChunk> rawChunks = textChunkingService.chunkText(cleanedText);
            List<TextChunk> validChunks = rawChunks != null ? rawChunks.stream()
                    .filter(c -> c.content() != null && !c.content().trim().isEmpty())
                    .toList() : Collections.emptyList();

            if (validChunks.isEmpty()) {
                throw new IllegalArgumentException("Text chunking produced zero valid chunks for document id: " + documentId);
            }
            logger.info("Chunking completed for document id: {}. Generated {} valid chunks.", documentId, validChunks.size());

            // 5. Embedding Generation
            logger.info("Generating embeddings for {} chunks of document id: {}", validChunks.size(), documentId);
            List<String> chunkTexts = validChunks.stream().map(TextChunk::content).toList();
            List<List<Float>> embeddings = embeddingService.generateEmbeddings(chunkTexts);

            if (embeddings == null || embeddings.size() != validChunks.size()) {
                throw new EmbeddingException("Mismatch between chunk count (" + validChunks.size() + ") and embeddings count (" + (embeddings != null ? embeddings.size() : 0) + ")");
            }
            logger.info("Successfully generated embeddings for document id: {}", documentId);

            // 6. Assemble Pinecone Vector Records
            List<VectorRecord> vectorRecords = new ArrayList<>(validChunks.size());
            for (int i = 0; i < validChunks.size(); i++) {
                TextChunk chunk = validChunks.get(i);
                List<Float> vector = embeddings.get(i);
                String vectorId = vectorStoreService.generateVectorId(workspaceId, documentId, chunk.index());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("workspaceId", workspaceId);
                metadata.put("documentId", documentId);
                metadata.put("chunkIndex", chunk.index());
                metadata.put("originalFilename", document.getOriginalFilename() != null ? document.getOriginalFilename() : document.getFileName());
                metadata.put("fileType", document.getFileType() != null ? document.getFileType() : "");
                metadata.put("characterCount", chunk.characterCount());
                metadata.put("content", chunk.content());

                vectorRecords.add(new VectorRecord(vectorId, vector, metadata));
            }

            // 7. Upsert to Pinecone Vector Database
            logger.info("Upserting {} vector records to Pinecone for document id: {} (workspace id: {})", vectorRecords.size(), documentId, workspaceId);
            vectorStoreService.upsert(workspaceId, vectorRecords);
            logger.info("Vector upsert completed for document id: {}", documentId);

            // 8. Update State to INDEXED
            updateStatus(documentId, DocumentStatus.INDEXED);
            logger.info("Document id {} successfully indexed and marked as INDEXED", documentId);

        } catch (Exception e) {
            logger.error("Document vector indexing pipeline failed for document id: {}. Reason: {}", documentId, e.getMessage(), e);
            try {
                if (workspaceId != null) {
                    vectorStoreService.deleteByDocumentId(workspaceId, documentId);
                }
            } catch (Exception cleanupEx) {
                logger.warn("Cleanup of partial vectors for document id: {} failed: {}", documentId, cleanupEx.getMessage());
            }
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
