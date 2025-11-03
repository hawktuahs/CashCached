# Customer Service - Test Suite Documentation

## Overview

This directory contains comprehensive unit tests for the Customer Service microservice, covering all controllers, services, and security components. The test suite ensures the reliability, security, and correctness of the authentication and customer management functionality.

## Test Structure

### Package Organization

```
com.bt.customer
├── controller
│   ├── AuthControllerTest.java
│   └── CustomerControllerTest.java
├── service
│   ├── AuthServiceTest.java
│   └── CustomerServiceTest.java
└── security
    └── JwtTokenProviderTest.java
```

## Test Classes

### 1. AuthControllerTest

**Purpose**: Validates REST API endpoints for user registration and authentication.

**Test Scenarios**:

- **shouldRegisterUserSuccessfully**: Verifies successful user registration with valid input data returns HTTP 201 with JWT token
- **shouldReturnConflictWhenUsernameExists**: Ensures duplicate username registration returns HTTP 409 Conflict
- **shouldReturnBadRequestForInvalidData**: Validates input validation for malformed registration data returns HTTP 400
- **shouldLoginUserSuccessfully**: Confirms valid credentials authenticate successfully and return JWT token
- **shouldReturnUnauthorizedForInvalidCredentials**: Verifies invalid credentials return HTTP 401 Unauthorized
- **shouldReturnBadRequestWhenLoginFieldsAreBlank**: Tests validation for empty login fields

**Technology**: MockMvc, JUnit 5, Mockito  
**Authentication**: Filters disabled for isolated controller testing

### 2. CustomerControllerTest

**Purpose**: Tests secured customer endpoints with role-based access control.

**Test Scenarios**:

- **shouldGetCurrentUserProfile**: Validates authenticated user can retrieve their profile
- **shouldGetAllCustomersForAdmin**: Confirms ADMIN role can access all customer records
- **shouldGetAllCustomersForBankOfficer**: Verifies BANKOFFICER role has access to customer list
- **shouldUpdateProfileSuccessfully**: Tests profile update with valid data
- **shouldReturnBadRequestForInvalidEmailUpdate**: Validates email format during profile updates
- **shouldGetServiceStatus**: Verifies service health check returns operational status with user context

**Technology**: MockMvc, JUnit 5, Mockito, Spring Security Test  
**Authentication**: @WithMockUser annotations simulate different role contexts

### 3. AuthServiceTest

**Purpose**: Validates business logic for registration and authentication operations.

**Test Scenarios**:

- **shouldRegisterUserSuccessfully**: Tests complete user registration flow with password encryption
- **shouldThrowExceptionWhenUsernameExists**: Ensures UserAlreadyExistsException for duplicate usernames
- **shouldThrowExceptionWhenEmailExists**: Validates email uniqueness constraint enforcement
- **shouldRegisterUserWithDefaultRole**: Confirms CUSTOMER role is assigned when no role specified
- **shouldLoginUserSuccessfully**: Tests authentication flow with valid credentials
- **shouldThrowExceptionForInvalidCredentials**: Verifies InvalidCredentialsException for wrong password
- **shouldThrowExceptionWhenUserNotFoundAfterAuth**: Handles edge case of authenticated but missing user

**Technology**: JUnit 5, Mockito  
**Coverage**: Repository interactions, password encoding, JWT generation

### 4. CustomerServiceTest

**Purpose**: Tests customer profile management and authorization business logic.

**Test Scenarios**:

- **shouldGetCurrentUserProfile**: Validates profile retrieval from security context
- **shouldThrowExceptionWhenCurrentUserNotFound**: Tests UserNotFoundException handling
- **shouldGetAllCustomers**: Verifies retrieval of all customer records
- **shouldUpdateProfileSuccessfully**: Tests complete profile update operation
- **shouldUpdateOnlyProvidedFields**: Confirms partial updates work correctly
- **shouldThrowExceptionForDuplicateEmail**: Validates email uniqueness during updates
- **shouldGetCurrentUserRole**: Tests role extraction from security context
- **shouldReturnUnknownWhenUserNotFoundForRole**: Handles missing user gracefully

