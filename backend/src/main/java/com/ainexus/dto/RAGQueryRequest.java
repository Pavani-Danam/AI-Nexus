package com.ainexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RAGQueryRequest(
        @NotBlank(message = "Query must not be empty or blank")
        @Size(max = 4000, message = "Query must not exceed 4000 characters")
        String query,

        Long workspaceId,

        Integer topK
) {}
