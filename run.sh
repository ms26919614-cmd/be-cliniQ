#!/bin/bash

# ===========================
# CliniQ Backend - Run Script
# Sets environment variables and starts the Spring Boot application
# ===========================

# Server
export SERVER_PORT=8080

# PostgreSQL Database (database: msc, schema: cliniq)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/msc?currentSchema=cliniq
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres

# JPA
export JPA_DDL_AUTO=validate
export JPA_SHOW_SQL=false

# JWT
export JWT_SECRET=Y2xpbmlxLXNlY3JldC1rZXktZm9yLWp3dC1hdXRoZW50aWNhdGlvbi0yNTYtYml0LW1pbmltdW0=
export JWT_EXPIRATION_MS=86400000

# CORS
export CORS_ALLOWED_ORIGINS=http://localhost:3000

echo "================================"
echo "  CliniQ Backend Starting..."
echo "  Port: $SERVER_PORT"
echo "  DB:   $SPRING_DATASOURCE_URL"
echo "================================"

./mvnw spring-boot:run
