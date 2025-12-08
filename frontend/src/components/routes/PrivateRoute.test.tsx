import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import PrivateRoute from './PrivateRoute';
import * as tokenUtils from '../../utils/token';
import { TestMemoryRouter } from '../../test-utils/router';

// Mock token utils
vi.mock('../../utils/token', () => ({
    isJwtExpired: vi.fn(),
}));

// Mock pages for navigation testing
const MockLoginPage = () => <div>Login Page</div>;
const MockProtectedPage = () => <div>Protected Content</div>;

describe('PrivateRoute', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        cleanup();
    });

    it('renders children when user is authenticated', async () => {
        localStorage.setItem('accessToken', 'valid-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(false);

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
        localStorage.removeItem('accessToken');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(true);

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
        localStorage.setItem('accessToken', 'expired-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(true);

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
