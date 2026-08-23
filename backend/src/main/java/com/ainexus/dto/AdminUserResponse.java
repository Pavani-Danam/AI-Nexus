package com.ainexus.dto;

import com.ainexus.entity.Role;
import com.ainexus.entity.User;

import java.util.List;

public record AdminUserResponse(
        Long id,
        String name,
        String username,
        String email,
        Role role,
        boolean enabled,
        List<UserWorkspaceMembershipDto> workspaceMemberships
) {
    public static AdminUserResponse fromUser(User user, List<UserWorkspaceMembershipDto> memberships) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                memberships != null ? memberships : List.of()
        );
    }
}
