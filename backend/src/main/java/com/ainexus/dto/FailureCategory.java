package com.ainexus.dto;

public enum FailureCategory {
    TRANSIENT_FAILURE(true),
    RATE_LIMIT_FAILURE(true),
    TIMEOUT_FAILURE(true),
    PERMANENT_FAILURE(false),
    AUTHORIZATION_FAILURE(false),
    VALIDATION_FAILURE(false),
    DEPENDENCY_FAILURE(false);

    private final boolean retryable;

    FailureCategory(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
