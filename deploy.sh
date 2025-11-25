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
JAR_NAME=${JAR_NAME:-authorization-service-0.0.1-SNAPSHOT.jar}
FINAL_IMAGE_NAME=${FINAL_IMAGE_NAME:-auth-service:latest}
DOCKERFILE_FINAL=${DOCKERFILE_FINAL:-backend/Docker-Final.Dockerfile}
BACKEND_DIR="$SCRIPT_DIR/backend"
JAR_PATH="$BACKEND_DIR/$JAR_NAME"

# Determine compose file based on argument or default
if [ -n "$1" ]; then
    case "$1" in
        local)
            COMPOSE_FILE="docker-compose.yml"
            echo -e "${YELLOW}Using local configuration (with ports)...${NC}"
            ;;
        server)
            COMPOSE_FILE="docker-compose-server.yaml"
            echo -e "${YELLOW}Using server configuration (no ports, with networks)...${NC}"
            ;;
        *)
            # Use argument as direct file path
            COMPOSE_FILE="$1"
            echo -e "${YELLOW}Using custom compose file: $COMPOSE_FILE${NC}"
            ;;
    esac
else
    # Use default from .env or fallback
    COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.yml}
    echo -e "${YELLOW}Using default compose file: $COMPOSE_FILE${NC}"
fi

echo -e "${GREEN}=== Deploying Authentication Service ===${NC}"

# Check if JAR file exists
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}Error: JAR file not found at $JAR_PATH${NC}"
    echo -e "${YELLOW}Please run build-and-test.sh first to build and test the application.${NC}"
    exit 1
fi

# Build the final production image
echo -e "${GREEN}Building final production image...${NC}"
docker build \
    --build-arg JAR_NAME="$JAR_NAME" \
    -t "$FINAL_IMAGE_NAME" \
    -f "$SCRIPT_DIR/$DOCKERFILE_FINAL" \
    "$BACKEND_DIR"

# Remove the temporary JAR file (optional, comment out if you want to keep it)
# echo -e "${YELLOW}Removing temporary JAR file...${NC}"
# rm "$JAR_PATH"

# Start services with docker-compose
echo -e "${GREEN}Starting services with docker-compose...${NC}"
docker compose -f "$COMPOSE_FILE" up -d

echo -e "${GREEN}=== Deployment completed successfully! ===${NC}"
echo -e "${GREEN}Services are starting. Check logs with: docker compose -f $COMPOSE_FILE logs -f${NC}"
echo -e "${YELLOW}Usage examples:${NC}"
echo -e "  ./deploy.sh          # Use default compose file from .env"
echo -e "  ./deploy.sh local     # Use docker-compose.yml (with ports)"
echo -e "  ./deploy.sh server   # Use docker-compose-server.yaml (no ports, with networks)"

