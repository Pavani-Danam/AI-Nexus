package com.ainexus.controller;

import com.ainexus.entity.Document;
import com.ainexus.entity.ProcessingJob;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.DocumentService;
import com.ainexus.service.ProcessingJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/processing-jobs")
public class ProcessingJobController {

    private final ProcessingJobService processingJobService;
    private final DocumentService documentService;

    public ProcessingJobController(ProcessingJobService processingJobService, DocumentService documentService) {
        this.processingJobService = processingJobService;
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<ProcessingJob> createJob(@RequestParam Long documentId,
                                                   @RequestParam(required = false, defaultValue = "EMBEDDING") String jobType) {
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        ProcessingJob job = processingJobService.createJob(document, jobType);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessingJob> getJobById(@PathVariable Long id) {
        ProcessingJob job = processingJobService.getJobById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processing job not found with id: " + id));
        return ResponseEntity.ok(job);
    }

    @GetMapping
    public ResponseEntity<List<ProcessingJob>> getJobsByDocument(@RequestParam Long documentId) {
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        return ResponseEntity.ok(processingJobService.getJobsByDocument(document));
    }
}
