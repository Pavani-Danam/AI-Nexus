package com.ainexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManageWorkspaceMembershipRequest(
        @NotNull(message = "Workspace ID must not be null")
        Long workspaceId,
        @NotBlank(message = "Role must not be blank")
        String role
) {
}
