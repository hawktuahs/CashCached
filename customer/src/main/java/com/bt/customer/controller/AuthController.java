package com.bt.customer.controller;

import com.bt.customer.dto.AuthResponse;
import com.bt.customer.dto.LoginRequest;
import com.bt.customer.dto.RegisterRequest;
import com.bt.customer.dto.MagicLinkRequest;
import com.bt.customer.service.AuthService;
import com.bt.customer.service.MagicLinkService;
import com.bt.customer.service.RedisSessionService;
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

    @Autowired
    private MagicLinkService magicLinkService;

    @Autowired
    private RedisSessionService redisSessionService;

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
        String email = String.valueOf(body.getOrDefault("email", ""));
        String code = String.valueOf(body.getOrDefault("code", ""));
        AuthResponse response = authService.verifyOtp(email, code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates current session")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String sessionId = authHeader.substring(7);
            redisSessionService.invalidateSession(sessionId);
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @PostMapping("/magic-link/request")
    @Operation(summary = "Request magic link", description = "Sends a magic link to the user's email for passwordless sign-in")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Magic link sent successfully"),
            @ApiResponse(responseCode = "404", description = "Email not found"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<Map<String, String>> requestMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        magicLinkService.sendMagicLink(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Magic link sent to your email. Valid for 15 minutes."));
    }

    @PostMapping("/magic-link/verify")
    @Operation(summary = "Verify and authenticate with magic link", description = "Verifies the magic link token and creates a session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired magic link")
    })
    public ResponseEntity<AuthResponse> verifyMagicLink(@RequestParam String token) {
        AuthResponse response = magicLinkService.verifyAndAuthenticateWithMagicLink(token);
        return ResponseEntity.ok(response);
    }
}
