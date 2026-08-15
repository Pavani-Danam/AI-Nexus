package com.ainexus.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private String title;
    private Long userId;
    private Long workspaceId;
    private LocalDateTime createdAt;
}
