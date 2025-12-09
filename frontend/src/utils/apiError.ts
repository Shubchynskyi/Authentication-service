import axios from 'axios';

interface ExtractErrorMessageOptions {
  fallbackMessage: string;
  transform?: (message: string) => string;
}

/**
 * Extracts human-friendly message from axios/non-axios errors.
 * Returns fallback when nothing suitable found.
 */
export const extractErrorMessage = (
  error: unknown,
  { fallbackMessage, transform }: ExtractErrorMessageOptions
): string => {
  let message = fallbackMessage;

  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data) {
      if (typeof data === 'string') {
        message = data;
      } else if (typeof data === 'object' && 'message' in data && typeof (data as any).message === 'string') {
        message = String((data as any).message);
      }
    } else if (error.message) {
      message = error.message;
    }
  } else if (error instanceof Error && error.message) {
    message = error.message;
  }

  return transform ? transform(message) : message;
};

