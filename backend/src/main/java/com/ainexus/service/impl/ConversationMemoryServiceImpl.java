package com.ainexus.service.impl;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.MemoryMessage;
import com.ainexus.entity.Conversation;
import com.ainexus.entity.Message;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.ConversationRepository;
import com.ainexus.repository.MessageRepository;
import com.ainexus.service.ConversationMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationMemoryServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Value("${app.chat.memory.max-messages:10}")
    private int maxMessages;

    public ConversationMemoryServiceImpl(ConversationRepository conversationRepository,
                                         MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationMemory getMemory(Long conversationId, Long workspaceId, User authenticatedUser) {
        if (conversationId == null) {
            return ConversationMemory.empty(null, workspaceId);
        }

        if (authenticatedUser == null) {
            throw new UnauthorizedAccessException("Authentication required to access conversation memory.");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        if (conversation.getUser() == null || !conversation.getUser().getId().equals(authenticatedUser.getId())) {
            logger.warn("Security Alert: User {} attempted to access unauthorized conversation {}",
                    authenticatedUser.getId(), conversationId);
            throw new UnauthorizedAccessException("Access denied: You do not own conversation " + conversationId);
        }

        if (workspaceId != null && conversation.getWorkspace() != null
                && !conversation.getWorkspace().getId().equals(workspaceId)) {
            logger.warn("Security Alert: Conversation {} belongs to workspace {}, but requested from workspace {}",
                    conversationId, conversation.getWorkspace().getId(), workspaceId);
            throw new UnauthorizedAccessException("Access denied: Conversation does not belong to the requested workspace.");
        }

        List<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (allMessages == null || allMessages.isEmpty()) {
            return ConversationMemory.empty(conversationId, workspaceId);
        }

        int startIdx = Math.max(0, allMessages.size() - maxMessages);
        List<Message> windowedMessages = allMessages.subList(startIdx, allMessages.size());

        List<MemoryMessage> memoryMessages = windowedMessages.stream()
                .map(m -> MemoryMessage.of(m.getId(), m.getSender(), m.getContent(), m.getCreatedAt()))
                .collect(Collectors.toList());

        String formatted = formatHistory(memoryMessages);

        return new ConversationMemory(
                conversationId,
                workspaceId,
                memoryMessages,
                formatted,
                memoryMessages.size()
        );
    }

    @Override
    public String formatHistory(List<MemoryMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (MemoryMessage msg : messages) {
            String role = (msg.role() != null) ? msg.role().toUpperCase().trim() : "USER";
            String content = (msg.content() != null) ? msg.content().trim() : "";
            sb.append(role).append(":\n").append(content).append("\n\n");
        }
        return sb.toString().trim();
    }
}
