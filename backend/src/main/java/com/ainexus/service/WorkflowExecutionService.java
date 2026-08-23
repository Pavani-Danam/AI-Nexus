package com.ainexus.service;

import com.ainexus.dto.WorkflowExecutionRequest;
import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.entity.User;

import java.util.List;

public interface WorkflowExecutionService {

    WorkflowExecutionResponse executeWorkflow(Long workflowId, WorkflowExecutionRequest request, User user);

    WorkflowExecutionResponse getExecutionById(Long executionId, User user);

    List<WorkflowExecutionResponse> getExecutionsByWorkflow(Long workflowId, User user);

    List<WorkflowExecutionResponse> getExecutionsByWorkspace(Long workspaceId, User user);
}
