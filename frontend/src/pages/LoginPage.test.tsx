import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import LoginPage from './LoginPage';
import { createMockAuthContext, createMockNotificationContext } from '../test-utils/mocks';
import { renderWithRouter, setupTestCleanup } from '../test-utils/test-helpers';

// Create mocks using vi.hoisted() to avoid hoisting issues
const { mockLogin, mockShowNotification, mockNavigate, mockLocation } = vi.hoisted(() => {
    const mockLogin = vi.fn();
    const mockShowNotification = vi.fn();
    const mockNavigate = vi.fn();
    const mockLocation: { pathname: string; search: string; hash: string; state: { error?: string } | null; key: string } = { pathname: '/login', search: '', hash: '', state: null, key: 'test' };
    return { mockLogin, mockShowNotification, mockNavigate, mockLocation };
});

// Mock api module to prevent interceptor errors
vi.mock('../api', () => ({
    default: {
        post: vi.fn(),
        get: vi.fn(),
        defaults: {
            headers: {
                common: {},
            },
        },
        interceptors: {
            request: {
                use: vi.fn(),
            },
            response: {
                use: vi.fn(),
            },
        },
    },
}));

// Mock AuthContext
vi.mock('../context/AuthContext', () => ({
    AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useAuth: () => createMockAuthContext({ login: mockLogin }),
}));

// Mock NotificationContext
vi.mock('../context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useNotification: () => createMockNotificationContext({ showNotification: mockShowNotification }),
}));


// Mock react-router-dom
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        useLocation: () => mockLocation,
    };
});

const renderLoginPage = () => {
    return renderWithRouter(<LoginPage />);
};

describe('LoginPage', () => {
    setupTestCleanup();

    it('renders login form', () => {
        renderLoginPage();

        expect(screen.getByRole('heading', { name: /Login/i })).toBeInTheDocument();
        expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
    });

    it('shows validation error when email is empty', async () => {
        renderLoginPage();

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Email is required', 'error');
        }, { timeout: 5000 });
    });

    it('shows validation error when password is empty', async () => {
        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Password is required', 'error');
        }, { timeout: 5000 });
    });

    it('submits form with valid data', async () => {
        mockLogin.mockResolvedValueOnce(undefined);

        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'Password123@');
            expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
        }, { timeout: 5000 });
    });

    it('handles login error', async () => {
        mockLogin.mockRejectedValueOnce(new Error('Login failed'));

        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Login failed',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('displays OAuth login button', () => {
        renderLoginPage();
        const oauthButton = screen.getByRole('button', { name: /Login with Google/i });
        expect(oauthButton).toBeInTheDocument();
    });

    it('has link to forgot password page', () => {
        renderLoginPage();
        const forgotPasswordLink = screen.getByRole('link', { name: /Forgot Password/i });
        expect(forgotPasswordLink).toHaveAttribute('href', '/forgot-password');
    });

    it('has link to registration page', () => {
        renderLoginPage();
        const registerLink = screen.getByRole('link', { name: /Register/i });
        expect(registerLink).toHaveAttribute('href', '/register');
    });

    it('shows error notification from location state', () => {
        mockLocation.state = { error: 'Test error message' };
        renderLoginPage();

        expect(mockShowNotification).toHaveBeenCalledWith('Test error message', 'error');
        mockLocation.state = null;
    });

    it('handles blacklist error during login', async () => {
        // Create axios error with blacklist message
        const axiosError = {
            isAxiosError: true,
            response: {
                data: 'This email is in blacklist. Login is forbidden.'
            }
        };
        mockLogin.mockRejectedValueOnce(axiosError);

        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'blocked@example.com' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // LoginPage shows generic error message for security (doesn't reveal account existence)
            // The actual error is handled in AuthContext, but LoginPage shows generic message
            // Global mock in setupTests.ts returns translated value 'Login failed' for 'errors.loginFailed'
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Login failed',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('handles whitelist error during login in WHITELIST mode', async () => {
        // Create axios error with whitelist message
        const axiosError = {
            isAxiosError: true,
            response: {
                data: 'This email is not in whitelist. Login is forbidden.'
            }
        };
        mockLogin.mockRejectedValueOnce(axiosError);

        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'notwhitelisted@example.com' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // LoginPage shows generic error message for security (doesn't reveal account existence)
            // The actual error is handled in AuthContext, but LoginPage shows generic message
            // Global mock in setupTests.ts returns translated value 'Login failed' for 'errors.loginFailed'
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Login failed',
                'error'
            );
        }, { timeout: 5000 });
    });
});
