package com.ainexus.dto;

public record TextChunk(
        int index,
        String content,
        int startOffset,
        int endOffset,
        int characterCount
) {
    public TextChunk(int index, String content, int startOffset, int endOffset) {
        this(index, content, startOffset, endOffset, content != null ? content.length() : 0);
    }
}
