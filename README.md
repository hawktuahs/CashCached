# BT Bank – Full Stack Dev Runner (hawktuah)

This repo contains Spring Boot microservices and a Vite + React frontend. This guide shows how to start everything on Windows using the provided scripts.

## Prerequisites
- Java 17+ and Maven on PATH
- Node.js 18+ on PATH
- pnpm on PATH (recommended). If not present, scripts fall back to npm

## Quick Start (Windows)
From the repository root:

- Start backend + frontend together
  ```bat
  start-all-with-frontend.bat
  ```
  This launches all Spring Boot services, then starts the Vite dev server for the frontend.

- Start only the backend
  ```bat
  start-all-services.bat
  ```

- Start only the frontend
  ```bat
  start-frontend.bat
  ```

## What the scripts do
- start-all-services.bat
  - Opens separate windows for:
    - Customer Service (8081)
    - Product Pricing Service (8082)
    - FD Calculator Service (8083)
    - Accounts Service (8084)
    - Main Gateway (8080)

- start-frontend.bat
  - Installs dependencies with pnpm (or npm if pnpm is missing)
  - Starts Vite dev server in a new window

- start-all-with-frontend.bat
  - Runs start-all-services.bat, then start-frontend.bat

## Service URLs
- Main Gateway API: http://localhost:8080
- Customer: http://localhost:8081
- Product Pricing: http://localhost:8082
- FD Calculator: http://localhost:8083
- Accounts: http://localhost:8084
- Frontend (Vite dev): http://localhost:5173

## Frontend (manual commands)
Location: `main/frontend`

```bat
pnpm install
pnpm dev
```
If pnpm is not available:
```bat
npm install
npm run dev
```

## Troubleshooting
- Do not run Maven inside `main/frontend` (it is a Node project). Use `pnpm dev` or `npm run dev` there
- If ports are busy, close existing Java/Vite windows and rerun the scripts
- Ensure Java, Maven, Node, and pnpm/npm are on PATH


## Stopping services
- Close the opened terminal windows, or press Ctrl+C in each window to stop
