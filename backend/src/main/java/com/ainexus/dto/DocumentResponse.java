package com.ainexus.dto;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String name;
    private String filename;
    private String contentType;
    private Long fileSize;
    private DocumentStatus status;
    private Long workspaceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentResponse fromEntity(Document doc) {
        if (doc == null) return null;
        return DocumentResponse.builder()
                .id(doc.getId())
                .name(doc.getOriginalFilename() != null ? doc.getOriginalFilename() : doc.getFileName())
                .filename(doc.getFileName())
                .contentType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .workspaceId(doc.getWorkspace() != null ? doc.getWorkspace().getId() : null)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
