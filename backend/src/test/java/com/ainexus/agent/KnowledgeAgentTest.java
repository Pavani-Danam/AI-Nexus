package com.ainexus.agent;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.entity.User;
import com.ainexus.service.RAGRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeAgentTest {

    @Mock
    private RAGRetrievalService ragRetrievalService;

    private TestableKnowledgeAgent knowledgeAgent;
    private User testUser;

    static class TestableKnowledgeAgent extends KnowledgeAgent {
        private String mockGeminiAnswer = "AI-Nexus uses vector search.";

        public TestableKnowledgeAgent(RAGRetrievalService retrievalService, RestClient.Builder restClientBuilder) {
            super(retrievalService, restClientBuilder);
        }

        public void setMockGeminiAnswer(String answer) {
            this.mockGeminiAnswer = answer;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            return mockGeminiAnswer;
        }
    }

    @BeforeEach
    void setUp() {
        knowledgeAgent = new TestableKnowledgeAgent(ragRetrievalService, RestClient.builder());
        ReflectionTestUtils.setField(knowledgeAgent, "geminiApiKey", "valid-gemini-key");
        ReflectionTestUtils.setField(knowledgeAgent, "generationModel", "gemini-1.5-flash");
        ReflectionTestUtils.setField(knowledgeAgent, "temperature", 0.2);
        ReflectionTestUtils.setField(knowledgeAgent, "maxOutputTokens", 2048);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Knowledge query with valid context produces answer and citations")
    void testKnowledgeQuerySuccess() {
        RAGChunk chunk = new RAGChunk(1L, "security.pdf", 0, 0.94, "MFA is required for all admin access.", 36);
        RAGContext ragContext = new RAGContext("MFA policy", 10L, List.of(chunk), "[Source 1: security.pdf]\nMFA is required.", 45);

        when(ragRetrievalService.retrieveAndAssembleContext("What is our MFA policy?", 10L, null, testUser))
                .thenReturn(ragContext);

        knowledgeAgent.setMockGeminiAnswer("Multi-factor authentication is mandatory for admin access.");

        AgentRequest request = AgentRequest.of("What is our MFA policy?", AgentType.KNOWLEDGE, 10L, testUser);
        AgentContext context = new AgentContext(request.traceId(), 10L, testUser);

        AgentResult result = knowledgeAgent.execute(request, context);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(AgentType.KNOWLEDGE, result.agentType());
        assertEquals("Multi-factor authentication is mandatory for admin access.", result.output());
        assertEquals(1, result.citations().size());
        assertEquals("security.pdf", result.citations().get(0).filename());
        assertTrue((Boolean) result.metadata().get("hasContext"));
    }

    @Test
    @DisplayName("TEST 2: Empty context produces controlled insufficient information result")
    void testEmptyContextHandling() {
        when(ragRetrievalService.retrieveAndAssembleContext(anyString(), anyLong(), isNull(), any()))
                .thenReturn(new RAGContext("query", 10L, Collections.emptyList(), "", 0));

        AgentRequest request = AgentRequest.of("Unknown question", AgentType.KNOWLEDGE, 10L, testUser);
        AgentContext context = new AgentContext(request.traceId(), 10L, testUser);

        AgentResult result = knowledgeAgent.execute(request, context);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("I do not have sufficient information in the knowledge base to answer this question.", result.output());
        assertTrue(result.citations().isEmpty());
        assertFalse((Boolean) result.metadata().get("hasContext"));
    }

    @Test
    @DisplayName("TEST 3: AgentType is KNOWLEDGE")
    void testAgentType() {
        assertEquals(AgentType.KNOWLEDGE, knowledgeAgent.getAgentType());
        assertTrue(knowledgeAgent.supports(AgentType.KNOWLEDGE));
        assertFalse(knowledgeAgent.supports(AgentType.SEARCH));
    }
}
