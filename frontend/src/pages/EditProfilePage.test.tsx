import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import EditProfilePage from './EditProfilePage';
import { TestBrowserRouter } from '../test-utils/router';

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
}));

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
let mockIsLoading = false;
let currentProfile: { email: string; name: string; roles: string[]; authProvider: 'LOCAL' | 'GOOGLE' } | null = mockProfile;

const mockUseProfile = vi.fn(() => ({
    profile: currentProfile,
    isLoading: mockIsLoading,
    isAdmin: false,
    updateProfile: mockUpdateProfile,
}));

vi.mock('../context/ProfileContext', () => ({
    useProfile: () => mockUseProfile(),
}));

// Mock NotificationContext
const mockShowNotification = vi.fn();
const mockRemoveNotification = vi.fn();
vi.mock('../context/NotificationContext', () => ({
    useNotification: () => ({
        showNotification: mockShowNotification,
        removeNotification: mockRemoveNotification,
        notifications: [],
    }),
}));

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
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
const mockChangeLanguage = vi.fn();
const mockI18n = {
    language: 'en',
    changeLanguage: mockChangeLanguage,
    resolvedLanguage: 'en',
};
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => {
            const translations: Record<string, string> = {
                'profile.editTitle': 'Edit Profile',
                'common.name': 'Name',
                'common.username': 'Username',
                'common.email': 'Email',
                'common.password': 'Password',
                'common.currentPassword': 'Current Password',
                'common.newPassword': 'New Password',
                'common.optional': 'optional',
                'common.confirmPassword': 'Confirm Password',
                'common.save': 'Save',
                'common.cancel': 'Cancel',
                'common.home': 'Home',
                'profile.backToProfile': 'Back to Profile',
                'profile.googleAuthInfo': 'Password change is not available for Google-authenticated accounts',
                'errors.requiredField': 'This field is required',
                'errors.passwordMismatch': 'Passwords do not match',
                'errors.confirmPasswordRequired': 'Confirm password is required',
                'errors.passwordRequirements': 'Password requirements',
                'profile.notifications.currentPasswordRequired': 'Enter current password to change password',
                'profile.notifications.updateSuccess': 'Profile updated successfully',
                'profile.notifications.incorrectCurrentPassword': 'Incorrect current password',
                'profile.notifications.updateError': 'Error updating profile',
                'notFound.backHome': 'Go to Home',
            };
            return translations[key] || key;
        },
        i18n: mockI18n,
    }),
}));

const renderEditProfilePage = () => {
    return render(
        <TestBrowserRouter>
            <EditProfilePage />
        </TestBrowserRouter>
    );
};

