# Stage 1: Build the frontend application
FROM node:18-alpine AS build

WORKDIR /app

# Copy package.json and package-lock.json (or yarn.lock) to leverage Docker cache
COPY package*.json ./

# Install all dependencies, including devDependencies for testing
RUN npm install

# Copy the rest of the application source code
COPY . .

# Run tests. If tests fail, the build process will stop here.
# Use test:ci script which runs tests once and exits (no watch mode)
# Set CI environment variable to ensure tests exit properly
ENV CI=true
RUN npm run test:ci

# Set build arguments and environment variables
ARG VITE_API_URL=http://localhost/api
ENV VITE_API_URL=$VITE_API_URL

# Build the production version of the application
# Ensure this command does NOT try to compile test files.
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM nginx:alpine

# Copy the built application from the 'build' stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]