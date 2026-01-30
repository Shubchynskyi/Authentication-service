import { describe, it, expect, vi, beforeAll } from 'vitest';
import { TFunction } from 'i18next';

vi.mock('../config', () => ({
  PASSWORD_REGEX: new RegExp(
    '^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!\\-_*?])(?=\\S+$).{8,}$'
  ),
}));

let validatePassword: (password: string, t: TFunction) => string;

// Mock translation function
const mockT = vi.fn((key: string) => {
  const translations: Record<string, string> = {
    'errors.passwordTooShort': 'Password is too short',
    'errors.passwordNoDigit': 'Password must contain at least one digit',
    'errors.passwordNoUppercase': 'Password must contain at least one uppercase letter',
    'errors.passwordNoLowercase': 'Password must contain at least one lowercase letter',
    'errors.passwordNoSpecial': 'Password must contain at least one special character',
    'errors.passwordNoSpaces': 'Password cannot contain spaces',
    'errors.passwordRequirements': 'Password does not meet requirements',
  };
  return translations[key] || key;
}) as unknown as TFunction;

describe('passwordValidation', () => {
  beforeAll(async () => {
    ({ validatePassword } = await import('./passwordValidation'));
  });

  describe('validatePassword', () => {
    it('returns empty string for valid password', () => {
      const result = validatePassword('ValidPass123!', mockT);
      expect(result).toBe('');
    });

    it('returns error for password that is too short', () => {
      const result = validatePassword('Short1!', mockT);
      expect(result).toBe('Password is too short');
    });

    it('returns error for password without digits', () => {
      const result = validatePassword('NoDigitPass!', mockT);
      expect(result).toBe('Password must contain at least one digit');
    });

    it('returns error for password without uppercase letters', () => {
      const result = validatePassword('nouppercase123!', mockT);
      expect(result).toBe('Password must contain at least one uppercase letter');
    });

    it('returns error for password without lowercase letters', () => {
      const result = validatePassword('NOLOWERCASE123!', mockT);
      expect(result).toBe('Password must contain at least one lowercase letter');
    });

    it('returns error for password without special characters', () => {
      const result = validatePassword('NoSpecial123', mockT);
      expect(result).toBe('Password must contain at least one special character');
    });

    it('returns error for password with spaces', () => {
      const result = validatePassword('Has Space123!', mockT);
      expect(result).toBe('Password cannot contain spaces');
    });

    it('returns error for empty password', () => {
      const result = validatePassword('', mockT);
      expect(result).toBe('Password is too short');
    });

    it('validates password with all required special characters', () => {
      const specialChars = ['@', '#', '$', '%', '^', '&', '+', '=', '!', '-', '_', '*', '?'];
      specialChars.forEach(char => {
        const password = `TestPass123${char}`;
        const result = validatePassword(password, mockT);
        expect(result).toBe('');
      });
    });

    it('returns generic error for password that fails multiple requirements', () => {
      // Password that fails multiple checks - should return first failing check
      const result = validatePassword('abc', mockT);
      expect(result).toBe('Password is too short');
    });
  });
});

