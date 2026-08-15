package com.ainexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {

    @NotBlank(message = "Filename is required")
    private String fileName;

    private String fileType;
    private Long fileSize;

    @NotBlank(message = "Storage path is required")
    private String storagePath;

    @NotNull(message = "User ID is required")
    private Long userId;
}
