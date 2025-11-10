package com.bt.customer.service;

import com.bt.customer.dto.*;
import com.bt.customer.entity.User;
import com.bt.customer.exception.InvalidCredentialsException;
import com.bt.customer.exception.OtpException;
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
import org.springframework.util.StringUtils;
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
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class AuthService {

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int BASE_USERNAME_MAX_LENGTH = 32;

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

    @Autowired
    private RedisSessionService redisSessionService;

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
        String normalizedUsername = resolveUsername(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User.CustomerClassification classification = computeClassification(request.getDateOfBirth());

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .aadhaarNumber(request.getAadhaarNumber())
                .panNumber(request.getPanNumber())
                .dateOfBirth(request.getDateOfBirth())
                .preferredCurrency(request.getPreferredCurrency() != null ? request.getPreferredCurrency() : "KWD")
                .classification(classification)
                .role(request.getRole() != null ? request.getRole() : User.Role.CUSTOMER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        String sessionId = redisSessionService.createSession(savedUser);
        recordLogin(savedUser.getEmail(), "REGISTRATION");

        return new AuthResponse(
                sessionId,
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            boolean twoFA = customerService.isTwoFactorEnabledForCurrentUser()
                    || customerServiceIsTwoFactorEnabled(user.getEmail());
            if (!twoFA && user.getRole() == User.Role.BANKOFFICER) {
                redisOtpService.removeOtp(user.getEmail());
            }
            if (twoFA) {
                redisOtpService.removeOtp(user.getEmail());
                String code = generateOtp();
                redisOtpService.storeOtp(user.getEmail(), code);
                log.info("Generated OTP for user {}: {} (valid 5m)", user.getEmail(), code);
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    try {
                        sendOtpEmail(user.getEmail(), code);
                    } catch (Exception ex) {
                        log.warn("Failed to send OTP email to {}", user.getEmail());
                    }
                }
                AuthResponse resp = new AuthResponse(null, user.getUsername(), user.getEmail(),
                        user.getRole().name(), "OTP required");
                resp.setTwoFactorRequired(true);
                return resp;
            }

            String sessionId = redisSessionService.createSession(user);
            recordLogin(user.getEmail(), "PASSWORD");
            return new AuthResponse(sessionId, user.getUsername(), user.getEmail(), user.getRole().name(),
                    "Authentication successful");
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid password");
        }
    }

    public AuthResponse verifyOtp(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new OtpException("Invalid or expired OTP"));

        if (!redisOtpService.validateOtp(email, code)) {
            throw new OtpException("Invalid or expired OTP");
        }

        String sessionId = redisSessionService.createSession(user);
        recordLogin(user.getEmail(), "OTP");
        return new AuthResponse(sessionId, user.getUsername(), user.getEmail(), user.getRole().name(),
                "Authentication successful");
    }

    private boolean customerServiceIsTwoFactorEnabled(String email) {
        try {
            return customerService != null && customerService.isTwoFactorEnabled(email);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateOtp() {
        int n = new Random().nextInt(900000) + 100000;
        return Integer.toString(n);
    }

    private String resolveUsername(RegisterRequest request) {
        String provided = request.getUsername();
        if (StringUtils.hasText(provided)) {
            String trimmed = provided.trim();
            if (trimmed.length() < USERNAME_MIN_LENGTH || trimmed.length() > USERNAME_MAX_LENGTH) {
                throw new IllegalArgumentException("Username must be between " + USERNAME_MIN_LENGTH + " and "
                        + USERNAME_MAX_LENGTH + " characters");
            }
            if (userRepository.existsByUsername(trimmed)) {
                throw new UserAlreadyExistsException("Username already registered: " + trimmed);
            }
            return trimmed;
        }
        return generateUniqueUsername(request);
    }

    private String generateUniqueUsername(RegisterRequest request) {
        String base = deriveBaseUsername(request);
        String candidate = base;
        for (int attempt = 0; attempt < 10; attempt++) {
            if (attempt > 0) {
                String suffix = randomNumericSuffix();
                int maxBaseLength = USERNAME_MAX_LENGTH - suffix.length();
                String truncatedBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
                candidate = truncatedBase + suffix;
            }

            candidate = ensureMinLength(candidate);
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        String fallback = ("user" + System.currentTimeMillis()).toLowerCase(Locale.ROOT);
        fallback = fallback.length() > USERNAME_MAX_LENGTH ? fallback.substring(0, USERNAME_MAX_LENGTH) : fallback;
        return fallback;
    }

    private String deriveBaseUsername(RegisterRequest request) {
        String fullName = request.getFullName();
        if (StringUtils.hasText(fullName)) {
            String sanitized = fullName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (sanitized.length() >= USERNAME_MIN_LENGTH) {
                return limitLength(sanitized);
            }
        }

        String email = request.getEmail();
        if (StringUtils.hasText(email) && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
            if (localPart.length() >= USERNAME_MIN_LENGTH) {
                return limitLength(localPart);
            }
        }

        String phone = request.getPhoneNumber();
        if (StringUtils.hasText(phone)) {
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.length() >= USERNAME_MIN_LENGTH) {
                return limitLength(digits);
            }
        }

        return "user";
    }

    private String ensureMinLength(String candidate) {
        if (candidate.length() >= USERNAME_MIN_LENGTH) {
            return candidate;
        }
        StringBuilder builder = new StringBuilder(candidate);
        while (builder.length() < USERNAME_MIN_LENGTH) {
            builder.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        String result = builder.toString();
        return result.length() > USERNAME_MAX_LENGTH ? result.substring(0, USERNAME_MAX_LENGTH) : result;
    }

    private String limitLength(String value) {
        if (value.length() > BASE_USERNAME_MAX_LENGTH) {
            return value.substring(0, BASE_USERNAME_MAX_LENGTH);
        }
        return value;
    }

    private String randomNumericSuffix() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
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

    private User.CustomerClassification computeClassification(java.time.LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return User.CustomerClassification.REGULAR;
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        long age = java.time.temporal.ChronoUnit.YEARS.between(dateOfBirth, today);

        if (age < 18) {
            return User.CustomerClassification.MINOR;
        } else if (age >= 60) {
            return User.CustomerClassification.SENIOR;
        } else {
            return User.CustomerClassification.REGULAR;
        }
    }
}