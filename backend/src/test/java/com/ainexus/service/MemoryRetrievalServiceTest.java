package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.MemoryMessage;
import com.ainexus.entity.User;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.service.impl.MemoryRetrievalServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryRetrievalServiceTest {

    @Mock
    private ConversationMemoryService conversationMemoryService;

    private MemoryRetrievalServiceImpl memoryRetrievalService;
    private User testUser;

    @BeforeEach
    void setUp() {
        memoryRetrievalService = new MemoryRetrievalServiceImpl(conversationMemoryService);
        ReflectionTestUtils.setField(memoryRetrievalService, "maxRelevantMessages", 4);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
    }

    @Test
    @DisplayName("TEST 1: Relevant older messages are selected when query matches topic")
    void testRelevantOlderMessagesSelected() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "What is the annual leave policy?", LocalDateTime.now().minusMinutes(10)),
                MemoryMessage.of(2L, "ASSISTANT", "Employees receive 20 days paid leave.", LocalDateTime.now().minusMinutes(9)),
                MemoryMessage.of(3L, "USER", "What is the salary review cycle?", LocalDateTime.now().minusMinutes(5)),
                MemoryMessage.of(4L, "ASSISTANT", "Salaries are reviewed each December.", LocalDateTime.now().minusMinutes(4))
        );
        ConversationMemory fullMemory = new ConversationMemory(100L, 10L, messages, "Full history", 4);

        when(conversationMemoryService.getMemory(100L, 10L, testUser)).thenReturn(fullMemory);

        ConversationMemory result = memoryRetrievalService.retrieveRelevantMemory("Who is eligible for annual leave?", 100L, 10L, testUser);

        assertNotNull(result);
        assertTrue(result.formattedHistory().contains("annual leave policy"));
        assertTrue(result.formattedHistory().contains("20 days paid leave"));
    }

    @Test
    @DisplayName("TEST 2: Completely irrelevant messages are deprioritized")
    void testIrrelevantMessagesDeprioritized() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "How do I configure Docker?", LocalDateTime.now().minusMinutes(10)),
                MemoryMessage.of(2L, "ASSISTANT", "Use docker compose up.", LocalDateTime.now().minusMinutes(9)),
                MemoryMessage.of(3L, "USER", "What is the reimbursement process?", LocalDateTime.now().minusMinutes(2)),
                MemoryMessage.of(4L, "ASSISTANT", "Submit receipts via the portal.", LocalDateTime.now().minusMinutes(1))
        );
        ConversationMemory fullMemory = new ConversationMemory(100L, 10L, messages, "Full history", 4);

        ConversationMemory result = memoryRetrievalService.filterRelevantMemory("Docker configuration container setup", fullMemory);

        assertNotNull(result);
        assertTrue(result.formattedHistory().contains("configure Docker"));
    }

    @Test
    @DisplayName("TEST 3: Chronological ordering is preserved among selected messages")
    void testChronologicalOrderingPreserved() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "First leave question", LocalDateTime.now().minusMinutes(10)),
                MemoryMessage.of(2L, "ASSISTANT", "First leave answer", LocalDateTime.now().minusMinutes(9)),
                MemoryMessage.of(3L, "USER", "Unrelated query", LocalDateTime.now().minusMinutes(5)),
                MemoryMessage.of(4L, "USER", "Second leave question", LocalDateTime.now().minusMinutes(2))
        );
        ConversationMemory fullMemory = new ConversationMemory(100L, 10L, messages, "History", 4);

        ConversationMemory result = memoryRetrievalService.filterRelevantMemory("leave question", fullMemory);

        assertNotNull(result);
        int firstIdx = result.formattedHistory().indexOf("First leave question");
        int secondIdx = result.formattedHistory().indexOf("Second leave question");
        assertTrue(firstIdx < secondIdx, "Selected messages must retain chronological order");
    }

    @Test
    @DisplayName("TEST 4: Summary is preserved when present in full memory")
    void testSummaryPreservedInRelevantContext() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "What about vacation?", LocalDateTime.now().minusMinutes(1))
        );
        String historyWithSummary = "CONVERSATION SUMMARY:\nUser inquired earlier about company PTO and holiday allowances.\n\nRECENT MESSAGES:\nUSER:\nWhat about vacation?";
        ConversationMemory fullMemory = new ConversationMemory(100L, 10L, messages, historyWithSummary, 1);

        ConversationMemory result = memoryRetrievalService.filterRelevantMemory("vacation PTO", fullMemory);

        assertNotNull(result);
        assertTrue(result.formattedHistory().contains("CONVERSATION SUMMARY:"));
        assertTrue(result.formattedHistory().contains("User inquired earlier about company PTO"));
    }

    @Test
    @DisplayName("TEST 5: Empty memory returns empty ConversationMemory")
    void testEmptyMemory() {
        ConversationMemory empty = ConversationMemory.empty(100L, 10L);
        ConversationMemory result = memoryRetrievalService.filterRelevantMemory("test query", empty);

        assertNotNull(result);
        assertFalse(result.hasHistory());
    }

    @Test
    @DisplayName("TEST 6: Memory limit restricts maximum selected messages")
    void testMemoryLimitEnforced() {
        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "policy 1", LocalDateTime.now().minusMinutes(6)),
                MemoryMessage.of(2L, "ASSISTANT", "policy 2", LocalDateTime.now().minusMinutes(5)),
                MemoryMessage.of(3L, "USER", "policy 3", LocalDateTime.now().minusMinutes(4)),
                MemoryMessage.of(4L, "ASSISTANT", "policy 4", LocalDateTime.now().minusMinutes(3)),
                MemoryMessage.of(5L, "USER", "policy 5", LocalDateTime.now().minusMinutes(2)),
                MemoryMessage.of(6L, "ASSISTANT", "policy 6", LocalDateTime.now().minusMinutes(1))
        );
        ConversationMemory fullMemory = new ConversationMemory(100L, 10L, messages, "History", 6);

        ConversationMemory result = memoryRetrievalService.filterRelevantMemory("policy details", fullMemory);

        assertNotNull(result);
        assertTrue(result.messages().size() <= 4, "Must not exceed maxRelevantMessages = 4");
    }

    @Test
    @DisplayName("TEST 7: Unauthenticated access throws UnauthorizedAccessException")
    void testUnauthenticatedAccessThrowsException() {
        assertThrows(UnauthorizedAccessException.class, () ->
                memoryRetrievalService.retrieveRelevantMemory("test", 100L, 10L, null));
    }
}
