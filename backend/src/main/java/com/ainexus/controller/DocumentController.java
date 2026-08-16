package com.ainexus.controller;

import com.ainexus.dto.DocumentRequest;
import com.ainexus.dto.DocumentResponse;
import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.User;
import com.ainexus.service.DocumentService;
import com.ainexus.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    public DocumentController(DocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@RequestBody DocumentRequest request) {
        User user = userService.getUserById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        Document document = Document.builder()
                .fileName(request.getFileName())
                .originalFilename(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .storagePath(request.getStoragePath())
                .status(DocumentStatus.UPLOADED)
                .user(user)
                .build();

        Document saved = documentService.saveDocument(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(doc -> ResponseEntity.ok(mapToResponse(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

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
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus() != null ? document.getStatus().name() : null,
                document.getUser() != null ? document.getUser().getId() : null,
                document.getCreatedAt()
        );
    }
}
