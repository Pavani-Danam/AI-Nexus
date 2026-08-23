package com.ainexus.service;

import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowFailureType;

public interface WorkflowRecoveryService {

    WorkflowExecutionResponse recoverExecution(Long executionId, User user);

    WorkflowFailureType classifyFailure(Throwable throwable);

    boolean isRecoverable(WorkflowFailureType failureType);
}
