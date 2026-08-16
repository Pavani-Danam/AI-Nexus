package com.ainexus.model.vector;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record VectorRecord(
        String id,
        List<Float> values,
        Map<String, Object> metadata
) {
    public VectorRecord {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Vector ID cannot be null or empty");
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Vector values cannot be null or empty");
        }
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
    }
}
