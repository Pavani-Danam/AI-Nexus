package com.ainexus.service;

import com.ainexus.dto.WorkflowRequest;
import com.ainexus.dto.WorkflowResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowStatus;

import java.util.List;

public interface WorkflowService {

    WorkflowResponse createWorkflow(WorkflowRequest request, User user);

    WorkflowResponse getWorkflowById(Long workflowId, User user);

    List<WorkflowResponse> getWorkflowsByWorkspace(Long workspaceId, User user);

    WorkflowResponse updateWorkflow(Long workflowId, WorkflowRequest request, User user);

    void deleteWorkflow(Long workflowId, User user);

    WorkflowResponse updateWorkflowStatus(Long workflowId, WorkflowStatus status, User user);
}
