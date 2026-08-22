package com.ainexus.service;

import com.ainexus.dto.FailureCategory;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

public class AgentRetryPolicy {

    private static final Logger logger = LoggerFactory.getLogger(AgentRetryPolicy.class);

    private final int maxAttempts;
    private final long initialDelayMs;
    private final long maxDelayMs;

    public AgentRetryPolicy() {
        this(3, 100L, 2000L);
    }

    public AgentRetryPolicy(int maxAttempts, long initialDelayMs, long maxDelayMs) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialDelayMs = Math.max(10L, initialDelayMs);
        this.maxDelayMs = Math.max(this.initialDelayMs, maxDelayMs);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long calculateDelay(int attempt) {
        if (attempt <= 1) {
            return 0L;
        }
        // Exponential backoff: initialDelay * 2^(attempt - 2)
        long exponential = initialDelayMs * (1L << Math.min(attempt - 2, 10));
        long bounded = Math.min(exponential, maxDelayMs);
        // Add small jitter (+/- 10%)
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1L, bounded / 10));
        return Math.min(bounded + jitter, maxDelayMs);
    }

    public FailureCategory classifyException(Throwable throwable) {
        if (throwable == null) {
            return FailureCategory.PERMANENT_FAILURE;
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String message = root.getMessage() != null ? root.getMessage().toLowerCase() : "";

        if (root instanceof UnauthorizedAccessException || message.contains("unauthorized") || message.contains("forbidden") || message.contains("access denied")) {
            return FailureCategory.AUTHORIZATION_FAILURE;
        }

        if (root instanceof IllegalArgumentException || root instanceof ResourceNotFoundException || message.contains("invalid plan") || message.contains("unsupported task type")) {
            return FailureCategory.VALIDATION_FAILURE;
        }

        if (root instanceof TimeoutException || root instanceof SocketTimeoutException || message.contains("timed out") || message.contains("timeout")) {
            return FailureCategory.TIMEOUT_FAILURE;
        }

        if (message.contains("rate limit") || message.contains("429") || message.contains("quota exceeded") || message.contains("resource exhausted")) {
            return FailureCategory.RATE_LIMIT_FAILURE;
        }

        if (message.contains("503") || message.contains("502") || message.contains("500") || message.contains("unavailable") || message.contains("connection refused") || message.contains("vector index") || message.contains("transient")) {
            return FailureCategory.TRANSIENT_FAILURE;
        }

        return FailureCategory.TRANSIENT_FAILURE;
    }
}
