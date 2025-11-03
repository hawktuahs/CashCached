@echo off
echo Building frontend for Spring Boot deployment...

cd frontend
call pnpm build

if %ERRORLEVEL% EQU 0 (
    echo Frontend built successfully!
    echo Files are now available in src/main/resources/static/
    echo You can now start the Spring Boot application.
) else (
    echo Frontend build failed!
    exit /b 1
)
