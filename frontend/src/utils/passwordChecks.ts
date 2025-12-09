import { TFunction } from 'i18next';
import { validatePassword } from './passwordValidation';

export interface PasswordCheckParams {
  password: string;
  confirmPassword?: string;
  currentPassword?: string;
  t: TFunction;
  requirePassword?: boolean;
  requireConfirm?: boolean;
  requireCurrent?: boolean;
}

/**
 * Runs common password validations (required, strength, confirm, current).
 * Returns empty string when validation passes; otherwise returns translated error text.
 */
export const validatePasswordFlow = ({
  password,
  confirmPassword,
  currentPassword,
  t,
  requirePassword = false,
  requireConfirm = false,
  requireCurrent = false,
}: PasswordCheckParams): string => {
  if (requirePassword && !password) {
    return t('errors.passwordRequired');
  }

  if (requireCurrent && password && !currentPassword) {
    return t('profile.notifications.currentPasswordRequired');
  }

  if (password) {
    const passwordError = validatePassword(password, t);
    if (passwordError) {
      return passwordError;
    }
  }

  if (password && requireConfirm && !confirmPassword) {
    return t('errors.confirmPasswordRequired');
  }

  if (password && confirmPassword !== undefined && password !== confirmPassword) {
    return t('errors.passwordMismatch');
  }

  return '';
};

