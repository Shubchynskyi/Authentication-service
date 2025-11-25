import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import PrivateRoute from './PrivateRoute';
import * as tokenUtils from '../../utils/token';

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
    });

    it('renders children when user is authenticated', () => {
        localStorage.setItem('accessToken', 'valid-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(false);

        render(
            <MemoryRouter initialEntries={['/protected']}>
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
            </MemoryRouter>
        );

        expect(screen.getByText('Protected Content')).toBeInTheDocument();
    });

    it('redirects to login when user is not authenticated', async () => {
        localStorage.removeItem('accessToken');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(true);

        render(
            <MemoryRouter initialEntries={['/protected']}>
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
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        });
    });

    it('redirects to login when token is expired', async () => {
        localStorage.setItem('accessToken', 'expired-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(true);

        render(
            <MemoryRouter initialEntries={['/protected']}>
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
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        });
    });
});
