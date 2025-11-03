# Customer Service - Authentication & Identity Microservice

## Overview

A secure, production-grade Spring Boot microservice providing authentication and identity management for a banking microservices ecosystem. This service handles user registration, JWT-based authentication, role-based authorization, and customer profile management.

## Architecture

### Technology Stack

- **Java**: 17+
- **Spring Boot**: 3.5.6
- **Spring Security**: JWT-based authentication
- **Spring Data JPA**: Database persistence
- **MySQL**: Primary database
- **Swagger/OpenAPI**: API documentation
- **Lombok**: Boilerplate reduction
- **JUnit 5 & Mockito**: Testing framework

### Design Principles

- Clean, self-documenting code without comments
- Microservice architecture patterns
- RESTful API design
- Stateless authentication with JWT
- Role-based access control (RBAC)
- Exception-driven error handling

## Features

### Authentication

- **User Registration**: Secure account creation with BCrypt password encryption
- **User Login**: Credential validation with JWT token issuance
- **JWT Token Management**: Secure token generation, validation, and expiration
- **Password Security**: BCrypt hashing with salt

### Authorization

- **Role-Based Access Control**: Three roles supported
  - `CUSTOMER`: Standard user with profile access
  - `ADMIN`: Full administrative privileges
  - `BANKOFFICER`: Customer data access for banking operations
- **Secured Endpoints**: Method-level security with @PreAuthorize

### Profile Management

- **View Profile**: Authenticated users can retrieve their profile
- **Update Profile**: Modify name, email, and phone number
- **List Customers**: Admin and BankOfficer can view all customers
- **Service Status**: Health check with user context

## Project Structure

```
com.bt.customer
├── config
│   ├── AppConfig.java              # CORS and application configuration
│   └── SwaggerConfig.java          # OpenAPI/Swagger documentation setup
├── controller
│   ├── AuthController.java         # Registration and login endpoints
│   └── CustomerController.java     # Customer management endpoints
├── dto
│   ├── AuthResponse.java           # Authentication response payload
│   ├── ErrorResponse.java          # Standardized error structure
│   ├── LoginRequest.java           # Login credentials
│   ├── RegisterRequest.java        # Registration data
│   ├── StatusResponse.java         # Service health status
│   ├── UpdateProfileRequest.java   # Profile update payload
│   └── UserProfileResponse.java    # User profile data
├── entity
│   └── User.java                   # User entity with JPA mapping
├── exception
│   ├── GlobalExceptionHandler.java # Centralized exception handling
│   ├── InvalidCredentialsException.java
│   ├── UserAlreadyExistsException.java
│   └── UserNotFoundException.java
├── repository
│   └── UserRepository.java         # JPA data access layer
├── security
│   ├── JwtAuthenticationFilter.java # JWT token validation filter
│   ├── JwtTokenProvider.java       # JWT generation and parsing
│   ├── SecurityConfig.java         # Spring Security configuration
│   └── UserPrincipal.java          # Custom UserDetails implementation
└── service
    ├── AuthService.java            # Authentication business logic
    ├── CustomerService.java        # Customer management logic
    └── CustomUserDetailsService.java # UserDetailsService implementation
```

## API Endpoints

### Authentication APIs (Public)

#### Register User

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890",
  "role": "CUSTOMER"
}

Response 201 Created:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "username": "john_doe",
  "role": "CUSTOMER",
  "message": "User registered successfully"
}
```

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!"
}

Response 200 OK:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "username": "john_doe",
  "role": "CUSTOMER",
  "message": "Authentication successful"
}
```

### Customer APIs (Secured)

#### Get Profile

```http
GET /api/customer/profile
Authorization: Bearer {token}

Response 200 OK:
{
  "id": 1,
  "username": "john_doe",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890",
  "role": "CUSTOMER",
  "active": true,
  "createdAt": "2025-10-23T10:00:00",
  "updatedAt": "2025-10-23T10:00:00"
}
```

