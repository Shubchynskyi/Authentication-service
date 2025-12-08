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
FRONTEND_IMAGE_NAME=${FRONTEND_IMAGE_NAME:-auth-frontend:latest}
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
DEFAULT_COMPOSE_PROJECT_NAME=$(basename "$SCRIPT_DIR" | tr '[:upper:]' '[:lower:]')
COMPOSE_PROJECT_NAME=${COMPOSE_PROJECT_NAME:-$DEFAULT_COMPOSE_PROJECT_NAME}
COMPOSE_SERVICE_PREFIX="${COMPOSE_PROJECT_NAME}-"

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

remove_container_if_exists() {
    local name="$1"
    if docker ps -a --format '{{.Names}}' | grep -Eq "^${name}\$"; then
        echo "Removing container $name..."
        docker rm -f "$name" || true
    else
        echo "Container $name not found. Skipping..."
    fi
}

remove_containers_by_pattern() {
    local pattern="$1"
    local matches
    matches=$(docker ps -a --format '{{.Names}}' | grep -E "$pattern" || true)
    if [ -n "$matches" ]; then
        echo "Removing containers matching pattern: $pattern"
        echo "$matches" | xargs -r docker rm -f || true
    fi
}

# Define all containers to be removed (in case some weren't stopped by compose)
ALL_CONTAINERS=(
    "$APP_TEST_CONTAINER_NAME"
    "$AUTH_SERVICE_CONTAINER_NAME"
    "$AUTH_FRONTEND_CONTAINER_NAME"
    "$AUTH_DB_CONTAINER_NAME"
)

# Containers created by docker-compose with project prefix
COMPOSE_CONTAINERS_PATTERNS=(
    "^${COMPOSE_SERVICE_PREFIX}auth-service(-[0-9]+)?$"
    "^${COMPOSE_SERVICE_PREFIX}frontend(-[0-9]+)?$"
    "^${COMPOSE_SERVICE_PREFIX}db(-[0-9]+)?$"
)

# Remove any remaining containers related to the application
echo -e "${YELLOW}Removing any remaining containers...${NC}"
for CONTAINER in "${ALL_CONTAINERS[@]}"; do
    remove_container_if_exists "$CONTAINER"
done

for PATTERN in "${COMPOSE_CONTAINERS_PATTERNS[@]}"; do
    remove_containers_by_pattern "$PATTERN"
done

# Define all images to be removed
FINAL_IMAGE_REPO="${FINAL_IMAGE_NAME%%:*}"
BUILDER_IMAGE_REPO="${BUILDER_IMAGE_NAME%%:*}"
FRONTEND_IMAGE_REPO="${FRONTEND_IMAGE_NAME%%:*}"

ALL_IMAGE_PATTERNS=(
    "$BUILDER_IMAGE_REPO"
    "$FINAL_IMAGE_REPO"
    "$FRONTEND_IMAGE_REPO"
    "authentication-service-auth-service"
    "authentication-service-frontend"
    "${COMPOSE_SERVICE_PREFIX}auth-service"
    "${COMPOSE_SERVICE_PREFIX}frontend"
)

collect_image_ids() {
    local pattern="$1"
    docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep -Ei "^${pattern}(:|$)" | awk '{print $2}' || true
}

echo -e "${YELLOW}Removing images...${NC}"
IMAGE_IDS=()
for IMAGE_PATTERN in "${ALL_IMAGE_PATTERNS[@]}"; do
    while IFS= read -r ID; do
        [ -n "$ID" ] && IMAGE_IDS+=("$ID")
    done < <(collect_image_ids "$IMAGE_PATTERN")
done

if [ "${#IMAGE_IDS[@]}" -gt 0 ]; then
    mapfile -t UNIQUE_IMAGE_IDS < <(printf "%s\n" "${IMAGE_IDS[@]}" | sort -u)
    echo "Found image IDs: ${UNIQUE_IMAGE_IDS[*]}"
    printf "%s\n" "${UNIQUE_IMAGE_IDS[@]}" | xargs -r docker rmi -f || true
else
    echo "No images matched configured patterns. Skipping..."
fi

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

