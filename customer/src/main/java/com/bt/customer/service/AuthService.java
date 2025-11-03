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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

    @Autowired
    private RedisOtpService redisOtpService;

    @Autowired
    private RedisTokenService redisTokenService;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    private static final Map<String, Deque<LoginEvent>> loginActivity = new ConcurrentHashMap<>();

    private static class LoginEvent {
        final String type;
        final String ip;
        final String agent;
        final Instant at;

        LoginEvent(String type, String ip, String agent, Instant at) {
            this.type = type;
            this.ip = ip;
            this.agent = agent;
            this.at = at;
        }
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

            boolean twoFA = customerService.isTwoFactorEnabledForCurrentUser()
                    || customerServiceIsTwoFactorEnabled(user.getUsername());
            if (twoFA) {
                String code = generateOtp();
                redisOtpService.storeOtp(user.getUsername(), code);
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
            redisTokenService.storeToken(token, user.getUsername());
            recordLogin(user.getUsername(), "PASSWORD");
            return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Authentication successful");
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    public AuthResponse verifyOtp(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!redisOtpService.validateOtp(username, code)) {
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }

        String token = tokenProvider.generateTokenForUser(user.getUsername(), user.getRole().name());
        redisTokenService.storeToken(token, user.getUsername());
        recordLogin(user.getUsername(), "OTP");
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

    private void recordLogin(String username, String type) {
        HttpServletRequest req = currentRequest();
        String ip = req != null ? clientIp(req) : "";
        String agent = req != null ? String.valueOf(req.getHeader("User-Agent")) : "";
        LoginEvent ev = new LoginEvent(type, ip, agent, Instant.now());
        loginActivity.computeIfAbsent(username, k -> new ArrayDeque<>()).addFirst(ev);
        Deque<LoginEvent> dq = loginActivity.get(username);
        while (dq.size() > 20)
            dq.removeLast();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank())
            return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    public List<Map<String, Object>> recentLoginActivity(String username, int limit) {
        Deque<LoginEvent> dq = loginActivity.getOrDefault(username, new ArrayDeque<>());
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 0;
        for (LoginEvent e : dq) {
            if (i++ >= limit)
                break;
            out.add(Map.of(
                    "type", e.type,
                    "ip", e.ip,
                    "agent", e.agent,
                    "timestamp", e.at.toString()));
        }
        return out;
    }
}
