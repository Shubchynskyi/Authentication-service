import '@testing-library/jest-dom';
import { vi, afterAll, beforeAll } from 'vitest';
import { testTranslations, getTranslation } from './test-utils/translations';

beforeAll(() => {
  window.alert = vi.fn();
  // Suppress console.error in tests to reduce noise
  const originalError = console.error;
  console.error = vi.fn((...args: any[]) => {
    // Only suppress known test-related errors
    const message = args[0]?.toString() || '';
    if (
      message.includes('Registration error:') ||
      message.includes('Error fetching') ||
      message.includes('Invalid JWT token format') ||
      message.includes('Failed to decode tokens')
    ) {
      return; // Suppress these expected errors in tests
    }
    originalError(...args);
  });
});

// Mock i18next-http-backend to prevent async loading in tests
vi.mock('i18next-http-backend', () => ({
  default: vi.fn(),
}));

// Mock i18next-browser-languagedetector
vi.mock('i18next-browser-languagedetector', () => ({
  default: vi.fn(),
}));

// Mock i18n initialization to prevent real initialization in tests
vi.mock('./i18n/i18n', () => ({
  availableLanguages: {
    ru: 'Русский',
    en: 'English',
    uk: 'Українська',
    de: 'Deutsch',
  },
  default: {
    language: 'en',
    changeLanguage: vi.fn(),
    on: vi.fn(),
    use: vi.fn().mockReturnThis(),
    init: vi.fn(),
  },
}));

// Common mock for react-i18next with translations from centralized file
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => getTranslation(key, testTranslations),
    i18n: {
      language: 'en',
      changeLanguage: vi.fn(),
      resolvedLanguage: 'en',
    },
  }),
  initReactI18next: {
    type: '3rdParty',
    init: vi.fn(),
  },
}));

// Cleanup after all tests to ensure process exits
afterAll(() => {
  // Force cleanup of any remaining timers
  vi.clearAllTimers();
  vi.clearAllMocks();
});

