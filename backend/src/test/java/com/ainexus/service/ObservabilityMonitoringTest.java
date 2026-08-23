package com.ainexus.service;

import com.ainexus.controller.HealthController;
import com.ainexus.filter.CorrelationIdFilter;
import com.ainexus.service.impl.ObservabilityMetricsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservabilityMonitoringTest {

    private ObservabilityMetricsService metricsService;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private HealthController healthController;
    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        metricsService = new ObservabilityMetricsServiceImpl();
        healthController = new HealthController(dataSource, metricsService);
        correlationIdFilter = new CorrelationIdFilter();
    }

    @Test
    @DisplayName("TEST 1: Metrics service tracks AI requests and average latency")
    void testAiMetricsTracking() {
        metricsService.recordAiRequest(true, 100);
        metricsService.recordAiRequest(true, 300);
        metricsService.recordAiRequest(false, 200);

        Map<String, Object> snapshot = metricsService.getSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> aiMetrics = (Map<String, Object>) snapshot.get("ai");

        assertEquals(3L, aiMetrics.get("totalRequests"));
        assertEquals(2L, aiMetrics.get("successful"));
        assertEquals(1L, aiMetrics.get("failed"));
        assertEquals(200L, aiMetrics.get("avgLatencyMs"));
    }

    @Test
    @DisplayName("TEST 2: Metrics service records workflows, retries, and agent replans")
    void testWorkflowAndAgentMetrics() {
        metricsService.recordWorkflowExecution(true);
        metricsService.recordWorkflowExecution(false);
        metricsService.recordWorkflowRetry();
        metricsService.recordAgentReplan();
        metricsService.recordHttpError(403);
        metricsService.recordHttpError(500);

        Map<String, Object> snapshot = metricsService.getSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> wfMetrics = (Map<String, Object>) snapshot.get("workflows");
        @SuppressWarnings("unchecked")
        Map<String, Object> agentMetrics = (Map<String, Object>) snapshot.get("agents");
        @SuppressWarnings("unchecked")
        Map<String, Object> errMetrics = (Map<String, Object>) snapshot.get("errors");

        assertEquals(2L, wfMetrics.get("totalExecutions"));
        assertEquals(1L, wfMetrics.get("retriesTriggered"));
        assertEquals(1L, agentMetrics.get("replanningAttempts"));
        assertEquals(1L, errMetrics.get("http4xx"));
        assertEquals(1L, errMetrics.get("http5xx"));
    }

    @Test
    @DisplayName("TEST 3: Health liveness endpoint returns UP status")
    void testLivenessProbe() {
        ResponseEntity<Map<String, String>> res = healthController.livenessCheck();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("UP", res.getBody().get("status"));
        assertEquals("liveness", res.getBody().get("probe"));
    }

    @Test
    @DisplayName("TEST 4: Readiness check passes when database connection is valid")
    void testReadinessProbeHealthyDb() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<Map<String, Object>> res = healthController.readinessCheck();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("UP", res.getBody().get("status"));
    }

    @Test
    @DisplayName("TEST 5: Readiness check returns 503 SERVICE_UNAVAILABLE when database is down")
    void testReadinessProbeUnhealthyDb() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database offline"));

        ResponseEntity<Map<String, Object>> res = healthController.readinessCheck();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertEquals("DOWN", res.getBody().get("status"));
    }

    @Test
    @DisplayName("TEST 6: Correlation ID filter generates and attaches X-Correlation-ID header")
    void testCorrelationIdFilter() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        correlationIdFilter.doFilter(req, res, filterChain);

        String header = res.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(header);
        assertFalse(header.isBlank());
        verify(filterChain, times(1)).doFilter(req, res);
    }
}
