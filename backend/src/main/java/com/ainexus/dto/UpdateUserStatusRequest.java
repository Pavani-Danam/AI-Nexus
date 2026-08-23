package com.ainexus.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Enabled flag must not be null")
        Boolean enabled
) {
}
