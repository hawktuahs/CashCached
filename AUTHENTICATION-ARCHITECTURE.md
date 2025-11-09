# Authentication Architecture in CashCached Banking System

## Overview

The CashCached banking system implements a centralized authentication architecture using Redis-based session management. This document explains in-depth how authentication works across all microservices, with a focus on how the product-pricing module validates user credentials without direct database access.

---

## Architecture Components

### 1. Centralized Authentication Service (Customer Service - Port 8081)

The Customer Service acts as the **authentication authority** for the entire system. It is the only service that:
- Has direct access to user credentials in the database
- Validates usernames and passwords
- Creates and manages user sessions
- Issues session tokens (session IDs)

### 2. Shared Redis Cache

Redis serves as the **distributed session store** accessible by all microservices:
- Stores session data with expiration times
- Enables stateless authentication across services
- Provides fast session validation (O(1) lookups)
- Ensures single source of truth for active sessions

### 3. Service-Level Authentication Filters

Each microservice (accounts, product-pricing, fd-calculator) implements:
- JWT/Session authentication filters
- Redis session validation
- Spring Security integration
- Role-based access control

---

## Complete Authentication Flow

### Phase 1: User Registration

```
User → Main Gateway → Customer Service → Database
```

**Step 1: Registration Request**
```bash
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
    "username": "john.doe",
    "password": "SecurePass@123",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "CUSTOMER"
}
```

**Step 2: Customer Service Processing**

Located in: `customer/src/main/java/com/bt/customer/controller/AuthController.java`

```java
@PostMapping("/register")
@Operation(summary = "Register new user")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
}
```

**Step 3: Password Encryption & Storage**

Located in: `customer/src/main/java/com/bt/customer/service/AuthService.java`

```java
public AuthResponse register(RegisterRequest request) {
    // 1. Check if username/email already exists
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new RuntimeException("Username already exists");
    }
    
    // 2. Create user entity
    User user = User.builder()
        .username(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword()))  // BCrypt encryption
        .email(request.getEmail())
        .fullName(request.getFullName())
        .role(Role.valueOf(request.getRole()))
        .build();
    
    // 3. Save to database
    User savedUser = userRepository.save(user);
    
    // 4. Create session in Redis
    String sessionId = redisSessionService.createSession(savedUser);
    
    // 5. Return response with session ID
    return AuthResponse.builder()
        .token(sessionId)
        .username(savedUser.getUsername())
        .email(savedUser.getEmail())
        .role(savedUser.getRole().name())
        .build();
}
```

---

### Phase 2: User Login (Session Creation)

```
User → Main Gateway → Customer Service → Database
                                      ↓
                                    Redis
```

**Step 1: Login Request**

```bash
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "username": "john.doe",
    "password": "SecurePass@123"
}
```

**Step 2: Credential Validation**

Located in: `customer/src/main/java/com/bt/customer/service/AuthService.java`

```java
public AuthResponse login(LoginRequest request) {
    // 1. Find user by username
    User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    
    // 2. Verify password using BCrypt
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid credentials");
    }
    
    // 3. Check if 2FA is enabled
    if (user.isTwoFactorEnabled()) {
        // Send OTP and return intermediate response
        return handleTwoFactorAuth(user);
    }
    
    // 4. Create session in Redis
    String sessionId = redisSessionService.createSession(user);
    
    // 5. Return session ID as token
    return AuthResponse.builder()
        .token(sessionId)
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();
}
```

**Step 3: Redis Session Creation**

Located in: `customer/src/main/java/com/bt/customer/service/RedisSessionService.java`

```java
@Service
public class RedisSessionService {
    
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String IDLE_TIMEOUT_PREFIX = "session_idle:";
    
    @Value("${app.session.timeout-seconds:3600}")
    private long sessionTimeoutSeconds;  // 1 hour
    
    @Value("${app.session.idle-timeout-seconds:900}")
    private long idleTimeoutSeconds;  // 15 minutes
    
    public String createSession(User user) {
        // 1. Generate unique session ID (UUID)
        String sessionId = UUID.randomUUID().toString();
        
        // 2. Invalidate any existing session for this user
        String existingSession = getActiveSessionForUser(user.getEmail());
        if (existingSession != null) {
            invalidateSession(existingSession);
        }
        
        // 3. Build session data object
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("email", user.getEmail());
        sessionData.put("userId", user.getId());
        sessionData.put("role", user.getRole().name());  // ADMIN, BANKOFFICER, CUSTOMER
        sessionData.put("createdAt", Instant.now().getEpochSecond());
        sessionData.put("lastActivity", Instant.now().getEpochSecond());
        
        // 4. Serialize to JSON
        String sessionJson = objectMapper.writeValueAsString(sessionData);
        
        // 5. Store in Redis with multiple keys
        String sessionKey = SESSION_PREFIX + sessionId;  // "session:uuid"
        String userSessionKey = USER_SESSIONS_PREFIX + user.getEmail();  // "user_sessions:email"
        String idleKey = IDLE_TIMEOUT_PREFIX + sessionId;  // "session_idle:uuid"
        
        // Primary session data (expires after 1 hour)
        redisTemplate.opsForValue().set(sessionKey, sessionJson, sessionTimeoutSeconds, TimeUnit.SECONDS);
        
        // User to session mapping (expires after 1 hour)
        redisTemplate.opsForValue().set(userSessionKey, sessionId, sessionTimeoutSeconds, TimeUnit.SECONDS);
        
        // Idle timeout tracker (expires after 15 minutes, refreshed on activity)
        redisTemplate.opsForValue().set(idleKey, String.valueOf(Instant.now().getEpochSecond()), 
            idleTimeoutSeconds, TimeUnit.SECONDS);
        
        return sessionId;
    }
}
```

