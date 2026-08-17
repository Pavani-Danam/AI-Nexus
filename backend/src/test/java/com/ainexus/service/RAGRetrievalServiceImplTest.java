package com.ainexus.service;

import com.ainexus.dto.EnhancedQuery;
import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.impl.ContextManagementServiceImpl;
import com.ainexus.service.impl.RAGRetrievalServiceImpl;
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
class RAGRetrievalServiceImplTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private QueryEnhancementService queryEnhancementService;

    private ContextManagementServiceImpl contextManagementService;
    private RAGRetrievalServiceImpl ragRetrievalService;
    private User testUser;

    @BeforeEach
    void setUp() {
        contextManagementService = new ContextManagementServiceImpl();
        ReflectionTestUtils.setField(contextManagementService, "minRelevanceScore", 0.35);
        ReflectionTestUtils.setField(contextManagementService, "maxContextCharacters", 8000);

        ragRetrievalService = new RAGRetrievalServiceImpl(
                semanticSearchService,
                contextManagementService,
                queryEnhancementService
        );
        ReflectionTestUtils.setField(ragRetrievalService, "defaultTopK", 5);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Valid retrieval uses enhanced query and returns assembled context")
    void testSuccessfulRetrieval() {
        when(queryEnhancementService.enhanceQuery("architecture"))
                .thenReturn(EnhancedQuery.of("architecture", "software architecture and microservice design"));

        SearchResultItem item1 = new SearchResultItem(1L, "arch.pdf", 0, 0.88, "AI-Nexus uses vector retrieval.", 31, "application/pdf", "vec-1");
        SearchResponse response = new SearchResponse("software architecture and microservice design", 1L, 1, List.of(item1));

        when(semanticSearchService.search(eq("software architecture and microservice design"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(response);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("architecture", 1L, null, testUser);

        assertNotNull(context);
        assertEquals("architecture", context.query());
        assertEquals(1, context.chunks().size());
        assertEquals("arch.pdf", context.chunks().get(0).filename());
        assertTrue(context.assembledContext().contains("AI-Nexus uses vector retrieval."));
    }

    @Test
    @DisplayName("TEST 2: Empty search results return empty RAGContext with original query")
    void testEmptySearchResults() {
        when(queryEnhancementService.enhanceQuery("unknown"))
                .thenReturn(EnhancedQuery.unchanged("unknown"));

        SearchResponse response = new SearchResponse("unknown", 1L, 0, Collections.emptyList());

        when(semanticSearchService.search(eq("unknown"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(response);

        RAGContext context = ragRetrievalService.retrieveAndAssembleContext("unknown", 1L, null, testUser);

        assertNotNull(context);
        assertTrue(context.chunks().isEmpty());
        assertEquals("", context.assembledContext());
        assertEquals("unknown", context.query());
    }

    @Test
    @DisplayName("TEST 3: Null or blank query throws IllegalArgumentException")
    void testInvalidQuery() {
        assertThrows(IllegalArgumentException.class, () ->
                ragRetrievalService.retrieveAndAssembleContext(null, 1L, null, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                ragRetrievalService.retrieveAndAssembleContext("   ", 1L, null, testUser));
    }

    @Test
    @DisplayName("TEST 4: Null workspaceId throws IllegalArgumentException")
    void testNullWorkspaceId() {
        assertThrows(IllegalArgumentException.class, () ->
                ragRetrievalService.retrieveAndAssembleContext("query", null, null, testUser));
    }
}
