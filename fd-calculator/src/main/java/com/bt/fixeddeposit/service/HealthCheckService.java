package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.dto.ServiceHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final DataSource dataSource;

    public ServiceHealthResponse checkServiceHealth() {
        String databaseStatus = checkDatabaseConnection();

        String overallStatus = "UP".equals(databaseStatus) ? "HEALTHY" : "DEGRADED";

        return ServiceHealthResponse.builder()
                .serviceName("FD Calculator Service")
                .status(overallStatus)
                .customerServiceAvailable(true)
                .productServiceAvailable(true)
                .databaseStatus(databaseStatus)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.error("Database connection check failed", e);
            return "DOWN";
        }
    }
}
