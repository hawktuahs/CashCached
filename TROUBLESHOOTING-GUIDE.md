# BT Bank Application - Troubleshooting Guide

## 🚨 Current Issues & Solutions

### Issue 1: Registration API Returns 500 Error
**Problem**: `POST http://localhost:8080/api/auth/register 500 (Internal Server Error)`

**Root Cause**: CORS configuration in microservices was blocking requests from localhost:8080

**Solution**: ✅ **FIXED** - Updated CORS configuration in all microservices:
- Customer Service: Added `http://localhost:8080` to allowed origins
- FD Calculator Service: Added `http://localhost:8080` to allowed origins
- Product Service: Already had `http://localhost:8080` configured
- Accounts Service: Already allows all origins (`*`)

### Issue 2: Services Not Starting
**Problem**: Services fail to start or are not accessible

**Solution**:
1. Start services in the correct order:
   ```bash
   # Terminal 1 - Customer Service
   cd customer
   mvn spring-boot:run
   
   # Terminal 2 - Main Gateway (after customer service is running)
   cd main
   mvn spring-boot:run
   ```
2. Wait for "Started [ServiceName]Application" message
3. Test registration at http://localhost:8080

### Issue 2: Swagger Documentation Not Loading
**Problem**: "Failed to load API definition. Fetch error response status is 500 /docs/accounts"

**Root Cause**: The microservices are not running, so the gateway cannot fetch their API documentation.

**Solution**: Start all microservices in the correct order.

## 🚀 Quick Start Guide

### Option 1: Start All Services (Recommended)
```bash
# Windows
./start-all-services.bat

# Linux/Mac
chmod +x start-all-services.sh
./start-all-services.sh
```

### Option 2: Start Services Manually
Open 5 separate terminal windows and run:

**Terminal 1 - Customer Service (Port 8081)**
```bash
cd customer
mvn spring-boot:run
```

**Terminal 2 - Product Service (Port 8082)**
```bash
cd product-pricing
mvn spring-boot:run
```

**Terminal 3 - FD Calculator Service (Port 8083)**
```bash
cd fd-calculator
mvn spring-boot:run
```

**Terminal 4 - Accounts Service (Port 8084)**
```bash
cd accounts
mvn spring-boot:run
```

**Terminal 5 - Main Gateway (Port 8080)**
```bash
cd main
mvn spring-boot:run
```

## 🔍 Verification Steps

### 1. Check Service Health
Test each service individually:

```bash
# Customer Service
curl http://localhost:8081/actuator/health

# Product Service  
curl http://localhost:8082/actuator/health

# FD Calculator Service
curl http://localhost:8083/actuator/health

# Accounts Service
curl http://localhost:8084/actuator/health

# Main Gateway
curl http://localhost:8080/actuator/health
```

### 2. Test API Endpoints
```bash
# Test customer registration
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","firstName":"John","lastName":"Doe"}'

# Test customer login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 3. Check Frontend
- Open http://localhost:8080
- Try to register a new account
- Check browser console for any errors

### 4. Check Swagger Documentation
- Open http://localhost:8080/swagger-ui.html
- Verify all API documentation loads correctly

## 🛠️ Common Issues & Solutions

### Issue: Port Already in Use
**Error**: `Port 8080 was already in use`

**Solution**:
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### Issue: Maven Build Fails
**Error**: `BUILD FAILURE`

**Solution**:
```bash
# Clean and rebuild
mvn clean compile
mvn spring-boot:run
```

### Issue: Frontend Not Loading
**Error**: Frontend shows blank page or 404

**Solution**:
1. Rebuild frontend:
   ```bash
   cd main/frontend
   pnpm build
   ```
2. Restart main gateway:
   ```bash
   cd main
   mvn spring-boot:run
   ```

### Issue: Database Connection Errors
**Error**: `H2 database connection failed`

**Solution**:
- H2 is embedded, so this usually means the service didn't start properly
- Restart the affected service
- Check logs for specific error messages

## 📊 Service Dependencies

```
Main Gateway (8080)
├── Customer Service (8081) - Authentication & Profile
├── Product Service (8082) - Product Management  
├── FD Calculator Service (8083) - FD Calculations
└── Accounts Service (8084) - Account Management
```

**Start Order**: Start microservices first, then the main gateway.

## 🔧 Development Workflow

### For Frontend Development
1. Start all backend services
2. Start frontend dev server:
   ```bash
   cd main/frontend
   pnpm dev
   ```
3. Frontend runs on http://localhost:5173 with API proxy

### For Backend Development
1. Start all services
2. Make changes to backend code
3. Services will auto-reload (if devtools enabled)

## 📝 Logs and Debugging

### Enable Debug Logging
Add to `application.properties`:
```properties
logging.level.com.bt=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Check Service Logs
Each service will show startup logs in its terminal window. Look for:
- `Started [ServiceName]Application`
- Any error messages
- Database initialization messages

### Browser Developer Tools
- Open F12 in browser
- Check Console tab for JavaScript errors
- Check Network tab for failed API calls

## 🎯 Success Criteria

The application is working correctly when:
- ✅ All 5 services start without errors
- ✅ Frontend loads at http://localhost:8080
- ✅ Registration works without 500 errors
- ✅ Login works and redirects to dashboard
- ✅ Swagger documentation loads at http://localhost:8080/swagger-ui.html
- ✅ All API endpoints respond correctly

## 🆘 Still Having Issues?

1. **Check Java Version**: Ensure Java 17+ is installed
2. **Check Maven Version**: Ensure Maven 3.6+ is installed
3. **Check Node.js Version**: Ensure Node.js 18+ is installed
4. **Check Ports**: Ensure ports 8080-8084 are available
5. **Check Logs**: Look at individual service logs for specific errors
6. **Restart Everything**: Stop all services and restart in correct order

---

**Remember**: Always start the microservices first, then the main gateway!
