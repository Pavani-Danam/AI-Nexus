package com.ainexus.controller;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentChunk;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.DocumentChunkService;
import com.ainexus.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-chunks")
public class DocumentChunkController {

    private final DocumentChunkService documentChunkService;
    private final DocumentService documentService;

    public DocumentChunkController(DocumentChunkService documentChunkService, DocumentService documentService) {
        this.documentChunkService = documentChunkService;
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentChunk>> getChunksByDocument(@RequestParam Long documentId) {
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        return ResponseEntity.ok(documentChunkService.getChunksByDocument(document));
    }
}
