package com.ainexus.service;

import com.ainexus.dto.SearchResponse;
import com.ainexus.entity.User;

public interface SemanticSearchService {
    SearchResponse search(String query, Long workspaceId, Integer topK, User authenticatedUser);
}
