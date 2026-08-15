package com.ainexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 100, message = "Workspace name cannot exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;
}
