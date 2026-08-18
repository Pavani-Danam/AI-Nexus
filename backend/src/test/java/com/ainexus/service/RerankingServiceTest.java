package com.ainexus.service;

import com.ainexus.dto.SearchResultItem;
import com.ainexus.service.impl.RerankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RerankingServiceTest {

    private RerankingServiceImpl rerankingService;

    @BeforeEach
    void setUp() {
        rerankingService = new RerankingServiceImpl();
        ReflectionTestUtils.setField(rerankingService, "rerankingEnabled", true);
        ReflectionTestUtils.setField(rerankingService, "maxResults", 10);
    }

    @Test
    @DisplayName("TEST 1: Results with higher lexical relevance and similarity score are ranked first")
    void testRerankingOrdering() {
        SearchResultItem generalItem = new SearchResultItem(
                1L, "handbook.pdf", 0, 0.85, "The company provides many general benefits.", 45, "application/pdf", "v-1"
        );
        SearchResultItem specificItem = new SearchResultItem(
                2L, "leave.pdf", 0, 0.82, "Full-time employees receive 20 days annual vacation leave policy.", 65, "application/pdf", "v-2"
        );

        List<SearchResultItem> reranked = rerankingService.rerank("vacation leave policy", List.of(generalItem, specificItem));

        assertNotNull(reranked);
        assertEquals(2, reranked.size());
        assertEquals(2L, reranked.get(0).documentId());
        assertEquals("leave.pdf", reranked.get(0).filename());
    }

    @Test
    @DisplayName("TEST 2: Metadata remains intact after reranking")
    void testMetadataPreservation() {
        SearchResultItem item = new SearchResultItem(
                10L, "security.pdf", 3, 0.90, "Two-factor authentication is mandatory.", 40, "application/pdf", "v-10"
        );

        List<SearchResultItem> reranked = rerankingService.rerank("authentication", List.of(item));

        assertEquals(1, reranked.size());
        SearchResultItem result = reranked.get(0);
        assertEquals(10L, result.documentId());
        assertEquals("security.pdf", result.filename());
        assertEquals(3, result.chunkIndex());
        assertEquals("Two-factor authentication is mandatory.", result.content());
        assertEquals("application/pdf", result.fileType());
        assertEquals("v-10", result.vectorId());
    }

    @Test
    @DisplayName("TEST 3: Empty or null candidate list returns empty list safely")
    void testEmptyCandidates() {
        assertTrue(rerankingService.rerank("query", Collections.emptyList()).isEmpty());
        assertTrue(rerankingService.rerank("query", null).isEmpty());
    }

    @Test
    @DisplayName("TEST 4: Deterministic ranking produces exact same order on repeated calls")
    void testDeterministicRanking() {
        SearchResultItem itemA = new SearchResultItem(1L, "docA.pdf", 0, 0.70, "Machine learning models.", 24, "application/pdf", "v-1");
        SearchResultItem itemB = new SearchResultItem(2L, "docB.pdf", 0, 0.70, "Deep neural networks.", 21, "application/pdf", "v-2");

        List<SearchResultItem> run1 = rerankingService.rerank("neural networks", List.of(itemA, itemB));
        List<SearchResultItem> run2 = rerankingService.rerank("neural networks", List.of(itemA, itemB));

        assertEquals(run1.get(0).documentId(), run2.get(0).documentId());
        assertEquals(run1.get(1).documentId(), run2.get(1).documentId());
    }

    @Test
    @DisplayName("TEST 5: Disabled reranking respects original order with max-results limit")
    void testDisabledReranking() {
        ReflectionTestUtils.setField(rerankingService, "rerankingEnabled", false);
        ReflectionTestUtils.setField(rerankingService, "maxResults", 1);

        SearchResultItem item1 = new SearchResultItem(1L, "doc1.pdf", 0, 0.50, "Content 1", 9, "application/pdf", "v-1");
        SearchResultItem item2 = new SearchResultItem(2L, "doc2.pdf", 0, 0.90, "Content 2", 9, "application/pdf", "v-2");

        List<SearchResultItem> reranked = rerankingService.rerank("search", List.of(item1, item2));

        assertEquals(1, reranked.size());
        assertEquals(1L, reranked.get(0).documentId());
    }
}
