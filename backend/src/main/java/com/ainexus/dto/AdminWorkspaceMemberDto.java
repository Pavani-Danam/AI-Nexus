package com.ainexus.dto;

import java.time.LocalDateTime;

public record AdminWorkspaceMemberDto(
        Long userId,
        String username,
        String name,
        String email,
        String role,
        LocalDateTime joinedAt
) {
}
