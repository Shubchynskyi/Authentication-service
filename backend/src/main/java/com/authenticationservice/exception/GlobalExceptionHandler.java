package com.authenticationservice.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;
    private static final String BAD_REQUEST = "Bad Request";

    private String resolveMessage(String messageKey, String fallback) {
        if (messageKey == null) {
            return fallback;
        }
        try {
            return messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return fallback != null ? fallback : messageKey;
        }
    }

    @ExceptionHandler(AccessListDuplicateException.class)
    public ResponseEntity<Map<String, String>> handleAccessListDuplicate(AccessListDuplicateException ex) {
        String message = ex.getResolvedMessage() != null
                ? ex.getResolvedMessage()
                : resolveMessage(ex.getMessageKey(), "User already in list");
        log.warn("Duplicate entry for {}: {}", ex.getListType(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", BAD_REQUEST, "message", message));
    }

    @ExceptionHandler(RegistrationForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleRegistrationForbidden(RegistrationForbiddenException ex) {
        String message = resolveMessage(ex.getMessageKey(), "Unable to complete registration. Contact administrator.");
        log.warn("Registration forbidden: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", BAD_REQUEST, "message", message));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, String>> handleAccountLocked(AccountLockedException ex) {
        // Return same message as InvalidCredentialsException to not reveal account existence
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Unauthorized",
                        "message", com.authenticationservice.constants.SecurityConstants.INVALID_CREDENTIALS_ERROR));
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<Map<String, String>> handleAccountBlocked(AccountBlockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Account blocked",
                        "message", ex.getMessage(),
                        "reason", ex.getReason() != null ? ex.getReason() : "Unknown"));
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidVerificationCode(InvalidVerificationCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", BAD_REQUEST,
                        "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Unauthorized",
                        "message", ex.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of(
                        "error", "Too Many Requests",
                        "message", ex.getMessage(),
                        "retryAfterSeconds", ex.getRetryAfterSeconds()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Forbidden",
                        "message", "Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String message = error.getDefaultMessage();
                    // Try to resolve localized message if it's a message key
                    if (message != null && message.startsWith("{") && message.endsWith("}")) {
                        String messageKey = message.substring(1, message.length() - 1);
                        try {
                            message = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
                        } catch (Exception e) {
                            // If message not found, use default
                        }
                    }
                    return message;
                })
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation Error", "message", errorMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", BAD_REQUEST, "message", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        // Business logic errors should return 400 (Bad Request)
        // Note: Registration errors during user registration (not in whitelist, blacklist) are handled
        // by RegistrationForbiddenException, but admin operations (createUser/updateUser) still use
        // RuntimeException with "already exists" message
        if (message != null && (
                message.contains("already exists") ||
                message.contains("not found") ||
                message.contains("already in whitelist") ||
                message.contains("Role not found") ||
                message.contains("Insufficient permissions") ||
                message.contains("Invalid/expired refresh token") ||
                message.contains("Invalid or expired reset token") ||
                message.contains("Expired reset token") ||
                message.contains("Current password is required") ||
                message.contains("Incorrect current password") ||
                message.contains("expired"))) {
            log.warn("Business logic error: {}", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "message", message));
        }
        // For other RuntimeExceptions, treat as internal server error
        log.error("RuntimeException occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "An error occurred. Please try again later."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // Security: Never expose internal error details to clients
        log.error("Internal server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "An error occurred. Please try again later."));
    }
}
