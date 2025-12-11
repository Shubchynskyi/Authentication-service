import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import AdminRoute from './AdminRoute';
import { TestMemoryRouter } from '../../test-utils/router';

const mockIsAuthenticated = vi.fn(() => false);
const mockIsLoading = vi.fn(() => false);
const mockCheckAccess = vi.fn();

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({
        isAuthenticated: mockIsAuthenticated(),
        isLoading: mockIsLoading(),
    }),
}));

vi.mock('../../api', () => ({
    checkAccess: (resource: string) => mockCheckAccess(resource),
}));

vi.mock('../NotFoundPage', () => ({
    default: () => <div>Not Found Page</div>,
}));

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

const MockLoginPage = () => <div>Login Page</div>;
const MockAdminPage = () => <div>Admin Content</div>;

describe('AdminRoute', () => {
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

    it('renders children when user is admin', async () => {
        mockIsAuthenticated.mockReturnValue(true);
        mockCheckAccess.mockResolvedValue(true);

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route path="/admin" element={<AdminRoute />}>
                        <Route index element={<MockAdminPage />} />
                    </Route>
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Admin Content')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('redirects to login when user is not authenticated', async () => {
        mockIsAuthenticated.mockReturnValue(false);
        mockCheckAccess.mockResolvedValue(true);

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route path="/admin" element={<AdminRoute />}>
                        <Route index element={<MockAdminPage />} />
                    </Route>
                    <Route path="/login" element={<MockLoginPage />} />
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Login Page')).toBeInTheDocument();
            expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('renders NotFoundPage when access is denied', async () => {
        mockIsAuthenticated.mockReturnValue(true);
        mockCheckAccess.mockResolvedValue(false);

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route path="/admin" element={<AdminRoute />}>
                        <Route index element={<MockAdminPage />} />
                    </Route>
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Not Found Page')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('shows loading state while checking admin status', () => {
        mockIsAuthenticated.mockReturnValue(true);
        mockCheckAccess.mockImplementation(() => new Promise(() => {}));

        render(
            <TestMemoryRouter initialEntries={['/admin']}>
                <Routes>
                    <Route path="/admin" element={<AdminRoute />}>
                        <Route index element={<MockAdminPage />} />
                    </Route>
                </Routes>
            </TestMemoryRouter>
        );

        expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
        expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
    });
});

