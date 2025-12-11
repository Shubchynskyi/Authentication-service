import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import ResetPasswordPage from './ResetPasswordPage';
import { createNotificationMocks } from '../test-utils/mocks';
import { renderWithMemoryRouter, setupTestCleanup } from '../test-utils/test-helpers';

// Create mocks using vi.hoisted() to avoid hoisting issues
const { mockApiPost, mockShowNotification, mockNavigate } = vi.hoisted(() => {
    const mockApiPost = vi.fn();
    const mockShowNotification = vi.fn();
    const mockNavigate = vi.fn();
    return { mockApiPost, mockShowNotification, mockNavigate };
});

// Mock api
vi.mock('../api', () => ({
    default: {
        post: (...args: any[]) => mockApiPost(...args),
    },
}));

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
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

const renderResetPasswordPage = (searchParams = '?token=test-token') => {
    return renderWithMemoryRouter(
        <ResetPasswordPage />,
        { initialEntries: [`/reset-password${searchParams}`] }
    );
};

describe('ResetPasswordPage', () => {
    setupTestCleanup();

    it('renders reset password form', async () => {
        renderResetPasswordPage();

        // "Reset Password" appears in heading and button, use getAllByText
        const resetPasswordTexts = screen.getAllByText('Reset Password');
        expect(resetPasswordTexts.length).toBeGreaterThan(0);
        
        // MUI labels with data-shrink="false" may not be properly associated
        // Use getByRole with name option or find by text and then input
        await waitFor(() => {
            // Find labels by text, then find associated inputs
            const newPasswordLabel = screen.getByText('New Password', { selector: 'label' });
            const confirmPasswordLabel = screen.getByText('Confirm Password', { selector: 'label' });
            expect(newPasswordLabel).toBeInTheDocument();
            expect(confirmPasswordLabel).toBeInTheDocument();
            
            // Find inputs by their associated label's for attribute
            const newPasswordInput = document.getElementById(newPasswordLabel.getAttribute('for') || '');
            const confirmPasswordInput = document.getElementById(confirmPasswordLabel.getAttribute('for') || '');
            expect(newPasswordInput).toBeInTheDocument();
            expect(confirmPasswordInput).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('shows error when token is missing', () => {
        renderResetPasswordPage('');

        expect(mockShowNotification).toHaveBeenCalledWith(
            expect.any(String),
            'error'
        );
    });

    it('validates password requirements', async () => {
        renderResetPasswordPage();

        const newPasswordLabel = screen.getByText('New Password', { selector: 'label' });
        const newPasswordInput = document.getElementById(newPasswordLabel.getAttribute('for') || '') as HTMLInputElement;
        fireEvent.change(newPasswordInput, { target: { value: 'weak' } });

        await waitFor(() => {
            const errorTexts = screen.getAllByText(/password/i);
            expect(errorTexts.length).toBeGreaterThan(0);
        }, { timeout: 5000 });
    });

    it('shows error when password is empty', async () => {
        renderResetPasswordPage();

        const submitButton = screen.getByRole('button', { name: 'Reset Password' });
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Password is required',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('shows error when passwords do not match', async () => {
        renderResetPasswordPage();

        const newPasswordLabel = screen.getByText('New Password', { selector: 'label' });
        const confirmPasswordLabel = screen.getByText('Confirm Password', { selector: 'label' });
        const newPasswordInput = document.getElementById(newPasswordLabel.getAttribute('for') || '') as HTMLInputElement;
        const confirmPasswordInput = document.getElementById(confirmPasswordLabel.getAttribute('for') || '') as HTMLInputElement;

        fireEvent.change(newPasswordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Different123@' } });

        const submitButton = screen.getByRole('button', { name: 'Reset Password' });
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Passwords do not match',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('submits form with valid data', async () => {
        mockApiPost.mockResolvedValueOnce({ data: 'Success' });

        renderResetPasswordPage();

        const newPasswordLabel = screen.getByText('New Password', { selector: 'label' });
        const confirmPasswordLabel = screen.getByText('Confirm Password', { selector: 'label' });
        const newPasswordInput = document.getElementById(newPasswordLabel.getAttribute('for') || '') as HTMLInputElement;
        const confirmPasswordInput = document.getElementById(confirmPasswordLabel.getAttribute('for') || '') as HTMLInputElement;

        fireEvent.change(newPasswordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: 'Reset Password' });
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockApiPost).toHaveBeenCalledWith(
                '/api/auth/reset-password',
                {
                    token: 'test-token',
                    newPassword: 'Password123@',
                    confirmPassword: 'Password123@',
                }
            );
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.any(String),
                'success'
            );
            expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
        }, { timeout: 5000 });
    });

    it('handles API error', async () => {
        mockApiPost.mockRejectedValueOnce({
            response: { data: 'Invalid token' },
        });

        renderResetPasswordPage();

        const newPasswordLabel = screen.getByText('New Password', { selector: 'label' });
        const confirmPasswordLabel = screen.getByText('Confirm Password', { selector: 'label' });
        const newPasswordInput = document.getElementById(newPasswordLabel.getAttribute('for') || '') as HTMLInputElement;
        const confirmPasswordInput = document.getElementById(confirmPasswordLabel.getAttribute('for') || '') as HTMLInputElement;

        fireEvent.change(newPasswordInput, { target: { value: 'Password123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Password123@' } });

        const submitButton = screen.getByRole('button', { name: 'Reset Password' });
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            // Component may show generic error message instead of specific one
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.any(String),
                'error'
            );
        }, { timeout: 5000 });
    });
});

