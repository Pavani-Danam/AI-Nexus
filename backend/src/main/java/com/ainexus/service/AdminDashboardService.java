package com.ainexus.service;

import com.ainexus.dto.AdminDashboardSummaryResponse;
import com.ainexus.entity.User;

public interface AdminDashboardService {

    AdminDashboardSummaryResponse getDashboardSummary(User requestingUser);

    boolean isAdmin(User user);
}
