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
      message.includes('Failed to decode tokens') ||
      message.includes('Warning: The current testing environment is not configured to support act(...)')
    ) {
      return; // Suppress these expected errors in tests
    }
    originalError(...args);
  });
  
  // Suppress act warnings from React Testing Library
  const originalWarn = console.warn;
  console.warn = vi.fn((...args: any[]) => {
    const message = args[0]?.toString() || '';
    if (
      message.includes('Warning: The current testing environment is not configured to support act(...)') ||
      message.includes('act(...) is not supported')
    ) {
      return; // Suppress act warnings
    }
    originalWarn(...args);
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

// Mock masked login service to avoid network calls and spinner in tests
vi.mock('./services/maskedLoginService', () => ({
  getMaskedLoginSettingsPublic: vi.fn().mockResolvedValue({ enabled: false, templateId: 1 }),
  getMaskedLoginSettingsAdmin: vi.fn().mockResolvedValue({
    enabled: false,
    templateId: 1,
    updatedAt: '2025-01-01T00:00:00Z',
    updatedBy: 'admin@example.com',
  }),
  getTemplate: vi.fn().mockResolvedValue('<div></div>'),
  updateMaskedLoginSettings: vi.fn(),
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

