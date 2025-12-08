import React from 'react';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import ProfilePage from './ProfilePage';
import { TestBrowserRouter } from '../test-utils/router';

// Mock ProfileContext
const mockProfile = {
    id: 1,
    email: 'test@example.com',
    name: 'Test User',
    roles: ['ROLE_USER'],
    enabled: true,
    blocked: false,
    emailVerified: true,
    authProvider: 'LOCAL' as const,
};

const mockUpdateProfile = vi.fn();
let mockIsAdmin = false;
let mockIsLoading = false;
let currentProfile: { email: string; name: string; roles: string[]; authProvider: 'LOCAL' | 'GOOGLE' } | null = mockProfile;

const mockUseProfile = vi.fn(() => ({
    profile: currentProfile,
    isLoading: mockIsLoading,
    isAdmin: mockIsAdmin,
    updateProfile: mockUpdateProfile,
}));

vi.mock('../context/ProfileContext', () => ({
    useProfile: () => mockUseProfile(),
}));

// Mock AuthContext
const mockLogout = vi.fn();
vi.mock('../context/AuthContext', () => ({
    useAuth: () => ({
        logout: mockLogout,
    }),
}));

// Mock AdminSection - it checks isAdmin internally
// We need to use the mockIsAdmin variable from the closure
vi.mock('./AdminSection', () => ({
    default: ({ children }: { children: React.ReactNode }) => {
        // Use mockIsAdmin from the outer scope
        return mockIsAdmin ? <div>{children}</div> : null;
    },
}));

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

const renderProfilePage = () => {
    return render(
        <TestBrowserRouter>
            <ProfilePage />
        </TestBrowserRouter>
    );
};

describe('ProfilePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockLogout.mockClear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockLogout.mockClear();
        cleanup();
    });

    it('renders profile information', () => {
        renderProfilePage();

        expect(screen.getByRole('heading', { name: /profile.title/i })).toBeInTheDocument();
        expect(screen.getByText('test@example.com')).toBeInTheDocument();
        expect(screen.getByText('Test User')).toBeInTheDocument();
    });

    it('shows loading state when profile is loading', () => {
        mockIsLoading = true;
        currentProfile = null;
        mockUseProfile.mockReturnValueOnce({
            profile: null,
            isLoading: true,
            isAdmin: false,
            updateProfile: mockUpdateProfile,
        });

        renderProfilePage();

        // Should show CircularProgress
        expect(screen.queryByText(/profile.title/i)).not.toBeInTheDocument();
        
        mockIsLoading = false;
        currentProfile = mockProfile;
    });

    it('returns null when profile is not available', () => {
        currentProfile = null;
        mockUseProfile.mockReturnValueOnce({
            profile: null,
            isLoading: false,
            isAdmin: false,
            updateProfile: mockUpdateProfile,
        });

        const { container } = renderProfilePage();
        expect(container.firstChild).toBeNull();
        
        currentProfile = mockProfile;
    });

    it('has link to edit profile page', () => {
        renderProfilePage();

        const editLink = screen.getByRole('link', { name: /common.editProfile/i });
        expect(editLink).toHaveAttribute('href', '/profile/edit');
    });

    it('has link to home page', () => {
        renderProfilePage();

        const homeLink = screen.getByRole('link', { name: /notFound.backHome/i });
        expect(homeLink).toHaveAttribute('href', '/');
    });

    it('calls logout when logout button is clicked', () => {
        renderProfilePage();

        const logoutButton = screen.getByRole('button', { name: /common.logout/i });
        fireEvent.click(logoutButton);

        expect(mockLogout).toHaveBeenCalled();
    });

    it('shows admin panel button when user is admin', () => {
        mockIsAdmin = true;
        mockUseProfile.mockReturnValueOnce({
            profile: { ...mockProfile, roles: ['ROLE_USER', 'ROLE_ADMIN'] },
            isLoading: false,
            isAdmin: true,
            updateProfile: mockUpdateProfile,
        });

        renderProfilePage();

        const adminButton = screen.getByRole('link', { name: /profile.adminPanel/i });
        expect(adminButton).toHaveAttribute('href', '/admin');
        
        mockIsAdmin = false;
    });

    it('does not show admin panel button when user is not admin', () => {
        renderProfilePage();

        const adminButton = screen.queryByRole('link', { name: /profile.adminPanel/i });
        expect(adminButton).not.toBeInTheDocument();
    });
});

