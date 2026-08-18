package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.entity.User;

public interface ConversationQueryRewriteService {

    /**
     * Rewrites a conversational follow-up query into a standalone retrieval query
     * using the provided conversation memory.
     *
     * If the memory is empty or the query is already standalone, returns the clean original query.
     */
    String rewriteToStandaloneQuery(String userQuery, ConversationMemory memory, Long workspaceId, User authenticatedUser);
}
