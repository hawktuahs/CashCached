@echo off
echo Starting BT Bank Microservices...
echo.

echo Starting Customer Service on port 8081...
start "Customer Service" cmd /k "cd customer && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak > nul

echo Starting Product Service on port 8082...
start "Product Service" cmd /k "cd product-pricing && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak > nul

echo Starting FD Calculator Service on port 8083...
start "FD Calculator Service" cmd /k "cd fd-calculator && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak > nul

echo Starting Accounts Service on port 8084...
start "Accounts Service" cmd /k "cd accounts && .\mvnw.cmd spring-boot:run"

timeout /t 10 /nobreak > nul

echo Starting Main Gateway with Frontend on port 8080...
start "Main Gateway" cmd /k "cd main && .\mvnw.cmd spring-boot:run"

echo.
echo All services are starting...
echo.
echo Service URLs:
echo - Frontend: http://localhost:8080
echo - Customer Service: http://localhost:8081
echo - Product Service: http://localhost:8082
echo - FD Calculator Service: http://localhost:8083
echo - Accounts Service: http://localhost:8084
echo - API Documentation: http://localhost:8080/swagger-ui.html
echo.
echo Please wait for all services to start completely before testing.
echo Check the individual command windows for startup status.
