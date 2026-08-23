package com.ainexus.service;

import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.dto.WorkflowMonitoringSummaryResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowAuditEvent;
import com.ainexus.entity.WorkflowAuditEventType;
import com.ainexus.entity.WorkflowExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WorkflowMonitoringService {

    void recordAuditEvent(WorkflowAuditEventType eventType, Long workflowId, Long workspaceId,
                          Long executionId, String actorUsername, String description);

    Page<WorkflowExecutionResponse> getExecutionHistory(Long workspaceId, Long workflowId,
                                                        WorkflowExecutionStatus status,
                                                        Pageable pageable, User user);

    WorkflowExecutionResponse getExecutionDetails(Long executionId, User user);

    List<WorkflowAuditEvent> getAuditEventsByExecution(Long executionId, User user);

    Page<WorkflowAuditEvent> getAuditEventsByWorkspace(Long workspaceId, Pageable pageable, User user);

    WorkflowMonitoringSummaryResponse getWorkspaceMonitoringSummary(Long workspaceId, User user);
}
