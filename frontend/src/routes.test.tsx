import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import AppRoutes from './routes';
import { TestMemoryRouter } from './test-utils/router';

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
        cleanup();
    });

    describe('PrivateRoute', () => {
        it('should show loading screen when isLoading is true', () => {
            mockIsLoading.mockReturnValue(true);

            render(
                <TestMemoryRouter initialEntries={['/profile']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            // Loading screen should be shown (CircularProgress from MUI)
            expect(screen.queryByText('Profile Page')).not.toBeInTheDocument();
        });

        it('should redirect to login when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/profile']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Login Page')).toBeInTheDocument();
            });
        });

        it('should render children when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/profile']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Profile Page')).toBeInTheDocument();
            });
        });

        it('should render nested routes when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/profile/edit']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            expect(screen.queryByText('Admin Page')).not.toBeInTheDocument();
        });

        it('should show NotFoundPage when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/login']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
        });

        it('should render children when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/login']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Login Page')).toBeInTheDocument();
            });
        });

        it('should show NotFoundPage when authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/login']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });

        it('should allow access to registration page when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/register']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Registration Page')).toBeInTheDocument();
            });
        });

        it('should allow access to verification pages when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/verify']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Verification Page')).toBeInTheDocument();
            });
        });

        it('should allow access to verify email page when not authenticated', async () => {
            mockIsAuthenticated.mockReturnValue(false);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/verify/email']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            expect(screen.queryByText('Home Page')).not.toBeInTheDocument();
        });

        it('should render HomePage for root path', async () => {
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Home Page')).toBeInTheDocument();
            });
        });

        it('should render OAuth2RedirectHandler for oauth2/success', async () => {
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/oauth2/success']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('OAuth2 Redirect Handler')).toBeInTheDocument();
            });
        });

        it('should render NotFoundPage for non-existent routes', async () => {
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/non-existent']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });

        it('should handle nested routes in profile', async () => {
            mockIsAuthenticated.mockReturnValue(true);
            mockIsLoading.mockReturnValue(false);

            render(
                <TestMemoryRouter initialEntries={['/profile/edit']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin']}>
                    <AppRoutes />
                </TestMemoryRouter>
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
                <TestMemoryRouter initialEntries={['/admin/invalid']}>
                    <AppRoutes />
                </TestMemoryRouter>
            );

            await waitFor(() => {
                expect(screen.getByText('Not Found Page')).toBeInTheDocument();
            });
        });
    });
});

