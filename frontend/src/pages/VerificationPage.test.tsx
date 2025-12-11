import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import VerificationPage from './VerificationPage';
import { TestMemoryRouter } from '../test-utils/router';
import { setupTestCleanup } from '../test-utils/test-helpers';

const mockShowNotification = vi.fn();
const mockApiPost = vi.fn();

vi.mock('axios', () => ({
    default: {
        post: (...args: any[]) => mockApiPost(...args),
        create: () => ({
            post: (...args: any[]) => mockApiPost(...args),
            get: vi.fn(),
            defaults: { headers: { common: {} } },
            interceptors: {
                request: { use: vi.fn() },
                response: { use: vi.fn() },
            },
        }),
        isAxiosError: () => false,
    },
    isAxiosError: () => false,
}));

vi.mock('../context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useNotification: () => ({
        notifications: [],
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
    }),
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

const renderVerificationPage = (initialState?: { email?: string }) => {
    return render(
        <TestMemoryRouter
            initialEntries={[
                {
                    pathname: '/verify',
                    state: initialState,
                },
            ]}
        >
            <VerificationPage />
        </TestMemoryRouter>
    );
};

describe('VerificationPage', () => {
    setupTestCleanup();

    beforeEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
        localStorage.clear();
        mockApiPost.mockClear();
    });

    afterEach(() => {
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
    });

    it('renders verification form', () => {
        renderVerificationPage();

        expect(screen.getByRole('heading', { name: /auth.verificationTitle/i })).toBeInTheDocument();
        expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/auth.verificationCode/i)).toBeInTheDocument();
    });

    it('pre-fills email from location state', () => {
        renderVerificationPage({ email: 'test@example.com' });

        const emailInput = screen.getByLabelText(/Email/i) as HTMLInputElement;
        expect(emailInput.value).toBe('test@example.com');
    });

    it('submits verification with valid data', async () => {
        mockApiPost.mockResolvedValueOnce({
            data: 'Verification successful',
        });

        renderVerificationPage({ email: 'test@example.com' });

        const codeInput = screen.getByLabelText(/auth.verificationCode/i);
        fireEvent.change(codeInput, { target: { value: '123456' } });

        const submitButton = screen.getByRole('button', { name: /auth.verify/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockApiPost).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/verify'),
                {
                    email: 'test@example.com',
                    code: '123456',
                }
            );
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Verification successful',
                'success'
            );
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        }, { timeout: 5000 });
    });

    it('handles verification error', async () => {
        mockApiPost.mockRejectedValueOnce({
            response: { data: 'Invalid verification code' },
        });

        renderVerificationPage({ email: 'test@example.com' });

        const emailInput = screen.getByLabelText(/Email/i);
        const codeInput = screen.getByLabelText(/auth.verificationCode/i);
        
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(codeInput, { target: { value: 'wrong' } });

        const submitButton = screen.getByRole('button', { name: /auth.verify/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // Error message is translated or comes from response.data
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringMatching(/Invalid verification code|auth.verification.error/),
                'error'
            );
        }, { timeout: 5000 });
    });

    it('resends verification code', async () => {
        mockApiPost.mockResolvedValueOnce({
            data: 'Verification code resent',
        });

        renderVerificationPage({ email: 'test@example.com' });

        const resendButton = screen.getByRole('button', { name: /auth.resendCode/i });
        fireEvent.click(resendButton);

        await waitFor(() => {
            expect(mockApiPost).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/resend-verification'),
                {
                    email: 'test@example.com',
                }
            );
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Verification code resent',
                'success'
            );
        }, { timeout: 5000 });
    });

    it('handles resend verification error', async () => {
        mockApiPost.mockRejectedValueOnce({
            response: { data: 'Resend failed' },
        });

        renderVerificationPage({ email: 'test@example.com' });

        const resendButton = screen.getByRole('button', { name: /auth.resendCode/i });
        fireEvent.click(resendButton);

        await waitFor(() => {
            // Error message is translated or comes from response.data
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringMatching(/Resend failed|auth.verification.resendError/),
                'error'
            );
        }, { timeout: 5000 });
    });

    it('disables resend during cooldown when backend returns 429', async () => {
        mockApiPost.mockRejectedValueOnce({
            response: {
                status: 429,
                data: { message: 'Too many resend attempts', retryAfterSeconds: 30 },
                headers: { 'retry-after': '30' },
            },
        });

        renderVerificationPage({ email: 'test@example.com' });

        const resendButton = screen.getByRole('button', { name: /auth.resendCode/i });
        fireEvent.click(resendButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('Too many resend attempts'),
                'error'
            );
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(resendButton).toBeDisabled();
            expect(resendButton.textContent).toContain('30');
        }, { timeout: 5000 });
    });

    it('shows loading state during verification', async () => {
        mockApiPost.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

        renderVerificationPage({ email: 'test@example.com' });

        const codeInput = screen.getByLabelText(/auth.verificationCode/i);
        const submitButton = screen.getByRole('button', { name: /auth.verify/i });

        fireEvent.change(codeInput, { target: { value: '123456' } });
        fireEvent.click(submitButton);

        // Button should be disabled during loading
        expect(submitButton).toBeDisabled();
    });

    it('has link to home page', () => {
        renderVerificationPage();

        const homeLink = screen.getByRole('link', { name: /notFound.backHome/i });
        expect(homeLink).toHaveAttribute('href', '/');
    });
});

