package com.ainexus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRequest {
    private String title;

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long workspaceId;
}
