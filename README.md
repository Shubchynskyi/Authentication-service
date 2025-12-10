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
  - Use the provided scripts `build-and-test.sh`, `deploy.sh`

### Environment and Security

- Use environment variables (see `env.example`) or secure secret management solutions for sensitive configuration.  

### Implemented Features

- **Whitelist/Blacklist Access Control**: Configurable access mode that allows restricting user registration and login to whitelisted emails or blocking specific emails via blacklist. Access mode can be set via `ACCESS_MODE_DEFAULT` environment variable (values: `WHITELIST`, `BLACKLIST`, `OPEN`).

- **OAuth2 Google Authentication**: Users can authenticate using their Google accounts. Tokens are securely passed via URL fragments to prevent server-side logging.

- **Asynchronous Email Notifications**: Lock and block notifications are sent asynchronously to prevent delays in API responses.

- **Multi-language Support**: Full localization for 4 languages (EN, DE, RU, UA) including validation error messages.

### Roadmap (Future Improvements)

1. **Authorization Code Flow for OAuth2**
   - Current implementation passes tokens via URL fragment which, while secure from server logs, still appears in browser history
   - Future improvement: implement one-time authorization code exchange where tokens are returned via secure API call instead of URL
   - This provides better security as tokens never appear in URLs

2. **Decoy Page Mode (Security Through Obscurity)**
   - New environment variable `HIDE_LOGIN_PAGE` (boolean)
   - When enabled, generates a decoy HTML page with multiple random links
   - Only one specific link leads to the actual login page
   - Provides additional protection against automated attacks and casual intruders
   - The real login endpoint would be accessible via a secret path or specific link

3. **Domain-Specific Handling for Whitelist Violations**
   - Replace generic `RuntimeException` with a dedicated `EmailNotWhitelistedException`
   - Map the exception to a consistent HTTP response (e.g., 403/400) via a global handler with a standardized error code/message
   - Keep user-facing messages and error codes centralized (config/constants) to avoid hardcoded strings and simplify localization

4. **Rate Limit for “Resend Code” Button**
   - Limit the “Resend code” action to once per minute to prevent spam and email/SMS abuse
   - Add user-facing feedback for remaining wait time to improve UX

5. **Localized Error for Expired/Old Verification Codes**
   - When a user requests a new code but tries activating with the old one, return a clear, localized error explaining the code is outdated and a new one was sent
   - Centralize the error message in existing localization resources to avoid hardcoded strings

6. **Password Reset Email Cooldown**
   - After a successful reset-code send, block further sends for 10 minutes to prevent spamming
   - Keep the same generic “email sent if account exists” response to avoid leaking account existence

7. Add session policy with short-lived access tokens, bounded refresh tokens, and a “remember this device” option with device-bound long-lived refresh and secure storage.


