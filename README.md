## Authentication Service

This project is a full-stack authentication and user profile management solution consisting of a Spring Boot backend and a React frontend.

### Backend (Java / Spring Boot)

- **Technology stack**: Spring Boot, Spring Security, JWT, OAuth2 (Google), JPA/Hibernate, REST API, Bucket4j rate limiting, SMTP Mail, Testcontainers + JUnit 5  
- **Responsibilities**:
  - User registration, login, logout, refresh tokens (access 15m, refresh 7d)
  - Email-based verification and password reset flows (1h token, cooldown between requests)
  - Role-based access control (USER/ADMIN) and protected endpoints
  - Whitelist/blacklist access control with switchable modes (OTP + password verification for mode changes)
  - Secure password storage/validation and account protection (locks/blocks after failed attempts)
  - User profile management (view and update profile data)
- **Configuration**:
  - Settings are managed via `application.yml`
  - Sensitive data (DB credentials, JWT secrets, mail, OAuth) must be provided via environment variables
  - **Logging**:
    - **Backend**: SLF4J + Logback with structured logging, MDC context (trace ID, user email, IP, HTTP method, path), and request correlation
    - **Configuration**: `logback-spring.xml` with console + rolling file appenders. Separate log files for application (`app.log`), errors (`error.log`), and admin actions (`admin.log`)
    - **Environment variables**: `LOG_LEVEL`, `APP_LOG_LEVEL`, `SPRING_SECURITY_LOG_LEVEL`, `LOG_FILE_ENABLED`, `LOG_FILE_PATH`, `LOG_ERROR_FILE_PATH`, `LOG_ADMIN_FILE_PATH`, `LOG_MAX_HISTORY`, `LOG_MAX_SIZE`, `LOG_TOTAL_SIZE_CAP`, `LOG_JSON_ENABLED`, `SLOW_REQUEST_THRESHOLD_MS`
    - **Features**:
      - Time-based log rotation (daily) with size limit and total size cap
      - HTTP request/response logging with performance metrics (slow requests logged as warnings)
      - Automatic request correlation via `X-Request-Id` header (generated if not present)
      - JSON structured logging support (when `LOG_JSON_ENABLED=true`)
      - Performance metrics logging for critical operations
      - Security event logging (rate limiting, authentication failures)
    - Mount `logs/` as a Docker volume for persistence


### Frontend (React / TypeScript)

- **Technology stack**: React, TypeScript, Vite, React Router, Context API, MUI, Axios, i18next, Vitest + Testing Library  
- **Responsibilities**:
  - Authentication UI (login, registration, email verification, password reset)
  - Protected routes for authenticated and admin-only pages
  - User profile pages and profile editing
  - Internationalization (EN, DE, RU, UA) and theming/notifications
  - Resend/verification timers and password strength checks aligned with backend regex
  - **Logging**: Structured error logging for critical failures (server errors, network failures, token refresh failures) with minimal production overhead


### Running the Project

- **Prerequisites**:
  - Java 21 and Maven for the backend
  - Node.js 18+ and npm for the frontend
  - Docker (optional) for containerized workflow and Testcontainers
- **Quick start**:
  - Scripts: `build-and-test.sh` (backend build/tests in Docker), `deploy.sh` (compose run: `./deploy.sh local` or `./deploy.sh server`)
  - Manual: `cd backend && mvn spring-boot:run`; `cd frontend && npm install && npm run dev -- --host --port 5173`


### Implemented Features

- **Whitelist/Blacklist Access Control**: Configurable mode via `ACCESS_MODE_DEFAULT` (`WHITELIST`, `BLACKLIST`); admin can switch modes with OTP + password verification and manage lists.
- **OAuth2 Google Authentication**: Tokens returned via URL fragment to avoid server-side logging.
- **Asynchronous Email Notifications**: Lock/block notifications sent asynchronously.
- **Account Protection**: 5 failed logins → 5m lock with email; 10 failed → full block; blacklist always blocks login.
- **Password Reset Flow**: 1h reset token, cooldown between requests; Google accounts receive an informational email instead of a reset link.
- **Multi-language Support**: Full localization for 4 languages (EN, DE, RU, UA), including validation errors.
- **Rate Limiting**: Bucket4j per-IP buckets (auth 120/min, admin 300/min by default) and a separate resend bucket; responses include `Retry-After` and `retryAfterSeconds`. Rate limit events are logged for security monitoring.
- **Resend Code Cooldown**: Backend enforces 1/min per email with HTTP 429; frontend shows countdown and disables the button.
- **Admin Initialization**: When `ADMIN_ENABLED=true`, an admin account is created on startup using `ADMIN_EMAIL` and receives a setup-password link via `FRONTEND_URL`.
- **Comprehensive Logging**: 
  - Request correlation with trace IDs across all logs
  - Performance metrics for critical operations (login, registration, email sending)
  - Security event logging (rate limiting, authentication failures)
  - Structured logging support for log aggregation systems
  - Admin action audit trail in separate log file

### Contact

For any questions or further information, please contact [d.shubchynskyi@gmail.com](mailto:d.shubchynskyi@gmail.com)