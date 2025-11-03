# Frontend Integration with Spring Boot Backend

This document explains how the React frontend is integrated with the Spring Boot backend to serve a complete banking application.

## 🏗️ Architecture Overview

The application uses a **monolithic deployment** approach where:
- Spring Boot backend runs on port 8080
- React frontend is built and served as static files by Spring Boot
- All API calls are proxied through the Spring Boot gateway
- Single deployment unit for easy management

## 📁 File Structure

```
main/
├── src/main/resources/static/     # Frontend build output
│   ├── index.html                 # Main HTML file
│   ├── assets/                    # CSS, JS, and other assets
│   └── vite.svg                   # Static assets
├── src/main/java/com/bt/main/
│   ├── config/
│   │   ├── WebConfig.java         # Static resource configuration
│   │   └── SecurityConfig.java    # Security configuration
│   └── controller/
│       └── IndexController.java   # Root route handler
└── frontend/                      # React source code
    ├── src/                       # React components and pages
    ├── vite.config.ts             # Vite configuration
    └── package.json               # Dependencies
```

## 🔧 Configuration Details

### 1. WebConfig.java
- Configures static resource handlers
- Sets up SPA routing for React Router
- Handles caching for static assets
- Maps frontend routes to index.html

### 2. SecurityConfig.java
- Allows access to static resources
- Permits frontend routes without authentication
- Protects API endpoints with JWT authentication
- Configures CORS for development

### 3. IndexController.java
- Serves the main index.html file
- Handles root route requests
- Sets proper content type headers

### 4. application.properties
- Configures static resource locations
- Sets up caching policies
- Defines servlet paths and patterns

## 🚀 Build and Deployment Process

### Prerequisites
- Node.js 18+ and pnpm installed
- Java 17+ and Maven installed
- All microservices running on their respective ports

### Build Steps

1. **Build the Frontend**
   ```bash
   # Windows
   ./build-frontend.bat
   
   # Linux/Mac
   ./build-frontend.sh
   
   # Or manually
   cd frontend
   pnpm build
   ```

2. **Start the Backend**
   ```bash
   cd main
   mvn spring-boot:run
   ```

3. **Access the Application**
   - Frontend: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

## 🔄 Development Workflow

### Frontend Development
1. Start the backend: `mvn spring-boot:run`
2. Start frontend dev server: `cd frontend && pnpm dev`
3. Frontend runs on http://localhost:5173 with API proxy to backend

### Production Deployment
1. Build frontend: `pnpm build` (outputs to `src/main/resources/static/`)
2. Build backend: `mvn clean package`
3. Run JAR: `java -jar target/main-0.0.1-SNAPSHOT.jar`

## 🌐 Routing Configuration

### Frontend Routes (React Router)
- `/` - Dashboard (redirects to `/dashboard`)
- `/login` - Login page
- `/register` - Registration page
- `/dashboard` - Main dashboard
- `/profile` - Customer profile
- `/products` - Product management
- `/fd-calculator` - FD calculator
- `/accounts` - Account management

### API Routes (Spring Boot Gateway)
- `/api/auth/**` - Authentication endpoints
- `/api/customer/**` - Customer service endpoints
- `/api/v1/product/**` - Product service endpoints
- `/api/fd/**` - FD calculator endpoints
- `/api/accounts/**` - Accounts service endpoints

### Static Resources
- `/static/**` - Frontend assets
- `/assets/**` - CSS, JS, images
- `/favicon.ico` - Favicon
- `/vite.svg` - Static assets

## 🔒 Security Configuration

### Public Routes (No Authentication Required)
- All static resources (`/static/**`, `/assets/**`)
- Frontend routes (`/`, `/login`, `/register`, etc.)
- API documentation (`/swagger-ui/**`, `/docs/**`)

### Protected Routes (JWT Authentication Required)
- All API endpoints (`/api/**`)
- JWT token validation via `JwtAuthenticationFilter`

## 📱 SPA (Single Page Application) Support

The configuration ensures that:
1. Direct URL access works (e.g., `/dashboard` loads correctly)
2. Browser refresh on any route serves the React app
3. React Router handles client-side navigation
4. API calls are properly proxied through the backend

## 🛠️ Troubleshooting

### Common Issues

1. **Frontend not loading**
   - Check if static files are in `src/main/resources/static/`
   - Verify `WebConfig.java` is properly configured
   - Ensure `IndexController.java` is serving index.html

2. **API calls failing**
   - Verify backend is running on port 8080
   - Check proxy configuration in `vite.config.ts`
   - Ensure API endpoints are properly mapped in `ProxyController.java`

3. **Routing issues**
   - Verify `WebConfig.java` SPA routing configuration
   - Check that all frontend routes are permitted in `SecurityConfig.java`
   - Ensure React Router is properly configured

4. **Build issues**
   - Run `pnpm install` in frontend directory
   - Check Node.js version compatibility
   - Verify Vite configuration

### Debug Mode

Enable debug logging by adding to `application.properties`:
```properties
logging.level.com.bt.main=DEBUG
logging.level.org.springframework.web=DEBUG
```

## 🔄 Hot Reload Development

For development with hot reload:
1. Start backend: `mvn spring-boot:run`
2. Start frontend dev server: `cd frontend && pnpm dev`
3. Frontend will proxy API calls to backend automatically

## 📦 Production Optimization

### Frontend Build Optimization
- Assets are minified and compressed
- CSS and JS are bundled efficiently
- Static assets are cached with proper headers
- Code splitting can be added for larger applications

### Backend Optimization
- Static resources are served efficiently
- Proper caching headers are set
- GZIP compression is enabled
- Security headers are configured

## 🚀 Deployment Options

### Traditional Deployment
1. Build frontend and backend together
2. Deploy as single JAR file
3. Serve everything from port 8080

### Container Deployment
```dockerfile
FROM openjdk:17-jre-slim
COPY target/main-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Cloud Deployment
- Deploy to AWS, Azure, or GCP
- Use load balancers for scaling
- Configure environment variables for different environments

## 📊 Monitoring and Logging

### Application Monitoring
- Spring Boot Actuator endpoints
- Custom health checks
- Performance metrics
- Error tracking

### Frontend Monitoring
- Error boundaries for React errors
- API call monitoring
- User interaction tracking
- Performance metrics

---

**The frontend is now fully integrated with the Spring Boot backend and ready for production deployment!**
