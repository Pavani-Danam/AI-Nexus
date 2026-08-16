package com.ainexus.dto;

public record RAGPrompt(
        String systemInstruction,
        String retrievedContext,
        String userQuestion,
        String fullPrompt,
        boolean hasContext
) {}
