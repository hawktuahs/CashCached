@echo off
echo Testing BT Bank Services...
echo.

echo Testing Customer Service (Port 8081)...
curl -s -o nul -w "Customer Service: %%{http_code}\n" http://localhost:8081/api/auth/register -X POST -H "Content-Type: application/json" -d "{\"email\":\"test@example.com\",\"password\":\"password123\",\"firstName\":\"John\",\"lastName\":\"Doe\"}"

echo Testing Main Gateway (Port 8080)...
curl -s -o nul -w "Main Gateway: %%{http_code}\n" http://localhost:8080/api/auth/register -X POST -H "Content-Type: application/json" -d "{\"email\":\"test@example.com\",\"password\":\"password123\",\"firstName\":\"John\",\"lastName\":\"Doe\"}"

echo Testing Frontend...
curl -s -o nul -w "Frontend: %%{http_code}\n" http://localhost:8080/

echo.
echo Expected responses:
echo - Customer Service: 400 (Bad Request - expected for test data)
echo - Main Gateway: 400 (Bad Request - expected for test data)  
echo - Frontend: 200 (OK)
echo.
echo If you see connection errors, the services are not running.
echo If you see 400 errors, the services are running correctly!
