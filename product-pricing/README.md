# Banking Microservices - Product and Pricing Module

## Overview

Enterprise-grade Product and Pricing microservice for banking applications, built with Spring Boot 3.x. This module manages product definitions, pricing rules, and integrates with the Customer authentication module using shared JWT tokens.

## Architecture

- **Framework**: Spring Boot 3.5.6
- **Java Version**: 17
- **Database**: MySQL 8.0
- **Security**: JWT-based authentication (shared with Customer module)
- **API Documentation**: OpenAPI 3.0 (Swagger UI)
- **Caching**: Spring Cache (Simple/Redis)
- **Build Tool**: Maven

## Key Features

1. **Product Management**

   - Define banking products (Savings, Loans, Fixed Deposits, Credit Cards)
   - Interest rate and term configuration
   - Currency and regulatory compliance
   - Effective/expiry date management

2. **Pricing Rules Engine**

   - Threshold-based pricing
   - Dynamic interest rate calculation
   - Fee and discount management
   - Priority-based rule application

3. **Secure Integration**

   - JWT token validation from Customer module
   - Role-based access control (ADMIN, BANKOFFICER, CUSTOMER)
   - Service-to-service authentication

4. **API Versioning**
   - RESTful APIs with version prefix `/api/v1/product`
   - Backward compatibility support

## Project Structure

```
product-pricing/
├── src/main/java/com/bt/product/
│   ├── ProductApplication.java
│   ├── controller/           # REST controllers
│   │   ├── ProductController.java
│   │   └── PricingRuleController.java
│   ├── service/              # Business logic
│   │   ├── ProductService.java
│   │   └── PricingRuleService.java
│   ├── repository/           # Data access
│   │   ├── ProductRepository.java
│   │   └── PricingRuleRepository.java
│   ├── entity/               # Domain models
│   │   ├── Product.java
│   │   ├── PricingRule.java
│   │   └── enums/
│   ├── dto/                  # Data transfer objects
│   ├── security/             # JWT integration
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   ├── exception/            # Custom exceptions
│   └── config/               # Configuration classes
├── src/test/java/            # Comprehensive tests
└── src/main/resources/
    └── application.yml       # Configuration
```

## API Endpoints

### Product Management APIs

```
POST   /api/v1/product                     - Create new product (ADMIN/BANKOFFICER)
PUT    /api/v1/product/{code}              - Update product (ADMIN/BANKOFFICER)
GET    /api/v1/product/{code}              - Get product by code (All authenticated)
GET    /api/v1/product                     - Get all products (All authenticated)
POST   /api/v1/product/search              - Search products with filters
GET    /api/v1/product/status/{code}       - Get product status and rules
DELETE /api/v1/product/{code}              - Delete product (ADMIN only)
```

### Pricing Rule APIs

```
POST   /api/v1/pricing-rule                      - Create pricing rule (ADMIN/BANKOFFICER)
PUT    /api/v1/pricing-rule/{ruleId}             - Update rule (ADMIN/BANKOFFICER)
GET    /api/v1/pricing-rule/{ruleId}             - Get rule by ID
GET    /api/v1/pricing-rule/product/{productId}  - Get all rules for product
GET    /api/v1/pricing-rule/product/{productId}/active - Get active rules
GET    /api/v1/pricing-rule/product/{productId}/applicable?amount={amount} - Get applicable rules
DELETE /api/v1/pricing-rule/{ruleId}             - Delete rule (ADMIN only)
```

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Customer module running on port 8081

### Configuration

Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/product_db
    username: root
    password: root

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 # Must match Customer module
  expiration: 86400000

services:
  customer:
    url: http://localhost:8081
```

### Build and Run

```bash
cd product-pricing
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

The service will start on port **8082**.

### Access Swagger UI

Navigate to: `http://localhost:8082/swagger-ui.html`

## Testing

### Run All Tests

```bash
.\mvnw.cmd test
```

### Run with Coverage

```bash
.\mvnw.cmd clean test jacoco:report
```

Test coverage report: `target/site/jacoco/index.html`

## Docker Deployment

### Build Docker Image

```bash
.\mvnw.cmd clean package
docker build -t product-service:latest .
```

### Run with Docker Compose (Both Services)

```bash
cd bt-lab-project
docker-compose up -d
```

This starts:

- MySQL database
- Customer Service (port 8081)
- Product Service (port 8082)

## Kubernetes Deployment

### Deploy to Production

```bash
kubectl apply -f k8s/production/configmap.yml
kubectl apply -f k8s/production/secret.yml
kubectl apply -f k8s/production/mysql-statefulset.yml
kubectl apply -f k8s/production/customer-deployment.yml
kubectl apply -f k8s/production/product-deployment.yml
kubectl apply -f k8s/production/ingress.yml
kubectl apply -f k8s/production/hpa.yml
```

### Verify Deployment

```bash
kubectl get pods -n production
kubectl get services -n production
kubectl logs -f deployment/product-service -n production
```

## CI/CD Pipeline

### GitHub Actions

The `.github/workflows/ci-cd.yml` pipeline automatically:

1. Builds both services
2. Runs unit tests
3. Builds Docker images
4. Pushes to container registry
5. Deploys to staging (develop branch)
6. Deploys to production (main branch with approval)

### Jenkins

The `Jenkinsfile` provides parallel build and deployment with:

- Maven build and test stages
- Docker image creation
- Multi-environment deployment
- Integration testing
- Production approval gate

## Integration with Customer Module

### Shared JWT Configuration

Both modules use the same JWT secret for token validation:

```yaml
jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

### Authentication Flow

1. User authenticates with Customer module (`POST /api/auth/login`)
2. Customer module returns JWT token
3. User includes token in Product module requests: `Authorization: Bearer <token>`
4. Product module validates token using shared secret
5. Role-based authorization applied

### Example Usage

```bash
# 1. Login to Customer module
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bank.com","password":"admin123"}'

# Response: {"token":"eyJhbGc..."}

# 2. Use token in Product module
curl -X POST http://localhost:8082/api/v1/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "productCode": "SAV001",
    "productName": "Premium Savings Account",
    "productType": "SAVINGS_ACCOUNT",
    "currency": "USD",
    "minInterestRate": 3.5,
    "maxInterestRate": 5.5,
    "effectiveDate": "2025-01-01"
  }'
```

## Environment Variables

```
SERVER_PORT=8082
DB_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=<shared-secret>
JWT_EXPIRATION=86400000
CUSTOMER_SERVICE_URL=http://localhost:8081
```

## Database Schema

The application auto-creates tables on startup (JPA `ddl-auto: update`).

**Products Table**: id, product_code (unique), product_name, product_type, description, interest rates, terms, amounts, currency, status, dates, regulatory_code

**Pricing Rules Table**: id, product_id (FK), rule_name, thresholds, interest_rate, fees, discounts, priority, is_active

## Monitoring and Health Checks

- Health: `http://localhost:8082/actuator/health`
- Info: `http://localhost:8082/actuator/info`
- Metrics: `http://localhost:8082/actuator/metrics`

## Support and Contribution

For issues or enhancements, create a pull request or issue in the project repository.

## License

Proprietary - Banking Technology Labs
