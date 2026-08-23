package com.ainexus.service;

import com.ainexus.dto.WorkflowScheduleRequest;
import com.ainexus.dto.WorkflowScheduleResponse;
import com.ainexus.entity.User;

import java.util.List;

public interface WorkflowScheduleService {

    WorkflowScheduleResponse createSchedule(Long workflowId, WorkflowScheduleRequest request, User user);

    WorkflowScheduleResponse updateSchedule(Long scheduleId, WorkflowScheduleRequest request, User user);

    WorkflowScheduleResponse toggleSchedule(Long scheduleId, boolean enabled, User user);

    WorkflowScheduleResponse getScheduleById(Long scheduleId, User user);

    List<WorkflowScheduleResponse> getSchedulesByWorkflow(Long workflowId, User user);

    List<WorkflowScheduleResponse> getSchedulesByWorkspace(Long workspaceId, User user);

    void deleteSchedule(Long scheduleId, User user);

    void processDueSchedules();
}
