package com.ainexus.controller;

import com.ainexus.dto.AdminDashboardSummaryResponse;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.AdminDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    private final AdminDashboardService adminDashboardService;
    private final UserRepository userRepository;

    public AdminDashboardController(AdminDashboardService adminDashboardService, UserRepository userRepository) {
        this.adminDashboardService = adminDashboardService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication principal missing.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<AdminDashboardSummaryResponse> getDashboardSummary(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        logger.info("REST: Fetching admin dashboard summary for user: {}", user.getUsername());
        AdminDashboardSummaryResponse summary = adminDashboardService.getDashboardSummary(user);
        return ResponseEntity.ok(summary);
    }
}
