package com.ainexus.agent;

import com.ainexus.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock
    private Agent searchAgent;

    @Mock
    private Agent analysisAgent;

    @Mock
    private Agent knowledgeAgent;

    private AgentOrchestrator orchestrator;
    private User testUser;

    @BeforeEach
    void setUp() {
        when(searchAgent.getAgentType()).thenReturn(AgentType.SEARCH);
        when(analysisAgent.getAgentType()).thenReturn(AgentType.ANALYSIS);
        when(knowledgeAgent.getAgentType()).thenReturn(AgentType.KNOWLEDGE);

        orchestrator = new AgentOrchestrator(List.of(searchAgent, analysisAgent, knowledgeAgent));

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Explicit target SEARCH routes directly to SearchAgent")
    void testExplicitSearchRouting() {
        AgentResult mockResult = AgentResult.success(AgentType.SEARCH, "trace-1", "Search output", List.of(), Map.of());
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class))).thenReturn(mockResult);

        AgentRequest request = AgentRequest.of("Find vacation policy", AgentType.SEARCH, 10L, testUser);
        AgentResult result = orchestrator.orchestrate(request);

        assertNotNull(result);
        assertEquals(AgentType.SEARCH, result.agentType());
        verify(searchAgent, times(1)).execute(any(), any());
        verify(analysisAgent, never()).execute(any(), any());
        verify(knowledgeAgent, never()).execute(any(), any());
    }

    @Test
    @DisplayName("TEST 2: Intent-based routing routes comparison request to AnalysisAgent")
    void testIntentAnalysisRouting() {
        AgentResult mockResult = AgentResult.success(AgentType.ANALYSIS, "trace-2", "Comparison output", List.of(), Map.of());
        when(analysisAgent.execute(any(AgentRequest.class), any(AgentContext.class))).thenReturn(mockResult);

        AgentRequest request = AgentRequest.of("Compare leave policies across departments", AgentType.ORCHESTRATOR, 10L, testUser);
        AgentResult result = orchestrator.orchestrate(request);

        assertNotNull(result);
        assertEquals(AgentType.ANALYSIS, result.agentType());
        verify(analysisAgent, times(1)).execute(any(), any());
    }

    @Test
    @DisplayName("TEST 3: Intent-based routing routes search/lookup keyword request to SearchAgent")
    void testIntentSearchRouting() {
        AgentResult mockResult = AgentResult.success(AgentType.SEARCH, "trace-3", "Search output", List.of(), Map.of());
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class))).thenReturn(mockResult);

        AgentRequest request = AgentRequest.of("Lookup onboarding handbook", AgentType.ORCHESTRATOR, 10L, testUser);
        AgentResult result = orchestrator.orchestrate(request);

        assertNotNull(result);
        assertEquals(AgentType.SEARCH, result.agentType());
        verify(searchAgent, times(1)).execute(any(), any());
    }

    @Test
    @DisplayName("TEST 4: Default inquiry routes to KnowledgeAgent")
    void testDefaultKnowledgeRouting() {
        AgentResult mockResult = AgentResult.success(AgentType.KNOWLEDGE, "trace-4", "Knowledge answer", List.of(), Map.of());
        when(knowledgeAgent.execute(any(AgentRequest.class), any(AgentContext.class))).thenReturn(mockResult);

        AgentRequest request = AgentRequest.of("What is the company mission statement?", AgentType.ORCHESTRATOR, 10L, testUser);
        AgentResult result = orchestrator.orchestrate(request);

        assertNotNull(result);
        assertEquals(AgentType.KNOWLEDGE, result.agentType());
        verify(knowledgeAgent, times(1)).execute(any(), any());
    }

    @Test
    @DisplayName("TEST 5: Unregistered agent type throws controlled AgentException")
    void testUnregisteredAgentHandling() {
        AgentOrchestrator emptyOrchestrator = new AgentOrchestrator(List.of());

        AgentRequest request = AgentRequest.of("Find documents", AgentType.SEARCH, 10L, testUser);
        AgentException ex = assertThrows(AgentException.class, () -> emptyOrchestrator.orchestrate(request));

        assertEquals(AgentType.SEARCH, ex.getAgentType());
        assertTrue(ex.getMessage().contains("No agent implementation available"));
    }

    @Test
    @DisplayName("TEST 6: Trace ID is preserved from request through to execution")
    void testTraceIdPreservation() {
        AgentResult mockResult = AgentResult.success(AgentType.KNOWLEDGE, "custom-trace-99", "Output", List.of(), Map.of());
        when(knowledgeAgent.execute(any(AgentRequest.class), any(AgentContext.class))).thenReturn(mockResult);

        AgentRequest request = new AgentRequest("What is X?", AgentType.KNOWLEDGE, 10L, testUser, "custom-trace-99", Map.of());
        AgentResult result = orchestrator.orchestrate(request);

        assertEquals("custom-trace-99", result.traceId());
    }
}
