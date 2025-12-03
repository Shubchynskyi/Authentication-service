import { TFunction } from 'i18next';
import { PASSWORD_REGEX } from '../config';

/**
 * Validates password against configured pattern.
 * Provides detailed feedback about what requirements are not met.
 * 
 * @param password - password to validate
 * @param t - translation function for error messages
 * @returns empty string if password is valid, error message otherwise
 */
export const validatePassword = (password: string, t: TFunction): string => {
  if (!PASSWORD_REGEX.test(password)) {
    // Provide detailed feedback
    if (password.length < 8) {
      return t('errors.passwordTooShort');
    }
    if (!/\d/.test(password)) {
      return t('errors.passwordNoDigit');
    }
    if (!/[A-Z]/.test(password)) {
      return t('errors.passwordNoUppercase');
    }
    if (!/[a-z]/.test(password)) {
      return t('errors.passwordNoLowercase');
    }
    if (!/[@#$%^&+=!\-_*?]/.test(password)) {
      return t('errors.passwordNoSpecial');
    }
    if (/\s/.test(password)) {
      return t('errors.passwordNoSpaces');
    }
    return t('errors.passwordRequirements');
  }
  return '';
};

