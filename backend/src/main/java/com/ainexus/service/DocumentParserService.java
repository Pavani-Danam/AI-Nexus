package com.ainexus.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Service
public class DocumentParserService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentParserService.class);
    private final Tika tika = new Tika();

    public String extractText(Path filePath) throws IOException {
        File file = filePath.toFile();
        if (!file.exists()) {
            throw new IOException("File not found at path: " + filePath);
        }

        try (InputStream stream = new FileInputStream(file)) {
            // Use unlimited write limit for full document text extraction
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();

            parser.parse(stream, handler, metadata, context);
            String content = handler.toString();

            if (content == null || content.trim().isEmpty()) {
                logger.warn("Extracted content is empty for file: {}", filePath);
                return "";
            }

            return cleanText(content);
        } catch (SAXException | TikaException e) {
            logger.error("Error parsing document with Tika: {}", filePath, e);
            throw new IOException("Failed to parse document content: " + e.getMessage(), e);
        }
    }

    public String detectContentType(Path filePath) throws IOException {
        return tika.detect(filePath.toFile());
    }

    private String cleanText(String text) {
        // Normalize whitespace and remove control characters
        return text.replaceAll("[\\r\\n]+", "\n")
                   .replaceAll("[ \\t]+", " ")
                   .trim();
    }
}
