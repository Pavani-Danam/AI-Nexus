package com.ainexus.controller;

import com.ainexus.entity.AuditLog;
import com.ainexus.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/user")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId));
    }
}
