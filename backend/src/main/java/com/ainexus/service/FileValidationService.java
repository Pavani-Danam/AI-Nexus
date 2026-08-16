package com.ainexus.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(FileValidationService.class);
    private final Tika tika = new Tika();

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".docx", ".txt");

    private static final Set<String> PDF_MIME_TYPES = Set.of(
            "application/pdf"
    );

    private static final Set<String> DOCX_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/x-tika-ooxml",
            "application/zip"
    );

    private static final Set<String> TXT_MIME_TYPES = Set.of(
            "text/plain",
            "text/x-matlab",
            "application/octet-stream"
    );

    public record ValidatedFileInfo(String safeFilename, String detectedMimeType) {}

    public ValidatedFileInfo validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be empty.");
        }

        // 1. Path Traversal & Filename Sanitization
        String safeName = Paths.get(originalFilename).getFileName().toString().trim();
        if (safeName.contains("..") || safeName.contains("/") || safeName.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename containing path traversal characters.");
        }

        // 2. Extension Check (including double extensions)
        String lowerName = safeName.toLowerCase(Locale.ROOT);
        String matchedExtension = null;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                matchedExtension = ext;
                break;
            }
        }

        if (matchedExtension == null) {
            throw new IllegalArgumentException("Unsupported file type. Allowed formats: .pdf, .docx, .txt");
        }

        // Check for suspicious trailing extension patterns (e.g., file.pdf.exe)
        String[] parts = lowerName.split("\\.");
        if (parts.length > 2) {
            String lastPart = "." + parts[parts.length - 1];
            if (!ALLOWED_EXTENSIONS.contains(lastPart)) {
                throw new IllegalArgumentException("Unsupported file type.");
            }
        }

        // 3. Apache Tika Content-Type Detection
        String detectedMimeType;
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, safeName);
            detectedMimeType = tika.detect(is, metadata);
        } catch (IOException e) {
            logger.error("Failed to inspect file stream for validation", e);
            throw new IllegalArgumentException("Failed to read and validate file content.");
        }

        if (detectedMimeType == null || detectedMimeType.isBlank()) {
            throw new IllegalArgumentException("Unable to detect document MIME type.");
        }

        // 4. Content Type & Extension Consistency Check
        switch (matchedExtension) {
            case ".pdf" -> {
                if (!PDF_MIME_TYPES.contains(detectedMimeType)) {
                    throw new IllegalArgumentException("File content does not match the .pdf file extension.");
                }
            }
            case ".docx" -> {
                if (!DOCX_MIME_TYPES.contains(detectedMimeType)) {
                    throw new IllegalArgumentException("File content does not match the .docx file extension.");
                }
            }
            case ".txt" -> {
                if (!detectedMimeType.startsWith("text/") && !TXT_MIME_TYPES.contains(detectedMimeType)) {
                    throw new IllegalArgumentException("File content does not match the .txt file extension.");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported file type.");
        }

        return new ValidatedFileInfo(safeName, detectedMimeType);
    }
}
