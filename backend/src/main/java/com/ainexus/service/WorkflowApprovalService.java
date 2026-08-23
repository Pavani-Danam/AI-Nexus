package com.ainexus.service;

import com.ainexus.dto.WorkflowApprovalDecisionRequest;
import com.ainexus.dto.WorkflowApprovalResponse;
import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowExecution;
import com.ainexus.entity.WorkflowStep;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkflowApprovalService {

    WorkflowApprovalResponse requestApproval(WorkflowExecution execution, WorkflowStep step, User requestedBy, String reason, LocalDateTime expiresAt);

    WorkflowExecutionResponse approveStep(Long approvalId, WorkflowApprovalDecisionRequest request, User approver);

    WorkflowExecutionResponse rejectStep(Long approvalId, WorkflowApprovalDecisionRequest request, User approver);

    WorkflowApprovalResponse getApprovalById(Long approvalId, User user);

    List<WorkflowApprovalResponse> getPendingApprovalsByWorkspace(Long workspaceId, User user);

    List<WorkflowApprovalResponse> getApprovalsByExecution(Long executionId, User user);

    void processExpiredApprovals();
}