**Technology**: JUnit 5, Mockito, Spring Security  
**Coverage**: Security context handling, transactional operations, validation logic

### 5. JwtTokenProviderTest

**Purpose**: Validates JWT token generation, parsing, and validation logic.

**Test Scenarios**:

- **shouldGenerateTokenFromAuthentication**: Tests token creation from Spring Authentication object
- **shouldGenerateTokenForUserWithRole**: Verifies token generation with username and role claims
- **shouldExtractUsernameFromToken**: Validates username extraction from valid JWT
- **shouldValidateValidToken**: Confirms valid token passes validation
- **shouldRejectInvalidToken**: Tests rejection of malformed tokens
- **shouldRejectMalformedToken**: Validates handling of non-JWT format strings
- **shouldRejectEmptyToken**: Tests empty string token rejection
- **shouldRejectNullToken**: Validates null safety in token validation
- **shouldGenerateDifferentTokensForDifferentUsers**: Ensures token uniqueness per user
- **shouldIncludeRoleClaimInToken**: Verifies role claim inclusion in token payload

**Technology**: JUnit 5, Mockito, ReflectionTestUtils  
**Coverage**: Token lifecycle, validation edge cases, security constraints

## Running the Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=AuthControllerTest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
```

## Test Configuration

### Dependencies

- **JUnit 5**: Modern testing framework with improved assertions
- **Mockito**: Mock object framework for isolated unit testing
- **Spring Boot Test**: Auto-configuration and test utilities
- **Spring Security Test**: Security context and authentication mocking
- **MockMvc**: REST API endpoint testing without server startup

### Annotations Used

- `@ExtendWith(MockitoExtension.class)`: Enables Mockito support
- `@WebMvcTest`: Loads only web layer for controller tests
- `@AutoConfigureMockMvc(addFilters = false)`: Disables security filters for isolated testing
- `@WithMockUser`: Simulates authenticated user with specific roles
- `@DisplayName`: Provides readable test descriptions
- `@BeforeEach`: Sets up test fixtures before each test method

## Test Data

### Default Test User

- **Username**: testuser
- **Password**: password123 (encoded in actual tests)
- **Email**: test@example.com
- **Role**: CUSTOMER
- **Phone**: +1234567890

### Admin Test User

- **Username**: admin
- **Role**: ADMIN

### Bank Officer Test User

- **Username**: bankofficer
- **Role**: BANKOFFICER

## Expected Outcomes

### Success Criteria

- All tests pass without failures
- Code coverage above 80% for critical paths
- No security vulnerabilities in authentication flow
- Proper exception handling for all error scenarios
- Role-based access control correctly enforced

### Validation Points

- JWT tokens contain correct claims and expiration
- Password encryption uses BCrypt
- Duplicate username/email prevention works
- HTTP status codes match REST conventions
- Error messages are descriptive and secure

## Integration with CI/CD

These tests are designed to run in continuous integration pipelines:

- No external dependencies required (uses mocks)
- Fast execution time (unit tests only)
- Deterministic results (no random failures)
- Clear failure messages for debugging

## Best Practices Demonstrated

- Arrange-Act-Assert pattern in all tests
- Descriptive test method names
- Isolated unit tests with mocked dependencies
- Comprehensive edge case coverage
- Security context simulation for authenticated endpoints
- Proper exception testing with assertThrows
- Verification of mock interactions

## Troubleshooting

### Common Issues

1. **Test fails with SecurityContext null**: Ensure @WithMockUser annotation is present
2. **JWT validation fails**: Check secret key configuration in test setup
3. **Mock not returning expected value**: Verify when() clause matches actual method call
4. **Validation errors**: Check @Valid annotation and constraint definitions

## Future Enhancements

- Integration tests with TestContainers for database
- End-to-end API tests with RestAssured
- Performance tests for high-load scenarios
- Mutation testing to verify test quality
