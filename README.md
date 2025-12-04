## Authentication Service

This project is a full-stack authentication and user profile management solution consisting of a Spring Boot backend and a React frontend.

### Backend (Java / Spring Boot)

- **Technology stack**: Spring Boot, Spring Security, JWT, JPA/Hibernate, REST API  
- **Responsibilities**:
  - User registration, login, logout
  - Email-based verification and password reset flows
  - Role-based access control (including admin functionality)
  - Secure password storage and validation
  - User profile management (view and update profile data)
- **Configuration**:
  - Application settings are managed via `application.yml`
  - Sensitive data (e.g. database credentials, JWT secrets, email credentials) must be provided via environment variables or external configuration

### Frontend (React / TypeScript)

- **Technology stack**: React, TypeScript, Vite, React Router, Context API  
- **Responsibilities**:
  - Authentication UI (login, registration, verification, password reset)
  - Protected routes for authenticated and admin-only pages
  - User profile pages and profile editing
  - Internationalization (i18n) with multiple languages (EN, DE, RU, UA)
  - Theming and notification system for user feedback

### Running the Project

- **Prerequisites**:
  - Java and Maven for the backend
  - Node.js and npm for the frontend
  - Docker (optional) for containerized deployment
- **Quick start**:
  - Use the provided scripts (`build-and-test.sh`, `deploy.sh`) or the Docker Compose files (`docker-compose.yml`, `docker-compose-server.yaml`) to build and run the full stack.

### Environment and Security

- Use environment variables (see `env.example`) or secure secret management solutions for sensitive configuration.  


