package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.MemoryMessage;
import com.ainexus.entity.User;

import java.util.List;

public interface ConversationMemoryService {

    /**
     * Retrieves previous messages for the given conversation with workspace & user ownership verification.
     * Returns a bounded ConversationMemory object (sliding window of max messages).
     */
    ConversationMemory getMemory(Long conversationId, Long workspaceId, User authenticatedUser);

    /**
     * Formats a list of memory messages into a clean dialogue history block.
     */
    String formatHistory(List<MemoryMessage> messages);
}
