package com.ainexus.service;

import com.ainexus.dto.DashboardSummaryResponse;
import com.ainexus.entity.User;

public interface DashboardService {
    DashboardSummaryResponse getDashboardSummary(Long workspaceId, User user);
}
