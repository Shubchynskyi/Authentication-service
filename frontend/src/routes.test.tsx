import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import AppRoutes from './routes';
import { checkAccess } from './api';

// Mock pages
vi.mock('./pages/HomePage', () => ({
    default: () => <div>Home Page</div>,
}));

vi.mock('./pages/LoginPage', () => ({
    default: () => <div>Login Page</div>,
}));

vi.mock('./pages/RegistrationPage', () => ({
    default: () => <div>Registration Page</div>,
}));

vi.mock('./pages/VerificationPage', () => ({
    default: () => <div>Verification Page</div>,
}));

vi.mock('./pages/VerifyPage', () => ({
    default: () => <div>Verify Page</div>,
}));

vi.mock('./pages/ForgotPasswordPage', () => ({
    default: () => <div>Forgot Password Page</div>,
}));

vi.mock('./pages/ResetPasswordPage', () => ({
    default: () => <div>Reset Password Page</div>,
}));

vi.mock('./pages/ProfilePage', () => ({
    default: () => <div>Profile Page</div>,
}));

vi.mock('./pages/EditProfilePage', () => ({
    default: () => <div>Edit Profile Page</div>,
}));

vi.mock('./pages/AdminPage', () => ({
    default: () => <div>Admin Page</div>,
}));

vi.mock('./pages/OAuth2RedirectHandler', () => ({
    default: () => <div>OAuth2 Redirect Handler</div>,
}));

vi.mock('./components/NotFoundPage', () => ({
    default: () => <div>Not Found Page</div>,
}));

// Mock api
const mockCheckAccess = vi.fn();
vi.mock('./api', () => ({
    checkAccess: (resource: string) => mockCheckAccess(resource),
}));

// Mock AuthContext
const mockIsAuthenticated = vi.fn(() => false);
const mockIsLoading = vi.fn(() => false);
const mockLogin = vi.fn();
const mockLogout = vi.fn();
const mockSetTokens = vi.fn();

vi.mock('./context/AuthContext', () => ({
    useAuth: () => ({
        isAuthenticated: mockIsAuthenticated(),
        isLoading: mockIsLoading(),
        login: mockLogin,
        logout: mockLogout,
        setTokens: mockSetTokens,
        error: null,
    }),
}));

describe('routes.tsx', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockIsAuthenticated.mockReturnValue(false);
        mockIsLoading.mockReturnValue(false);
        mockCheckAccess.mockResolvedValue(false);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('PrivateRoute', () => {
        it('should show loading screen when isLoading is true', () => {
            mockIsLoading.mockReturnValue(true);

            render(
                <MemoryRouter initialEntries={['/profile']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            // Loading screen should be shown (CircularProgress from MUI)
            expect(screen.queryByText('Profile Page')).not.toBeInTheDocument();
        });

        it('should redirect to login when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/profile']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Login Page')).toBeInTheDocument();
            });
        });

        it('should render children when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/profile']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Profile Page')).toBeInTheDocument();
            });
        });

        it('should render nested routes when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/profile/edit']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Edit Profile Page')).toBeInTheDocument();
            });
        });
    });

    describe('AdminRoute', () => {
        it('should show loading screen when isLoading is true', () => {
            mockIsLoading.mockReturnValue(true);

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            expect(screen.queryByText('Admin Page')).not.toBeInTheDocument();
        });

        it('should show NotFoundPage when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });

        it('should check access when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);
            mockCheckAccess.mockResolvedValue(true);

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(mockCheckAccess).toHaveBeenCalledWith('admin-panel');
            });
        });

        it('should show NotFoundPage when access is denied', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);
            mockCheckAccess.mockResolvedValue(false);

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });

        it('should render AdminPage when access is granted', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);
            mockCheckAccess.mockResolvedValue(true);

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Admin Page')).toBeInTheDocument();
            });
        });

        it('should handle checkAccess error', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);
            mockCheckAccess.mockRejectedValue(new Error('Access check failed'));

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });
    });

    describe('GuestRoute', () => {
        it('should show loading screen when isLoading is true', () => {
            mockIsLoading.mockReturnValue(true);

            render(
                <MemoryRouter initialEntries={['/login']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
        });

        it('should render children when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/login']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Login Page')).toBeInTheDocument();
            });
        });

        it('should show NotFoundPage when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/login']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });

        it('should allow access to registration page when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/register']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Registration Page')).toBeInTheDocument();
            });
        });

        it('should allow access to verification pages when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/verify']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Verification Page')).toBeInTheDocument();
            });
        });

        it('should allow access to verify email page when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/verify/email']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Verify Page')).toBeInTheDocument();
            });
        });
    });

    describe('AppRoutes', () => {
        it('should show loading screen when isLoading is true', () => {
            mockIsLoading.mockReturnValue(true);

            render(
                <MemoryRouter initialEntries={['/']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            expect(screen.queryByText('Home Page')).not.toBeInTheDocument();
        });

        it('should render HomePage for root path', async () => {
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Home Page')).toBeInTheDocument();
            });
        });

        it('should render OAuth2RedirectHandler for oauth2/success', async () => {
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/oauth2/success']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('OAuth2 Redirect Handler')).toBeInTheDocument();
            });
        });

        it('should render NotFoundPage for non-existent routes', async () => {
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/non-existent']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });

        it('should handle nested routes in profile', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <MemoryRouter initialEntries={['/profile/edit']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Edit Profile Page')).toBeInTheDocument();
            });
        });

        it('should handle nested routes in admin', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);
            mockCheckAccess.mockResolvedValue(true);

            render(
                <MemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Admin Page')).toBeInTheDocument();
            });
        });

        it('should show NotFoundPage for invalid nested routes in admin', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);
            mockCheckAccess.mockResolvedValue(true);

            render(
                <MemoryRouter initialEntries={['/admin/invalid']}>
                    <AppRoutes />
                </MemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });
    });
});

