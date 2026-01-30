#!/usr/bin/env bash
set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Load environment variables from .env file if it exists
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo -e "${YELLOW}Loading environment variables from .env...${NC}"
    # Use set -a to export all variables, and handle quoted values properly
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
else
    echo -e "${YELLOW}Warning: .env file not found. Using default values.${NC}"
fi

# Configuration - use variables from .env or defaults
BUILDER_IMAGE_NAME=${BUILDER_IMAGE_NAME:-auth-service-builder}
APP_TEST_CONTAINER_NAME=${APP_TEST_CONTAINER_NAME:-auth-service-test-builder}
JAR_NAME=${JAR_NAME:-authorization-service-1.0.0.jar}
DOCKERFILE_BUILD=${DOCKERFILE_BUILD:-backend/Docker-Build.Dockerfile}
BACKEND_DIR="$SCRIPT_DIR/backend"
JAR_PATH="$BACKEND_DIR/$JAR_NAME"

echo -e "${GREEN}=== Building and Testing Authentication Service ===${NC}"

# Function to clean up existing containers and images
cleanup_existing_resources() {
    echo -e "${YELLOW}Cleaning up existing resources...${NC}"
    
    # Remove container if exists
    if docker ps -a --format '{{.Names}}' | grep -Eq "^${APP_TEST_CONTAINER_NAME}\$"; then
        echo "Removing container $APP_TEST_CONTAINER_NAME..."
        docker rm -f "$APP_TEST_CONTAINER_NAME" || true
    fi
    
    # Remove builder image if exists
    if docker images -q "$BUILDER_IMAGE_NAME" > /dev/null 2>&1; then
        echo "Removing builder image $BUILDER_IMAGE_NAME..."
        docker rmi "$BUILDER_IMAGE_NAME" || true
    fi
    
    # Remove old JAR if exists
    if [ -f "$JAR_PATH" ]; then
        echo "Removing old JAR file..."
        rm -f "$JAR_PATH"
    fi
}

# Cleanup before building
cleanup_existing_resources

# Build the builder image
echo -e "${GREEN}Building the builder image...${NC}"
docker build -t "$BUILDER_IMAGE_NAME" -f "$SCRIPT_DIR/$DOCKERFILE_BUILD" "$BACKEND_DIR"

# Determine the environment: Windows or Linux
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    echo -e "${YELLOW}Running in Windows environment.${NC}"
    DOCKER_SOCKET="//var/run/docker.sock"
    HOST_OVERRIDE="host.docker.internal"
else
    echo -e "${YELLOW}Running in Linux/Mac environment.${NC}"
    DOCKER_SOCKET="/var/run/docker.sock"
    HOST_OVERRIDE="172.17.0.1"
fi

# Trap to clean up on error
trap 'echo -e "${RED}An error occurred. Cleaning up...${NC}"; \
      docker rm -f "$APP_TEST_CONTAINER_NAME" 2>/dev/null || true; \
      docker rmi "$BUILDER_IMAGE_NAME" 2>/dev/null || true; \
      rm -f "$JAR_PATH" 2>/dev/null || true; \
      exit 1' ERR

# Run the container to build the JAR (tests disabled)
echo -e "${GREEN}Building JAR (tests disabled)...${NC}"
# Note: Testcontainers configuration for Docker-in-Docker scenario
docker run --name "$APP_TEST_CONTAINER_NAME" \
    -v "$DOCKER_SOCKET:/var/run/docker.sock" \
    -e TESTCONTAINERS_HOST_OVERRIDE="$HOST_OVERRIDE" \
    -e TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
    -e DOCKER_HOST=unix:///var/run/docker.sock \
    -e TESTCONTAINERS_RYUK_DISABLED=true \
    -e TESTCONTAINERS_CHECKS_DISABLE=true \
    "$BUILDER_IMAGE_NAME" mvn clean verify

# Check the exit status of the container
EXIT_CODE=$(docker inspect "$APP_TEST_CONTAINER_NAME" --format='{{.State.ExitCode}}')
echo -e "${YELLOW}Container finished with exit code: $EXIT_CODE${NC}"

if [ "$EXIT_CODE" -ne 0 ]; then
    echo -e "${RED}Tests failed! Container logs:${NC}"
    docker logs "$APP_TEST_CONTAINER_NAME"
    exit 1
fi

# Copy the JAR file from the container
echo -e "${GREEN}Copying JAR file from container...${NC}"
if docker cp "$APP_TEST_CONTAINER_NAME:/app/target/$JAR_NAME" "$JAR_PATH" 2>/dev/null; then
    echo -e "${GREEN}JAR file copied successfully to $JAR_PATH${NC}"
else
    echo -e "${RED}Failed to copy JAR file from /app/target/$JAR_NAME${NC}"
    echo -e "${YELLOW}Attempting to list files in /app/target directory:${NC}"
    docker cp "$APP_TEST_CONTAINER_NAME:/app/target/" /tmp/target_listing 2>/dev/null && ls -la /tmp/target_listing/ 2>/dev/null || echo "Could not list target directory"
    echo -e "${RED}Container logs:${NC}"
    docker logs "$APP_TEST_CONTAINER_NAME"
    exit 1
fi

# Cleanup
echo -e "${YELLOW}Cleaning up test container...${NC}"
docker rm "$APP_TEST_CONTAINER_NAME"
docker rmi "$BUILDER_IMAGE_NAME" || echo "Note: Could not remove builder image (may be in use)"

echo -e "${GREEN}=== Build and test completed successfully! ===${NC}"
echo -e "${GREEN}JAR file location: $JAR_PATH${NC}"

