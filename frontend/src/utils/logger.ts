/**
 * Simple structured logger for frontend critical errors.
 * Only logs errors that are important for debugging production issues.
 */

interface LogContext {
  [key: string]: string | number | boolean | null | undefined;
}

/**
 * Logs critical errors with structured context
 */
export const logError = (message: string, error?: unknown, context?: LogContext): void => {
  const logData: Record<string, unknown> = {
    level: 'error',
    message,
    timestamp: new Date().toISOString(),
    ...context,
  };

  if (error instanceof Error) {
    logData.error = {
      name: error.name,
      message: error.message,
      stack: error.stack,
    };
  } else if (error) {
    logData.error = error;
  }

  // Log to console in development, could be extended to send to server in production
  if (import.meta.env.DEV) {
    console.error('[ERROR]', logData);
  } else {
    // In production, only log critical errors
    if (error) {
      console.error(`[ERROR] ${message}`, error, context);
    } else {
      console.error(`[ERROR] ${message}`, context);
    }
  }
};

/**
 * Logs warnings for important events
 */
export const logWarn = (message: string, context?: LogContext): void => {
  if (import.meta.env.DEV) {
    console.warn('[WARN]', { message, timestamp: new Date().toISOString(), ...context });
  }
};

/**
 * Logs info messages (only in development)
 */
export const logInfo = (message: string, context?: LogContext): void => {
  if (import.meta.env.DEV) {
    console.log('[INFO]', { message, timestamp: new Date().toISOString(), ...context });
  }
};

