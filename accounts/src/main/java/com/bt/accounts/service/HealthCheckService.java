package com.bt.accounts.service;

import com.bt.accounts.dto.ServiceHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final DataSource dataSource;

    public ServiceHealthResponse checkHealth(String authToken) {
        return ServiceHealthResponse.builder()
                .serviceName("Accounts Service")
                .status("UP")
                .version("1.0.0")
                .databaseConnected(checkDatabaseConnection())
                .customerServiceConnected(true)
                .productServiceConnected(true)
                .fdCalculatorServiceConnected(true)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private Boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.error("Database connection check failed", e);
            return false;
        }
    }
}
