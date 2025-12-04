import { render, screen, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import PublicRoute from './PublicRoute';
import * as tokenUtils from '../../utils/token';
import { TestMemoryRouter } from '../../test-utils/router';

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
        // Clear localStorage to ensure clean state
        localStorage.clear();
        // Reset mock to ensure clean state between tests
        vi.mocked(tokenUtils.isJwtExpired).mockReset();
        vi.mocked(tokenUtils.isJwtExpired).mockClear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        // Reset mock after each test
        vi.mocked(tokenUtils.isJwtExpired).mockReset();
        vi.mocked(tokenUtils.isJwtExpired).mockClear();
    });

    it('renders children when user is not authenticated', async () => {
        // Ensure no token in localStorage
        localStorage.removeItem('accessToken');
        // Mock isJwtExpired to return true (token expired or no token)
        vi.mocked(tokenUtils.isJwtExpired).mockImplementation((token: string | null) => {
            return token === null || token === undefined || token === '';
        });

        render(
            <TestMemoryRouter initialEntries={['/login']}>
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
            </TestMemoryRouter>
        );

        // Use findByText which automatically waits for async rendering
        const content = await screen.findByText('Public Content', {}, { timeout: 5000 });
        expect(content).toBeInTheDocument();
    });

    it('redirects to profile when user is authenticated', async () => {
        localStorage.setItem('accessToken', 'valid-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/login']}>
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
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Public Content')).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('redirects to profile when token is valid', async () => {
        localStorage.setItem('accessToken', 'valid-token');
        vi.mocked(tokenUtils.isJwtExpired).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/register']}>
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
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(screen.queryByText('Public Content')).not.toBeInTheDocument();
        }, { timeout: 5000 });
    });
});
