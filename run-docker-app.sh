#!/bin/bash
# Linux/Mac script: Starts the application with Docker
cd "$(dirname "$0")"
if [ ! -f .env ]; then
  echo ".env file not found!"
  exit 1
fi
echo "Starting Docker Compose..."
docker compose -f docker/docker-compose.yaml --env-file .env up -d --build
if [ $? -ne 0 ]; then
  echo "Failed to start Docker Compose!"
  exit 1
fi
echo "Application and services started successfully."