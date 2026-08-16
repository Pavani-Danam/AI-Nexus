package com.ainexus.agent;

import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchAgentTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @InjectMocks
    private SearchAgent searchAgent;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Valid query with matching documents returns structured results and updates context")
    void testSearchAgentSuccessWithResults() {
        SearchResultItem item1 = new SearchResultItem(
                10L, "guide.pdf", 0, 0.92, "Spring Boot setup guide", 22, "application/pdf", "vec-1"
        );
        SearchResponse mockResponse = new SearchResponse("Spring Boot", 5L, 1, List.of(item1));

        when(semanticSearchService.search(eq("Spring Boot"), eq(5L), isNull(), eq(testUser)))
                .thenReturn(mockResponse);

        AgentRequest request = AgentRequest.of("Spring Boot", AgentType.SEARCH, 5L, testUser);
        AgentContext context = new AgentContext(request.traceId(), 5L, testUser);

        AgentResult result = searchAgent.execute(request, context);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(AgentType.SEARCH, result.agentType());
        assertEquals(request.traceId(), result.traceId());
        assertTrue(result.output().contains("1 relevant document chunk(s)"));
        assertEquals(1, result.metadata().get("totalResults"));

        // Verify context was populated for downstream agents
        assertEquals(1, context.getRetrievedChunks().size());
        assertEquals(10L, context.getRetrievedChunks().get(0).documentId());
        assertEquals("guide.pdf", context.getRetrievedChunks().get(0).filename());
        assertEquals(0.92, context.getRetrievedChunks().get(0).score());
    }

    @Test
    @DisplayName("TEST 2: Query with no matching documents returns empty results safely")
    void testSearchAgentEmptyResults() {
        SearchResponse mockResponse = new SearchResponse("Unmatched query", 5L, 0, Collections.emptyList());

        when(semanticSearchService.search(eq("Unmatched query"), eq(5L), isNull(), eq(testUser)))
                .thenReturn(mockResponse);

        AgentRequest request = AgentRequest.of("Unmatched query", AgentType.SEARCH, 5L, testUser);
        AgentContext context = new AgentContext(request.traceId(), 5L, testUser);

        AgentResult result = searchAgent.execute(request, context);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(0, result.metadata().get("totalResults"));
        assertTrue(context.getRetrievedChunks().isEmpty());
    }

    @Test
    @DisplayName("TEST 3: Blank or null query throws validation exception")
    void testBlankQueryValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentRequest.of("", AgentType.SEARCH, 5L, testUser));

        assertThrows(IllegalArgumentException.class, () ->
                AgentRequest.of("   ", AgentType.SEARCH, 5L, testUser));
    }

    @Test
    @DisplayName("TEST 4: Custom topK parameter is passed to search service")
    void testCustomTopKParameter() {
        AgentRequest request = new AgentRequest(
                "Kubernetes",
                AgentType.SEARCH,
                10L,
                testUser,
                "custom-trace",
                Map.of("topK", 8)
        );
        AgentContext context = new AgentContext("custom-trace", 10L, testUser);

        SearchResponse mockResponse = new SearchResponse("Kubernetes", 10L, 0, Collections.emptyList());
        when(semanticSearchService.search("Kubernetes", 10L, 8, testUser)).thenReturn(mockResponse);

        AgentResult result = searchAgent.execute(request, context);

        assertNotNull(result);
        assertTrue(result.success());
        verify(semanticSearchService, times(1)).search("Kubernetes", 10L, 8, testUser);
    }

    @Test
    @DisplayName("TEST 5: Underlying search service failure is caught and wrapped in AgentException")
    void testSearchServiceFailure() {
        when(semanticSearchService.search(anyString(), anyLong(), any(), any()))
                .thenThrow(new RuntimeException("Vector database timeout"));

        AgentRequest request = AgentRequest.of("Failing query", AgentType.SEARCH, 5L, testUser);
        AgentContext context = new AgentContext(request.traceId(), 5L, testUser);

        AgentException ex = assertThrows(AgentException.class, () ->
                searchAgent.execute(request, context));

        assertEquals(AgentType.SEARCH, ex.getAgentType());
        assertEquals(request.traceId(), ex.getTraceId());
        assertTrue(ex.getMessage().contains("Vector database timeout"));
    }

    @Test
    @DisplayName("TEST 6: AgentType is SEARCH")
    void testAgentType() {
        assertEquals(AgentType.SEARCH, searchAgent.getAgentType());
        assertTrue(searchAgent.supports(AgentType.SEARCH));
        assertFalse(searchAgent.supports(AgentType.KNOWLEDGE));
    }
}
