package com.ainexus.util;

import java.util.regex.Pattern;

public final class AuditDataSanitizer {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|pass|secret|token|apiKey|authorization|bearer)[\\s]*[=:][\\s]*([^,\\s&]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9-_=.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*");

    private AuditDataSanitizer() {}

    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String sanitized = PASSWORD_PATTERN.matcher(input).replaceAll("$1=***REDACTED***");
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer ***REDACTED***");
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("***REDACTED_JWT***");
        return sanitized;
    }
}
