package com.bt.customer.controller;

import com.bt.customer.dto.AuthResponse;
import com.bt.customer.dto.LoginRequest;
import com.bt.customer.dto.RegisterRequest;
import com.bt.customer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account with encrypted password. Default role is CUSTOMER.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and issues JWT token containing username and role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Completes login when two-factor authentication is enabled")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody Map<String, String> body) {
        String username = String.valueOf(body.getOrDefault("username", ""));
        String code = String.valueOf(body.getOrDefault("code", ""));
        AuthResponse response = authService.verifyOtp(username, code);
        return ResponseEntity.ok(response);
    }
}
