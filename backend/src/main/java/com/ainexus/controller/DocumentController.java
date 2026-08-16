package com.ainexus.controller;

import com.ainexus.dto.DocumentResponse;
import com.ainexus.dto.TextChunk;
import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.DocumentService;
import com.ainexus.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/documents", "/api/documents"})
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    public DocumentController(DocumentService documentService,
                              UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("workspaceId") Long workspaceId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        Document saved = documentService.uploadDocument(file, workspaceId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @RequestParam("workspaceId") Long workspaceId,
            @RequestParam(value = "search", required = false) String search,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        List<DocumentResponse> documents = documentService.getDocuments(workspaceId, user, search)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        Document doc = documentService.getDocumentByIdAndUser(id, user);
        return ResponseEntity.ok(mapToResponse(doc));
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<Map<String, Object>> getDocumentExtractedText(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        String text = documentService.extractDocumentText(id, user);
        return ResponseEntity.ok(Map.of(
                "documentId", id,
                "extractedText", text,
                "characterCount", text.length()
        ));
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<Map<String, Object>> getDocumentChunks(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        List<TextChunk> chunks = documentService.extractAndChunkDocument(id, user);
        return ResponseEntity.ok(Map.of(
                "documentId", id,
                "totalChunks", chunks.size(),
                "chunks", chunks
        ));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByWorkspace(
            @PathVariable Long workspaceId,
            Pageable pageable) {
        Page<DocumentResponse> page = documentService.getDocumentsByWorkspace(workspaceId, pageable)
                .map(this::mapToResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<DocumentResponse> response = documentService.getDocumentsByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DocumentResponse> updateDocumentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        DocumentStatus newStatus = DocumentStatus.valueOf(status.toUpperCase());
        Document updated = documentService.updateDocumentStatus(id, newStatus);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        documentService.deleteDocument(id, user);
        return ResponseEntity.ok(Map.of("message", "Document successfully deleted"));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .status(document.getStatus() != null ? document.getStatus().name() : null)
                .userId(document.getUser() != null ? document.getUser().getId() : null)
                .workspaceId(document.getWorkspace() != null ? document.getWorkspace().getId() : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