describe('EditProfilePage', () => {
    let originalLocation: Location;

    beforeEach(() => {
        vi.clearAllMocks();
        mockUpdateProfile.mockClear();
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
        mockUpdateProfile.mockResolvedValue(undefined);
        mockIsLoading = false;
        currentProfile = mockProfile;
        originalLocation = window.location;
        delete (window as any).location;
        (window as any).location = {
            pathname: '/profile/edit',
            search: '',
            hash: '',
            origin: 'http://localhost',
            href: 'http://localhost/profile/edit',
            replace: vi.fn(),
        };
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockUpdateProfile.mockClear();
        mockShowNotification.mockClear();
        mockNavigate.mockClear();
        (window as any).location = originalLocation;
        cleanup();
    });

    it('renders edit profile form', async () => {
        renderEditProfilePage();

        await waitFor(() => {
            expect(screen.getByRole('heading', { name: /Edit Profile/i })).toBeInTheDocument();
        }, { timeout: 5000 });
        // Wait for profile to load and name field to be set
        await waitFor(() => {
            const nameInput = screen.getByLabelText(/^Name/i) as HTMLInputElement;
            expect(nameInput).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('pre-fills name from profile', async () => {
        renderEditProfilePage();

        // Wait for profile to load and name to be set via useEffect
        await waitFor(() => {
            const nameInput = screen.getByLabelText(/^Name/i) as HTMLInputElement;
            expect(nameInput).toBeInTheDocument();
            expect(nameInput.value).toBe('Test User');
        }, { timeout: 5000 });
    });

    it('shows loading state when profile is loading', () => {
        mockIsLoading = true;
        mockUseProfile.mockReturnValueOnce({
            profile: mockProfile,
            isLoading: true,
            isAdmin: false,
            updateProfile: mockUpdateProfile,
        });

        renderEditProfilePage();

        // Should show CircularProgress
        expect(screen.queryByText('Edit Profile')).not.toBeInTheDocument();
        
        mockIsLoading = false;
    });

    it('shows Google auth info for Google users', () => {
        currentProfile = { ...mockProfile, authProvider: 'GOOGLE' as const };
        mockUseProfile.mockReturnValueOnce({
            profile: currentProfile,
            isLoading: false,
            isAdmin: false,
            updateProfile: mockUpdateProfile,
        });

        renderEditProfilePage();

        expect(screen.getByText(/Password change is not available for Google-authenticated accounts/i)).toBeInTheDocument();
        expect(screen.queryByLabelText(/Current Password/i)).not.toBeInTheDocument();
        
        currentProfile = mockProfile;
    });

    it('shows password fields for local users', () => {
        renderEditProfilePage();

        expect(screen.getByLabelText(/Current Password/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/New Password \(optional\)/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Confirm Password/i)).toBeInTheDocument();
    });

    it('validates password when new password is entered', async () => {
        renderEditProfilePage();

        await waitFor(() => {
            expect(screen.getByLabelText(/New Password \(optional\)/i)).toBeInTheDocument();
        }, { timeout: 5000 });

        const newPasswordInput = screen.getByLabelText(/New Password \(optional\)/i);
        fireEvent.change(newPasswordInput, { target: { value: 'weak' } });

        await waitFor(() => {
            const errorTexts = screen.getAllByText(/password/i);
            expect(errorTexts.length).toBeGreaterThan(0);
        }, { timeout: 5000 });
    });

    it('shows error when new password is set without current password', async () => {
        renderEditProfilePage();

        await waitFor(() => {
            expect(screen.getByLabelText(/New Password \(optional\)/i)).toBeInTheDocument();
        }, { timeout: 5000 });

        const newPasswordInput = screen.getByLabelText(/New Password \(optional\)/i);
        const nameInput = await screen.findByLabelText(/^Name/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'New Name' } });
        fireEvent.change(newPasswordInput, { target: { value: 'NewPassword123@' } });
        
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Enter current password to change password',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('shows error when confirm password is missing for new password', async () => {
        renderEditProfilePage();

        const nameInput = await screen.findByLabelText(/^Name/i);
        const currentPasswordInput = screen.getByLabelText(/Current Password/i);
        const newPasswordInput = screen.getByLabelText(/New Password \(optional\)/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
        fireEvent.change(currentPasswordInput, { target: { value: 'OldPassword123@' } });
        fireEvent.change(newPasswordInput, { target: { value: 'NewPassword123@' } });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Confirm password is required',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('shows error when confirm password does not match', async () => {
        renderEditProfilePage();

        const nameInput = await screen.findByLabelText(/^Name/i);
        const currentPasswordInput = screen.getByLabelText(/Current Password/i);
        const newPasswordInput = screen.getByLabelText(/New Password \(optional\)/i);
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
        fireEvent.change(currentPasswordInput, { target: { value: 'OldPassword123@' } });
        fireEvent.change(newPasswordInput, { target: { value: 'NewPassword123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'Mismatch123@' } });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Passwords do not match',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('updates profile with name only', async () => {
        renderEditProfilePage();

        const nameInput = await screen.findByLabelText(/^Name/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
        
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockUpdateProfile).toHaveBeenCalledWith({
                name: 'Updated Name',
                password: '',
                currentPassword: '',
            });
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Profile updated successfully',
                'success'
            );
            expect(mockNavigate).toHaveBeenCalledWith('/profile', { replace: true });
        }, { timeout: 5000 });
    });

    it('updates profile with name and password', async () => {
        renderEditProfilePage();

        const nameInput = await screen.findByLabelText(/^Name/i);
        const currentPasswordInput = screen.getByLabelText(/Current Password/i);
        const newPasswordInput = screen.getByLabelText(/New Password \(optional\)/i);
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
        fireEvent.change(currentPasswordInput, { target: { value: 'OldPassword123@' } });
        fireEvent.change(newPasswordInput, { target: { value: 'NewPassword123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'NewPassword123@' } });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockUpdateProfile).toHaveBeenCalledWith({
                name: 'Updated Name',
                password: 'NewPassword123@',
                currentPassword: 'OldPassword123@',
            });
        }, { timeout: 5000 });
    });

    it('handles incorrect current password error', async () => {
        const axiosError = {
            isAxiosError: true,
            response: { data: 'Incorrect current password' },
        };
        mockUpdateProfile.mockRejectedValueOnce(axiosError);

        renderEditProfilePage();

        const nameInput = await screen.findByLabelText(/^Name/i);
        const currentPasswordInput = screen.getByLabelText(/Current Password/i);
        const newPasswordInput = screen.getByLabelText(/New Password \(optional\)/i);
        const confirmPasswordInput = screen.getByLabelText(/Confirm Password/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
        fireEvent.change(currentPasswordInput, { target: { value: 'WrongPassword123@' } });
        fireEvent.change(newPasswordInput, { target: { value: 'NewPassword123@' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'NewPassword123@' } });
        
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Incorrect current password',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('handles update error', async () => {
        const axiosError = {
            isAxiosError: true,
            response: { data: 'Update failed' },
        };
        mockUpdateProfile.mockRejectedValueOnce(axiosError);

        renderEditProfilePage();

        const nameInput = await screen.findByLabelText(/^Name/i);
        const submitButton = screen.getByRole('button', { name: /Save/i });

        fireEvent.change(nameInput, { target: { value: 'Updated Name' } });
        
        const form = submitButton.closest('form');
        if (form) {
            fireEvent.submit(form);
        } else {
            fireEvent.click(submitButton);
        }

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                'Error updating profile',
                'error'
            );
        }, { timeout: 5000 });
    });

    it('calls full reload when home button is clicked', () => {
        renderEditProfilePage();

        const homeButton = screen.getByRole('button', { name: /Go to Home/i });
        fireEvent.click(homeButton);

        expect(window.location.replace).toHaveBeenCalledWith('/');
    });

    it('has link to profile page', () => {
        renderEditProfilePage();

        const profileLink = screen.getByRole('link', { name: /Back to Profile/i });
        expect(profileLink).toHaveAttribute('href', '/profile');
    });
});

