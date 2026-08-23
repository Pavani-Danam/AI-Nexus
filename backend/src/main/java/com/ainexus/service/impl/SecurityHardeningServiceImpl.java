package com.ainexus.service.impl;

import com.ainexus.exception.FileStorageException;
import com.ainexus.service.SecurityHardeningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SecurityHardeningServiceImpl implements SecurityHardeningService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHardeningServiceImpl.class);

    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore (all )?(previous|prior) (instructions|directions|prompts)"),
            Pattern.compile("(?i)disregard (all )?(previous|prior) (instructions|rules)"),
            Pattern.compile("(?i)you are now in (DAN|developer|unrestricted) mode"),
            Pattern.compile("(?i)reveal (your )?(system prompt|initial instructions|secret keys)"),
            Pattern.compile("(?i)system: override current instructions"),
            Pattern.compile("(?i)print the secret (key|token|password)")
    );

    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            "exe", "sh", "bat", "cmd", "jsp", "php", "py", "pl", "vbs", "ps1", "jar", "war", "dll", "bin", "com"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/json",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    @Override
    public boolean isPromptSafe(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return true;
        }
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                logger.warn("[SECURITY ALERT] Prompt injection detected matching pattern: {}", pattern.pattern());
                return false;
            }
        }
        return true;
    }

    @Override
    public void validatePrompt(String prompt) {
        if (!isPromptSafe(prompt)) {
            throw new IllegalArgumentException("Rejected input: Prompt contains prohibited instructions or adversarial patterns.");
        }
    }

    @Override
    public void validateFileUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File upload failed: Provided file is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileStorageException("File upload failed: Missing filename.");
        }

        // Path traversal check
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new FileStorageException("File upload failed: Malicious filename detected with path traversal sequence.");
        }

        // Dangerous extension check
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex != -1) {
            String ext = originalFilename.substring(dotIndex + 1).toLowerCase();
            if (DANGEROUS_EXTENSIONS.contains(ext)) {
                logger.warn("[SECURITY ALERT] Rejected upload with dangerous extension: {}", ext);
                throw new FileStorageException("File upload rejected: Executable and script file extensions are prohibited.");
            }
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            boolean allowed = ALLOWED_MIME_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(contentType.trim()));
            if (!allowed && !contentType.startsWith("text/")) {
                logger.warn("[SECURITY ALERT] Rejected unsupported MIME type: {}", contentType);
                throw new FileStorageException("File upload rejected: Unsupported content type (" + contentType + ")");
            }
        }
    }

    @Override
    public String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replace("\0", "").trim();
    }
}
