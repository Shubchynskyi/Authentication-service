# Authentication Service

<div style="text-align: center;">

**Version: 1.0.0**

A full-stack authentication and user profile management solution built with Spring Boot (backend) and React (frontend). This service provides comprehensive user authentication, authorization, security features, and administrative tools.

</div>

## Overview

This project implements a production-ready authentication service with features including JWT-based authentication, OAuth2 (Google) integration, role-based access control, whitelist/blacklist management, masked login functionality, and comprehensive security measures.

## Features

### 🔐 Authentication

- **User Registration**: Email-based registration with email verification
- **Login/Logout**: JWT-based authentication with access tokens (15 minutes) and refresh tokens (7 days, stored in httpOnly cookies and rotated on refresh)
- **Email Verification**: Token-based verification with resend functionality
- **Password Reset**: Secure password reset flow with 1-hour token expiration and cooldown between requests
- **OAuth2 Google Authentication**: Social login via Google OAuth2 with access token returned via URL fragment
- **Token Refresh**: Automatic access token refresh via httpOnly refresh cookie with refresh token rotation and re-use detection

### 🛡️ Authorization

- **Role-Based Access Control (RBAC)**: USER and ADMIN roles with protected endpoints
- **Resource Access Control**: Endpoint-level access checks for admin panel and user management
- **Whitelist/Blacklist Access Control**: 
  - Configurable access mode (WHITELIST/BLACKLIST) via `ACCESS_MODE_DEFAULT`
  - Admin can switch modes with OTP + password verification
  - Admin can manage whitelist and blacklist entries
  - Blacklist always blocks login regardless of credentials

### 🔒 Security

- **Account Protection**: 
  - 5 failed login attempts → 5-minute temporary lock with email notification
  - 10 failed attempts → permanent account block with email notification
- **Rate Limiting**: Bucket4j per-IP rate limiting
  - Authentication endpoints: 120 requests/minute (2 requests/second)
  - Admin endpoints: 120 requests/minute (2 requests/second)
  - Resend endpoints: 1 request/minute per email
  - Responses include `Retry-After` header and `retryAfterSeconds` field to inform clients when to retry
- **Password Validation**: Strong password requirements with regex validation
- **Secure Password Storage**: BCrypt password hashing
- **JWT Security**: Refresh token stored in httpOnly cookie with rotation and re-use detection; access token kept in memory with configurable expiration
- **CSRF Protection**: Enabled for cookie-based refresh/logout flows
- **Content Security Policy (CSP)**: Default CSP headers to reduce XSS risk
- **Security Event Logging**: Comprehensive logging of rate limiting and authentication failures

### 👥 User Management

- **Profile Management**: View and update user profile data
- **User Management**: Admin can view user details, create new users, update name/email/block status, update roles via dedicated endpoint, and delete users
- **User Search & Pagination**: Search users with pagination support
- **Role Management**: Admin can assign and update user roles

### ⚙️ Admin Panel

- **User Management Interface**: Admin interface for managing users (view, create, update, delete)
- **Access List Management**: Manage whitelist and blacklist entries
- **Access Mode Control**: Switch between WHITELIST and BLACKLIST modes (requires OTP + password)
- **Admin Initialization**: Automatic admin account creation on startup (when `ADMIN_ENABLED=true`)
  - Admin receives setup password link via email
- **Masked Login Settings**: Configure masked login functionality (enable/disable, select template)
- **Audit Trail**: Separate log file for all admin actions

### 🎭 Masked Login

**Masked Login** is a security feature that displays a fake page (template) to unauthenticated users instead of the real login page. This helps protect the actual login endpoint from automated attacks and reconnaissance.

- **10 Pre-built Templates**: 
  1. 404 Not Found
  2. Site Maintenance
  3. Cooking Recipe
  4. Terms of Service
  5. About Us
  6. Cat Facts
  7. Lorem Ipsum
  8. Weather
  9. Coming Soon
  10. Database Error

<div style="text-align: center;">
<img src="docs/images/masked-login-templates-collage.png" alt="Masked Login Templates Collage" style="max-width: 800px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>
*All 10 pre-built masked login templates*

- **Admin Configuration**: Enable/disable masked login and select template via admin panel (requires password confirmation)
- **Public Settings Endpoint**: Frontend can check if masked login is enabled without authentication
- **Secret Access**: Authenticated users and users with `?secret=true` parameter can access the real login page

### ✨ Other Features

- **Multi-language Support**: Full localization for 4 languages (EN, DE, RU, UA), including validation errors
- **Asynchronous Email Notifications**: Lock/block notifications sent asynchronously
- **Resend Cooldown**: Backend enforces 1 request/minute per email with HTTP 429; frontend shows countdown timer
- **Comprehensive Logging**: 
  - Request correlation with trace IDs across all logs
  - Performance metrics for critical operations
  - Structured logging support (JSON format when `LOG_JSON_ENABLED=true`)
  - Time-based log rotation with size limits
  - Separate log files for application, errors, and admin actions

