package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.MemoryMessage;
import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.service.impl.RAGGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryContextIntegrationTest {

    @Mock
    private RAGRetrievalService ragRetrievalService;

    @Mock
    private RAGPromptBuilder ragPromptBuilder;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @Mock
    private ConversationQueryRewriteService conversationQueryRewriteService;

    @Mock
    private MemoryRetrievalService memoryRetrievalService;

    private TestableRAGGenerationServiceImpl ragGenerationService;
    private User testUser;

    static class TestableRAGGenerationServiceImpl extends RAGGenerationServiceImpl {
        private String mockLlmResponse = "Grounded synthesized response.";

        public TestableRAGGenerationServiceImpl(RAGRetrievalService retrievalService, RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        public void setMockLlmResponse(String response) {
            this.mockLlmResponse = response;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            return mockLlmResponse;
        }
    }

    @BeforeEach
    void setUp() {
        ragGenerationService = new TestableRAGGenerationServiceImpl(ragRetrievalService, ragPromptBuilder);
        ragGenerationService.setSemanticCacheService(semanticCacheService);
        ragGenerationService.setConversationMemoryService(conversationMemoryService);
        ragGenerationService.setConversationQueryRewriteService(conversationQueryRewriteService);
        ragGenerationService.setMemoryRetrievalService(memoryRetrievalService);

        ReflectionTestUtils.setField(ragGenerationService, "geminiApiKey", "test-key");
        ReflectionTestUtils.setField(ragGenerationService, "generationModel", "gemini-1.5-flash");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Normal question without conversation context executes standard RAG")
    void testNormalQuestionExecutesStandardRAG() {
        RAGChunk chunk = new RAGChunk(10L, "handbook.pdf", 1, 0.92, "Standard office hours: 9am-5pm.", 30);
        RAGContext context = new RAGContext("What are office hours?", 1L, List.of(chunk), "Context", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("What are office hours?"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System", "Context", "What are office hours?", "Full Prompt", true);
        when(ragPromptBuilder.buildPrompt(eq("What are office hours?"), eq(context), isNull()))
                .thenReturn(prompt);
        when(semanticCacheService.lookup("What are office hours?", 1L, testUser)).thenReturn(Optional.empty());

        RAGResponse response = ragGenerationService.generateAnswer("What are office hours?", 1L, 5, null, testUser);

        assertNotNull(response);
        assertEquals(1, response.citations().size());
        assertEquals("Grounded synthesized response.", response.answer());
        verifyNoInteractions(memoryRetrievalService);
        verifyNoInteractions(conversationQueryRewriteService);
    }

    @Test
    @DisplayName("TEST 2: Follow-up question uses relevant conversation memory and query rewriting")
    void testFollowUpQuestionUsesMemoryAndRewriting() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "What is the company leave policy?", LocalDateTime.now().minusMinutes(3)),
                MemoryMessage.of(2L, "ASSISTANT", "The company offers 20 days paid leave.", LocalDateTime.now().minusMinutes(2))
        );
        String formatted = "USER: What is the company leave policy?\nASSISTANT: The company offers 20 days paid leave.";
        ConversationMemory relevantMemory = new ConversationMemory(100L, 1L, messages, formatted, 2);

        when(memoryRetrievalService.retrieveRelevantMemory(eq("How many days can I take?"), eq(100L), eq(1L), eq(testUser)))
                .thenReturn(relevantMemory);
        when(conversationQueryRewriteService.rewriteToStandaloneQuery(eq("How many days can I take?"), eq(relevantMemory), eq(1L), eq(testUser)))
                .thenReturn("How many paid annual leave days can employees take under the leave policy?");

        RAGChunk chunk = new RAGChunk(10L, "policy.pdf", 2, 0.96, "Annual leave allowance is capped at 20 days per year.", 54);
        RAGContext context = new RAGContext("How many paid annual leave days can employees take under the leave policy?", 1L, List.of(chunk), "Context", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("How many paid annual leave days can employees take under the leave policy?"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System", "Context with Memory", "How many days can I take?", "Full Prompt with Memory", true);
        when(ragPromptBuilder.buildPrompt(eq("How many days can I take?"), eq(context), eq(relevantMemory)))
                .thenReturn(prompt);

        RAGResponse response = ragGenerationService.generateAnswer("How many days can I take?", 1L, 5, 100L, testUser);

        assertNotNull(response);
        assertEquals(1, response.citations().size());
        assertEquals("Grounded synthesized response.", response.answer());
        verify(memoryRetrievalService).retrieveRelevantMemory(eq("How many days can I take?"), eq(100L), eq(1L), eq(testUser));
        verify(conversationQueryRewriteService).rewriteToStandaloneQuery(eq("How many days can I take?"), eq(relevantMemory), eq(1L), eq(testUser));
    }

    @Test
    @DisplayName("TEST 3: Irrelevant history is filtered and only relevant memory influences generation")
    void testIrrelevantHistoryIsFiltered() {
        List<MemoryMessage> filteredMessages = List.of(
                MemoryMessage.of(5L, "USER", "Can you explain the maternity leave terms?", LocalDateTime.now().minusMinutes(2)),
                MemoryMessage.of(6L, "ASSISTANT", "Maternity leave is 26 weeks paid.", LocalDateTime.now().minusMinutes(1))
        );
        String filteredFormatted = "USER: Can you explain the maternity leave terms?\nASSISTANT: Maternity leave is 26 weeks paid.";
        ConversationMemory filteredMemory = new ConversationMemory(100L, 1L, filteredMessages, filteredFormatted, 2);

        when(memoryRetrievalService.retrieveRelevantMemory(eq("How long is maternity leave?"), eq(100L), eq(1L), eq(testUser)))
                .thenReturn(filteredMemory);
        when(conversationQueryRewriteService.rewriteToStandaloneQuery(eq("How long is maternity leave?"), eq(filteredMemory), eq(1L), eq(testUser)))
                .thenReturn("How long is the company maternity leave duration?");

        RAGChunk chunk = new RAGChunk(12L, "maternity.pdf", 1, 0.94, "Maternity benefits include 26 paid weeks.", 42);
        RAGContext context = new RAGContext("How long is the company maternity leave duration?", 1L, List.of(chunk), "Context", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("How long is the company maternity leave duration?"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System", "Context", "How long is maternity leave?", "Prompt", true);
        when(ragPromptBuilder.buildPrompt(eq("How long is maternity leave?"), eq(context), eq(filteredMemory)))
                .thenReturn(prompt);

        RAGResponse response = ragGenerationService.generateAnswer("How long is maternity leave?", 1L, 5, 100L, testUser);

        assertNotNull(response);
        verify(memoryRetrievalService).retrieveRelevantMemory(eq("How long is maternity leave?"), eq(100L), eq(1L), eq(testUser));
    }

    @Test
    @DisplayName("TEST 4: Empty conversation memory falls back smoothly to normal RAG")
    void testEmptyConversationMemoryFallsBackToNormalRAG() {
        ConversationMemory emptyMemory = ConversationMemory.empty(200L, 1L);
        when(memoryRetrievalService.retrieveRelevantMemory(eq("What is 401k match?"), eq(200L), eq(1L), eq(testUser)))
                .thenReturn(emptyMemory);

        RAGChunk chunk = new RAGChunk(15L, "benefits.pdf", 1, 0.89, "401k matching is up to 5%.", 26);
        RAGContext context = new RAGContext("What is 401k match?", 1L, List.of(chunk), "Context", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("What is 401k match?"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System", "Context", "What is 401k match?", "Prompt", true);
        when(ragPromptBuilder.buildPrompt(eq("What is 401k match?"), eq(context), eq(emptyMemory)))
                .thenReturn(prompt);

        RAGResponse response = ragGenerationService.generateAnswer("What is 401k match?", 1L, 5, 200L, testUser);

        assertNotNull(response);
        assertEquals("Grounded synthesized response.", response.answer());
    }

    @Test
    @DisplayName("TEST 5: Memory retrieval failure gracefully falls back to direct RAG generation")
    void testMemoryRetrievalFailureGracefulFallback() {
        when(memoryRetrievalService.retrieveRelevantMemory(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("Memory database connectivity timeout"));

        RAGChunk chunk = new RAGChunk(10L, "policy.pdf", 1, 0.90, "Leave policy rules...", 21);
        RAGContext context = new RAGContext("leave policy", 1L, List.of(chunk), "Context", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("leave policy"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System", "Context", "leave policy", "Prompt", true);
        when(ragPromptBuilder.buildPrompt(eq("leave policy"), eq(context), isNull()))
                .thenReturn(prompt);

        RAGResponse response = ragGenerationService.generateAnswer("leave policy", 1L, 5, 300L, testUser);

        assertNotNull(response);
        assertEquals("Grounded synthesized response.", response.answer());
    }
}
