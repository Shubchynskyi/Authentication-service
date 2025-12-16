package com.authenticationservice.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

/**
 * Utility class for structured logging with key-value pairs.
 * Helps create more parseable and searchable log messages.
 */
@UtilityClass
public class StructuredLogger {

    /**
     * Logs a structured INFO message with key-value pairs.
     * Format: message | key1=value1 | key2=value2 | ...
     */
    public void logInfo(Logger logger, String message, String... keyValuePairs) {
        if (logger.isInfoEnabled()) {
            String structuredMessage = buildStructuredMessage(message, keyValuePairs);
            logger.info(structuredMessage);
        }
    }

    /**
     * Logs a structured DEBUG message with key-value pairs.
     */
    public void logDebug(Logger logger, String message, String... keyValuePairs) {
        if (logger.isDebugEnabled()) {
            String structuredMessage = buildStructuredMessage(message, keyValuePairs);
            logger.debug(structuredMessage);
        }
    }

    /**
     * Logs a structured WARN message with key-value pairs.
     */
    public void logWarn(Logger logger, String message, String... keyValuePairs) {
        if (logger.isWarnEnabled()) {
            String structuredMessage = buildStructuredMessage(message, keyValuePairs);
            logger.warn(structuredMessage);
        }
    }

    /**
     * Logs a structured ERROR message with key-value pairs and exception.
     */
    public void logError(Logger logger, String message, Throwable throwable, String... keyValuePairs) {
        if (logger.isErrorEnabled()) {
            String structuredMessage = buildStructuredMessage(message, keyValuePairs);
            logger.error(structuredMessage, throwable);
        }
    }

    /**
     * Logs a structured ERROR message with key-value pairs.
     */
    public void logError(Logger logger, String message, String... keyValuePairs) {
        if (logger.isErrorEnabled()) {
            String structuredMessage = buildStructuredMessage(message, keyValuePairs);
            logger.error(structuredMessage);
        }
    }

    /**
     * Logs performance metrics with execution time.
     */
    public void logPerformance(Logger logger, String operation, long durationMs, String... additionalKeyValues) {
        String[] keyValues = new String[additionalKeyValues.length + 2];
        keyValues[0] = "operation=" + operation;
        keyValues[1] = "durationMs=" + durationMs;
        System.arraycopy(additionalKeyValues, 0, keyValues, 2, additionalKeyValues.length);
        
        if (durationMs > 1000) {
            logWarn(logger, "Slow operation detected", keyValues);
        } else {
            logDebug(logger, "Operation completed", keyValues);
        }
    }

    /**
     * Builds a structured message from base message and key-value pairs.
     * Expected format: key1, value1, key2, value2, ...
     */
    private String buildStructuredMessage(String message, String... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0) {
            return message;
        }

        if (keyValuePairs.length % 2 != 0) {
            // If odd number of arguments, log warning and use all as-is
            return message + " | " + String.join(" | ", keyValuePairs);
        }

        StringBuilder sb = new StringBuilder(message);
        sb.append(" | ");

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i > 0) {
                sb.append(" | ");
            }
            String key = keyValuePairs[i];
            String value = i + 1 < keyValuePairs.length ? keyValuePairs[i + 1] : "";
            sb.append(key).append("=").append(value);
        }

        return sb.toString();
    }
}

