# ===========================
# CliniQ Backend - Dockerfile
# Multi-stage build
# ===========================

# Stage 1: Build
FROM --platform=linux/amd64 eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Install required tools and make mvnw executable
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests -B

# Stage 2: Run
FROM --platform=linux/amd64 eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user
RUN groupadd -r cliniq && useradd -r -g cliniq cliniq

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R cliniq:cliniq /app

# Switch to non-root user
USER cliniq

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8080/api/queue/display || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
