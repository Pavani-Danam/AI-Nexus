package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.entity.Conversation;
import com.ainexus.entity.Message;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.ConversationRepository;
import com.ainexus.repository.MessageRepository;
import com.ainexus.service.impl.ConversationMemoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationSummaryService conversationSummaryService;

    private ConversationMemoryServiceImpl memoryService;

    private User testUser;
    private User otherUser;
    private Workspace testWorkspace;
    private Workspace otherWorkspace;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        memoryService = new ConversationMemoryServiceImpl(conversationRepository, messageRepository);
        memoryService.setConversationSummaryService(conversationSummaryService);
        ReflectionTestUtils.setField(memoryService, "maxMessages", 10);
        ReflectionTestUtils.setField(memoryService, "recentMessagesWindow", 6);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("bob");

        testWorkspace = new Workspace();
        testWorkspace.setId(10L);

        otherWorkspace = new Workspace();
        otherWorkspace.setId(20L);

        testConversation = Conversation.builder()
                .id(100L)
                .title("Test Thread")
                .user(testUser)
                .workspace(testWorkspace)
                .summary(null)
                .build();
    }

    @Test
    @DisplayName("TEST 1: Empty conversation returns empty ConversationMemory")
    void testEmptyConversation() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertNotNull(memory);
        assertEquals(100L, memory.conversationId());
        assertEquals(10L, memory.workspaceId());
        assertFalse(memory.hasHistory());
        assertEquals(0, memory.messageCount());
        assertTrue(memory.messages().isEmpty());
    }

    @Test
    @DisplayName("TEST 2: Second message receives previous message in memory")
    void testSecondMessageReceivesPrevious() {
        Message m1 = Message.builder()
                .id(1L)
                .content("What is the annual leave policy?")
                .sender("USER")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
        Message m2 = Message.builder()
                .id(2L)
                .content("Employees receive 20 days annual leave.")
                .sender("ASSISTANT")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(conversationRepository.findById(100L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(m1, m2));
        when(conversationSummaryService.getOrUpdateSummary(eq(testConversation), any(), eq(testUser))).thenReturn(null);

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertNotNull(memory);
        assertTrue(memory.hasHistory());
        assertEquals(2, memory.messageCount());
        assertTrue(memory.formattedHistory().contains("USER:\nWhat is the annual leave policy?"));
        assertTrue(memory.formattedHistory().contains("ASSISTANT:\nEmployees receive 20 days annual leave."));
    }

    @Test
    @DisplayName("TEST 3: Multiple messages are ordered chronologically")
    void testMessagesOrderedChronologically() {
        Message m1 = Message.builder().id(1L).content("Q1").sender("USER").createdAt(LocalDateTime.now().minusMinutes(4)).build();
        Message m2 = Message.builder().id(2L).content("A1").sender("ASSISTANT").createdAt(LocalDateTime.now().minusMinutes(3)).build();
        Message m3 = Message.builder().id(3L).content("Q2").sender("USER").createdAt(LocalDateTime.now().minusMinutes(2)).build();

        when(conversationRepository.findById(100L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(m1, m2, m3));
        when(conversationSummaryService.getOrUpdateSummary(eq(testConversation), any(), eq(testUser))).thenReturn(null);

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertEquals(3, memory.messageCount());
        int q1Idx = memory.formattedHistory().indexOf("Q1");
        int a1Idx = memory.formattedHistory().indexOf("A1");
        int q2Idx = memory.formattedHistory().indexOf("Q2");
        assertTrue(q1Idx < a1Idx && a1Idx < q2Idx, "Messages must be formatted chronologically");
    }

    @Test
    @DisplayName("TEST 4: Memory incorporates summary and recent messages window for long conversations")
    void testSummaryIncorporationWithRecentMessages() {
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            messages.add(Message.builder()
                    .id((long) i)
                    .content("Message content " + i)
                    .sender(i % 2 != 0 ? "USER" : "ASSISTANT")
                    .createdAt(LocalDateTime.now().minusMinutes(20 - i))
                    .build());
        }

        when(conversationRepository.findById(100L)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(messages);
        when(conversationSummaryService.getOrUpdateSummary(eq(testConversation), any(), eq(testUser)))
                .thenReturn("Compact summary of earlier conversation.");

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertNotNull(memory);
        assertEquals(6, memory.messageCount()); // Retains recent 6 messages
        assertTrue(memory.formattedHistory().contains("CONVERSATION SUMMARY:\nCompact summary of earlier conversation."));
        assertTrue(memory.formattedHistory().contains("RECENT MESSAGES:"));
        assertTrue(memory.formattedHistory().contains("Message content 14"));
    }

    @Test
    @DisplayName("TEST 5: Unauthorized user cannot access another user's conversation")
    void testUnauthorizedUserAccessDenied() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(testConversation));

        assertThrows(UnauthorizedAccessException.class, () ->
                memoryService.getMemory(100L, 10L, otherUser));
    }

    @Test
    @DisplayName("TEST 6: Accessing conversation across wrong workspace is rejected")
    void testCrossWorkspaceAccessDenied() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(testConversation));

        assertThrows(UnauthorizedAccessException.class, () ->
                memoryService.getMemory(100L, 20L, testUser));
    }

    @Test
    @DisplayName("TEST 7: Non-existent conversation throws ResourceNotFoundException")
    void testNonExistentConversationThrowsException() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                memoryService.getMemory(999L, 10L, testUser));
    }

    @Test
    @DisplayName("TEST 8: Null conversationId safely returns empty memory without database call")
    void testNullConversationIdReturnsEmpty() {
        ConversationMemory memory = memoryService.getMemory(null, 10L, testUser);

        assertNotNull(memory);
        assertNull(memory.conversationId());
        assertEquals(10L, memory.workspaceId());
        assertFalse(memory.hasHistory());
        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(messageRepository);
    }
}
