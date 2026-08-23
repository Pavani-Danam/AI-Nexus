package com.ainexus.entity;

public enum WorkflowFailureType {
    TRANSIENT_FAILURE(true),
    TIMEOUT_FAILURE(true),
    RATE_LIMIT_FAILURE(true),
    DEPENDENCY_FAILURE(true),
    PERMANENT_FAILURE(false),
    AUTHORIZATION_FAILURE(false),
    VALIDATION_FAILURE(false);

    private final boolean recoverable;

    WorkflowFailureType(boolean recoverable) {
        this.recoverable = recoverable;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public static WorkflowFailureType classify(Throwable throwable) {
        if (throwable == null) return PERMANENT_FAILURE;
        String msg = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";

        if (msg.contains("unauthorized") || msg.contains("forbidden") || msg.contains("access denied")) {
            return AUTHORIZATION_FAILURE;
        }
        if (msg.contains("validation") || msg.contains("invalid") || msg.contains("bad request") || msg.contains("null")) {
            return VALIDATION_FAILURE;
        }
        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("deadline exceeded")) {
            return TIMEOUT_FAILURE;
        }
        if (msg.contains("rate limit") || msg.contains("429") || msg.contains("too many requests") || msg.contains("quota")) {
            return RATE_LIMIT_FAILURE;
        }
        if (msg.contains("dependency") || msg.contains("connection refused") || msg.contains("service unavailable") || msg.contains("503")) {
            return DEPENDENCY_FAILURE;
        }
        if (msg.contains("transient") || msg.contains("temporary") || msg.contains("retryable") || msg.contains("socket")) {
            return TRANSIENT_FAILURE;
        }

        return PERMANENT_FAILURE;
    }
}
