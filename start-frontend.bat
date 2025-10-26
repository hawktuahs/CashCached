@echo off
setlocal

set "ROOT=%~dp0"
pushd "%ROOT%main\frontend"

echo Checking package manager
where pnpm >nul 2>nul
if %errorlevel% EQU 0 (
  echo Using pnpm
  call pnpm install
  echo Starting Vite dev server
  start "BT Frontend" cmd /k "pnpm dev"
) else (
  where npm >nul 2>nul
  if %errorlevel% EQU 0 (
    echo Using npm
    call npm install
    echo Starting Vite dev server
    start "BT Frontend" cmd /k "npm run dev"
  ) else (
    echo Neither pnpm nor npm found on PATH
    popd
    exit /b 1
  )
)

popd
