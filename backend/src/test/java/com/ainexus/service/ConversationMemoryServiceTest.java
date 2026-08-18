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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private ConversationMemoryServiceImpl memoryService;

    private User testUser;
    private User attackerUser;
    private Workspace workspace;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        memoryService = new ConversationMemoryServiceImpl(conversationRepository, messageRepository);
        memoryService.setMaxMessages(10);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");

        attackerUser = new User();
        attackerUser.setId(2L);
        attackerUser.setUsername("mallory");

        workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("HR Workspace");

        conversation = Conversation.builder()
                .id(100L)
                .title("Leave Policy Chat")
                .user(testUser)
                .workspace(workspace)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TEST 1: Empty conversation returns empty memory object without error")
    void testEmptyConversation() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertNotNull(memory);
        assertEquals(100L, memory.conversationId());
        assertEquals(0, memory.messageCount());
        assertFalse(memory.hasHistory());
        assertEquals("", memory.formattedHistory());
    }

    @Test
    @DisplayName("TEST 2: Multiple messages ordered chronologically with correct roles")
    void testMessageOrderingAndRoles() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        List<Message> messages = List.of(
                Message.builder().id(1L).conversation(conversation).sender("USER").content("What is the leave policy?").createdAt(LocalDateTime.now().minusMinutes(5)).build(),
                Message.builder().id(2L).conversation(conversation).sender("ASSISTANT").content("Employees get 20 days.").createdAt(LocalDateTime.now().minusMinutes(4)).build()
        );

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(messages);

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertNotNull(memory);
        assertEquals(2, memory.messageCount());
        assertTrue(memory.hasHistory());
        assertEquals("USER", memory.messages().get(0).role());
        assertEquals("ASSISTANT", memory.messages().get(1).role());
        assertTrue(memory.formattedHistory().contains("USER:\nWhat is the leave policy?"));
        assertTrue(memory.formattedHistory().contains("ASSISTANT:\nEmployees get 20 days."));
    }

    @Test
    @DisplayName("TEST 3: Memory window limits history to max configured messages (e.g. 3 messages)")
    void testMemoryWindowLimit() {
        memoryService.setMaxMessages(3);
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            messages.add(Message.builder()
                    .id((long) i)
                    .conversation(conversation)
                    .sender(i % 2 == 1 ? "USER" : "ASSISTANT")
                    .content("Message " + i)
                    .createdAt(LocalDateTime.now().minusMinutes(10 - i))
                    .build());
        }

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L)).thenReturn(messages);

        ConversationMemory memory = memoryService.getMemory(100L, 10L, testUser);

        assertEquals(3, memory.messageCount());
        assertEquals("Message 4", memory.messages().get(0).content());
        assertEquals("Message 5", memory.messages().get(1).content());
        assertEquals("Message 6", memory.messages().get(2).content());
    }

    @Test
    @DisplayName("TEST 4: Unauthorized user cannot access conversation memory")
    void testUnauthorizedUserAccessDenied() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        assertThrows(UnauthorizedAccessException.class, () ->
                memoryService.getMemory(100L, 10L, attackerUser));
    }

    @Test
    @DisplayName("TEST 5: Access denied when requesting from mismatched workspace ID")
    void testCrossWorkspaceAccessDenied() {
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        assertThrows(UnauthorizedAccessException.class, () ->
                memoryService.getMemory(100L, 20L, testUser));
    }

    @Test
    @DisplayName("TEST 6: Non-existent conversation throws ResourceNotFoundException")
    void testNonExistentConversation() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                memoryService.getMemory(999L, 10L, testUser));
    }

    @Test
    @DisplayName("TEST 7: Null conversationId returns empty memory safely")
    void testNullConversationId() {
        ConversationMemory memory = memoryService.getMemory(null, 10L, testUser);
        assertNotNull(memory);
        assertNull(memory.conversationId());
        assertFalse(memory.hasHistory());
    }

    @Test
    @DisplayName("TEST 8: Null authenticatedUser throws UnauthorizedAccessException")
    void testNullUser() {
        assertThrows(UnauthorizedAccessException.class, () ->
                memoryService.getMemory(100L, 10L, null));
    }
}
