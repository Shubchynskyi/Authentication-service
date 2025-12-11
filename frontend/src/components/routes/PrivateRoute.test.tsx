import { render, screen, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { vi, describe, it, expect } from 'vitest';
import PrivateRoute from './PrivateRoute';
import { TestMemoryRouter } from '../../test-utils/router';
import { setupTestCleanup } from '../../test-utils/test-helpers';

const mockIsAuthenticated = vi.fn(() => false);
const mockIsLoading = vi.fn(() => false);

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({
        isAuthenticated: mockIsAuthenticated(),
        isLoading: mockIsLoading(),
    }),
}));

const MockLoginPage = () => <div>Login Page</div>;
const MockProtectedPage = () => <div>Protected Content</div>;

describe('PrivateRoute', () => {
    setupTestCleanup();

    it('renders children when user is authenticated', async () => {
        mockIsAuthenticated.mockReturnValue(true);
        mockIsLoading.mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/protected']}>
                <Routes>
                    <Route
                        path="/protected"
                        element={
                            <PrivateRoute>
                                <MockProtectedPage />
                            </PrivateRoute>
                        }
                    />
                </Routes>
            </TestMemoryRouter>
        );

        // Use findByText which automatically waits
        const content = await screen.findByText('Protected Content', {}, { timeout: 5000 });
        expect(content).toBeInTheDocument();
    });

    it('redirects to login when user is not authenticated', async () => {
        mockIsAuthenticated.mockReturnValue(false);
        mockIsLoading.mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/protected']}>
                <Routes>
                    <Route
                        path="/protected"
                        element={
                            <PrivateRoute>
                                <MockProtectedPage />
                            </PrivateRoute>
                        }
                    />
                    <Route path="/login" element={<MockLoginPage />} />
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('redirects to login when token is expired', async () => {
        mockIsAuthenticated.mockReturnValue(false);
        mockIsLoading.mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/protected']}>
                <Routes>
                    <Route
                        path="/protected"
                        element={
                            <PrivateRoute>
                                <MockProtectedPage />
                            </PrivateRoute>
                        }
                    />
                    <Route path="/login" element={<MockLoginPage />} />
                </Routes>
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });
});