## Tech Stack

### 🔧 Backend

- **Java 25**
- **Spring Boot 4.0.2 (Spring Framework 7.0.3)**
- **Spring Security** - Authentication and authorization
- **Spring Data JPA / Hibernate** - Database persistence
- **PostgreSQL 17.5** - Database
- **JWT (jjwt 0.12.6)** - Token-based authentication
- **OAuth2 Client** - Google OAuth2 integration
- **Bucket4j 8.10.1** - Rate limiting
- **SMTP Mail** - Email notifications
- **Lombok** - Boilerplate reduction
- **SLF4J + Logback** - Logging with structured logging support
- **Testcontainers 2.0.2** - Integration testing with Docker containers
- **JUnit 5** - Unit and integration testing
- **Mockito** - Mocking framework

### 🎨 Frontend

- **React 18**
- **TypeScript 5.2**
- **Vite 5** - Build tool and dev server
- **Material-UI (MUI) 5** - UI component library
- **React Router 6** - Client-side routing
- **Context API** - State management
- **Axios** - HTTP client
- **i18next** - Internationalization (EN, DE, RU, UA)
- **Vitest** - Unit testing
- **Testing Library** - React component testing

## Installation & Setup

### 📋 Prerequisites

- **Java 25** and **Maven** for the backend
- **Node.js 18+** and **npm** for the frontend
- **Docker** (optional) for containerized workflow and Testcontainers
- **PostgreSQL** database (or use Docker)

### 🚀 Quick Start

#### 🐳 Using Docker (Recommended)

1. **Build and test backend**:
   ```bash
   ./build-and-test.sh
   ```

2. **Deploy with Docker Compose**:
   ```bash
   # For local development (with exposed ports)
   ./deploy.sh local
   
   # For server deployment (no exposed ports, uses networks)
   ./deploy.sh server
   ```

#### 💻 Manual Setup

1. **Backend**:
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

