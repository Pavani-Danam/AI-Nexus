package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentChunk;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.ProcessingJob;
import com.ainexus.repository.DocumentChunkRepository;
import com.ainexus.repository.DocumentRepository;
import com.ainexus.repository.ProcessingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentParserService documentParserService;
    private final TextChunkerService textChunkerService;
    private final EmbeddingService embeddingService;
    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ProcessingJobRepository processingJobRepository;

    public DocumentProcessingService(DocumentParserService documentParserService,
                                     TextChunkerService textChunkerService,
                                     EmbeddingService embeddingService,
                                     FileStorageService fileStorageService,
                                     DocumentRepository documentRepository,
                                     DocumentChunkRepository documentChunkRepository,
                                     ProcessingJobRepository processingJobRepository) {
        this.documentParserService = documentParserService;
        this.textChunkerService = textChunkerService;
        this.embeddingService = embeddingService;
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.processingJobRepository = processingJobRepository;
    }

    @Async("documentProcessingExecutor")
    @Transactional
    public void processDocumentAsync(Long documentId, Long jobId) {
        logger.info("Starting async document processing & embedding generation for documentId={}, jobId={}", documentId, jobId);

        Document document = documentRepository.findById(documentId).orElse(null);
        ProcessingJob job = processingJobRepository.findById(jobId).orElse(null);

        if (document == null || job == null) {
            logger.error("Document or Job not found. documentId={}, jobId={}", documentId, jobId);
            return;
        }

        try {
            job.setStatus("PROCESSING");
            processingJobRepository.save(job);

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            // 1. Resolve path and extract text
            Path fullFilePath = fileStorageService.getRootLocation().resolve(document.getStoragePath()).normalize();
            String extractedText = documentParserService.extractText(fullFilePath);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IllegalStateException("No text content could be extracted from file: " + document.getFileName());
            }

            // 2. Chunking
            List<TextChunkerService.ChunkResult> chunkResults = textChunkerService.chunkText(extractedText);

            documentChunkRepository.deleteByDocument(document);

            // 3. Vector Embeddings Generation
            List<DocumentChunk> chunks = chunkResults.stream()
                    .map(cr -> {
                        List<Double> vector = embeddingService.generateEmbedding(cr.content());
                        String serializedVec = embeddingService.serializeEmbedding(vector);

                        return DocumentChunk.builder()
                                .document(document)
                                .chunkIndex(cr.index())
                                .content(cr.content())
                                .tokenCount(cr.tokenCount())
                                .embedding(serializedVec)
                                .build();
                    })
                    .collect(Collectors.toList());

            documentChunkRepository.saveAll(chunks);

            // 4. Mark completed / indexed
            document.setStatus(DocumentStatus.INDEXED);
            documentRepository.save(document);

            job.setStatus("COMPLETED");
            job.setErrorMessage(null);
            processingJobRepository.save(job);

            logger.info("Successfully processed and vectorized documentId={} with {} chunks generated", documentId, chunks.size());

        } catch (Exception ex) {
            logger.error("Error processing documentId={}: {}", documentId, ex.getMessage(), ex);

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            job.setStatus("FAILED");
            job.setErrorMessage(ex.getMessage());
            processingJobRepository.save(job);
        }
    }
}
