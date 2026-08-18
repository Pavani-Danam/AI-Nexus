package com.ainexus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatResponse(
        Long conversationId,
        Long messageId,
        String answer,
        List<CitationDto> citations,
        LocalDateTime createdAt
) {}