#### Update Profile

```http
PUT /api/customer/update
Authorization: Bearer {token}
Content-Type: application/json

{
  "fullName": "John Updated Doe",
  "email": "john.updated@example.com",
  "phoneNumber": "+9876543210"
}

Response 200 OK: (Updated profile)
```

#### Get All Customers (Admin/BankOfficer only)

```http
GET /api/customer/all
Authorization: Bearer {token}

Response 200 OK: [Array of UserProfileResponse]
```

#### Service Status

```http
GET /api/customer/status
Authorization: Bearer {token}

Response 200 OK:
{
  "status": "OPERATIONAL",
  "role": "CUSTOMER",
  "username": "john_doe",
  "timestamp": "2025-10-23T10:00:00",
  "message": "Customer service is running"
}
```

## Configuration

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/customer_db?createDatabaseIfNotExist=true
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
```

### JWT Configuration

```yaml
jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000 # 24 hours in milliseconds
```

### Server Configuration

```yaml
server:
  port: 8081
```

## Security

### Authentication Flow

1. User sends credentials to `/api/auth/login`
2. Service validates credentials against database
3. If valid, JWT token generated with username and role claims
4. Client includes token in Authorization header for subsequent requests
5. JwtAuthenticationFilter validates token and sets SecurityContext

### Password Security

- Passwords encrypted using BCryptPasswordEncoder
- Minimum 6 characters enforced
- Plain passwords never stored or logged

### Token Security

- HMAC-SHA256 signing algorithm
- 24-hour expiration
- Claims: username, role, issued time, expiration time
- Stateless authentication (no server-side session storage)

### Role-Based Authorization

- Method-level security with `@PreAuthorize`
- Hierarchical role checks
- Automatic 403 Forbidden for insufficient permissions

## Running the Application

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- MySQL 8.0+

### Database Setup

```sql
CREATE DATABASE customer_db;
```

### Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

### Access Swagger UI

```
http://localhost:8081/swagger-ui.html
```

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

## Testing

### Run All Tests

```bash
mvn test
```

### Test Coverage

Comprehensive unit tests cover:

- All REST endpoints (AuthController, CustomerController)
- Business logic (AuthService, CustomerService)
- Security components (JwtTokenProvider)
- Exception handling scenarios
- Role-based access control

See `/src/test/java/com/bt/customer/README.md` for detailed test documentation.

## Error Handling

All errors return standardized JSON format:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-10-23T10:00:00",
  "path": "/api/auth/register"
}
```

### HTTP Status Codes

- `200 OK`: Successful GET/PUT requests
- `201 Created`: Successful registration
- `400 Bad Request`: Validation failures
- `401 Unauthorized`: Invalid credentials or missing token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Duplicate username/email
- `500 Internal Server Error`: Unexpected errors

## Integration with Other Services

### Microservice Communication

This service can be integrated with:

- **API Gateway**: Route requests through centralized entry point
- **Service Discovery**: Eureka or Consul for dynamic service registration
- **Config Server**: Externalized configuration management
- **Other Banking Services**: FD Calculator, Product Pricing, Accounts

### JWT Token Usage

Other services can validate JWT tokens using the shared secret key, enabling:

- Distributed authentication across microservices
- Stateless session management
- Role-based authorization in downstream services

## Production Considerations

### Environment Variables

Configure sensitive data via environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://prod-db:3306/customer_db
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export JWT_SECRET=your_production_secret_key
```

### Monitoring

- Enable Spring Boot Actuator endpoints
- Integrate with Prometheus/Grafana for metrics
- Configure centralized logging (ELK stack)

### Scaling

- Stateless design allows horizontal scaling
- Load balancer compatible
- Database connection pooling configured

## API Documentation

Full interactive API documentation available at:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/api-docs

## License

Copyright © 2025 Banking Technology Team

## Support

For issues or questions, contact: support@banktech.com
