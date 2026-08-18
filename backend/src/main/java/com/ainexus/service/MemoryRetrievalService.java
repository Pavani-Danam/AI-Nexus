package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.entity.User;

public interface MemoryRetrievalService {

    /**
     * Inspects the full conversation memory and returns a focused ConversationMemory
     * containing only the messages and summary context relevant to the currentUserQuery.
     */
    ConversationMemory retrieveRelevantMemory(String currentUserQuery, Long conversationId, Long workspaceId, User user);

    /**
     * Filters an existing ConversationMemory instance to retain only items relevant to userQuery.
     */
    ConversationMemory filterRelevantMemory(String currentUserQuery, ConversationMemory fullMemory);
}
