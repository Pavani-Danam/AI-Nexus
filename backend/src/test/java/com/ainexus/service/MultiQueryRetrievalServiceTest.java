package com.ainexus.service;

import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.impl.MultiQueryRetrievalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiQueryRetrievalServiceTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    private TestableMultiQueryRetrievalServiceImpl multiQueryRetrievalService;
    private User testUser;

    static class TestableMultiQueryRetrievalServiceImpl extends MultiQueryRetrievalServiceImpl {
        private List<String> mockVariations = Collections.emptyList();
        private boolean shouldThrowException = false;

        public TestableMultiQueryRetrievalServiceImpl(SemanticSearchService semanticSearchService) {
            super(semanticSearchService);
        }

        public void setMockVariations(List<String> variations) {
            this.mockVariations = variations;
            this.shouldThrowException = false;
        }

        public void setShouldThrowException(boolean shouldThrow) {
            this.shouldThrowException = shouldThrow;
        }

        @Override
        protected List<String> callGeminiForVariations(String query, int count) throws Exception {
            if (shouldThrowException) {
                throw new RuntimeException("Simulated Gemini error");
            }
            return mockVariations;
        }
    }

    @BeforeEach
    void setUp() {
        multiQueryRetrievalService = new TestableMultiQueryRetrievalServiceImpl(semanticSearchService);
        ReflectionTestUtils.setField(multiQueryRetrievalService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(multiQueryRetrievalService, "generationModel", "gemini-1.5-flash");
        ReflectionTestUtils.setField(multiQueryRetrievalService, "multiQueryEnabled", true);
        ReflectionTestUtils.setField(multiQueryRetrievalService, "maxVariations", 3);
        ReflectionTestUtils.setField(multiQueryRetrievalService, "timeoutSeconds", 15);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Multiple query variations generated and executed across workspace")
    void testMultiQueryGenerationAndExecution() {
        multiQueryRetrievalService.setMockVariations(List.of("leave policy rules", "annual vacation entitlements"));

        SearchResultItem item1 = new SearchResultItem(1L, "policy.pdf", 0, 0.90, "Leave policy text", 20, "application/pdf", "v-1");
        SearchResultItem item2 = new SearchResultItem(1L, "policy.pdf", 1, 0.85, "Vacation text", 20, "application/pdf", "v-2");

        when(semanticSearchService.search(eq("leave policy"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("leave policy", 10L, 1, List.of(item1)));
        when(semanticSearchService.search(eq("leave policy rules"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("leave policy rules", 10L, 1, List.of(item2)));
        when(semanticSearchService.search(eq("annual vacation entitlements"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("annual vacation entitlements", 10L, 0, Collections.emptyList()));

        List<SearchResultItem> results = multiQueryRetrievalService.retrieveMultiQueryResults("leave policy", 10L, 5, testUser);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(semanticSearchService, times(3)).search(anyString(), eq(10L), eq(5), eq(testUser));
    }

    @Test
    @DisplayName("TEST 2: Duplicate chunks are merged and highest similarity score is preserved")
    void testDeduplicationPreservesMaxScore() {
        multiQueryRetrievalService.setMockVariations(List.of("query variation 2"));

        SearchResultItem itemQuery1 = new SearchResultItem(10L, "handbook.pdf", 0, 0.75, "Same chunk content", 30, "application/pdf", "v-10");
        SearchResultItem itemQuery2 = new SearchResultItem(10L, "handbook.pdf", 0, 0.92, "Same chunk content", 30, "application/pdf", "v-10");

        when(semanticSearchService.search(eq("primary query"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("primary query", 10L, 1, List.of(itemQuery1)));
        when(semanticSearchService.search(eq("query variation 2"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("query variation 2", 10L, 1, List.of(itemQuery2)));

        List<SearchResultItem> results = multiQueryRetrievalService.retrieveMultiQueryResults("primary query", 10L, 5, testUser);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0.92, results.get(0).score());
        assertEquals("handbook.pdf", results.get(0).filename());
    }

    @Test
    @DisplayName("TEST 3: Merged results are deterministically sorted by score descending")
    void testMergedResultsSortedByScoreDescending() {
        multiQueryRetrievalService.setMockVariations(List.of("query variation 2"));

        SearchResultItem lowScore = new SearchResultItem(1L, "a.pdf", 0, 0.60, "Low score", 10, "application/pdf", "v-1");
        SearchResultItem highScore = new SearchResultItem(2L, "b.pdf", 0, 0.95, "High score", 10, "application/pdf", "v-2");
        SearchResultItem midScore = new SearchResultItem(3L, "c.pdf", 0, 0.80, "Mid score", 10, "application/pdf", "v-3");

        when(semanticSearchService.search(eq("query1"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("query1", 10L, 2, List.of(lowScore, midScore)));
        when(semanticSearchService.search(eq("query variation 2"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("query variation 2", 10L, 1, List.of(highScore)));

        List<SearchResultItem> results = multiQueryRetrievalService.retrieveMultiQueryResults("query1", 10L, 5, testUser);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals(0.95, results.get(0).score());
        assertEquals(0.80, results.get(1).score());
        assertEquals(0.60, results.get(2).score());
    }

    @Test
    @DisplayName("TEST 4: Variation generation failure falls back to single query retrieval")
    void testVariationFailureFallback() {
        multiQueryRetrievalService.setShouldThrowException(true);

        SearchResultItem item = new SearchResultItem(1L, "doc.pdf", 0, 0.88, "Content", 15, "application/pdf", "v-1");
        when(semanticSearchService.search(eq("single query"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("single query", 10L, 1, List.of(item)));

        List<SearchResultItem> results = multiQueryRetrievalService.retrieveMultiQueryResults("single query", 10L, 5, testUser);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(semanticSearchService, times(1)).search(eq("single query"), eq(10L), eq(5), eq(testUser));
    }

    @Test
    @DisplayName("TEST 5: Empty search results across all queries returns empty list")
    void testEmptyResultsAcrossAllQueries() {
        multiQueryRetrievalService.setMockVariations(List.of("var1", "var2"));

        when(semanticSearchService.search(anyString(), eq(10L), eq(5), eq(testUser)))
                .thenReturn(new SearchResponse("query", 10L, 0, Collections.emptyList()));

        List<SearchResultItem> results = multiQueryRetrievalService.retrieveMultiQueryResults("primary", 10L, 5, testUser);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("TEST 6: Null or blank query throws IllegalArgumentException")
    void testInvalidQuery() {
        assertThrows(IllegalArgumentException.class, () ->
                multiQueryRetrievalService.retrieveMultiQueryResults(null, 10L, 5, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                multiQueryRetrievalService.retrieveMultiQueryResults("   ", 10L, 5, testUser));
    }
}
