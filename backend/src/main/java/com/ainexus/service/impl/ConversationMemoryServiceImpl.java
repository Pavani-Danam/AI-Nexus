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
import com.ainexus.service.ConversationSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationMemoryServiceImpl.class);

    @Value("${app.chat.memory.max-messages:10}")
    private int maxMessages;

    @Value("${app.chat.memory.recent-messages:6}")
    private int recentMessagesWindow;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private ConversationSummaryService conversationSummaryService;

    public ConversationMemoryServiceImpl(ConversationRepository conversationRepository,
                                         MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Autowired(required = false)
    public void setConversationSummaryService(ConversationSummaryService conversationSummaryService) {
        this.conversationSummaryService = conversationSummaryService;
    }

    @Override
    @Transactional
    public ConversationMemory getMemory(Long conversationId, Long workspaceId, User authenticatedUser) {
        if (conversationId == null) {
            return ConversationMemory.empty(null, workspaceId);
        }
        if (authenticatedUser == null) {
            throw new UnauthorizedAccessException("Authenticated user required to access conversational memory.");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        // 1. Authorization: Verify user ownership
        if (!conversation.getUser().getId().equals(authenticatedUser.getId())) {
            logger.warn("Security Alert: User {} attempted to access unauthorized conversation {}",
                    authenticatedUser.getId(), conversationId);
            throw new UnauthorizedAccessException("You are not authorized to access conversation " + conversationId);
        }

        // 2. Authorization: Verify workspace boundary
        if (workspaceId != null && !conversation.getWorkspace().getId().equals(workspaceId)) {
            logger.warn("Security Alert: Conversation {} belongs to workspace {}, but requested from workspace {}",
                    conversationId, conversation.getWorkspace().getId(), workspaceId);
            throw new UnauthorizedAccessException("Conversation does not belong to the specified workspace.");
        }

        // 3. Retrieve all chronological messages
        List<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (allMessages == null || allMessages.isEmpty()) {
            return ConversationMemory.empty(conversationId, conversation.getWorkspace().getId());
        }

        // 4. Update/Get Conversation Summary if older messages exist
        String summary = null;
        if (conversationSummaryService != null) {
            try {
                summary = conversationSummaryService.getOrUpdateSummary(conversation, allMessages, authenticatedUser);
            } catch (Exception e) {
                logger.warn("Error updating summary for conversation {}: {}. Proceeding without updated summary.",
                        conversationId, e.getMessage());
                summary = conversation.getSummary();
            }
        } else {
            summary = conversation.getSummary();
        }

        // 5. Select recent messages window
        int total = allMessages.size();
        int windowSize = (summary != null && !summary.isBlank()) ? recentMessagesWindow : maxMessages;
        int startIndex = Math.max(0, total - windowSize);
        List<Message> recentRawMessages = allMessages.subList(startIndex, total);

        List<MemoryMessage> memoryMessages = new ArrayList<>();
        for (Message msg : recentRawMessages) {
            String role = (msg.getSender() != null && !msg.getSender().isBlank()) ? msg.getSender().trim() : "USER";
            memoryMessages.add(MemoryMessage.of(msg.getId(), role, msg.getContent(), msg.getCreatedAt()));
        }

        // 6. Format combined history
        String formattedContext = formatMemoryContext(summary, memoryMessages);

        return new ConversationMemory(
                conversationId,
                conversation.getWorkspace().getId(),
                memoryMessages,
                formattedContext,
                memoryMessages.size()
        );
    }

    @Override
    public String formatHistory(List<MemoryMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return formatMemoryContext(null, messages);
    }

    private String formatMemoryContext(String summary, List<MemoryMessage> messages) {
        StringBuilder sb = new StringBuilder();

        if (summary != null && !summary.isBlank()) {
            sb.append("CONVERSATION SUMMARY:\n")
              .append(summary.trim())
              .append("\n\n");
        }

        if (messages != null && !messages.isEmpty()) {
            if (summary != null && !summary.isBlank()) {
                sb.append("RECENT MESSAGES:\n");
            }
            for (MemoryMessage msg : messages) {
                sb.append(msg.role()).append(":\n")
                  .append(msg.content()).append("\n\n");
            }
        }

        return sb.toString().trim();
    }
}
