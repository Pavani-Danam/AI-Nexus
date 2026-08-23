package com.ainexus.dto;

import java.time.LocalDateTime;

public record UserWorkspaceMembershipDto(
        Long workspaceId,
        String workspaceName,
        String memberRole,
        LocalDateTime joinedAt
) {
}
