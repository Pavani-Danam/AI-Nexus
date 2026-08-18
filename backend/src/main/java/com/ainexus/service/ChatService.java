package com.ainexus.service;

import com.ainexus.dto.CitationDto;
import com.ainexus.dto.ConversationDto;
import com.ainexus.dto.ChatResponse;
import com.ainexus.dto.RAGCitation;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final WorkspaceRepository workspaceRepository;
    private final RAGGenerationService ragGenerationService;
    private final ConversationMemoryService conversationMemoryService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       CitationRepository citationRepository,
                       DocumentChunkRepository documentChunkRepository,
                       WorkspaceRepository workspaceRepository,
                       @Autowired(required = false) RAGGenerationService ragGenerationService,
                       @Autowired(required = false) ConversationMemoryService conversationMemoryService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.workspaceRepository = workspaceRepository;
        this.ragGenerationService = ragGenerationService;
        this.conversationMemoryService = conversationMemoryService;
    }

    @Transactional
    public ChatResponse processChat(Long conversationId, Long workspaceId, User user, String userQuery) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }
        if (user == null) {
            throw new UnauthorizedAccessException("Authenticated user required.");
        }
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Query must not be empty.");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        Conversation conversation;
        if (conversationId != null) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

            if (conversation.getUser() == null || !conversation.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedAccessException("You do not own conversation: " + conversationId);
            }
            if (conversation.getWorkspace() != null && !conversation.getWorkspace().getId().equals(workspaceId)) {
                throw new UnauthorizedAccessException("Conversation does not belong to the requested workspace.");
            }
        } else {
            String title = userQuery.trim().length() > 40
                    ? userQuery.trim().substring(0, 37) + "..."
                    : userQuery.trim();

            conversation = conversationRepository.save(Conversation.builder()
                    .title(title)
                    .workspace(workspace)
                    .user(user)
                    .build());
        }

        // 1. Generate answer using RAG pipeline with prior conversational memory
        String botAnswer;
        List<RAGCitation> ragCitations = Collections.emptyList();

        if (ragGenerationService != null) {
            RAGResponse ragResponse = ragGenerationService.generateAnswer(
                    userQuery.trim(),
                    workspaceId,
                    5,
                    conversation.getId(),
                    user
            );
            botAnswer = ragResponse.answer();
            ragCitations = ragResponse.citations();
        } else {
            botAnswer = "RAG generation service is currently unavailable.";
        }

        // 2. Persist User Message
        messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender("USER")
                .content(userQuery.trim())
                .build());

        // 3. Persist Assistant Message
        Message botMessage = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender("ASSISTANT")
                .content(botAnswer)
                .build());

        // 4. Persist Citations
        List<CitationDto> citationDtos = new ArrayList<>();
        if (ragCitations != null) {
            for (RAGCitation ragCit : ragCitations) {
                Long docId = ragCit.documentId();
                Double score = ragCit.similarityScore() != null ? ragCit.similarityScore() : 0.90;

                Citation citation = citationRepository.save(Citation.builder()
                        .message(botMessage)
                        .score(score)
                        .build());

                citationDtos.add(new CitationDto(
                        citation.getId(),
                        docId,
                        docId,
                        ragCit.filename(),
                        ragCit.snippet(),
                        score
                ));
            }
        }

        return new ChatResponse(
                conversation.getId(),
                botMessage.getId(),
                botAnswer,
                citationDtos,
                botMessage.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(User user, Long workspaceId) {
        if (user == null) {
            throw new UnauthorizedAccessException("Authenticated user required.");
        }
        return conversationRepository.findAll().stream()
                .filter(c -> c.getUser() != null && c.getUser().getId().equals(user.getId()))
                .filter(c -> workspaceId == null || (c.getWorkspace() != null && c.getWorkspace().getId().equals(workspaceId)))
                .map(c -> new ConversationDto(c.getId(), c.getTitle(), c.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
