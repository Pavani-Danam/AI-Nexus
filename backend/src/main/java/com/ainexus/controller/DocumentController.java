package com.ainexus.controller;

import com.ainexus.dto.DocumentResponse;
import com.ainexus.dto.SearchResponse;
import com.ainexus.entity.Document;
import com.ainexus.entity.User;
import com.ainexus.service.DocumentProcessingService;
import com.ainexus.service.DocumentService;
import com.ainexus.service.SemanticSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;
    private final SemanticSearchService semanticSearchService;

    public DocumentController(DocumentService documentService,
                              DocumentProcessingService documentProcessingService,
                              SemanticSearchService semanticSearchService) {
        this.documentService = documentService;
        this.documentProcessingService = documentProcessingService;
        this.semanticSearchService = semanticSearchService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceIdHeader,
            @RequestParam(value = "workspaceId", required = false) Long workspaceIdParam) {
        Long workspaceId = workspaceIdHeader != null ? workspaceIdHeader : workspaceIdParam;
        if (workspaceId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Document> docs = documentService.getDocumentsByWorkspace(workspaceId);
        List<DocumentResponse> response = docs.stream().map(DocumentResponse::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "workspaceId", required = false) Long workspaceIdParam,
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceIdHeader,
            @AuthenticationPrincipal User currentUser) {
        Long workspaceId = workspaceIdHeader != null ? workspaceIdHeader : workspaceIdParam;
        Document doc = documentService.uploadDocument(file, workspaceId, currentUser);
        documentProcessingService.processDocumentAsync(doc.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.fromEntity(doc));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User currentUser) {
        documentService.deleteDocument(documentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchDocuments(
            @RequestParam("query") String query,
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceIdHeader,
            @RequestParam(value = "workspaceId", required = false) Long workspaceIdParam,
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @AuthenticationPrincipal User currentUser) {
        Long workspaceId = workspaceIdHeader != null ? workspaceIdHeader : workspaceIdParam;
        if (workspaceId == null || query == null || query.isBlank()) {
            return ResponseEntity.ok(new SearchResponse(query, workspaceId, 0, List.of()));
        }
        SearchResponse results = semanticSearchService.search(query.trim(), workspaceId, topK, currentUser);
        return ResponseEntity.ok(results);
    }
}
