package com.ainexus.dto;

import java.time.LocalDateTime;

public record MemoryMessage(
        Long id,
        String role,
        String content,
        LocalDateTime timestamp
) {
    public static MemoryMessage user(Long id, String content, LocalDateTime timestamp) {
        return new MemoryMessage(id, "USER", content, timestamp);
    }

    public static MemoryMessage assistant(Long id, String content, LocalDateTime timestamp) {
        return new MemoryMessage(id, "ASSISTANT", content, timestamp);
    }

    public static MemoryMessage of(Long id, String sender, String content, LocalDateTime timestamp) {
        String role = (sender != null && sender.equalsIgnoreCase("ASSISTANT")) ? "ASSISTANT" : "USER";
        return new MemoryMessage(id, role, content, timestamp);
    }
}
