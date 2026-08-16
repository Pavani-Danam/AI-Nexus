package com.ainexus.model.vector;

import java.util.Collections;
import java.util.Map;

public record VectorQueryResult(
        String id,
        Double score,
        Map<String, Object> metadata
) {
    public VectorQueryResult {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
    }
}
