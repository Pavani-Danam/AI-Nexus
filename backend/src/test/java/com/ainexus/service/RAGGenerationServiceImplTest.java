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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGGenerationServiceImplTest {

    @Mock
    private RAGRetrievalService retrievalService;

    @Mock
    private RAGPromptBuilder promptBuilder;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock
    private ConversationMemoryService conversationMemoryService;

    private TestableRAGGenerationServiceImpl generationService;
    private User testUser;

    static class TestableRAGGenerationServiceImpl extends RAGGenerationServiceImpl {
        private String mockGeminiResponse = "Default mock answer";

        public TestableRAGGenerationServiceImpl(RAGRetrievalService retrievalService, RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        public void setMockGeminiResponse(String response) {
            this.mockGeminiResponse = response;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            return mockGeminiResponse;
        }
    }

    @BeforeEach
    void setUp() {
        generationService = new TestableRAGGenerationServiceImpl(retrievalService, promptBuilder);
        generationService.setSemanticCacheService(semanticCacheService);
        generationService.setConversationMemoryService(conversationMemoryService);

        ReflectionTestUtils.setField(generationService, "geminiApiKey", "test-gemini-key");
        ReflectionTestUtils.setField(generationService, "generationModel", "gemini-1.5-flash");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
    }

    @Test
    @DisplayName("TEST 1: Successful RAG Generation with Chunks, Prompt, Citations, and LLM output")
    void testSuccessfulGenerationWithContext() {
        List<RAGChunk> chunks = List.of(
                new RAGChunk(10L, "handbook.pdf", 0, 0.95, "Leave policy is 20 days per year.", 35),
                new RAGChunk(10L, "handbook.pdf", 1, 0.88, "Sick leave is 10 days per year.", 31)
        );

        RAGContext mockContext = new RAGContext("leave policy", 1L, chunks, "Assembled Context", 66);
        RAGPrompt mockPrompt = new RAGPrompt("System Instructions", "Formatted Context", "leave policy", "Full Prompt", true);

        when(semanticCacheService.lookup("leave policy", 1L, testUser)).thenReturn(Optional.empty());
        when(retrievalService.retrieveAndAssembleContext("leave policy", 1L, 5, testUser)).thenReturn(mockContext);
        when(promptBuilder.buildPrompt(eq("leave policy"), eq(mockContext), any())).thenReturn(mockPrompt);
        generationService.setMockGeminiResponse("Employees are entitled to 20 days of annual leave and 10 days of sick leave.");

        RAGResponse response = generationService.generateAnswer("leave policy", 1L, 5, testUser);

        assertNotNull(response);
        assertEquals("Employees are entitled to 20 days of annual leave and 10 days of sick leave.", response.answer());
        assertEquals("leave policy", response.query());
        assertEquals(1L, response.workspaceId());
        assertTrue(response.hasContext());
        assertEquals(2, response.citations().size());
        assertEquals(10L, response.citations().get(0).documentId());
        assertEquals("handbook.pdf", response.citations().get(0).filename());

        verify(semanticCacheService, times(1)).store(eq("leave policy"), eq(1L), eq(testUser), any(RAGResponse.class));
    }

    @Test
    @DisplayName("TEST 2: Semantic Cache Hit immediately returns cached RAGResponse without calling retrieval/LLM")
    void testSemanticCacheHitBypassesLLM() {
        RAGResponse cached = new RAGResponse("Cached answer.", "leave policy", 1L, Collections.emptyList(), Collections.emptyList(), true);
        when(semanticCacheService.lookup("leave policy", 1L, testUser)).thenReturn(Optional.of(cached));

        RAGResponse response = generationService.generateAnswer("leave policy", 1L, 5, testUser);

        assertNotNull(response);
        assertEquals("Cached answer.", response.answer());
        verifyNoInteractions(retrievalService);
        verifyNoInteractions(promptBuilder);
    }

    @Test
    @DisplayName("TEST 3: Semantic Cache Miss proceeds through full RAG generation pipeline")
    void testSemanticCacheMissPipelineExecution() {
        List<RAGChunk> chunks = List.of(new RAGChunk(10L, "arch.pdf", 0, 0.90, "AI-Nexus microservices.", 22));
        RAGContext mockContext = new RAGContext("architecture", 1L, chunks, "[1] AI-Nexus microservices.", 22);
        RAGPrompt mockPrompt = new RAGPrompt("System", "[1] AI-Nexus", "architecture", "Full", true);

        when(semanticCacheService.lookup("architecture", 1L, testUser)).thenReturn(Optional.empty());
        when(retrievalService.retrieveAndAssembleContext("architecture", 1L, 5, testUser)).thenReturn(mockContext);
        when(promptBuilder.buildPrompt(eq("architecture"), eq(mockContext), any())).thenReturn(mockPrompt);
        generationService.setMockGeminiResponse("Microservices architecture.");

        RAGResponse response = generationService.generateAnswer("architecture", 1L, 5, testUser);

        assertNotNull(response);
        assertEquals("Microservices architecture.", response.answer());
        verify(semanticCacheService, times(1)).store(eq("architecture"), eq(1L), eq(testUser), any(RAGResponse.class));
    }

    @Test
    @DisplayName("TEST 4: Conversational Memory bypasses semantic cache and injects dialogue history")
    void testConversationalMemoryBypassesCache() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "Prior question", LocalDateTime.now().minusMinutes(2)),
                MemoryMessage.of(2L, "ASSISTANT", "Prior answer", LocalDateTime.now().minusMinutes(1))
        );

        ConversationMemory mockMemory = new ConversationMemory(
                100L, 1L, messages, "USER:\nPrior question\n\nASSISTANT:\nPrior answer", messages.size()
        );

        when(conversationMemoryService.getMemory(100L, 1L, testUser)).thenReturn(mockMemory);

        List<RAGChunk> chunks = List.of(new RAGChunk(10L, "doc.pdf", 0, 0.90, "Content", 7));
        RAGContext mockContext = new RAGContext("follow up", 1L, chunks, "Content", 7);
        RAGPrompt mockPrompt = new RAGPrompt("System", "Content", "follow up", "Full with history", true);

        when(retrievalService.retrieveAndAssembleContext("follow up", 1L, 5, testUser)).thenReturn(mockContext);
        when(promptBuilder.buildPrompt(eq("follow up"), eq(mockContext), eq(mockMemory))).thenReturn(mockPrompt);
        generationService.setMockGeminiResponse("Follow up answer.");

        RAGResponse response = generationService.generateAnswer("follow up", 1L, 5, 100L, testUser);

        assertNotNull(response);
        assertEquals("Follow up answer.", response.answer());
        verify(semanticCacheService, never()).lookup(anyString(), anyLong(), any(User.class));
        verify(semanticCacheService, never()).store(anyString(), anyLong(), any(User.class), any(RAGResponse.class));
    }

    @Test
    @DisplayName("TEST 5: Invalid arguments throw IllegalArgumentException")
    void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateAnswer(null, 1L, 5, testUser));

        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateAnswer("   ", 1L, 5, testUser));

        assertThrows(IllegalArgumentException.class, () ->
                generationService.generateAnswer("valid query", null, 5, testUser));
    }
}
