package com.ainexus.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class QuotaExceededException extends RuntimeException {

    private final String resourceType;
    private final long currentUsage;
    private final long limit;

    public QuotaExceededException(String resourceType, long currentUsage, long limit) {
        super(String.format("Workspace quota exceeded for '%s'. Current usage: %d, Limit: %d", resourceType, currentUsage, limit));
        this.resourceType = resourceType;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }

    public String getResourceType() {
        return resourceType;
    }

    public long getCurrentUsage() {
        return currentUsage;
    }

    public long getLimit() {
        return limit;
    }
}
