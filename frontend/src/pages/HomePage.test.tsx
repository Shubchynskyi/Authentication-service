import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { vi, afterEach, beforeEach, describe, it, expect } from 'vitest';
import HomePage from './HomePage';
import { TestBrowserRouter } from '../test-utils/router';
import React from 'react';

// Mock i18n with hoisted translations
const { mockTranslations, mockI18n } = vi.hoisted(() => {
    const mockTranslations: Record<string, string> = {
        'home.title': 'Welcome',
        'home.subtitle': 'Authentication Service',
        'home.links.login': 'Login',
        'home.links.register': 'Register',
        'home.links.verify': 'Verify Email',
        'home.links.profile': 'Profile',
        'home.links.editProfile': 'Edit Profile',
        'home.links.adminPanel': 'Admin Panel',
    };
    const mockI18n = {
        language: 'en',
        changeLanguage: vi.fn(),
        resolvedLanguage: 'en',
    };
    return { mockTranslations, mockI18n };
});

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => mockTranslations[key] || key,
        i18n: mockI18n,
    }),
    initReactI18next: {
        type: '3rdParty',
        init: vi.fn(),
    },
}));

// Mock AuthContext to avoid provider errors and skip real auth logic
vi.mock('../context/AuthContext', () => ({
    AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useAuth: () => ({
        isAuthenticated: false,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
        setTokens: vi.fn(),
        error: null,
    }),
}));

describe('HomePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
        cleanup();
    });

    it('renders correctly', async () => {
        render(
            <TestBrowserRouter>
                <HomePage />
            </TestBrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Welcome')).toBeInTheDocument();
        }, { timeout: 5000 });
        await waitFor(() => {
            expect(screen.getByText('Authentication Service')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('displays all navigation links', async () => {
        render(
            <TestBrowserRouter>
                <HomePage />
            </TestBrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByRole('link', { name: /Login/i })).toBeInTheDocument();
        }, { timeout: 5000 });
        await waitFor(() => {
            expect(screen.getByRole('link', { name: /Register/i })).toBeInTheDocument();
        });
        await waitFor(() => {
            expect(screen.getByRole('link', { name: /Verify Email/i })).toBeInTheDocument();
        });
        // Profile/admin links are shown only when authenticated.
    });

    it('has correct links to routes', async () => {
        render(
            <TestBrowserRouter>
                <HomePage />
            </TestBrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByRole('link', { name: /Login/i })).toHaveAttribute('href', '/login');
        }, { timeout: 5000 });
        await waitFor(() => {
            expect(screen.getByRole('link', { name: /Register/i })).toHaveAttribute('href', '/register');
        });
        await waitFor(() => {
            expect(screen.getByRole('link', { name: /Verify Email/i })).toHaveAttribute('href', '/verify');
        });
        // Profile/admin links are shown only when authenticated.
    });
});
