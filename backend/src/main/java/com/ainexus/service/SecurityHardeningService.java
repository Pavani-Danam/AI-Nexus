package com.ainexus.service;

import org.springframework.web.multipart.MultipartFile;

public interface SecurityHardeningService {

    boolean isPromptSafe(String prompt);

    void validatePrompt(String prompt);

    void validateFileUpload(MultipartFile file);

    String sanitizeInput(String input);
}
