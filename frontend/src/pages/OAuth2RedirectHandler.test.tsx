import { render, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import OAuth2RedirectHandler from './OAuth2RedirectHandler';
import * as tokenUtils from '../utils/token';
import { TestMemoryRouter } from '../test-utils/router';

// Mock token utils
vi.mock('../utils/token', () => ({
    clearTokens: vi.fn(),
    isValidJwtFormat: vi.fn(),
    setTokens: vi.fn(),
}));

// Mock AuthContext
const mockSetTokens = vi.fn();
vi.mock('../context/AuthContext', () => ({
    useAuth: () => ({
        setTokens: mockSetTokens,
    }),
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

// Helper to set window.location.hash
const setLocationHash = (hash: string) => {
    Object.defineProperty(window, 'location', {
        value: { 
            ...window.location,
            hash,
            pathname: '/oauth2/success'
        },
        writable: true,
    });
};

describe('OAuth2RedirectHandler', () => {
    const originalLocation = window.location;

    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(tokenUtils.clearTokens).mockClear();
        vi.mocked(tokenUtils.isValidJwtFormat).mockClear();
        mockSetTokens.mockClear();
        mockNavigate.mockClear();
        // Reset location hash
        setLocationHash('');
    });

    afterEach(() => {
        vi.clearAllMocks();
        vi.mocked(tokenUtils.clearTokens).mockClear();
        vi.mocked(tokenUtils.isValidJwtFormat).mockClear();
        mockSetTokens.mockClear();
        mockNavigate.mockClear();
        // Restore original location
        Object.defineProperty(window, 'location', {
            value: originalLocation,
            writable: true,
        });
    });

    it('handles error parameter and navigates with error message', async () => {
        const errorMessage = encodeURIComponent('Account is blocked. Reason: Suspicious activity');
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={[`/oauth2/redirect?error=${errorMessage}`]}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith(
                '/',
                expect.objectContaining({ replace: true })
            );
        });
    });

    it('handles account blocked error', async () => {
        const errorMessage = encodeURIComponent('Account is blocked. Suspicious activity');
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={[`/oauth2/redirect?error=${errorMessage}`]}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
        }, { timeout: 5000 });
    });

    it('handles account disabled error', async () => {
        const errorMessage = encodeURIComponent('Account is disabled');
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={[`/oauth2/redirect?error=${errorMessage}`]}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
        }, { timeout: 5000 });
    });

    it('handles whitelist error', async () => {
        const errorMessage = encodeURIComponent('Email not in whitelist');
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={[`/oauth2/redirect?error=${errorMessage}`]}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
        }, { timeout: 5000 });
    });

    it('handles invalid_credentials error and redirects to login with secret parameter', async () => {
        const errorMessage = encodeURIComponent('invalid_credentials');
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={[`/oauth2/redirect?error=${errorMessage}`]}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith(
                '/login?secret=true',
                expect.objectContaining({
                    replace: true,
                    state: { error: 'errors.loginFailed' }
                })
            );
        }, { timeout: 5000 });
    });

    it('sets tokens and navigates on successful OAuth', async () => {
        const accessToken = 'valid.access.token';
        const encodedAccessToken = encodeURIComponent(accessToken);

        // Tokens come via URL hash fragment
        setLocationHash(`#accessToken=${encodedAccessToken}`);
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(true);

        render(
            <TestMemoryRouter initialEntries={['/oauth2/success']}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.isValidJwtFormat).toHaveBeenCalledWith(accessToken);
            expect(mockSetTokens).toHaveBeenCalledWith(accessToken);
            expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
        }, { timeout: 5000 });
    });

    it('clears tokens and navigates when tokens are missing', async () => {
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/oauth2/redirect']}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith(
                '/',
                expect.objectContaining({ replace: true })
            );
        });
    });

    it('clears tokens and navigates when token format is invalid', async () => {
        const accessToken = 'invalid-token';
        const encodedAccessToken = encodeURIComponent(accessToken);

        // Tokens come via URL hash fragment
        setLocationHash(`#accessToken=${encodedAccessToken}`);
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/oauth2/success']}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith(
                '/',
                expect.objectContaining({ replace: true })
            );
        });
    });

    it('handles decode error gracefully', async () => {
        // Invalid URL encoding that will cause decode error in hash fragment
        setLocationHash('#accessToken=%E0%A4%A');
        vi.mocked(tokenUtils.isValidJwtFormat).mockReturnValue(false);

        render(
            <TestMemoryRouter initialEntries={['/oauth2/success']}>
                <OAuth2RedirectHandler />
            </TestMemoryRouter>
        );

        await waitFor(() => {
            expect(tokenUtils.clearTokens).toHaveBeenCalled();
        }, { timeout: 5000 });
    });
});

