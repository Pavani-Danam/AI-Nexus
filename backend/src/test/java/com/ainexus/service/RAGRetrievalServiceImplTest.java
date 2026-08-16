package com.ainexus.service;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.impl.RAGRetrievalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGRetrievalServiceImplTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @InjectMocks
    private RAGRetrievalServiceImpl ragRetrievalService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ragRetrievalService, "defaultTopK", 5);
        ReflectionTestUtils.setField(ragRetrievalService, "minRelevanceScore", 0.35);
        ReflectionTestUtils.setField(ragRetrievalService, "maxContextCharacters", 1000);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1 & 7: Relevant chunks from multiple documents are retrieved and assembled into structured context")
    void testSuccessfulRetrievalAndAssembly() {
        SearchResultItem item1 = new SearchResultItem(
                10L, "architecture.pdf", 0, 0.88, "Microservices architecture patterns and best practices.", 54, "pdf", "ws_1_doc_10_0"
        );
        SearchResultItem item2 = new SearchResultItem(
                11L, "security.docx", 2, 0.75, "OAuth2 token rotation and RBAC access guidelines.", 48, "docx", "ws_1_doc_11_2"
        );

        SearchResponse searchResponse = new SearchResponse("architecture guidelines", 1L, 2, List.of(item1, item2));
        when(semanticSearchService.search("architecture guidelines", 1L, 5, testUser)).thenReturn(searchResponse);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("architecture guidelines", 1L, 5, testUser);

        assertNotNull(context);
        assertEquals("architecture guidelines", context.query());
        assertEquals(1L, context.workspaceId());
        assertEquals(2, context.chunks().size());
        assertTrue(context.totalCharacters() > 0);

        // Verify chunk metadata
        RAGChunk c1 = context.chunks().get(0);
        assertEquals(10L, c1.documentId());
        assertEquals("architecture.pdf", c1.filename());
        assertEquals(0, c1.chunkIndex());
        assertEquals(0.88, c1.score());

        // Verify assembled string contains source labels
        assertTrue(context.assembledContext().contains("[Source: architecture.pdf, Chunk: 1, Score: 0.880]"));
        assertTrue(context.assembledContext().contains("Microservices architecture patterns"));
        assertTrue(context.assembledContext().contains("[Source: security.docx, Chunk: 3, Score: 0.750]"));
    }

    @Test
    @DisplayName("TEST 2: When semantic search returns no results, empty RAGContext is returned safely")
    void testEmptySearchResultsReturnEmptyContext() {
        SearchResponse emptyResponse = new SearchResponse("unknown query", 1L, 0, Collections.emptyList());
        when(semanticSearchService.search("unknown query", 1L, 5, testUser)).thenReturn(emptyResponse);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("unknown query", 1L, 5, testUser);

        assertNotNull(context);
        assertEquals(0, context.chunks().size());
        assertEquals("", context.assembledContext());
        assertEquals(0, context.totalCharacters());
    }

    @Test
    @DisplayName("TEST 3: Chunks below the minimum relevance threshold are excluded")
    void testFilterBelowMinRelevanceScore() {
        SearchResultItem lowScoreItem = new SearchResultItem(
                10L, "random.pdf", 0, 0.20, "Low relevance text", 18, "pdf", "vec_1"
        );
        SearchResultItem highScoreItem = new SearchResultItem(
                10L, "random.pdf", 1, 0.65, "High relevance text", 19, "pdf", "vec_2"
        );

        SearchResponse response = new SearchResponse("test", 1L, 2, List.of(highScoreItem, lowScoreItem));
        when(semanticSearchService.search("test", 1L, 5, testUser)).thenReturn(response);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("test", 1L, 5, testUser);

        assertNotNull(context);
        assertEquals(1, context.chunks().size());
        assertEquals(0.65, context.chunks().get(0).score());
        assertFalse(context.assembledContext().contains("Low relevance text"));
    }

    @Test
    @DisplayName("TEST 4: Duplicate chunks with identical keys are deduplicated")
    void testDuplicateChunkRemoval() {
        SearchResultItem item1 = new SearchResultItem(
                10L, "doc.pdf", 0, 0.90, "Identical chunk content", 23, "pdf", "vec_dup"
        );
        SearchResultItem item2 = new SearchResultItem(
                10L, "doc.pdf", 0, 0.89, "Identical chunk content", 23, "pdf", "vec_dup"
        );

        SearchResponse response = new SearchResponse("test", 1L, 2, List.of(item1, item2));
        when(semanticSearchService.search("test", 1L, 5, testUser)).thenReturn(response);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("test", 1L, 5, testUser);

        assertNotNull(context);
        assertEquals(1, context.chunks().size());
        assertEquals(0.90, context.chunks().get(0).score());
    }

    @Test
    @DisplayName("TEST 5: Workspace isolation is delegated to SemanticSearchService")
    void testWorkspaceIsolationDelegation() {
        when(semanticSearchService.search("query", 99L, 5, testUser))
                .thenReturn(new SearchResponse("query", 99L, 0, Collections.emptyList()));

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("query", 99L, 5, testUser);

        assertEquals(99L, context.workspaceId());
        verify(semanticSearchService).search("query", 99L, 5, testUser);
    }

    @Test
    @DisplayName("TEST 6: Context character budget truncation limits maximum assembled size")
    void testContextSizeLimitation() {
        // Set max characters to 120
        ReflectionTestUtils.setField(ragRetrievalService, "maxContextCharacters", 120);

        SearchResultItem item1 = new SearchResultItem(
                10L, "doc1.pdf", 0, 0.95, "A".repeat(80), 80, "pdf", "vec_1"
        );
        SearchResultItem item2 = new SearchResultItem(
                10L, "doc2.pdf", 1, 0.85, "B".repeat(80), 80, "pdf", "vec_2"
        );

        SearchResponse response = new SearchResponse("test", 1L, 2, List.of(item1, item2));
        when(semanticSearchService.search("test", 1L, 5, testUser)).thenReturn(response);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("test", 1L, 5, testUser);

        assertNotNull(context);
        // Only first chunk should fit within budget
        assertEquals(1, context.chunks().size());
        assertEquals(0.95, context.chunks().get(0).score());
        assertTrue(context.totalCharacters() <= 120);
    }

    @Test
    @DisplayName("TEST 8: Blank or null query throws IllegalArgumentException")
    void testBlankQueryValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                ragRetrievalService.retrieveAndAssembleContext("", 1L, 5, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                ragRetrievalService.retrieveAndAssembleContext("   ", 1L, 5, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                ragRetrievalService.retrieveAndAssembleContext(null, 1L, 5, testUser));
    }
}
