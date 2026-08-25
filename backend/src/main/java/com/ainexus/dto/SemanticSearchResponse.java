package com.ainexus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {
    private Long documentId;
    private String documentName;
    private String chunkText;
    private Double score;
    private Integer chunkIndex;
}