2. **Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev -- --host --port 5173
   ```

### 🔑 Environment Variables

Create a `.env` file based on `env.example`. Required variables:

- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `JWT_ACCESS_SECRET` - Secret key for access tokens
- `JWT_REFRESH_SECRET` - Secret key for refresh tokens
- `GOOGLE_CLIENT_ID` - Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth2 client secret
- `MAIL_HOST` - SMTP server host
- `MAIL_USERNAME` - SMTP username
- `MAIL_PASSWORD` - SMTP password
- `FRONTEND_URL` - Frontend application URL
- `ADMIN_ENABLED` - Enable admin initialization (true/false)
- `ADMIN_EMAIL` - Admin email address
- `ADMIN_USERNAME` - Admin username

See `env.example` for all available configuration options.

## Integration Guide

Use this service as a central auth provider while keeping other apps separate.

### ✅ Recommended setup (single domain, path-based routing)

Route the following paths to different services (via nginx/traefik/reverse proxy):

- Auth frontend pages (e.g. `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify`, `/oauth2/success`, `/profile`, `/admin`) → auth-service frontend
- `/api/auth/*` → auth-service backend
- `/superapp` → other app frontend
- `/superapi/*` → other app backend

### 🌐 Nginx example

Use path-based routing on a single domain. This example routes `/superapp` and `/superapi` first, then defaults to the auth frontend for all other pages.

```nginx
server {
    listen 80;
    server_name my-app.com;

    # Auth API
    location /api/auth/ {
        proxy_pass http://auth-backend;
    }

    # Other app API
    location /superapi/ {
        proxy_pass http://superapp-backend;
    }

    # Other app frontend
    location /superapp/ {
        proxy_pass http://superapp-frontend;
    }

    # Auth frontend (login/profile/admin/register/etc.)
    location / {
        proxy_pass http://auth-frontend;
    }

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### 🔁 Login redirect flow

Send users to: `/login?redirect=/superapp`

Only relative paths are accepted (e.g. `/superapp`, `/superapp/page`).

### 🧭 Frontend guard (in the other app)

On app start:

```ts
await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' });
// 200 -> save accessToken in memory and continue
// 401 -> window.location.replace('/login?redirect=/superapp')
```

Attach `Authorization: Bearer <accessToken>` for API calls. If you receive 401, refresh and retry.

### 🔐 Backend validation (in the other app)

Validate access tokens with the same `JWT_ACCESS_SECRET` used by this service.
Refresh tokens are httpOnly cookies and are not available to your backend.

### 🧪 Local development notes

- Refresh cookie is `Secure=true` by default. For HTTP local dev set:
  - `SECURITY_REFRESH_COOKIE_SECURE=false`
- Refresh cookie uses `SameSite=Strict` and `Path=/api/auth`.
- `POST /api/auth/refresh` and `POST /api/auth/logout` require `X-XSRF-TOKEN`.
  Call `GET /api/auth/csrf` once per session to set the CSRF cookie if your client doesn't already send it.

## Configuration

### ⚙️ Application Configuration

Settings are managed via `application.yml`. Sensitive data (database credentials, JWT secrets, mail settings, OAuth) must be provided via environment variables.

### 📝 Logging Configuration

- **Configuration file**: `logback-spring.xml`
- **Log files**: 
  - `logs/app.log` - General application logs
  - `logs/error.log` - Errors and warnings only
  - `logs/admin.log` - Admin action audit trail
- **Environment variables**:
  - `LOG_LEVEL` - Root log level
  - `LOG_FILE_ENABLED` - Enable file logging (default: true)
  - `LOG_JSON_ENABLED` - Enable JSON structured logging (default: false)
  - `LOG_MAX_HISTORY` - Number of days to keep logs (default: 30)
  - `LOG_MAX_SIZE` - Maximum log file size (default: 10MB)
  - `LOG_TOTAL_SIZE_CAP` - Total size cap for all logs (default: 1GB)
  - `SLOW_REQUEST_THRESHOLD_MS` - Threshold for slow request warnings (default: 1000ms)

**Note**: Mount `logs/` as a Docker volume for log persistence.

## API Documentation

### 🔐 Authentication Endpoints (`/api/auth`)

- `POST /register` - User registration
- `POST /login` - User login
- `POST /refresh` - Refresh access token
- `POST /logout` - Logout and clear refresh cookie
- `POST /verify` - Verify email address
- `POST /resend-verification` - Resend verification email
- `POST /forgot-password` - Initiate password reset
- `POST /reset-password` - Reset password with token
- `GET /csrf` - Issue CSRF cookie for XSRF protection
- `GET /check-access/{resource}` - Check user access to resource
- `GET /oauth2/success` - OAuth2 callback endpoint

Notes:
- `POST /login` and `POST /refresh` set the refresh token httpOnly cookie and return `accessToken` in JSON.
- `POST /refresh` and `POST /logout` require the `X-XSRF-TOKEN` header (call `GET /csrf` to prime it).

### 👤 User Profile Endpoints (`/api/protected`)

- `GET /profile` - Get user profile (authenticated)
- `POST /profile` - Update user profile (authenticated)
- `GET /check` - Check authentication status

### ⚙️ Admin Endpoints (`/api/admin`)

- `GET /users` - Get all users (paginated, with search)
- `POST /users` - Create new user
- `GET /users/{id}` - Get user by ID
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user
- `PUT /users/{id}/roles` - Update user roles
- `GET /roles` - Get all roles
- `GET /whitelist` - Get whitelist entries
- `POST /whitelist/add` - Add email to whitelist
- `DELETE /whitelist/remove` - Remove email from whitelist
- `GET /blacklist` - Get blacklist entries
- `POST /blacklist/add` - Add email to blacklist
- `DELETE /blacklist/remove` - Remove email from blacklist
- `GET /access-mode` - Get current access mode settings
- `POST /access-mode/request-otp` - Request OTP for mode change
- `POST /access-mode/change` - Change access mode (requires OTP + password)
- `POST /verify-admin` - Verify admin password
- `GET /masked-login/settings` - Get masked login settings
- `PUT /masked-login/settings` - Update masked login settings (requires password)

### 🌐 Public Endpoints (`/api/public/masked-login`)

- `GET /settings` - Get masked login public settings (enabled/disabled, template ID)
- `GET /template/{templateId}` - Get masked login template HTML (template ID: 1-10)

## Project Structure

```
Authentication-service/
├── backend/              # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/     # Java source code
│   │   │   └── resources/ # Configuration files
│   │   └── test/         # Test code
│   └── pom.xml           # Maven dependencies
├── frontend/             # React frontend
│   ├── src/
│   │   ├── components/   # React components
│   │   ├── pages/        # Page components
│   │   ├── services/     # API services
│   │   └── assets/       # Static assets
│   └── package.json      # npm dependencies
├── docker-compose.yml    # Docker Compose configuration
└── README.md             # This file
```

## Future Improvements

The following improvements are planned for future versions:

### 📊 Distributed Rate Limiting (Optional)

**Rate Limiting Service**: Currently uses in-memory storage (`ConcurrentHashMap`) for rate limiting buckets. This works well for single-instance deployments, but has limitations:

- Rate limits are not shared across multiple instances
- When scaling horizontally (multiple instances behind a load balancer), each instance maintains its own rate limit counters
- This means the effective rate limit becomes: `configured_limit × number_of_instances`

**Improvement** (only needed for horizontal scaling): Migrate to Redis-backed rate limiting using Bucket4j's Redis integration. This ensures rate limits are shared across all instances and work correctly in clustered deployments.

**Note**: For single-instance deployments, the current in-memory implementation is perfectly adequate and performant.

### ☕ Java Version & Performance (Implemented)

Service runs on Java 25 LTS with virtual threads enabled for request handling to improve throughput under high concurrent load. To disable, set `spring.threads.virtual.enabled=false`.

### 🏗️ Microservice Architecture

To use this authentication service in a microservices architecture, the following components need to be implemented:

- **JWT Validation Endpoint**: Add a public endpoint (e.g., `/api/auth/validate` or `/api/auth/user-info`) that accepts JWT tokens and returns user information (email, roles). This allows other services to validate tokens and get user context without direct database access.

- **Client Library/Module**: Create a reusable authentication client library (Maven/Gradle module) that other services can integrate to:
  - Validate JWT tokens locally (using shared secret/public key)
  - Extract user information and roles from tokens
  - Handle token expiration and refresh scenarios
  - Provide consistent authentication handling across services

**Current State**: Service is designed as a standalone authentication service but requires additional endpoints and client libraries for microservice integration.

### 🔮 Additional Planned Enhancements

- **Monitoring & Metrics**: Add Spring Boot Actuator or Micrometer + Prometheus integration
- **API Documentation**: Add OpenAPI/Swagger documentation
- **Caching**: Implement caching for frequently accessed data (roles, settings)
- **Health Checks**: Enhanced health check endpoints for Kubernetes/Docker orchestration

## Application Screenshots

### 🔄 Authentication Flow

Complete step-by-step authentication flow demonstration:

<table>
<tr>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-5.png" alt="Authentication Flow Step 5" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-4.png" alt="Authentication Flow Step 4" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-3.png" alt="Authentication Flow Step 3" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-6.png" alt="Authentication Flow Step 6" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
</tr>
<tr>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-1.png" alt="Authentication Flow Step 1" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-2.png" alt="Authentication Flow Step 2" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-7.png" alt="Authentication Flow Step 7" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
<td align="center" width="25%">
<img src="docs/images/app-screenshots/authentication-flow/auth-flow-step-8.png" alt="Authentication Flow Step 8" style="max-width: 100%; width: 240px; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</td>
</tr>
</table>

### 🔐 Authentication Pages

#### Login Page
Standard login form with email and password fields. Also includes Google OAuth2 authentication option.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/login-page.png" alt="Login Page" style="max-width: 600px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Registration Page
User registration form for creating new accounts.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/login-page-oauth.png" alt="Registration Page" style="max-width: 600px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Email Verification
Email verification page for confirming account registration.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/verification-page.png" alt="Verification Page" style="max-width: 600px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

### 👤 User Profile

#### Profile View
User profile page displaying account information and status. For admin users, includes a link to the admin panel.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/profile-page.png" alt="Profile Page" style="max-width: 600px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Profile Edit
Profile editing interface for updating user information.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/profile-edit-page.png" alt="Profile Edit Page" style="max-width: 600px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

### ⚙️ Admin Panel

#### Users Management
Admin panel - Users management tab with pagination.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/admin-users-tab.png" alt="Admin Users Tab" style="max-width: 800px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Add User Form
Form for creating new users. Allows immediate admin role assignment and account blocking during creation.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/admin-add-user-dialog.png" alt="Admin Add User" style="max-width: 600px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Whitelist / Blacklist Management
Admin panel - Whitelist and blacklist management for access control.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/admin-whitelist-tab.png" alt="Admin Whitelist Tab" style="max-width: 800px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Access Mode Control
Admin panel - Access mode control for switching between WHITELIST and BLACKLIST modes for the entire application.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/admin-blacklist-tab.png" alt="Access Mode Control" style="max-width: 800px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

#### Masked Login Settings
Admin panel - Masked login settings with template preview.

<div style="text-align: center;">
<img src="docs/images/app-screenshots/admin-masked-login-settings-1.png" alt="Masked Login Settings 1" style="max-width: 800px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px; margin-bottom: 10px;" />
</div>

<div style="text-align: center;">
<img src="docs/images/app-screenshots/admin-masked-login-settings-2.png" alt="Masked Login Settings 2" style="max-width: 800px; width: 100%; height: auto; border: 1px solid #e4e7ec; border-radius: 8px;" />
</div>

## Authors

- **Dmytro Shubchynskyi**

For any questions or further information, please contact [d.shubchynskyi@gmail.com](mailto:d.shubchynskyi@gmail.com)









