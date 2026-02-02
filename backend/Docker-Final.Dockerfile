# Final production Docker image
# This image contains only the built JAR file
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Build argument for JAR name
ARG JAR_NAME=authorization-service-1.0.0.jar

# Copy the built JAR file from the host machine
# The JAR should be copied by build-and-test.sh script
COPY ${JAR_NAME} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