**Redis Data Structure After Login:**

```
Key: "session:a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Value: {
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "john@example.com",
    "userId": 123,
    "role": "CUSTOMER",
    "createdAt": 1699564800,
    "lastActivity": 1699564800
}
TTL: 3600 seconds

Key: "user_sessions:john@example.com"
Value: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
TTL: 3600 seconds

Key: "session_idle:a1b2c3d4-e5f6-7890-abcd-ef1234567890"
Value: "1699564800"
TTL: 900 seconds
```

---

### Phase 3: Authenticated Request to Product-Pricing Service

```
User → Main Gateway → Product-Pricing Service → Redis (validate session)
                                               ↓
                                          Process Request
```

**Step 1: Client Makes Authenticated Request**

```bash
POST http://localhost:8083/api/v1/pricing-rule
Authorization: Bearer a1b2c3d4-e5f6-7890-abcd-ef1234567890
Content-Type: application/json

{
    "productId": 1,
    "minAmount": 1000,
    "maxAmount": 100000,
    "interestRate": 6.5,
    "tenureMonths": 12
}
```

**Step 2: Request Intercepted by Authentication Filter**

Located in: `product-pricing/src/main/java/com/bt/product/security/JwtAuthenticationFilter.java`

```java
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String SESSION_PREFIX = "session:";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // STEP 1: Extract session ID from Authorization header
            String sessionId = getSessionIdFromRequest(request);

            // STEP 2: Validate session exists in Redis
            if (StringUtils.hasText(sessionId) && isSessionValid(sessionId)) {
                
                // STEP 3: Retrieve session data from Redis
                Map<String, Object> sessionData = getSessionData(sessionId);

                if (sessionData != null) {
                    // STEP 4: Extract user information
                    String email = (String) sessionData.get("email");
                    String role = (String) sessionData.get("role");

                    if (email != null && !email.isEmpty()) {
                        // STEP 5: Create Spring Security authorities
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        if (role != null) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }

                        // STEP 6: Create authentication token
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // STEP 7: Set authentication in Spring Security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        // STEP 8: Continue filter chain (proceed to controller)
        filterChain.doFilter(request, response);
    }

    private String getSessionIdFromRequest(HttpServletRequest request) {
        // Extract token from "Authorization: Bearer <token>" header
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);  // Remove "Bearer " prefix
        }
        return null;
    }

    private boolean isSessionValid(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String idleKey = "session_idle:" + sessionId;

            // Check if session exists
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson == null) {
                return false;
            }

            // Check if idle timeout hasn't expired
            String idleData = redisTemplate.opsForValue().get(idleKey);
            return idleData != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> getSessionData(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson != null) {
                // Deserialize JSON to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(sessionJson, Map.class);
                return data;
            }
            return null;
        } catch (Exception e) {
            log.error("Error retrieving session data", e);
            return null;
        }
    }
}
```

**Step 3: Security Configuration**

