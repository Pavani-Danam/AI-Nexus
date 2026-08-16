package com.ainexus.agent;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGCitation;
import com.ainexus.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentFoundationTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: AgentRequest can be created with valid data and auto-generates traceId")
    void testAgentRequestCreation() {
        AgentRequest request = AgentRequest.of("Summarize docs", AgentType.KNOWLEDGE, 10L, testUser);

        assertNotNull(request);
        assertEquals("Summarize docs", request.query());
        assertEquals(AgentType.KNOWLEDGE, request.targetAgent());
        assertEquals(10L, request.workspaceId());
        assertEquals(testUser, request.user());
        assertNotNull(request.traceId());
        assertFalse(request.traceId().isBlank());
        assertTrue(request.parameters().isEmpty());
    }

    @Test
    @DisplayName("TEST 1b: AgentRequest validation rejects invalid or blank inputs")
    void testAgentRequestValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new AgentRequest(null, AgentType.SEARCH, 10L, testUser, "trace-1", Map.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new AgentRequest("   ", AgentType.SEARCH, 10L, testUser, "trace-1", Map.of()));
        assertThrows(NullPointerException.class, () ->
                new AgentRequest("query", null, 10L, testUser, "trace-1", Map.of()));
        assertThrows(NullPointerException.class, () ->
                new AgentRequest("query", AgentType.SEARCH, null, testUser, "trace-1", Map.of()));
        assertThrows(NullPointerException.class, () ->
                new AgentRequest("query", AgentType.SEARCH, 10L, null, "trace-1", Map.of()));
    }

    @Test
    @DisplayName("TEST 2: AgentContext preserves workspace, user, and trace information")
    void testAgentContextPreservation() {
        AgentContext context = new AgentContext("trace-xyz", 10L, testUser);

        assertEquals("trace-xyz", context.getTraceId());
        assertEquals(10L, context.getWorkspaceId());
        assertEquals(testUser, context.getUser());

        context.setMetadata("step", "retrieval");
        assertEquals("retrieval", context.getMetadata().get("step"));

        RAGChunk chunk = new RAGChunk(5L, "test.pdf", 0, 0.9, "Sample content", 14);
        context.addRetrievedChunks(List.of(chunk));
        assertEquals(1, context.getRetrievedChunks().size());
        assertEquals("test.pdf", context.getRetrievedChunks().get(0).filename());

        context.setIntermediateResult("score", 0.95);
        assertEquals(0.95, context.getIntermediateResults().get("score"));
    }

    @Test
    @DisplayName("TEST 3: AgentResult represents successful execution")
    void testAgentResultSuccess() {
        RAGChunk chunk = new RAGChunk(1L, "doc.pdf", 0, 0.92, "Content", 7);
        RAGCitation citation = RAGCitation.fromChunk(chunk);

        AgentResult result = AgentResult.success(
                AgentType.KNOWLEDGE,
                "trace-123",
                "Answer output",
                List.of(citation),
                Map.of("tokens", 120)
        );

        assertTrue(result.success());
        assertEquals(AgentType.KNOWLEDGE, result.agentType());
        assertEquals("trace-123", result.traceId());
        assertEquals("Answer output", result.output());
        assertEquals(1, result.citations().size());
        assertEquals("doc-1-chunk-0", result.citations().get(0).sourceId());
        assertEquals(120, result.metadata().get("tokens"));
        assertNull(result.errorMessage());
    }

    @Test
    @DisplayName("TEST 4: AgentResult represents failure safely without exposing internal details")
    void testAgentResultFailure() {
        AgentResult failure = AgentResult.failure(AgentType.SEARCH, "trace-456", "Service temporarily unavailable");

        assertFalse(failure.success());
        assertEquals(AgentType.SEARCH, failure.agentType());
        assertEquals("trace-456", failure.traceId());
        assertNull(failure.output());
        assertTrue(failure.citations().isEmpty());
        assertEquals("Service temporarily unavailable", failure.errorMessage());
    }

    @Test
    @DisplayName("TEST 5: AgentType contains planned agent types")
    void testAgentTypes() {
        assertNotNull(AgentType.valueOf("ORCHESTRATOR"));
        assertNotNull(AgentType.valueOf("SEARCH"));
        assertNotNull(AgentType.valueOf("ANALYSIS"));
        assertNotNull(AgentType.valueOf("KNOWLEDGE"));
        assertEquals(4, AgentType.values().length);
    }

    @Test
    @DisplayName("TEST 6: Agent interface can be implemented by a concrete test agent")
    void testAgentInterfaceImplementation() {
        Agent testAgent = new Agent() {
            @Override
            public AgentType getAgentType() {
                return AgentType.KNOWLEDGE;
            }

            @Override
            public AgentResult execute(AgentRequest request, AgentContext context) {
                return AgentResult.success(
                        getAgentType(),
                        context.getTraceId(),
                        "Processed: " + request.query(),
                        List.of(),
                        Map.of()
                );
            }
        };

        assertTrue(testAgent.supports(AgentType.KNOWLEDGE));
        assertFalse(testAgent.supports(AgentType.SEARCH));

        AgentRequest request = AgentRequest.of("Hello Agent", AgentType.KNOWLEDGE, 1L, testUser);
        AgentContext context = new AgentContext("trace-agent-1", 1L, testUser);
        AgentResult result = testAgent.execute(request, context);

        assertTrue(result.success());
        assertEquals("Processed: Hello Agent", result.output());
        assertEquals("trace-agent-1", result.traceId());
    }

    @Test
    @DisplayName("TEST 7: AgentException carries traceId and agentType")
    void testAgentException() {
        AgentException ex = new AgentException("Retrieval failed", AgentType.SEARCH, "trace-999");

        assertEquals("Retrieval failed", ex.getMessage());
        assertEquals(AgentType.SEARCH, ex.getAgentType());
        assertEquals("trace-999", ex.getTraceId());
    }
}
