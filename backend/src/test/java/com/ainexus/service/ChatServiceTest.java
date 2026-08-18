package com.ainexus.service;

import com.ainexus.dto.ChatResponse;
import com.ainexus.dto.RAGCitation;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private CitationRepository citationRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private RAGGenerationService ragGenerationService;

    @Mock
    private ConversationMemoryService conversationMemoryService;

    private ChatService chatService;
    private User testUser;
    private User attackerUser;
    private Workspace workspace;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                citationRepository,
                documentChunkRepository,
                workspaceRepository,
                ragGenerationService,
                conversationMemoryService
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");

        attackerUser = new User();
        attackerUser.setId(2L);
        attackerUser.setUsername("mallory");

        workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("Engineering");

        conversation = Conversation.builder()
                .id(100L)
                .title("Leave Policy")
                .user(testUser)
                .workspace(workspace)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TEST 1: New conversation created and user + assistant messages persisted")
    void testNewConversationMessage() {
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Message savedAssistantMsg = Message.builder()
                .id(200L)
                .conversation(conversation)
                .sender("ASSISTANT")
                .content("20 days annual leave.")
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedAssistantMsg);

        Citation savedCitation = Citation.builder()
                .id(300L)
                .message(savedAssistantMsg)
                .score(0.95)
                .build();

        when(citationRepository.save(any(Citation.class))).thenReturn(savedCitation);

        RAGResponse ragResponse = new RAGResponse(
                "20 days annual leave.",
                "What is annual leave?",
                10L,
                List.of(new RAGCitation(1L, "hr.pdf", 0, 0.95, "doc-1-chunk-0", "20 days annual leave.")),
                Collections.emptyList(),
                true
        );

        when(ragGenerationService.generateAnswer(eq("What is annual leave?"), eq(10L), eq(5), eq(100L), eq(testUser)))
                .thenReturn(ragResponse);

        ChatResponse response = chatService.processChat(null, 10L, testUser, "What is annual leave?");

        assertNotNull(response);
        assertEquals(100L, response.conversationId());
        assertEquals("20 days annual leave.", response.answer());
        assertEquals(1, response.citations().size());
        assertEquals(300L, response.citations().get(0).citationId());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(messageRepository, times(2)).save(any(Message.class));
        verify(citationRepository, times(1)).save(any(Citation.class));
    }

    @Test
    @DisplayName("TEST 2: Existing conversation message verifies ownership and workspace")
    void testExistingConversationMessage() {
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        Message savedAssistantMsg = Message.builder()
                .id(201L)
                .conversation(conversation)
                .sender("ASSISTANT")
                .content("All full-time staff are eligible.")
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedAssistantMsg);

        RAGResponse ragResponse = new RAGResponse(
                "All full-time staff are eligible.",
                "Who is eligible?",
                10L,
                Collections.emptyList(),
                Collections.emptyList(),
                true
        );

        when(ragGenerationService.generateAnswer(eq("Who is eligible?"), eq(10L), eq(5), eq(100L), eq(testUser)))
                .thenReturn(ragResponse);

        ChatResponse response = chatService.processChat(100L, 10L, testUser, "Who is eligible?");

        assertNotNull(response);
        assertEquals("All full-time staff are eligible.", response.answer());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("TEST 3: Unauthorized user cannot chat in another user's conversation")
    void testUnauthorizedUserChatRejected() {
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        assertThrows(UnauthorizedAccessException.class, () ->
                chatService.processChat(100L, 10L, attackerUser, "Hacking attempt"));
    }

    @Test
    @DisplayName("TEST 4: Non-existent workspace throws ResourceNotFoundException")
    void testNonExistentWorkspaceThrowsException() {
        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.processChat(null, 999L, testUser, "Hello"));
    }
}
