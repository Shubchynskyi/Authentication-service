# Dockerfile for building and testing
# This image is used to run tests with testcontainers and build the JAR
FROM eclipse-temurin:23-jdk-alpine AS build

WORKDIR /app

# Install Maven and Docker CLI (for testcontainers)
RUN apk add --no-cache maven docker-cli

# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Run tests and build the JAR
# This will be executed when container runs with docker.sock mounted
CMD ["mvn", "clean", "verify"]

