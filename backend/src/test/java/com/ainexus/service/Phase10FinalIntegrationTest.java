package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.Conversation;
import com.ainexus.entity.Message;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.ConversationRepository;
import com.ainexus.repository.MessageRepository;
import com.ainexus.service.impl.ConversationMemoryServiceImpl;
import com.ainexus.service.impl.ConversationQueryRewriteServiceImpl;
import com.ainexus.service.impl.MemoryRetrievalServiceImpl;
import com.ainexus.service.impl.RAGGenerationServiceImpl;
import com.ainexus.service.impl.RAGPromptBuilderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase10FinalIntegrationTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RAGRetrievalService ragRetrievalService;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock
    private ConversationSummaryService conversationSummaryService;

    private ConversationMemoryServiceImpl conversationMemoryService;
    private MemoryRetrievalServiceImpl memoryRetrievalService;
    private RAGPromptBuilderImpl ragPromptBuilder;
    private TestableRAGGenerationService ragGenerationService;

    private User testUserA;
    private User testUserB;
    private Workspace testWorkspaceA;
    private Workspace testWorkspaceB;
    private Conversation conversationA;

    static class TestableRAGGenerationService extends RAGGenerationServiceImpl {
        private String stubbedAnswer = "Synthesized grounded answer.";

        public TestableRAGGenerationService(RAGRetrievalService retrievalService, RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            return stubbedAnswer;
        }
    }

    static class TestableQueryRewriteService extends ConversationQueryRewriteServiceImpl {
        @Override
        protected String callGeminiRewrite(String prompt) {
            return "What are the specific eligibility requirements for the annual leave policy?";
        }
    }

    @BeforeEach
    void setUp() {
        testUserA = new User();
        testUserA.setId(101L);
        testUserA.setUsername("userA");

        testUserB = new User();
        testUserB.setId(102L);
        testUserB.setUsername("userB");

        testWorkspaceA = new Workspace();
        testWorkspaceA.setId(1L);
        testWorkspaceA.setName("Workspace A");

        testWorkspaceB = new Workspace();
        testWorkspaceB.setId(2L);
        testWorkspaceB.setName("Workspace B");

        conversationA = new Conversation();
        conversationA.setId(500L);
        conversationA.setUser(testUserA);
        conversationA.setWorkspace(testWorkspaceA);
        conversationA.setTitle("Leave Policy Discussion");

        conversationMemoryService = new ConversationMemoryServiceImpl(conversationRepository, messageRepository);
        ReflectionTestUtils.setField(conversationMemoryService, "maxMessages", 10);
        ReflectionTestUtils.setField(conversationMemoryService, "recentMessagesWindow", 6);
        conversationMemoryService.setConversationSummaryService(conversationSummaryService);

        memoryRetrievalService = new MemoryRetrievalServiceImpl(conversationMemoryService);
        ReflectionTestUtils.setField(memoryRetrievalService, "maxRelevantMessages", 6);

        TestableQueryRewriteService queryRewriteService = new TestableQueryRewriteService();
        ReflectionTestUtils.setField(queryRewriteService, "geminiApiKey", "test-key");

        ragPromptBuilder = new RAGPromptBuilderImpl();

        ragGenerationService = new TestableRAGGenerationService(ragRetrievalService, ragPromptBuilder);
        ragGenerationService.setConversationMemoryService(conversationMemoryService);
        ragGenerationService.setMemoryRetrievalService(memoryRetrievalService);
        ragGenerationService.setConversationQueryRewriteService(queryRewriteService);
        ragGenerationService.setSemanticCacheService(semanticCacheService);
        ReflectionTestUtils.setField(ragGenerationService, "geminiApiKey", "test-key");
    }

    @Test
    @DisplayName("INTEGRATION TEST 1: Full multi-turn conversational RAG pipeline with query rewrite and citations")
    void testFullMultiTurnConversationalRAGFlow() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversationA));

        List<Message> messageList = List.of(
                new Message(1L, conversationA, "USER", "What is the annual leave policy?", LocalDateTime.now().minusMinutes(2)),
                new Message(2L, conversationA, "ASSISTANT", "Employees receive 20 annual paid leave days.", LocalDateTime.now().minusMinutes(1))
        );
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(500L)).thenReturn(messageList);

        RAGChunk policyChunk = new RAGChunk(10L, "employee_handbook.pdf", 3, 0.95, "All full-time employees receive 20 days paid leave.", 50);
        RAGContext ragContext = new RAGContext("What are the specific eligibility requirements for the annual leave policy?", 1L, List.of(policyChunk), "Context", 1);
        when(ragRetrievalService.retrieveAndAssembleContext(anyString(), eq(1L), eq(5), eq(testUserA))).thenReturn(ragContext);

        RAGResponse response = ragGenerationService.generateAnswer("Who is eligible?", 1L, 5, 500L, testUserA);

        assertNotNull(response);
        assertEquals("Who is eligible?", response.query());
        assertEquals(1L, response.workspaceId());
        assertEquals(1, response.citations().size());
        assertEquals("employee_handbook.pdf", response.citations().get(0).filename());
        assertTrue(response.hasContext());
        verify(semanticCacheService, never()).lookup(any(), any(), any());
    }

    @Test
    @DisplayName("INTEGRATION TEST 2: Long conversation triggers summarization while keeping recent message window")
    void testLongConversationSummarizationFlow() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversationA));
        when(conversationSummaryService.getOrUpdateSummary(eq(conversationA), anyList(), eq(testUserA)))
                .thenReturn("Summary: Prior discussion covered company vacation allotments.");

        List<Message> longHistory = new ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            longHistory.add(new Message(i, conversationA, i % 2 == 1 ? "USER" : "ASSISTANT", "Turn " + i + " content", LocalDateTime.now().minusMinutes(20 - i)));
        }
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(500L)).thenReturn(longHistory);

        ConversationMemory memory = conversationMemoryService.getMemory(500L, 1L, testUserA);

        assertNotNull(memory);
        assertEquals(500L, memory.conversationId());
        assertEquals(6, memory.messages().size());
        assertTrue(memory.formattedHistory().contains("CONVERSATION SUMMARY:"));
        assertTrue(memory.formattedHistory().contains("Turn 12 content"));
    }

    @Test
    @DisplayName("INTEGRATION TEST 3: Memory security isolation denies unauthorized cross-user access")
    void testMemorySecurityCrossUserDenial() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversationA));

        assertThrows(UnauthorizedAccessException.class, () ->
                ragGenerationService.generateAnswer("Tell me the salary", 1L, 5, 500L, testUserB));
    }

    @Test
    @DisplayName("INTEGRATION TEST 4: Memory security isolation denies cross-workspace boundary requests")
    void testMemorySecurityCrossWorkspaceDenial() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversationA));

        assertThrows(UnauthorizedAccessException.class, () ->
                ragGenerationService.generateAnswer("Tell me the salary", 2L, 5, 500L, testUserA));
    }

    @Test
    @DisplayName("INTEGRATION TEST 5: Standalone question without conversation context engages semantic cache safely")
    void testStandaloneQuestionEngagesSemanticCache() {
        RAGContext ragContext = new RAGContext("What are office working hours?", 1L, List.of(), "Empty context", 0);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("What are office working hours?"), eq(1L), eq(5), eq(testUserA)))
                .thenReturn(ragContext);
        when(semanticCacheService.lookup("What are office working hours?", 1L, testUserA)).thenReturn(Optional.empty());

        RAGResponse response = ragGenerationService.generateAnswer("What are office working hours?", 1L, 5, null, testUserA);

        assertNotNull(response);
        verify(semanticCacheService).lookup("What are office working hours?", 1L, testUserA);
        verify(semanticCacheService).store(eq("What are office working hours?"), eq(1L), eq(testUserA), any(RAGResponse.class));
    }

    @Test
    @DisplayName("INTEGRATION TEST 6: Graceful fallback when memory retrieval encounters runtime failure")
    void testGracefulFallbackOnMemoryRetrievalFailure() {
        MemoryRetrievalService failingMemoryService = mock(MemoryRetrievalService.class);
        when(failingMemoryService.retrieveRelevantMemory(anyString(), anyLong(), anyLong(), any(User.class)))
                .thenThrow(new RuntimeException("Simulated Redis/DB memory failure"));
        ragGenerationService.setMemoryRetrievalService(failingMemoryService);

        RAGContext ragContext = new RAGContext("What is the leave policy?", 1L, List.of(), "Context", 0);
        when(ragRetrievalService.retrieveAndAssembleContext(eq("What is the leave policy?"), eq(1L), eq(5), eq(testUserA))).thenReturn(ragContext);

        RAGResponse response = ragGenerationService.generateAnswer("What is the leave policy?", 1L, 5, 500L, testUserA);

        assertNotNull(response);
        assertEquals("Synthesized grounded answer.", response.answer());
    }
}
