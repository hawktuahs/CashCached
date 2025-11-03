# BT Bank Application - Complete Deployment Guide

## 🎯 Overview

This guide provides step-by-step instructions for deploying the complete BT Bank application with integrated frontend and backend.

## 🏗️ Architecture

- **Backend**: Spring Boot API Gateway (Port 8080)
- **Frontend**: React SPA served by Spring Boot
- **Microservices**: Customer, Product, FD Calculator, Accounts (Ports 8081-8084)
- **Database**: H2 (embedded) for each microservice

## 📋 Prerequisites

- Java 17+
- Node.js 18+
- pnpm (recommended) or npm
- Maven 3.6+

## 🚀 Quick Start

### 1. Build Frontend
```bash
# Windows
./build-frontend.bat

# Linux/Mac
./build-frontend.sh

# Manual
cd frontend
pnpm install
pnpm build
```

### 2. Start All Services
```bash
# Terminal 1: Customer Service
cd customer
mvn spring-boot:run

# Terminal 2: Product Service  
cd product-pricing
mvn spring-boot:run

# Terminal 3: FD Calculator Service
cd fd-calculator
mvn spring-boot:run

# Terminal 4: Accounts Service
cd accounts
mvn spring-boot:run

# Terminal 5: Main Gateway (with frontend)
cd main
mvn spring-boot:run
```

### 3. Access Application
- **Frontend**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

## 🔧 Configuration Details

### Frontend Integration
- Frontend files are built to `src/main/resources/static/`
- Spring Boot serves static files and handles SPA routing
- API calls are proxied through the gateway
- JWT authentication is handled automatically

### Security Configuration
- Static resources are publicly accessible
- API endpoints require JWT authentication
- CORS is configured for development
- Session management is stateless

### API Gateway
- Routes API calls to appropriate microservices
- Handles authentication and authorization
- Aggregates Swagger documentation
- Serves frontend static files

## 📱 User Roles & Features

### Customer
- Login/Register
- View dashboard
- Manage profile
- Use FD calculator
- View accounts
- Browse products

### Bank Officer
- All customer features
- Manage products
- View all accounts
- Administrative functions

### Admin
- All bank officer features
- Full system access
- User management
- System configuration

## 🛠️ Development Workflow

### Frontend Development
```bash
# Start backend
cd main
mvn spring-boot:run

# Start frontend dev server (separate terminal)
cd frontend
pnpm dev
```

### Backend Development
```bash
# Start all microservices
# Then start main gateway
cd main
mvn spring-boot:run
```

## 🔍 Testing

### Manual Testing
1. Access http://localhost:8080
2. Register a new account
3. Login with credentials
4. Test all features based on role

### API Testing
1. Access http://localhost:8080/swagger-ui.html
2. Use the aggregated API documentation
3. Test endpoints with JWT tokens

## 📦 Production Deployment

### Option 1: JAR Deployment
```bash
# Build all services
mvn clean package

# Run each service
java -jar customer/target/customer-0.0.1-SNAPSHOT.jar
java -jar product-pricing/target/product-pricing-0.0.1-SNAPSHOT.jar
java -jar fd-calculator/target/fd-calculator-0.0.1-SNAPSHOT.jar
java -jar accounts/target/accounts-0.0.1-SNAPSHOT.jar
java -jar main/target/main-0.0.1-SNAPSHOT.jar
```

### Option 2: Docker Deployment
```bash
# Build Docker images
docker build -t bt-bank-customer ./customer
docker build -t bt-bank-product ./product-pricing
docker build -t bt-bank-fd ./fd-calculator
docker build -t bt-bank-accounts ./accounts
docker build -t bt-bank-main ./main

# Run containers
docker run -p 8081:8081 bt-bank-customer
docker run -p 8082:8082 bt-bank-product
docker run -p 8083:8083 bt-bank-fd
docker run -p 8084:8084 bt-bank-accounts
docker run -p 8080:8080 bt-bank-main
```

## 🔧 Environment Configuration

### Development
```properties
# application.properties
server.port=8080
services.customer.url=http://localhost:8081
services.product.url=http://localhost:8082
services.fdcalculator.url=http://localhost:8083
services.accounts.url=http://localhost:8084
```

### Production
```properties
# application-prod.properties
server.port=8080
services.customer.url=http://customer-service:8081
services.product.url=http://product-service:8082
services.fdcalculator.url=http://fd-service:8083
services.accounts.url=http://accounts-service:8084
```

## 📊 Monitoring

### Health Checks
- Spring Boot Actuator endpoints
- Custom health indicators
- Database connectivity checks
- Service availability monitoring

### Logging
- Structured logging with SLF4J
- Log levels configurable
- Request/response logging
- Error tracking and alerting

## 🚨 Troubleshooting

### Common Issues

1. **Frontend not loading**
   - Check if static files exist in `src/main/resources/static/`
   - Verify Spring Boot is running on port 8080
   - Check browser console for errors

2. **API calls failing**
   - Ensure all microservices are running
   - Check gateway configuration
   - Verify JWT token is valid

3. **Authentication issues**
   - Check JWT secret configuration
   - Verify token expiration settings
   - Clear browser localStorage

4. **Build failures**
   - Check Java and Node.js versions
   - Run `mvn clean` and rebuild
   - Check for port conflicts

### Debug Mode
```properties
# Enable debug logging
logging.level.com.bt=DEBUG
logging.level.org.springframework.web=DEBUG
```

## 🔒 Security Considerations

### Production Security
- Use strong JWT secrets
- Enable HTTPS
- Configure CORS properly
- Implement rate limiting
- Use environment variables for secrets

### Database Security
- Use production databases (PostgreSQL, MySQL)
- Enable connection encryption
- Implement proper backup strategies
- Use connection pooling

## 📈 Performance Optimization

### Frontend
- Enable gzip compression
- Use CDN for static assets
- Implement code splitting
- Optimize bundle size

### Backend
- Enable connection pooling
- Use caching strategies
- Optimize database queries
- Implement load balancing

## 🎉 Success Criteria

The deployment is successful when:
- ✅ All services start without errors
- ✅ Frontend loads at http://localhost:8080
- ✅ User can register and login
- ✅ All features work based on user role
- ✅ API documentation is accessible
- ✅ Static files are served correctly

---

**The BT Bank application is now ready for production deployment!**
