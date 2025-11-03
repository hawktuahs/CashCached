package com.bt.accounts.controller;

import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.dto.ServiceHealthResponse;
import com.bt.accounts.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Health & Status", description = "Service health check and status APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class HealthController {

    private final HealthCheckService healthCheckService;

    @GetMapping("/status")
    @Operation(summary = "Service health check", description = "Returns service health status and connectivity with dependent services")
    public ResponseEntity<ApiResponse<ServiceHealthResponse>> getServiceStatus(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authToken) {

        ServiceHealthResponse health = healthCheckService.checkHealth(authToken);

        ApiResponse<ServiceHealthResponse> response = ApiResponse.<ServiceHealthResponse>builder()
                .success(true)
                .message("Service status retrieved successfully")
                .data(health)
                .build();

        return ResponseEntity.ok(response);
    }
}
