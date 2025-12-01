import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import LoginPage from './LoginPage';

// Mock api module to prevent interceptor errors
const mockApiInstance = {
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
};
vi.mock('../api', () => ({
    default: mockApiInstance,
}));

// Mock AuthContext
const mockLogin = vi.fn();
const mockLogout = vi.fn();
const mockSetTokens = vi.fn();
vi.mock('../context/AuthContext', () => ({
    AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useAuth: () => ({
        isAuthenticated: false,
        login: mockLogin,
        logout: mockLogout,
        error: null,
        isLoading: false,
        setTokens: mockSetTokens,
    }),
}));

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

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
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
        mockLogin.mockClear();
        mockNavigate.mockClear();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockLogin.mockClear();
        mockNavigate.mockClear();
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
        mockLogin.mockResolvedValueOnce(undefined);

        renderLoginPage();

        const emailInput = screen.getByLabelText(/Email Address/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
        fireEvent.change(passwordInput, { target: { value: 'password123' } });

        const submitButton = screen.getByRole('button', { name: /Sign In/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
            expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
        });
    });
});
