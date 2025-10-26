package com.bt.customer.service;

import com.bt.customer.dto.*;
import com.bt.customer.entity.User;
import com.bt.customer.exception.InvalidCredentialsException;
import com.bt.customer.exception.UserAlreadyExistsException;
import com.bt.customer.repository.UserRepository;
import com.bt.customer.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    private static final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static class OtpEntry {
        final String code;
        final Instant expiresAt;
        OtpEntry(String code, Instant expiresAt) { this.code = code; this.expiresAt = expiresAt; }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole() != null ? request.getRole() : User.Role.CUSTOMER)
                .active(true)
                .build();

        userRepository.save(user);

        String token = tokenProvider.generateTokenForUser(user.getUsername(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name(),
                "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            boolean twoFA = customerService.isTwoFactorEnabledForCurrentUser() || customerServiceIsTwoFactorEnabled(user.getUsername());
            if (twoFA) {
                String code = generateOtp();
                Instant exp = Instant.now().plusSeconds(300);
                otpStore.put(user.getUsername(), new OtpEntry(code, exp));
                log.info("Generated OTP for user {}: {} (valid 5m)", user.getUsername(), code);
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    try {
                        sendOtpEmail(user.getEmail(), code);
                    } catch (Exception ex) {
                        log.warn("Failed to send OTP email to {}", user.getEmail());
                    }
                }
                AuthResponse resp = new AuthResponse(null, user.getUsername(), user.getRole().name(), "OTP required");
                resp.setTwoFactorRequired(true);
                return resp;
            }

            String token = tokenProvider.generateTokenForUser(user.getUsername(), user.getRole().name());
            return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Authentication successful");
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    public AuthResponse verifyOtp(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        OtpEntry entry = otpStore.get(username);
        if (entry == null || entry.expiresAt.isBefore(Instant.now()) || !entry.code.equals(code)) {
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }
        otpStore.remove(username);
        String token = tokenProvider.generateTokenForUser(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Authentication successful");
    }

    private boolean customerServiceIsTwoFactorEnabled(String username) {
        try {
            return customerService != null && customerService.isTwoFactorEnabled(username);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateOtp() {
        int n = new Random().nextInt(900000) + 100000;
        return Integer.toString(n);
    }

    private void sendOtpEmail(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(to);
        msg.setSubject("Your One-Time Password (OTP)");
        msg.setText("Your OTP is: " + code + "\nIt will expire in 5 minutes.");
        mailSender.send(msg);
    }
}
