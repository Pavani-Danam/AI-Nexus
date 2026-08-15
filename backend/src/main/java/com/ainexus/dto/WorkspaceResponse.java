package com.ainexus.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
