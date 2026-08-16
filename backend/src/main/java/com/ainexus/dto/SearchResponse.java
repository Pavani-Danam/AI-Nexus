package com.ainexus.dto;

import java.util.List;

public record SearchResponse(
        String query,
        Long workspaceId,
        int totalResults,
        List<SearchResultItem> results
) {}
