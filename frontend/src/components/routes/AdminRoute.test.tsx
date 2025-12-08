import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import AdminRoute from './AdminRoute';
import { TestMemoryRouter } from '../../test-utils/router';

// Mock api
const mockApiGet = vi.fn();

vi.mock('../../api', () => ({
    default: {
        get: (...args: any[]) => mockApiGet(...args),
    },
}));

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
}));

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

// Mock pages for navigation testing
const MockLoginPage = () => <div>Login Page</div>;
const MockAdminPage = () => <div>Admin Content</div>;

describe('AdminRoute', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockApiGet.mockClear();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockApiGet.mockClear();
        localStorage.clear();
        cleanup();
    });

    it('renders children when user is admin', async () => {
        mockApiGet.mockResolvedValueOnce({
            data: { roles: ['ROLE_ADMIN', 'ROLE_USER'] },
        });

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route
                        path="/admin"
                        element={<AdminRoute />}
                    >
                        <Route index element={<MockAdminPage />} />
                    </Route>
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            // AdminRoute uses Outlet, so children are rendered via nested route
            expect(screen.getByText('Admin Content')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('redirects to login when user is not admin', async () => {
        mockApiGet.mockResolvedValueOnce({
            data: { roles: ['ROLE_USER'] },
        });

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route
                        path="/admin"
                        element={<AdminRoute />}
                    >
                        <Route index element={<MockAdminPage />} />
                    </Route>
                    <Route path="/login" element={<MockLoginPage />} />
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('redirects to login when API call fails with 401', async () => {
        mockApiGet.mockRejectedValueOnce({
            response: { status: 401 },
        });

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route
                        path="/admin"
                        element={<AdminRoute />}
                    >
                        <Route index element={<MockAdminPage />} />
                    </Route>
                    <Route path="/login" element={<MockLoginPage />} />
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(localStorage.getItem('accessToken')).toBeNull();
            expect(localStorage.getItem('refreshToken')).toBeNull();
        }, { timeout: 5000 });
    });

    it('shows nothing while checking admin status', () => {
        mockApiGet.mockImplementation(() => new Promise(() => {})); // Never resolves

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route
                        path="/admin"
                        element={<AdminRoute />}
                    >
                        <Route index element={<MockAdminPage />} />
                    </Route>
                </Routes>
            </TestMemoryRouter>
        );

        expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
        expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
    });
});

