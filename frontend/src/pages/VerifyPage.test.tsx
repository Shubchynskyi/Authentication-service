import { render, screen, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import VerifyPage from './VerifyPage';
import { TestMemoryRouter } from '../test-utils/router';
import { setupTestCleanup } from '../test-utils/test-helpers';

// Mock api
const mockApiPost = vi.fn();

vi.mock('../api', () => ({
    default: {
        post: (...args: any[]) => mockApiPost(...args),
    },
}));

const mockShowNotification = vi.fn();

vi.mock('../context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    useNotification: () => ({
        notifications: [],
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
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

// Mock maskedLoginService
const mockGetMaskedLoginSettingsPublic = vi.fn();
vi.mock('../services/maskedLoginService', () => ({
    getMaskedLoginSettingsPublic: (...args: any[]) => mockGetMaskedLoginSettingsPublic(...args),
}));

const renderVerifyPage = (searchParams = '?verificationToken=token123&email=test@example.com') => {
    return render(
        <TestMemoryRouter initialEntries={[`/verify/email${searchParams}`]}>
            <VerifyPage />
        </TestMemoryRouter>
    );
};

describe('VerifyPage', () => {
    setupTestCleanup();

    beforeEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
        mockApiPost.mockClear();
        mockGetMaskedLoginSettingsPublic.mockClear();
        // Default: masked login disabled
        mockGetMaskedLoginSettingsPublic.mockResolvedValue({ enabled: false, templateId: 1 });
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('shows loading state initially', () => {
        mockApiPost.mockImplementation(() => new Promise(() => {})); // Never resolves

        renderVerifyPage();

        expect(screen.getByText(/auth.verification.verifying/i)).toBeInTheDocument();
    });

    it('verifies email successfully and redirects to /login when masked login is disabled', async () => {
        mockApiPost.mockResolvedValueOnce({ data: 'Success' });
        mockGetMaskedLoginSettingsPublic.mockResolvedValueOnce({ enabled: false, templateId: 1 });

        renderVerifyPage();

        await waitFor(() => {
            expect(mockApiPost).toHaveBeenCalledWith('/api/auth/verify', {
                email: 'test@example.com',
                code: 'token123',
            });
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('successRedirect'),
                'success'
            );
        }, { timeout: 5000 });

        // Wait for setTimeout to complete (1500ms)
        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login', expect.any(Object));
        }, { timeout: 3000 });
    });

    it('verifies email successfully and redirects to /login?secret=true when masked login is enabled', async () => {
        mockApiPost.mockResolvedValueOnce({ data: 'Success' });
        mockGetMaskedLoginSettingsPublic.mockResolvedValueOnce({ enabled: true, templateId: 1 });

        renderVerifyPage();

        await waitFor(() => {
            expect(mockApiPost).toHaveBeenCalledWith('/api/auth/verify', {
                email: 'test@example.com',
                code: 'token123',
            });
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('successRedirect'),
                'success'
            );
        }, { timeout: 5000 });

        // Wait for setTimeout to complete (1500ms)
        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login?secret=true', expect.any(Object));
        }, { timeout: 3000 });
    });

    it('shows error when verification token is missing', async () => {
        renderVerifyPage('?email=test@example.com');

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('invalidLink'),
                'error'
            );
        }, { timeout: 5000 });
    });

    it('shows error when email is missing', async () => {
        renderVerifyPage('?verificationToken=token123');

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('invalidLink'),
                'error'
            );
        }, { timeout: 5000 });
    });

    it('handles verification error', async () => {
        mockApiPost.mockRejectedValueOnce(new Error('Verification failed'));

        renderVerifyPage();

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('error'),
                'error'
            );
        }, { timeout: 5000 });
    });
});

