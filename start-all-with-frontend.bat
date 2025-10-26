@echo off
setlocal

set "ROOT=%~dp0"

echo Starting all backend services...
call "%ROOT%start-all-services.bat"

echo Starting frontend...
call "%ROOT%start-frontend.bat"

echo Launched backend services and frontend. Check the opened windows for logs.
exit /b 0
