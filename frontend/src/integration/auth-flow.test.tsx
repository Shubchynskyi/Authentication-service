import React from 'react';
import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import { Routes, Route } from 'react-router-dom';
import { TestBrowserRouter, TestMemoryRouter } from '../test-utils/router';

// Mock token utils BEFORE any imports that use it
const mockIsJwtExpired = vi.fn();
vi.mock('../utils/token', () => ({
    isJwtExpired: mockIsJwtExpired,
    isValidJwtFormat: vi.fn(() => true),
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    clearTokens: vi.fn(),
}));

// Mock axios
vi.mock('axios');

// Mock api
const mockApiGet = vi.fn();
const mockApiPost = vi.fn();
vi.mock('../api', () => ({
    default: {
        get: mockApiGet,
        post: mockApiPost,
    },
}));

// Mock contexts
const mockLogin = vi.fn();
const mockLogout = vi.fn();
const mockSetTokens = vi.fn();
const mockShowNotification = vi.fn();

vi.mock('../context/AuthContext', () => ({
    AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useAuth: () => ({
        isAuthenticated: false,
        login: mockLogin,
        logout: mockLogout,
        setTokens: mockSetTokens,
        error: null,
        isLoading: false,
    }),
}));

vi.mock('../context/ProfileContext', () => ({
    ProfileProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useProfile: () => ({
        profile: null,
        isLoading: false,
        isAdmin: false,
        updateProfile: vi.fn(),
    }),
}));

vi.mock('../context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useNotification: () => ({
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
        notifications: [],
    }),
}));

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
    useNotification: () => ({
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
        notifications: [],
    }),
}));

// Mock routes
vi.mock('../pages/LoginPage', () => ({
    default: () => <div>Login Page</div>,
}));

vi.mock('../pages/RegistrationPage', () => ({
    default: () => <div>Registration Page</div>,
}));

vi.mock('../pages/ProfilePage', () => ({
    default: () => <div>Profile Page</div>,
}));

describe('Authentication Flow Integration', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        mockLogin.mockClear();
        mockShowNotification.mockClear();
        mockIsJwtExpired.mockReset();
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        cleanup();
    });

    it('allows user to navigate from home to login', async () => {
        const { default: HomePage } = await import('../pages/HomePage');
        
        render(
            <TestBrowserRouter>
                <HomePage />
            </TestBrowserRouter>
        );

        const loginLink = screen.getByRole('link', { name: /Login/i });
        expect(loginLink).toHaveAttribute('href', '/login');
    });

    it('allows user to navigate from home to registration', async () => {
        const { default: HomePage } = await import('../pages/HomePage');
        
        render(
            <TestBrowserRouter>
                <HomePage />
            </TestBrowserRouter>
        );

        const registerLink = screen.getByRole('link', { name: /Register/i });
        expect(registerLink).toHaveAttribute('href', '/register');
    });

    it('redirects authenticated user from login to profile', async () => {
        localStorage.setItem('accessToken', 'valid-token');
        mockIsJwtExpired.mockReturnValue(false);
        
        const { default: PublicRoute } = await import('../components/routes/PublicRoute');
        const { default: LoginPage } = await import('../pages/LoginPage');
        const MockProfilePage = () => <div>Profile Page</div>;

        render(
            <TestMemoryRouter initialEntries={['/login']}>
                <Routes>
                    <Route
                        path="/login"
                        element={
                            <PublicRoute>
                                <LoginPage />
                            </PublicRoute>
                        }
                    />
                    <Route path="/profile" element={<MockProfilePage />} />
                </Routes>
            </TestMemoryRouter>
        );

        // Wait for redirect to complete - Navigate component should render
        // After redirect, Login Page should not be visible and Profile Page should be
        await waitFor(() => {
            expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
            expect(screen.getByText('Profile Page')).toBeInTheDocument();
        }, { timeout: 5000 });
        
        // Reset the mock
        mockIsJwtExpired.mockReset();
    });

    it('redirects unauthenticated user from profile to login', async () => {
        localStorage.removeItem('accessToken');
        mockIsJwtExpired.mockReturnValue(true);
        
        const { default: PrivateRoute } = await import('../components/routes/PrivateRoute');
        const { default: ProfilePage } = await import('../pages/ProfilePage');
        const MockLoginPage = () => <div>Login Page</div>;

        render(
            <TestMemoryRouter initialEntries={['/profile']}>
                <Routes>
                    <Route
                        path="/profile"
                        element={
                            <PrivateRoute>
                                <ProfilePage />
                            </PrivateRoute>
                        }
                    />
                    <Route path="/login" element={<MockLoginPage />} />
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            // Should redirect, so Profile Page should not be visible and Login Page should be
            expect(screen.queryByText('Profile Page')).not.toBeInTheDocument();
            expect(screen.getByText('Login Page')).toBeInTheDocument();
        }, { timeout: 5000 });
        
        // Reset the mock
        mockIsJwtExpired.mockReset();
    });
});

