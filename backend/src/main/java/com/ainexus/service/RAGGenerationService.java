package com.ainexus.service;

import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;

public interface RAGGenerationService {

    /**
     * Generates a fully grounded RAG answer without conversation memory.
     */
    RAGResponse generateAnswer(String query, Long workspaceId, Integer topK, User authenticatedUser);

    /**
     * Generates a fully grounded RAG answer incorporating previous conversation memory.
     */
    RAGResponse generateAnswer(String query, Long workspaceId, Integer topK, Long conversationId, User authenticatedUser);
}
