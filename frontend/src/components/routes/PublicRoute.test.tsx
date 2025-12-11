import { render, screen, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { vi, describe, it, expect } from 'vitest';
import PublicRoute from './PublicRoute';
import { TestMemoryRouter } from '../../test-utils/router';
import { setupTestCleanup } from '../../test-utils/test-helpers';
import { setupTokenMocks } from '../../test-utils/mocks';

const { mockIsJwtExpired } = setupTokenMocks('../../utils/token');

// Mock pages for navigation testing
const MockProfilePage = () => <div>Profile Page</div>;
const MockPublicPage = () => <div>Public Content</div>;

describe('PublicRoute', () => {
    setupTestCleanup();

    it('renders children when user is not authenticated', async () => {
        // Ensure no token in localStorage
        localStorage.removeItem('accessToken');
        // Mock isJwtExpired to return true (token expired or no token)
        mockIsJwtExpired.mockImplementation((token: string | null | undefined) => {
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
        mockIsJwtExpired.mockReturnValue(false);

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
        mockIsJwtExpired.mockReturnValue(false);

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
