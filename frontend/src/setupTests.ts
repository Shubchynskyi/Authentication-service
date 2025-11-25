import '@testing-library/jest-dom';
import { vi, afterAll } from 'vitest';

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

// Common mock for react-i18next with translations
const translations: Record<string, string> = {
  // Home page
  'home.title': 'Welcome',
  'home.subtitle': 'Authentication Service',
  'home.links.login': 'Login',
  'home.links.register': 'Register',
  'home.links.verify': 'Verify Email',
  'home.links.profile': 'Profile',
  'home.links.editProfile': 'Edit Profile',
  'home.links.adminPanel': 'Admin Panel',
  // Auth
  'auth.loginTitle': 'Login',
  'auth.registerTitle': 'Register',
  'common.email': 'Email Address',
  'common.password': 'Password',
  'common.login': 'Sign In',
  'common.username': 'Username',
  // Errors
  'errors.emailRequired': 'Email is required',
  'errors.passwordRequired': 'Password is required',
  'errors.usernameRequired': 'Username is required',
  // Not found
  'notFound.title': 'Page Not Found',
  'notFound.description': 'The page you are looking for does not exist.',
  'notFound.backHome': 'Back to Home',
  // Common
  'common.language': 'Language',
  // Registration
  'auth.registerSuccess': 'Registration successful',
  'auth.verificationTitle': 'Verify Email',
};

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => translations[key] || key,
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
  // Remove all storage event listeners that might prevent exit
  // This is needed because api.ts adds a storage listener that never gets removed
  const storageEvent = new StorageEvent('storage', { key: null, newValue: null });
  window.dispatchEvent(storageEvent);
  
  // Force cleanup of any remaining timers
  vi.clearAllTimers();
  vi.clearAllMocks();
});

