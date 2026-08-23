package com.ainexus.controller;

import com.ainexus.service.ObservabilityMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final DataSource dataSource;
    private final ObservabilityMetricsService metricsService;

    public HealthController(DataSource dataSource, ObservabilityMetricsService metricsService) {
        this.dataSource = dataSource;
        this.metricsService = metricsService;
    }

    private boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> fullHealthCheck() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());

        boolean dbUp = isDatabaseHealthy();
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", Map.of("status", dbUp ? "UP" : "DOWN"));

        File disk = new File(".");
        long freeSpaceMb = disk.getFreeSpace() / (1024 * 1024);
        components.put("diskSpace", Map.of("status", freeSpaceMb > 500 ? "UP" : "LOW", "freeMb", freeSpaceMb));
        components.put("vectorStore", Map.of("status", "UP"));

        boolean overallUp = dbUp;
        response.put("status", overallUp ? "UP" : "DOWN");
        response.put("components", components);

        return overallUp ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> livenessCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "probe", "liveness",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        boolean dbHealthy = isDatabaseHealthy();
        if (dbHealthy) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "probe", "readiness",
                    "database", "UP"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "probe", "readiness",
                    "database", "DOWN"
            ));
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(metricsService.getSnapshot());
    }
}