Located in: `product-pricing/src/main/java/com/bt/product/security/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (using stateless sessions)
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management (no HttpSession)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", 
                                "/swagger-ui.html", "/webjars/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/pricing-rule/product-code/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated())
            
            // Add JWT filter before Spring Security's default authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**Step 4: Controller-Level Authorization**

Located in: `product-pricing/src/main/java/com/bt/product/controller/PricingRuleController.java`

```java
@RestController
@RequestMapping("/api/v1/pricing-rule")
@RequiredArgsConstructor
@Tag(name = "Pricing Rule Management")
@SecurityRequirement(name = "Bearer Authentication")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BANKOFFICER')")  // Only ADMIN and BANKOFFICER allowed
    @Operation(summary = "Create pricing rule")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> createPricingRule(
            @Valid @RequestBody PricingRuleRequest request) {
        
        // Spring Security has already validated:
        // 1. Session exists in Redis
        // 2. User has ADMIN or BANKOFFICER role
        
        PricingRuleResponse response = pricingRuleService.createPricingRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.<PricingRuleResponse>builder()
                .success(true)
                .message("Pricing rule created successfully")
                .data(response)
                .build()
        );
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BANKOFFICER')")  // All authenticated users
    @Operation(summary = "Get pricing rule by ID")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> getPricingRuleById(
            @PathVariable Long ruleId) {
        
        PricingRuleResponse response = pricingRuleService.getPricingRuleById(ruleId);
        return ResponseEntity.ok(
            ApiResponse.<PricingRuleResponse>builder()
                .success(true)
                .message("Pricing rule retrieved successfully")
                .data(response)
                .build()
        );
    }

    @GetMapping("/product-code/{productCode}")
    // No @PreAuthorize - Public endpoint for internal service communication
    @Operation(summary = "Get pricing rules by product code")
    public ResponseEntity<ApiResponse<List<PricingRuleResponse>>> getPricingRulesByProductCode(
            @PathVariable String productCode) {
        
        // This endpoint is public for accounts service to validate pricing during FD creation
        List<PricingRuleResponse> responses = pricingRuleService.getPricingRulesByProductCode(productCode);
        return ResponseEntity.ok(
            ApiResponse.<List<PricingRuleResponse>>builder()
                .success(true)
                .message("Pricing rules retrieved successfully")
                .data(responses)
                .build()
        );
    }
}
```

---

### Phase 4: Session Expiration & Logout

**Automatic Expiration:**

Redis automatically deletes expired keys:
- **Absolute timeout**: 1 hour from creation (configurable)
- **Idle timeout**: 15 minutes from last activity (refreshed on each request)

**Manual Logout:**

```bash
POST http://localhost:8081/api/auth/logout
Authorization: Bearer a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

Located in: `customer/src/main/java/com/bt/customer/controller/AuthController.java`

```java
@PostMapping("/logout")
@Operation(summary = "Logout user", description = "Invalidates current session")
public ResponseEntity<Map<String, String>> logout(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String sessionId = authHeader.substring(7);
        
        // Delete all Redis keys associated with this session
        redisSessionService.invalidateSession(sessionId);
    }
    
    return ResponseEntity.ok(Map.of("message", "Logout successful"));
}
```

Located in: `customer/src/main/java/com/bt/customer/service/RedisSessionService.java`

```java
public void invalidateSession(String sessionId) {
    try {
        String sessionKey = SESSION_PREFIX + sessionId;
        String idleKey = IDLE_TIMEOUT_PREFIX + sessionId;
        
        // Get session to find user email
        String sessionJson = redisTemplate.opsForValue().get(sessionKey);
        if (sessionJson != null) {
            Map<String, Object> data = objectMapper.readValue(sessionJson, Map.class);
            String email = (String) data.get("email");
            
            // Delete all related keys
            String userSessionKey = USER_SESSIONS_PREFIX + email;
            redisTemplate.delete(sessionKey);
            redisTemplate.delete(userSessionKey);
            redisTemplate.delete(idleKey);
        }
    } catch (Exception e) {
        log.error("Error invalidating session", e);
    }
}
```

---

## Cross-Service Integration

### How Product-Pricing Integrates with Other Services

#### 1. **Accounts Service Integration**

When creating an FD account, the accounts service calls product-pricing to validate rates:

```java
// In accounts service
@Service
public class AccountService {
    
    @Autowired
    private WebClient webClient;
    
    public AccountResponse createAccount(AccountCreationRequest request, String authToken) {
        // 1. Validate product exists and get pricing rules
        List<PricingRuleResponse> pricingRules = webClient.get()
            .uri("http://localhost:8083/api/v1/pricing-rule/product-code/" + request.getProductCode())
            // Note: This endpoint is public, no auth token needed
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<PricingRuleResponse>>>() {})
            .block()
            .getData();
        
        // 2. Find applicable pricing rule based on amount and tenure
        PricingRuleResponse applicableRule = findApplicableRule(
            pricingRules, 
            request.getAmount(), 
            request.getTenureMonths()
        );
        
        // 3. Create account with calculated interest rate
        Account account = Account.builder()
            .accountNo(generateAccountNumber())
            .customerId(request.getCustomerId())
            .productCode(request.getProductCode())
            .principalAmount(request.getAmount())
            .interestRate(applicableRule.getInterestRate())
            .tenureMonths(request.getTenureMonths())
            .build();
        
        return accountRepository.save(account);
    }
}
```

