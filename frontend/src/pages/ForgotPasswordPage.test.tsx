import React from 'react';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import ForgotPasswordPage from './ForgotPasswordPage';
import axios from 'axios';
import { TestBrowserRouter } from '../test-utils/router';

// Mock axios
vi.mock('axios');
const mockedAxios = axios as any;

// Mock NotificationContext
const mockShowNotification = vi.fn();
const mockRemoveNotification = vi.fn();
vi.mock('../context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useNotification: () => ({
        showNotification: mockShowNotification,
        removeNotification: mockRemoveNotification,
        notifications: [],
    }),
}));

const renderForgotPasswordPage = () => {
    return render(
        <TestBrowserRouter>
            <ForgotPasswordPage />
        </TestBrowserRouter>
    );
};

describe('ForgotPasswordPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        localStorage.clear();
        cleanup();
    });

    it('renders forgot password form', () => {
        renderForgotPasswordPage();

        expect(screen.getByText(/Forgot Password/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Reset Password/i })).toBeInTheDocument();
    });

    it('submits form with valid email', async () => {
        mockedAxios.post.mockResolvedValueOnce({ data: 'Success' });

        renderForgotPasswordPage();

        const emailInput = screen.getByLabelText(/Email/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Reset Password/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockedAxios.post).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/forgot-password'),
                { email: 'test@example.com' }
            );
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.any(String),
                'success'
            );
        }, { timeout: 5000 });

        expect(submitButton).toBeDisabled();
        expect(screen.getByText(/request another link/i)).toBeInTheDocument();
    });

    it('handles API error', async () => {
        mockedAxios.post.mockRejectedValueOnce(new Error('Network error'));

        renderForgotPasswordPage();

        const emailInput = screen.getByLabelText(/Email/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Reset Password/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.any(String),
                'error'
            );
        }, { timeout: 5000 });
    });

    it('shows loading state during submission', async () => {
        mockedAxios.post.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

        renderForgotPasswordPage();

        const emailInput = screen.getByLabelText(/Email/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Reset Password/i });
        fireEvent.click(submitButton);

        // Button should be disabled during loading
        expect(submitButton).toBeDisabled();
    });
});

