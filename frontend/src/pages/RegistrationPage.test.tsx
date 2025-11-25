import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import RegistrationPage from './RegistrationPage';
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

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const renderRegistrationPage = () => {
    return render(
        <BrowserRouter>
            <RegistrationPage />
        </BrowserRouter>
    );
};

describe('RegistrationPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
        localStorage.clear();
    });

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
        });
    });

    it('shows validation error when username is empty', async () => {
        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith('Username is required', 'error');
        });
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
        });
    });

    it('submits form with valid data', async () => {
        mockedAxios.post.mockResolvedValueOnce({
            data: 'Registration successful',
        });

        renderRegistrationPage();

        const emailInput = screen.getByLabelText(/Email/i);
        const usernameInput = screen.getByLabelText(/Username/i);
        const passwordInput = screen.getByLabelText(/Password/i);

        fireEvent.change(emailInput, { target: { value: 'newuser@example.com' } });
        fireEvent.change(usernameInput, { target: { value: 'New User' } });
        fireEvent.change(passwordInput, { target: { value: 'password123' } });

        const submitButton = screen.getByRole('button', { name: /Register/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockedAxios.post).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/register'),
                {
                    email: 'newuser@example.com',
                    name: 'New User',
                    password: 'password123',
                }
            );
        });
    });
});
