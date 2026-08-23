package com.ainexus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminWorkspaceDetailResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        String ownerUsername,
        long memberCount,
        long documentCount,
        long workflowCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AdminWorkspaceMemberDto> members
) {
}
