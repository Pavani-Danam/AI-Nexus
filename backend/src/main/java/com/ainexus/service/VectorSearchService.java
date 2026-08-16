package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentChunk;
import com.ainexus.repository.DocumentChunkRepository;
import com.ainexus.repository.DocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        List<Float> queryVector;
        try {
            queryVector = embeddingService.generateEmbedding(query);
        } catch (Exception e) {
            logger.warn("Could not generate query embedding: {}", e.getMessage());
            return Collections.emptyList();
        }

        if (queryVector == null || queryVector.isEmpty()) {
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
                List<Float> chunkVector = deserializeEmbedding(chunk.getEmbedding());
                double score = calculateCosineSimilarity(queryVector, chunkVector);

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

    private List<Float> deserializeEmbedding(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Float>>() {});
        } catch (Exception e) {
            logger.error("Error deserializing vector: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private double calculateCosineSimilarity(List<Float> vecA, List<Float> vecB) {
        if (vecA == null || vecB == null || vecA.isEmpty() || vecB.isEmpty() || vecA.size() != vecB.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            float a = vecA.get(i);
            float b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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
