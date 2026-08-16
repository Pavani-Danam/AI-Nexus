package com.ainexus.service;

import com.ainexus.exception.ResourceNotFoundException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentTextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTextExtractionService.class);
    private final Tika tika = new Tika();

    @Value("${app.document.max-extracted-characters:5000000}")
    private int maxExtractedCharacters;

    public String extractTextFromFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new ResourceNotFoundException("Document file not found at path: " + (filePath != null ? filePath.getFileName() : "null"));
        }

        try {
            long fileSize = Files.size(filePath);
            if (fileSize == 0) {
                throw new IllegalArgumentException("Document file is empty: no text can be extracted.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read document file size.", e);
        }

        String fileName = filePath.getFileName().toString().toLowerCase();

        // 1. Direct UTF-8 reading for TXT files
        if (fileName.endsWith(".txt")) {
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                String content = new String(bytes, StandardCharsets.UTF_8);
                if (content.trim().isEmpty()) {
                    throw new IllegalArgumentException("Extracted plain text document is empty.");
                }
                return validateExtractedSize(content);
            } catch (IOException e) {
                logger.error("Failed to read text file: {}", filePath.getFileName(), e);
                throw new IllegalArgumentException("Failed to read text file.", e);
            }
        }

        // 2. Apache Tika parsing for PDF, DOCX, and other structured documents
        try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath))) {
            tika.setMaxStringLength(maxExtractedCharacters);
            String extracted = tika.parseToString(is);

            if (extracted == null || extracted.trim().isEmpty()) {
                throw new IllegalArgumentException("No extractable text content found in document.");
            }

            return validateExtractedSize(extracted);
        } catch (TikaException e) {
            logger.error("Tika extraction failed for file: {}", filePath.getFileName(), e);
            throw new IllegalArgumentException("Failed to parse and extract text from document: file may be corrupted or password-protected.", e);
        } catch (IOException e) {
            logger.error("IO error during document parsing: {}", filePath.getFileName(), e);
            throw new IllegalArgumentException("IO error while reading document for text extraction.", e);
        }
    }

    private String validateExtractedSize(String text) {
        if (text.length() > maxExtractedCharacters) {
            logger.warn("Extracted text exceeds maximum character limit ({} > {}). Truncating.", text.length(), maxExtractedCharacters);
            return text.substring(0, maxExtractedCharacters);
        }
        return text;
    }
}
