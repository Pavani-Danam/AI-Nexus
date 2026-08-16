package com.ainexus.controller;

import com.ainexus.dto.DocumentResponse;
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

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Document saved = documentService.uploadDocument(file, workspaceId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(doc -> ResponseEntity.ok(mapToResponse(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<Map<String, Object>> getDocumentExtractedText(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        String text = documentService.extractDocumentText(id, user);
        return ResponseEntity.ok(Map.of(
                "documentId", id,
                "extractedText", text,
                "characterCount", text.length()
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
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
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
