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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGGenerationServiceImplTest {

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
        private String mockLlmResponse = "This is a synthesized test response.";

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

        ReflectionTestUtils.setField(ragGenerationService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(ragGenerationService, "generationModel", "gemini-1.5-flash");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: RAG generation with conversation context uses MemoryRetrievalService")
    void testConversationMemoryIntegratedWithRelevanceRetrieval() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "What is policy?", LocalDateTime.now().minusMinutes(2)),
                MemoryMessage.of(2L, "ASSISTANT", "Policy X.", LocalDateTime.now().minusMinutes(1))
        );
        ConversationMemory relevantMemory = new ConversationMemory(100L, 1L, messages, "USER: What is policy?\nASSISTANT: Policy X.", 2);

        when(memoryRetrievalService.retrieveRelevantMemory(eq("follow up"), eq(100L), eq(1L), eq(testUser)))
                .thenReturn(relevantMemory);
        when(conversationQueryRewriteService.rewriteToStandaloneQuery(eq("follow up"), eq(relevantMemory), eq(1L), eq(testUser)))
                .thenReturn("What are the specifics of Policy X?");

        RAGChunk chunk = new RAGChunk(10L, "doc.pdf", 1, 0.9, "Policy details...", 17);
        RAGContext context = new RAGContext("What are the specifics of Policy X?", 1L, List.of(chunk), "Context text", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("What are the specifics of Policy X?"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System prompt", "User prompt", "USER: What is policy?\nASSISTANT: Policy X.", "System + User prompt", true);
        when(ragPromptBuilder.buildPrompt(eq("follow up"), eq(context), eq(relevantMemory)))
                .thenReturn(prompt);

        RAGResponse response = ragGenerationService.generateAnswer("follow up", 1L, 5, 100L, testUser);

        assertNotNull(response);
        assertEquals(1, response.citations().size());
        assertEquals("This is a synthesized test response.", response.answer());
        verify(semanticCacheService, never()).lookup(anyString(), any(), any());
    }

    @Test
    @DisplayName("TEST 2: Standalone query without conversation uses SemanticCache")
    void testStandaloneQueryUsesSemanticCache() {
        RAGChunk chunk = new RAGChunk(10L, "handbook.pdf", 2, 0.95, "Leave policy details", 20);
        RAGContext context = new RAGContext("leave policy", 1L, List.of(chunk), "Context text", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("leave policy"), eq(1L), eq(5), eq(testUser)))
                .thenReturn(context);

        RAGPrompt prompt = new RAGPrompt("System", "User", null, "Full Prompt", true);
        when(ragPromptBuilder.buildPrompt(eq("leave policy"), eq(context), isNull()))
                .thenReturn(prompt);
        when(semanticCacheService.lookup("leave policy", 1L, testUser)).thenReturn(Optional.empty());

        RAGResponse response = ragGenerationService.generateAnswer("leave policy", 1L, 5, null, testUser);

        assertNotNull(response);
        verify(semanticCacheService, times(1)).store(eq("leave policy"), eq(1L), eq(testUser), any(RAGResponse.class));
    }

    @Test
    @DisplayName("TEST 3: Semantic Cache HIT returns immediately without LLM call")
    void testSemanticCacheHitReturnsImmediately() {
        RAGResponse cached = new RAGResponse("Cached Answer", "leave policy", 1L, List.of(), List.of(), true);
        when(semanticCacheService.lookup("leave policy", 1L, testUser)).thenReturn(Optional.of(cached));

        RAGResponse response = ragGenerationService.generateAnswer("leave policy", 1L, 5, null, testUser);

        assertNotNull(response);
        assertEquals("Cached Answer", response.answer());
        verifyNoInteractions(ragRetrievalService);
        verifyNoInteractions(ragPromptBuilder);
    }
}
