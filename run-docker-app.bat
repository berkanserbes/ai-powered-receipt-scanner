@echo off
REM Windows script: Starts the application with Docker
cd /d "%~dp0"
REM .env file must exist in the project root
if not exist .env (
  echo .env file not found!
  pause
  exit /b 1
)
echo Starting Docker Compose...
docker compose -f docker\docker-compose.yaml --env-file .env up -d --build
if %errorlevel% neq 0 (
  echo Failed to start Docker Compose!
  pause
  exit /b 1
)
echo Application and services started successfully.
pause