import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import PublicRoute from './PublicRoute';
import * as tokenUtils from '../../utils/token';

// Mock token utils
vi.mock('../../utils/token', () => ({
    isJwtExpired: vi.fn(),
}));

// Mock pages for navigation testing
const MockProfilePage = () => <div>Profile Page</div>;
const MockPublicPage = () => <div>Public Content</div>;

describe('PublicRoute', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });

    it('renders children when user is not authenticated', () => {
        localStorage.removeItem('accessToken');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(true);

        render(
            <MemoryRouter initialEntries={['/login']}>
                <Routes>
                    <Route
                        path="/login"
                        element={
                            <PublicRoute>
                                <MockPublicPage />
                            </PublicRoute>
                        }
                    />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Public Content')).toBeInTheDocument();
    });

    it('redirects to profile when user is authenticated', async () => {
        localStorage.setItem('accessToken', 'valid-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(false);

        render(
            <MemoryRouter initialEntries={['/login']}>
                <Routes>
                    <Route
                        path="/login"
                        element={
                            <PublicRoute>
                                <MockPublicPage />
                            </PublicRoute>
                        }
                    />
                    <Route path="/profile" element={<MockProfilePage />} />
                </Routes>
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Public Content')).not.toBeInTheDocument();
        });
    });

    it('redirects to profile when token is valid', async () => {
        localStorage.setItem('accessToken', 'valid-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(false);

        render(
            <MemoryRouter initialEntries={['/register']}>
                <Routes>
                    <Route
                        path="/register"
                        element={
                            <PublicRoute>
                                <MockPublicPage />
                            </PublicRoute>
                        }
                    />
                    <Route path="/profile" element={<MockProfilePage />} />
                </Routes>
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Public Content')).not.toBeInTheDocument();
        });
    });
});
