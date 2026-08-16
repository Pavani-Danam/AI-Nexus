package com.ainexus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class DocumentTextCleaningService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTextCleaningService.class);

    // Matches any control character except tab (\t) and newline (\n)
    private static final Pattern UNWANTED_CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    // Matches horizontal whitespace (spaces, non-breaking spaces, tabs)
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[\\h\\t]+");
    // Matches 3 or more consecutive newlines
    private static final Pattern EXCESSIVE_NEWLINES = Pattern.compile("\n{3,}");

    public String cleanText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Text content cannot be empty or blank.");
        }

        // 1. Normalize line endings (\r\n and \r to \n)
        String normalized = rawText.replace("\r\n", "\n").replace("\r", "\n");

        // 2. Remove unwanted control characters while preserving valid Unicode and formatting
        normalized = UNWANTED_CONTROL_CHARS.matcher(normalized).replaceAll("");

        // 3. Process line by line: normalize inline multiple spaces and trim line edges
        String[] lines = normalized.split("\n", -1);
        StringBuilder cleanedBuilder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Collapse multiple horizontal spaces into a single space
            String cleanLine = HORIZONTAL_WHITESPACE.matcher(line).replaceAll(" ").trim();
            cleanedBuilder.append(cleanLine);
            if (i < lines.length - 1) {
                cleanedBuilder.append("\n");
            }
        }

        String result = cleanedBuilder.toString();

        // 4. Reduce excessive consecutive blank lines (limit to 2 newlines, preserving paragraph breaks)
        result = EXCESSIVE_NEWLINES.matcher(result).replaceAll("\n\n");

        // 5. Trim leading and trailing overall whitespace
        result = result.trim();

        // 6. Verify meaningful content remains
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Text content is empty after cleaning and normalization.");
        }

        return result;
    }
}
