package com.ainexus.dto;

import com.ainexus.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role must not be null")
        Role role
) {
}
