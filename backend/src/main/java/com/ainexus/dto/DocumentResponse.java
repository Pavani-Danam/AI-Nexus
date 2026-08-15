package com.ainexus.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private Long userId;
    private LocalDateTime createdAt;
}
