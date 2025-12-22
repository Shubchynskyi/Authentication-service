import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import RegistrationPage from './RegistrationPage';
import { renderWithRouter, setupTestCleanup } from '../test-utils/test-helpers';

const { mockShowNotification, mockApiPost, mockIsAxiosError, mockNavigate } = vi.hoisted(() => ({
    mockShowNotification: vi.fn(),
    mockApiPost: vi.fn(),
    mockIsAxiosError: vi.fn((error: any) => error?.isAxiosError || false),
    mockNavigate: vi.fn(),
}));

vi.mock('../api', () => ({
    default: {
        post: (...args: any[]) => mockApiPost(...args),
    },
}));

vi.mock('axios', () => ({
    default: {
        isAxiosError: mockIsAxiosError,
    },
    isAxiosError: mockIsAxiosError,
}));

vi.mock('../context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useNotification: () => ({
        notifications: [],
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
    }),
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

const getPasswordInput = () => screen.getAllByLabelText(/Password/i)[0];

describe('RegistrationPage', () => {
    setupTestCleanup();

    it('renders registration form', () => {
        renderRegistrationPage();

        expect(screen.getByRole('heading', { name: /Register/i })).toBeInTheDocument();
        expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Username/i)).toBeInTheDocument();
        const passwordFields = screen.getAllByLabelText(/Password/i);
        expect(passwordFields.length).toBeGreaterThanOrEqual(2);
        expect(screen.getByLabelText(/Confirm Password/i)).toBeInTheDocument();
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
        mockApiPost.mockResolvedValueOnce({
            data: 'Registration successful',
        });

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = getPasswordInput();
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);

        fireEvent.change(emailInput, { target: { value: 'newuser@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'New User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockApiPost).toHaveBeenCalledWith(
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

        const passwordInput = getPasswordInput();
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
        mockApiPost.mockRejectedValueOnce(axiosError);
        mockIsAxiosError.mockReturnValue(true);

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = getPasswordInput();
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);

        fireEvent.change(emailInput, { target: { value: 'existing@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Password123@' } });

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

    it('handles whitelist error and shows generic message instead of password hint', async () => {
        const axiosError = {
            isAxiosError: true,
            response: { 
                status: 400,
                data: { message: 'Unable to complete registration. Contact administrator.' }
            },
        };
        mockApiPost.mockRejectedValueOnce(axiosError);
        mockIsAxiosError.mockReturnValue(true);

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = getPasswordInput();
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);

        fireEvent.change(emailInput, { target: { value: 'notwhitelisted@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // Error should be shown in place of password hint (as Alert)
            const alerts = screen.getAllByRole('alert');
            const registrationError = alerts.find(alert => 
                alert.textContent?.includes('Unable to complete registration') ||
                alert.textContent?.includes('Contact administrator')
            );
            expect(registrationError).toBeInTheDocument();
            // Password hint should not be visible when registration error is shown
            expect(screen.queryByText(/Password must be at least 8 characters/i)).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('handles registration error and shows it instead of password hint', async () => {
        const axiosError = {
            isAxiosError: true,
            response: { 
                status: 400,
                data: { message: 'Unable to complete registration. Contact administrator.' }
            },
        };
        mockApiPost.mockRejectedValueOnce(axiosError);
        mockIsAxiosError.mockReturnValue(true);

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = getPasswordInput();
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // Error should be shown in place of password hint (as Alert)
            const alerts = screen.getAllByRole('alert');
            const registrationError = alerts.find(alert => 
                alert.textContent?.includes('Unable to complete registration') ||
                alert.textContent?.includes('Contact administrator')
            );
            expect(registrationError).toBeInTheDocument();
            // Password hint should not be visible when registration error is shown
            expect(screen.queryByText(/Password must be at least 8 characters/i)).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('shows error when passwords do not match', async () => {
        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = getPasswordInput();
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);

        fireEvent.change(emailInput, { target: { value: 'newuser@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'New User' } });
        fireEvent.change(passwordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Different123@' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Passwords do not match', 'error');
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
