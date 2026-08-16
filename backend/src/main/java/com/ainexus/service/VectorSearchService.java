package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentChunk;
import com.ainexus.repository.DocumentChunkRepository;
import com.ainexus.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public VectorSearchService(EmbeddingService embeddingService,
                               DocumentChunkRepository documentChunkRepository,
                               DocumentRepository documentRepository) {
        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> searchSimilarChunks(Long workspaceId, String query, int topK, double minScore) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Double> queryVector = embeddingService.generateEmbedding(query);
        if (queryVector.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> docs = documentRepository.findByWorkspace_Id(workspaceId);
        List<SearchResult> allResults = new ArrayList<>();

        for (Document doc : docs) {
            List<DocumentChunk> chunks = documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(doc.getId());
            for (DocumentChunk chunk : chunks) {
                if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                    continue;
                }
                List<Double> chunkVector = embeddingService.deserializeEmbedding(chunk.getEmbedding());
                double score = embeddingService.calculateCosineSimilarity(queryVector, chunkVector);

                if (score >= minScore) {
                    allResults.add(new SearchResult(
                            chunk.getId(),
                            doc.getId(),
                            doc.getFileName(),
                            chunk.getChunkIndex(),
                            chunk.getContent(),
                            chunk.getTokenCount(),
                            score
                    ));
                }
            }
        }

        return allResults.stream()
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK > 0 ? topK : 5)
                .collect(Collectors.toList());
    }

    public record SearchResult(
            Long chunkId,
            Long documentId,
            String fileName,
            Integer chunkIndex,
            String content,
            Integer tokenCount,
            Double score
    ) {}
}
