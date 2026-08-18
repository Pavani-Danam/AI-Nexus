package com.ainexus.service.impl;

import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.service.EmbeddingService;
import com.ainexus.service.SemanticCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SemanticCacheServiceImpl implements SemanticCacheService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticCacheServiceImpl.class);

    private final EmbeddingService embeddingService;

    @Value("${app.rag.semantic-cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.rag.semantic-cache.similarity-threshold:0.92}")
    private double similarityThreshold;

    @Value("${app.rag.semantic-cache.max-entries:1000}")
    private int maxEntries;

    @Value("${app.rag.semantic-cache.ttl:3600}")
    private long ttlSeconds;

    // In-memory cache partitioned by workspaceId -> List of CacheEntry
    private final Map<Long, List<CacheEntry>> workspaceCaches = new ConcurrentHashMap<>();

    public SemanticCacheServiceImpl(EmbeddingService embeddingService) {
        this.embeddingService = Objects.requireNonNull(embeddingService, "EmbeddingService must not be null");
    }

    @Override
    public Optional<RAGResponse> lookup(String query, Long workspaceId, User user) {
        if (!cacheEnabled || query == null || query.trim().isEmpty() || workspaceId == null) {
            return Optional.empty();
        }

        try {
            List<CacheEntry> entries = workspaceCaches.get(workspaceId);
            if (entries == null || entries.isEmpty()) {
                logger.debug("CACHE MISS: No entries found for workspace id: {}", workspaceId);
                return Optional.empty();
            }

            // Clean expired entries
            Instant now = Instant.now();
            entries.removeIf(entry -> entry.expiresAt().isBefore(now));

            if (entries.isEmpty()) {
                logger.debug("CACHE MISS: All entries expired for workspace id: {}", workspaceId);
                return Optional.empty();
            }

            // Generate embedding for incoming query
            List<Float> queryEmbedding = embeddingService.generateEmbedding(query.trim());
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                return Optional.empty();
            }

            CacheEntry bestMatch = null;
            double highestScore = -1.0;

            for (CacheEntry entry : entries) {
                double sim = computeCosineSimilarity(queryEmbedding, entry.embedding());
                if (sim > highestScore) {
                    highestScore = sim;
                    bestMatch = entry;
                }
            }

            if (bestMatch != null && highestScore >= similarityThreshold) {
                logger.info("CACHE HIT: Query '{}' matched cached query '{}' with similarity {} in workspace id: {}",
                        query.trim(), bestMatch.query(), Math.round(highestScore * 10000.0) / 10000.0, workspaceId);
                return Optional.of(bestMatch.response());
            }

            logger.debug("CACHE MISS: Highest similarity {} below threshold {} for query: '{}'",
                    Math.round(highestScore * 10000.0) / 10000.0, similarityThreshold, query.trim());
            return Optional.empty();

        } catch (Exception e) {
            logger.warn("Semantic cache lookup failed gracefully: {}. Falling back to normal RAG pipeline.", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void store(String query, Long workspaceId, User user, RAGResponse response) {
        if (!cacheEnabled || query == null || query.trim().isEmpty() || workspaceId == null || response == null) {
            return;
        }

        // Do not cache answers without meaningful content
        if (response.answer() == null || response.answer().trim().isEmpty()) {
            return;
        }

        try {
            List<Float> embedding = embeddingService.generateEmbedding(query.trim());
            if (embedding == null || embedding.isEmpty()) {
                return;
            }

            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(ttlSeconds);

            CacheEntry newEntry = new CacheEntry(
                    query.trim(),
                    workspaceId,
                    embedding,
                    response,
                    now,
                    expiresAt
            );

            workspaceCaches.compute(workspaceId, (k, existingList) -> {
                List<CacheEntry> list = (existingList != null) ? existingList : Collections.synchronizedList(new ArrayList<>());
                list.removeIf(entry -> entry.expiresAt().isBefore(now));

                if (list.size() >= maxEntries) {
                    list.remove(0); // Evict oldest
                }
                list.add(newEntry);
                return list;
            });

            logger.info("CACHE STORE: Cached response for query '{}' in workspace id: {} (TTL: {}s)",
                    query.trim(), workspaceId, ttlSeconds);

        } catch (Exception e) {
            logger.warn("Semantic cache store failed gracefully: {}", e.getMessage());
        }
    }

    @Override
    public void invalidateWorkspace(Long workspaceId) {
        if (workspaceId != null) {
            workspaceCaches.remove(workspaceId);
            logger.info("CACHE INVALIDATED for workspace id: {}", workspaceId);
        }
    }

    private double computeCosineSimilarity(List<Float> vecA, List<Float> vecB) {
        if (vecA == null || vecB == null || vecA.size() != vecB.size() || vecA.isEmpty()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.size(); i++) {
            float valA = vecA.get(i);
            float valB = vecB.get(i);
            dotProduct += valA * valB;
            normA += valA * valA;
            normB += valB * valB;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record CacheEntry(
            String query,
            Long workspaceId,
            List<Float> embedding,
            RAGResponse response,
            Instant createdAt,
            Instant expiresAt
    ) {}
}
