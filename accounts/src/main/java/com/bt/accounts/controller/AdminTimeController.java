package com.bt.accounts.controller;

import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.time.TimeProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/accounts/admin/time", "/api/admin/time"})
@RequiredArgsConstructor
@Tag(name = "Admin Time", description = "Admin controls for system time")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminTimeController {

    private final TimeProvider timeProvider;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
    @Operation(summary = "Get current system time")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("systemTime", timeProvider.now().toString());
        data.put("offsetSeconds", timeProvider.getOffsetSeconds());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("OK")
                .data(data)
                .build());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
    @Operation(summary = "Set absolute system time")
    public ResponseEntity<ApiResponse<Void>> setTime(@RequestBody Map<String, String> body) {
        String iso = body.get("systemTime");
        timeProvider.setAbsolute(Instant.parse(iso));
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Time set").build());
    }

    @PostMapping("/advance")
    @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
    @Operation(summary = "Advance system time by seconds")
    public ResponseEntity<ApiResponse<Void>> advance(@RequestBody Map<String, Long> body) {
        long seconds = body.getOrDefault("seconds", 0L);
        timeProvider.advanceSeconds(seconds);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Time advanced").build());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
    @Operation(summary = "Reset system time to real clock")
    public ResponseEntity<ApiResponse<Void>> reset() {
        timeProvider.reset();
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Time reset").build());
    }
}