#### 2. **Customer Service Integration**

Product-pricing can call customer service to validate user permissions:

```java
// In product-pricing service
@Service
public class ProductService {
    
    @Autowired
    private WebClient webClient;
    
    public ProductResponse createProduct(ProductRequest request, String authToken) {
        // 1. Validate user has ADMIN or BANKOFFICER role
        UserResponse user = webClient.get()
            .uri("http://localhost:8081/api/customers/me")
            .header("Authorization", "Bearer " + authToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserResponse>>() {})
            .block()
            .getData();
        
        // 2. Create product
        Product product = Product.builder()
            .code(request.getCode())
            .name(request.getName())
            .minAmount(request.getMinAmount())
            .maxAmount(request.getMaxAmount())
            .createdBy(user.getEmail())
            .build();
        
        return productRepository.save(product);
    }
}
```

---

## Security Features

### 1. **Password Security**
- Passwords hashed using BCrypt (cost factor: 10)
- Never stored in plain text
- Never transmitted except during login
- Not accessible by other microservices

### 2. **Session Security**
- UUIDs prevent session ID guessing
- Sessions expire automatically
- Idle timeout prevents abandoned sessions
- One active session per user (new login invalidates old session)

### 3. **Role-Based Access Control (RBAC)**
- Three roles: CUSTOMER, BANKOFFICER, ADMIN
- Fine-grained permissions at controller method level
- Enforced by Spring Security's `@PreAuthorize`

### 4. **Stateless Architecture**
- No server-side session storage (HttpSession)
- Each request independently authenticated
- Horizontal scaling without session affinity

### 5. **CORS Protection**
- Centralized CORS configuration in Main Gateway
- Allowed origins: http://localhost:5173 (frontend)
- Credentials support enabled for cookies/auth headers

---

## Configuration Files

### Redis Configuration

Located in: `product-pricing/src/main/resources/application.properties`

```properties
# Redis connection
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms

# Session configuration
app.session.timeout-seconds=3600
app.session.idle-timeout-seconds=900
```

### Security Configuration

Located in: `product-pricing/pom.xml`

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Redis for session management -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## Testing Authentication

### 1. **Postman Testing**

**Collection Setup:**
```javascript
// Pre-request script for login
pm.sendRequest({
    url: 'http://localhost:8081/api/auth/login',
    method: 'POST',
    header: {
        'Content-Type': 'application/json'
    },
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            username: 'officer1',
            password: 'Officer@123'
        })
    }
}, function (err, res) {
    const response = res.json();
    pm.environment.set('auth_token', response.token);
});

// Authorization header for all requests
Authorization: Bearer {{auth_token}}
```

### 2. **Integration Testing**

```bash
# Step 1: Register user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123",
    "email": "test@example.com",
    "fullName": "Test User",
    "role": "BANKOFFICER"
  }'

# Step 2: Login and capture token
TOKEN=$(curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123"
  }' | jq -r '.token')

# Step 3: Test product-pricing endpoint
curl -X GET http://localhost:8083/api/v1/product \
  -H "Authorization: Bearer $TOKEN"

# Step 4: Test unauthorized access (should fail with 403)
curl -X POST http://localhost:8083/api/v1/pricing-rule \
  -H "Authorization: Bearer invalid-token" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "interestRate": 6.5
  }'
```

---

## Key Advantages of This Architecture

### 1. **Separation of Concerns**
- Customer service: Handles authentication and user management
- Other services: Focus on business logic, delegate auth to Redis

### 2. **Scalability**
- Stateless services scale horizontally
- Redis handles session state efficiently
- No database queries for authentication on every request

### 3. **Security**
- Single authentication point reduces attack surface
- Credentials never leave customer service
- JWT tokens (session IDs) are opaque and non-decodable

### 4. **Performance**
- Redis lookups are extremely fast (sub-millisecond)
- No database joins or complex queries for authentication
- Cached session data reduces network overhead

### 5. **Maintainability**
- Authentication logic centralized in customer service
- Other services use identical authentication filter pattern
- Easy to add new services with authentication

---

## Summary

The CashCached authentication system uses a **centralized authentication with distributed session validation** pattern:

1. **Customer Service** is the authentication authority - validates credentials and creates sessions
2. **Redis** stores session data accessible to all microservices
3. **Each microservice** validates requests by checking session existence in Redis
4. **Spring Security** enforces role-based access control at the method level
5. **Session IDs** act as bearer tokens, providing stateless authentication

This architecture ensures secure, scalable, and maintainable authentication across the entire microservices ecosystem while keeping the product-pricing module (and other services) simple and focused on their core business logic.