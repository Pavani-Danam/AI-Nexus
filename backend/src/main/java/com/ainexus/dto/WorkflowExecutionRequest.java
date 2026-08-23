package com.ainexus.dto;

import java.util.Map;

public record WorkflowExecutionRequest(
        String inputQuery,
        Map<String, Object> inputVariables
) {}
