package com.ainexus.service;

import com.ainexus.dto.RAGCitation;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.service.impl.SemanticCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    private SemanticCacheServiceImpl semanticCacheService;
    private User testUser;

    @BeforeEach
    void setUp() {
        semanticCacheService = new SemanticCacheServiceImpl(embeddingService);
        ReflectionTestUtils.setField(semanticCacheService, "cacheEnabled", true);
        ReflectionTestUtils.setField(semanticCacheService, "similarityThreshold", 0.90);
        ReflectionTestUtils.setField(semanticCacheService, "maxEntries", 100);
        ReflectionTestUtils.setField(semanticCacheService, "ttlSeconds", 3600L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: First query produces cache MISS, subsequent store and lookup produces cache HIT")
    void testCacheStoreAndLookupHit() {
        List<Float> embedding = List.of(0.1f, 0.8f, 0.5f);
        when(embeddingService.generateEmbedding("What is the leave policy?"))
                .thenReturn(embedding);

        Optional<RAGResponse> initialLookup = semanticCacheService.lookup("What is the leave policy?", 10L, testUser);
        assertTrue(initialLookup.isEmpty());

        RAGResponse response = new RAGResponse(
                "Employees get 20 days annual leave.",
                "What is the leave policy?",
                10L,
                List.of(new RAGCitation(1L, "handbook.pdf", 0, 0.9, "doc-1-chunk-0", "20 days annual leave.")),
                Collections.emptyList(),
                true
        );

        semanticCacheService.store("What is the leave policy?", 10L, testUser, response);

        Optional<RAGResponse> cachedLookup = semanticCacheService.lookup("What is the leave policy?", 10L, testUser);
        assertTrue(cachedLookup.isPresent());
        assertEquals("Employees get 20 days annual leave.", cachedLookup.get().answer());
        assertEquals(1, cachedLookup.get().citations().size());
    }

    @Test
    @DisplayName("TEST 2: Semantically similar query produces cache HIT")
    void testSemanticallySimilarQueryHit() {
        List<Float> baseEmbedding = List.of(1.0f, 0.0f, 0.0f);
        List<Float> similarEmbedding = List.of(0.99f, 0.05f, 0.0f);

        when(embeddingService.generateEmbedding("vacation entitlement"))
                .thenReturn(baseEmbedding);
        when(embeddingService.generateEmbedding("how many vacation days do I get?"))
                .thenReturn(similarEmbedding);

        RAGResponse response = new RAGResponse(
                "20 vacation days per year.",
                "vacation entitlement",
                10L,
                List.of(new RAGCitation(1L, "handbook.pdf", 0, 0.9, "doc-1-chunk-0", "20 vacation days.")),
                Collections.emptyList(),
                true
        );

        semanticCacheService.store("vacation entitlement", 10L, testUser, response);

        Optional<RAGResponse> hit = semanticCacheService.lookup("how many vacation days do I get?", 10L, testUser);
        assertTrue(hit.isPresent());
        assertEquals("20 vacation days per year.", hit.get().answer());
    }

    @Test
    @DisplayName("TEST 3: Distinct query produces cache MISS")
    void testDistinctQueryMiss() {
        List<Float> queryA = List.of(1.0f, 0.0f, 0.0f);
        List<Float> queryB = List.of(0.0f, 1.0f, 0.0f);

        when(embeddingService.generateEmbedding("leave policy"))
                .thenReturn(queryA);
        when(embeddingService.generateEmbedding("salary structure"))
                .thenReturn(queryB);

        RAGResponse response = new RAGResponse("Leave details", "leave policy", 10L, List.of(), Collections.emptyList(), true);
        semanticCacheService.store("leave policy", 10L, testUser, response);

        Optional<RAGResponse> lookup = semanticCacheService.lookup("salary structure", 10L, testUser);
        assertTrue(lookup.isEmpty());
    }

    @Test
    @DisplayName("TEST 4: Workspace isolation prevents cross-workspace cache hit")
    void testWorkspaceIsolation() {
        List<Float> embedding = List.of(0.5f, 0.5f, 0.5f);
        when(embeddingService.generateEmbedding("confidential project"))
                .thenReturn(embedding);

        RAGResponse response = new RAGResponse("Workspace 10 secret", "confidential project", 10L, List.of(), Collections.emptyList(), true);
        semanticCacheService.store("confidential project", 10L, testUser, response);

        Optional<RAGResponse> crossWorkspaceLookup = semanticCacheService.lookup("confidential project", 20L, testUser);
        assertTrue(crossWorkspaceLookup.isEmpty());
    }

    @Test
    @DisplayName("TEST 5: Workspace cache invalidation removes cached items")
    void testWorkspaceInvalidation() {
        List<Float> embedding = List.of(0.5f, 0.5f, 0.5f);
        when(embeddingService.generateEmbedding("policy"))
                .thenReturn(embedding);

        RAGResponse response = new RAGResponse("Old policy", "policy", 10L, List.of(), Collections.emptyList(), true);
        semanticCacheService.store("policy", 10L, testUser, response);

        semanticCacheService.invalidateWorkspace(10L);

        Optional<RAGResponse> lookup = semanticCacheService.lookup("policy", 10L, testUser);
        assertTrue(lookup.isEmpty());
    }
}
