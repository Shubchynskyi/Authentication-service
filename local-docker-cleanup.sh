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

# Use variables from .env or defaults
BUILDER_IMAGE_NAME=${BUILDER_IMAGE_NAME:-auth-service-builder}
FINAL_IMAGE_NAME=${FINAL_IMAGE_NAME:-auth-service:latest}
APP_TEST_CONTAINER_NAME=${APP_TEST_CONTAINER_NAME:-auth-service-test-builder}
AUTH_SERVICE_CONTAINER_NAME=${AUTH_SERVICE_CONTAINER_NAME:-auth-service}
AUTH_FRONTEND_CONTAINER_NAME=${AUTH_FRONTEND_CONTAINER_NAME:-auth-frontend}
AUTH_DB_CONTAINER_NAME=${AUTH_DB_CONTAINER_NAME:-auth-db}
JAR_NAME=${JAR_NAME:-authorization-service-0.0.1-SNAPSHOT.jar}
COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.yml}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
JAR_PATH="$BACKEND_DIR/$JAR_NAME"
VOLUME_NAME=${VOLUME_NAME:-postgres_data}

echo -e "${GREEN}=== Starting Docker Cleanup ===${NC}"

# First, stop and remove containers via docker compose (if compose file exists)
if [ -f "$SCRIPT_DIR/$COMPOSE_FILE" ]; then
    echo -e "${YELLOW}Stopping containers via docker compose...${NC}"
    cd "$SCRIPT_DIR"
    docker compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || \
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    echo -e "${GREEN}Containers stopped via docker compose.${NC}"
else
    echo -e "${YELLOW}Compose file not found. Stopping containers manually...${NC}"
fi

# Define all containers to be removed (in case some weren't stopped by compose)
ALL_CONTAINERS=(
    "$APP_TEST_CONTAINER_NAME"
    "$AUTH_SERVICE_CONTAINER_NAME"
    "$AUTH_FRONTEND_CONTAINER_NAME"
    "$AUTH_DB_CONTAINER_NAME"
)

# Remove any remaining containers related to the application
echo -e "${YELLOW}Removing any remaining containers...${NC}"
for CONTAINER in "${ALL_CONTAINERS[@]}"; do
    if docker ps -a --format '{{.Names}}' | grep -Eq "^${CONTAINER}\$"; then
        echo "Removing container $CONTAINER..."
        docker rm -f "$CONTAINER" || true
    else
        echo "Container $CONTAINER not found. Skipping..."
    fi
done

# Define all images to be removed
ALL_IMAGES=(
    "$BUILDER_IMAGE_NAME"
    "$FINAL_IMAGE_NAME"
    "authentication-service-auth-service"
    "authentication-service-frontend"
)

# Remove all images related to the application
echo -e "${YELLOW}Removing images...${NC}"
# Remove all images related to the application
echo -e "${YELLOW}Removing images...${NC}"
for IMAGE in "${ALL_IMAGES[@]}"; do
    # Find all image IDs associated with the repository name
    IMAGE_IDS=$(docker images --format "{{.ID}} {{.Repository}}" | grep "$IMAGE" | awk '{print $1}')
    
    if [ -n "$IMAGE_IDS" ]; then
        echo "Found images for $IMAGE. Removing..."
        # Convert newlines to spaces
        echo "$IMAGE_IDS" | xargs -r docker rmi -f || true
    else
        echo "No images found for $IMAGE. Skipping..."
    fi
done

# Remove the JAR file if it exists
if [ -f "$JAR_PATH" ]; then
    echo -e "${YELLOW}Removing JAR file: $JAR_PATH...${NC}"
    rm -f "$JAR_PATH" || true
else
    echo "JAR file not found at $JAR_PATH. Skipping..."
fi

# Remove the volume if it exists
if docker volume ls --format '{{.Name}}' | grep -Eq "^${VOLUME_NAME}\$"; then
    echo -e "${YELLOW}Removing volume $VOLUME_NAME...${NC}"
    docker volume rm "$VOLUME_NAME" || true
else
    echo "Volume $VOLUME_NAME not found. Skipping..."
fi

# Also try to remove volumes with project prefix
PROJECT_VOLUME="${COMPOSE_PROJECT_NAME:-authentication-service}_${VOLUME_NAME}"
if docker volume ls --format '{{.Name}}' | grep -Eq "^${PROJECT_VOLUME}\$"; then
    echo -e "${YELLOW}Removing volume $PROJECT_VOLUME...${NC}"
    docker volume rm "$PROJECT_VOLUME" || true
fi

echo -e "${GREEN}=== Cleanup completed successfully! ===${NC}"

