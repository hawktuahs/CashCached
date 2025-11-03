package com.bt.fixeddeposit.controller;

import com.bt.fixeddeposit.dto.ServiceHealthResponse;
import com.bt.fixeddeposit.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fd")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Service health and status APIs")
public class HealthController {

    private final HealthCheckService healthCheckService;

    @GetMapping("/status")
    @Operation(summary = "Service health check", description = "Check health status of FD Calculator service and its dependencies")
    public ResponseEntity<ServiceHealthResponse> getServiceHealth() {
        ServiceHealthResponse response = healthCheckService.checkServiceHealth();
        return ResponseEntity.ok(response);
    }
}
