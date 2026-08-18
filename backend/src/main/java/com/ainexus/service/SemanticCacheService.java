package com.ainexus.service;

import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;

import java.util.Optional;

/**
 * Service responsible for semantic caching of RAG query responses.
 * Uses query embeddings and cosine similarity within tenant/workspace boundaries
 * to reuse grounded answers for semantically equivalent queries.
 */
public interface SemanticCacheService {

    /**
     * Looks up a semantically similar cached response for the given query and workspace.
     *
     * @param query the user query
     * @param workspaceId the workspace identifier
     * @param user the authenticated user
     * @return Optional containing cached RAGResponse if a valid, unexpired match exists
     */
    Optional<RAGResponse> lookup(String query, Long workspaceId, User user);

    /**
     * Stores a successful RAG response into the semantic cache.
     *
     * @param query the user query
     * @param workspaceId the workspace identifier
     * @param user the authenticated user
     * @param response the successful RAG response to cache
     */
    void store(String query, Long workspaceId, User user, RAGResponse response);

    /**
     * Clears all cached entries for a specific workspace (e.g. when workspace documents are updated/deleted).
     *
     * @param workspaceId the workspace identifier
     */
    void invalidateWorkspace(Long workspaceId);
}
