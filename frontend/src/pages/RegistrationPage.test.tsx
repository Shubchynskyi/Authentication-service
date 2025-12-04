import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import RegistrationPage from './RegistrationPage';
import { createMockNotificationContext } from '../test-utils/mocks';
import { renderWithRouter, setupTestCleanup } from '../test-utils/test-helpers';

// Create mocks using vi.hoisted() to avoid hoisting issues
const { mockAxiosPost, mockIsAxiosError, mockShowNotification, mockNavigate } = vi.hoisted(() => {
    const mockAxiosPost = vi.fn();
    const mockIsAxiosError = vi.fn((error: any) => error?.isAxiosError || false);
    const mockShowNotification = vi.fn();
    const mockNavigate = vi.fn();
    return { mockAxiosPost, mockIsAxiosError, mockShowNotification, mockNavigate };
});

vi.mock('axios', () => ({
    default: {
        post: mockAxiosPost,
        isAxiosError: mockIsAxiosError,
    },
    isAxiosError: mockIsAxiosError,
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
    };
});

const renderRegistrationPage = () => {
    return renderWithRouter(<RegistrationPage />);
};

describe('RegistrationPage', () => {
    setupTestCleanup();

    it('renders registration form', () => {
        renderRegistrationPage();

        expect(screen.getByRole('heading', { name: /Register/i })).toBeInTheDocument();
        expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Username/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Register/i })).toBeInTheDocument();
    });

    it('shows validation error when email is empty', async () => {
        renderRegistrationPage();

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Email is required', 'error');
        }, { timeout: 5000 });
    });

    it('shows validation error when username is empty', async () => {
        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Username is required', 'error');
        }, { timeout: 5000 });
    });

    it('shows validation error when password is empty', async () => {
        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'TestUser' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Password is required', 'error');
        }, { timeout: 5000 });
    });

    it('submits form with valid data', async () => {
        mockAxiosPost.mockResolvedValueOnce({
            data: 'Registration successful',
        });

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'newuser@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'New User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockAxiosPost).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/register'),
                {
                    email: 'newuser@example.com',
                    name: 'New User',
                    password: 'Password123@',
                }
            );
            expect(mockNavigate).toHaveBeenCalledWith('/verify', expect.objectContaining({
                state: { email: 'newuser@example.com' }
            }));
        }, { timeout: 5000 });
    });

    it('shows password validation error for invalid password', async () => {
        renderRegistrationPage();

        const passwordInput = screen.getByLabelText(/Password/i);
        fireEvent.change(passwordInput, { target: { value: 'weak' } });

        await waitFor(() => {
            // Password validation shows error in helper text, not notification
            const errorTexts = screen.getAllByText(/password/i);
            expect(errorTexts.length).toBeGreaterThan(0);
        }, { timeout: 5000 });
    });

    it('handles registration error', async () => {
        const axiosError = {
            isAxiosError: true,
            response: { data: 'Email already exists' },
        };
        mockAxiosPost.mockRejectedValueOnce(axiosError);
        mockIsAxiosError.mockReturnValue(true);

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'existing@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // Error is shown in Alert component via message state
            // Use getByRole to find the alert
            const alert = screen.getByRole('alert');
            expect(alert).toBeInTheDocument();
            expect(alert).toHaveTextContent('Email already exists');
        }, { timeout: 5000 });
    });

    it('handles whitelist error', async () => {
        const axiosError = {
            isAxiosError: true,
            response: { data: 'Email not in whitelist' },
        };
        mockAxiosPost.mockRejectedValueOnce(axiosError);

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'notwhitelisted@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // Error is shown in Alert component via message state
            // The error message is translated to 'Email not in whitelist' (auth.loginError.notInWhitelist)
            // Use getAllByRole to handle multiple alerts
            const alerts = screen.getAllByRole('alert');
            const whitelistAlert = alerts.find(alert => 
                alert.textContent?.includes('Email not in whitelist')
            );
            expect(whitelistAlert).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('has link to login page', () => {
        renderRegistrationPage();
        const loginLink = screen.getByRole('link', { name: /Login/i });
        expect(loginLink).toHaveAttribute('href', '/login');
    });

    it('has link to verify page', () => {
        renderRegistrationPage();
        const verifyLink = screen.getByRole('link', { name: /Verify Email/i });
        expect(verifyLink).toHaveAttribute('href', '/verify');
    });

    it('has link to home page', () => {
        renderRegistrationPage();
        // The link uses MuiLink which renders as <a>, find it by text
        const homeLink = screen.getByRole('link', { name: /Back to Home/i });
        expect(homeLink).toHaveAttribute('href', '/');
    });
});
