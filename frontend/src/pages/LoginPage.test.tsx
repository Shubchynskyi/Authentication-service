import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import LoginPage from './LoginPage';
import axios from 'axios';

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

// Mock window.location
const mockLocation = { href: '' };
Object.defineProperty(window, 'location', {
    writable: true,
    value: mockLocation,
});

const renderLoginPage = () => {
    return render(
        <BrowserRouter>
            <LoginPage />
        </BrowserRouter>
    );
};

describe('LoginPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockLocation.href = '';
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        localStorage.clear();
    });

    it('renders login form', () => {
        renderLoginPage();

        expect(screen.getByText('Login')).toBeInTheDocument();
        expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
    });

    it('shows validation error when email is empty', async () => {
        renderLoginPage();

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Email is required', 'error');
        });
    });

    it('shows validation error when password is empty', async () => {
        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email Address/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Password is required', 'error');
        });
    });

    it('submits form with valid data', async () => {
        mockedAxios.post.mockResolvedValueOnce({
            data: {
                accessToken: 'fake-access-token',
                refreshToken: 'fake-refresh-token',
            },
        });

        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email Address/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(passwordInput, { target: { value: 'password123' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockedAxios.post).toHaveBeenCalledWith(
                expect.stringContaining('/auth/login'),
                {
                    email: 'test@example.com',
                    password: 'password123',
                }
            );
        });
    });
});
