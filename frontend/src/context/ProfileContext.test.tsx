import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import { ProfileProvider, useProfile } from './ProfileContext';
import { AuthProvider, useAuth } from './AuthContext';
import api from '../api';
import * as tokenUtils from '../utils/token';

// Create mocks using vi.hoisted() to avoid hoisting issues
const { mockApiGet, mockApiPost, mockGetAccessToken, mockGetRefreshToken, mockIsJwtExpired, mockClearTokens } = vi.hoisted(() => {
    const mockApiGet = vi.fn();
    const mockApiPost = vi.fn();
    const mockGetAccessToken = vi.fn();
    const mockGetRefreshToken = vi.fn();
    const mockIsJwtExpired = vi.fn();
    const mockClearTokens = vi.fn();
    
    return { mockApiGet, mockApiPost, mockGetAccessToken, mockGetRefreshToken, mockIsJwtExpired, mockClearTokens };
});

// Mock api
vi.mock('../api', () => ({
    default: {
        get: mockApiGet,
        post: mockApiPost,
        defaults: {
            headers: {
                common: {} as Record<string, string>,
            },
        },
    },
}));

// Mock token utils
vi.mock('../utils/token', () => ({
    getAccessToken: () => mockGetAccessToken(),
    getRefreshToken: () => mockGetRefreshToken(),
    isJwtExpired: (token: string) => mockIsJwtExpired(token),
    clearTokens: () => mockClearTokens(),
}));

// Mock axios for AuthContext
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
}));

// Test component that uses ProfileContext
const TestComponent: React.FC = () => {
    const { profile, isLoading, error, isAdmin, updateProfile } = useProfile();
    const [updateError, setUpdateError] = React.useState<string | null>(null);

    const handleUpdate = async () => {
        try {
            await updateProfile({ name: 'New Name', password: 'newpass', currentPassword: 'oldpass' });
        } catch (err) {
            setUpdateError(err instanceof Error ? err.message : 'Update failed');
        }
    };

    return (
        <div>
            <div data-testid="profile">{profile ? JSON.stringify(profile) : 'null'}</div>
            <div data-testid="isLoading">{isLoading ? 'true' : 'false'}</div>
            <div data-testid="error">{error || 'null'}</div>
            <div data-testid="isAdmin">{isAdmin ? 'true' : 'false'}</div>
            <div data-testid="updateError">{updateError || 'null'}</div>
            <button data-testid="update-btn" onClick={handleUpdate}>
                Update Profile
            </button>
        </div>
    );
};

// Wrapper component with AuthProvider
const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    return (
        <AuthProvider>
            <ProfileProvider>{children}</ProfileProvider>
        </AuthProvider>
    );
};

describe('ProfileContext', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        mockGetAccessToken.mockReturnValue(null);
        mockGetRefreshToken.mockReturnValue(null);
        mockIsJwtExpired.mockReturnValue(true);
        mockClearTokens.mockImplementation(() => {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });

    describe('Initialization', () => {
        it('should not fetch profile when not authenticated', async () => {
            mockGetAccessToken.mockReturnValue(null);
            mockGetRefreshToken.mockReturnValue(null);

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            // Wait for AuthContext to initialize
            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            }, { timeout: 5000 });

            // Use real timers for async operations
            vi.useRealTimers();
            
            // Wait a bit for async operations wrapped in act()
            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(mockApiGet).not.toHaveBeenCalled();
                expect(screen.getByTestId('profile')).toHaveTextContent('null');
            }, { timeout: 5000 });
        });

        it('should fetch profile when authenticated', async () => {
            const mockProfile = {
                email: 'test@example.com',
                name: 'Test User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            const validToken = 'valid.token.here';
            mockGetAccessToken.mockReturnValue(validToken);
            mockGetRefreshToken.mockReturnValue(null);
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            // Wait for AuthContext to initialize
            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            }, { timeout: 5000 });

            // Use real timers for async operations
            vi.useRealTimers();
            
            // Wait for the initial delay wrapped in act()
            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(mockApiGet).toHaveBeenCalledWith('/api/protected/profile');
            }, { timeout: 5000 });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(mockProfile));
            });

            expect(screen.getByTestId('error')).toHaveTextContent('null');
            expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
        });

        it('should retry fetching when token is not ready', async () => {
            const mockProfile = {
                email: 'test@example.com',
                name: 'Test User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            // Initially return null to simulate token not ready
            mockGetAccessToken.mockReturnValue(null);
            mockIsJwtExpired.mockReturnValue(true);
            mockApiGet.mockResolvedValue({ data: mockProfile });

            const TestComponentWithSetTokens: React.FC = () => {
                const { profile, isLoading, error, isAdmin } = useProfile();
                const { setTokens } = useAuth();
                
                React.useEffect(() => {
                    // Set tokens after a delay to simulate token becoming available
                    const timer = setTimeout(() => {
                        mockGetAccessToken.mockReturnValue('token');
                        mockIsJwtExpired.mockReturnValue(false);
                        setTokens('token', 'refresh-token');
                    }, 100);
                    return () => clearTimeout(timer);
                }, [setTokens]);

                return (
                    <div>
                        <div data-testid="profile">{profile ? JSON.stringify(profile) : 'null'}</div>
                        <div data-testid="isLoading">{isLoading ? 'true' : 'false'}</div>
                        <div data-testid="error">{error || 'null'}</div>
                        <div data-testid="isAdmin">{isAdmin ? 'true' : 'false'}</div>
                    </div>
                );
            };

            render(
                <TestWrapper>
                    <TestComponentWithSetTokens />
                </TestWrapper>
            );

            // Wait for AuthContext to initialize
            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            }, { timeout: 5000 });

            // Wait for tokens to be set and ProfileContext to retry
            // Delay for setTokens (100ms) + initial delay (50ms) + retry delay (100ms) + buffer
            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 300));
            });

            await waitFor(() => {
                expect(mockApiGet).toHaveBeenCalledWith('/api/protected/profile');
            }, { timeout: 5000 });
        });
    });

    describe('Profile loading', () => {
        it('should handle successful profile fetch', async () => {
            const mockProfile = {
                email: 'admin@example.com',
                name: 'Admin User',
                roles: ['ROLE_ADMIN', 'ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(mockProfile));
            });

            expect(screen.getByTestId('isAdmin')).toHaveTextContent('true');
            expect(screen.getByTestId('error')).toHaveTextContent('null');
        });

        it('should handle profile fetch error', async () => {
            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockRejectedValue(new Error('Network error'));

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('error')).toHaveTextContent('Failed to fetch profile');
            });

            expect(screen.getByTestId('profile')).toHaveTextContent('null');
            expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
        });

        it('should set isLoading during fetch', async () => {
            const mockProfile = {
                email: 'test@example.com',
                name: 'Test User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            let resolvePromise: (value: any) => void;
            const promise = new Promise((resolve) => {
                resolvePromise = resolve;
            });

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockReturnValue(promise);

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('true');
            });

            await act(async () => {
                resolvePromise!({ data: mockProfile });
            });
            
            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });
        });
    });

    describe('isAdmin', () => {
        it('should return true when user has ROLE_ADMIN', async () => {
            const mockProfile = {
                email: 'admin@example.com',
                name: 'Admin',
                roles: ['ROLE_ADMIN', 'ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('isAdmin')).toHaveTextContent('true');
            });
        });

        it('should return false when user does not have ROLE_ADMIN', async () => {
            const mockProfile = {
                email: 'user@example.com',
                name: 'User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('isAdmin')).toHaveTextContent('false');
            });
        });

        it('should return false when profile is null', () => {
            mockGetAccessToken.mockReturnValue(null);
            mockIsJwtExpired.mockReturnValue(true);

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            expect(screen.getByTestId('isAdmin')).toHaveTextContent('false');
        });
    });

    describe('updateProfile', () => {
        it('should successfully update profile', async () => {
            const initialProfile = {
                email: 'test@example.com',
                name: 'Old Name',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            const updatedProfile = {
                email: 'test@example.com',
                name: 'New Name',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet
                .mockResolvedValueOnce({ data: initialProfile })
                .mockResolvedValueOnce({ data: updatedProfile });
            mockApiPost.mockResolvedValue({});

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(initialProfile));
            });

            const updateButton = screen.getByTestId('update-btn');
            await act(async () => {
                updateButton.click();
            });

            await waitFor(() => {
                expect(mockApiPost).toHaveBeenCalledWith('/api/protected/profile', {
                    name: 'New Name',
                    password: 'newpass',
                    currentPassword: 'oldpass',
                });
            });

            await waitFor(() => {
                expect(mockApiGet).toHaveBeenCalledTimes(2);
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(updatedProfile));
            });
        });

        it('should throw error when not authenticated', async () => {  
            mockGetAccessToken.mockReturnValue(null);
            mockIsJwtExpired.mockReturnValue(true);

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const updateButton = screen.getByTestId('update-btn');
            await act(async () => {
                updateButton.click();
            });

            await waitFor(() => {
                expect(screen.getByTestId('updateError')).toHaveTextContent('Not authenticated');
            });
        });

        it('should handle update error', async () => {
            const mockProfile = {
                email: 'test@example.com',
                name: 'Test User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });
            mockApiPost.mockRejectedValue(new Error('Update failed'));

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(mockProfile));
            });

            const updateButton = screen.getByTestId('update-btn');
            await act(async () => {
                updateButton.click();
            });

            await waitFor(() => {
                expect(screen.getByTestId('updateError')).toHaveTextContent('Update failed');
            });
        });

        it('should set isLoading during update', async () => {
            const mockProfile = {
                email: 'test@example.com',
                name: 'Test User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            let resolvePost: (value: any) => void;
            const postPromise = new Promise((resolve) => {
                resolvePost = resolve;
            });

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });
            mockApiPost.mockReturnValue(postPromise);

            render(
                <TestWrapper>
                    <TestComponent />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(mockProfile));
            });

            const updateButton = screen.getByTestId('update-btn');
            await act(async () => {
                updateButton.click();
            });

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('true');
            });

            await act(async () => {
                resolvePost!({});
            });
            mockApiGet.mockResolvedValue({ data: mockProfile });

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });
        });
    });

    describe('useProfile hook', () => {
        it('should throw error when used outside ProfileProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            expect(() => {
                render(<TestComponent />);
            }).toThrow('useProfile must be used within a ProfileProvider');

            consoleSpy.mockRestore();
        });
    });

    describe('Authentication state changes', () => {
        it('should clear profile when authentication becomes false', async () => {
            const mockProfile = {
                email: 'test@example.com',
                name: 'Test User',
                roles: ['ROLE_USER'],
                authProvider: 'LOCAL' as const,
            };

            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);
            mockApiGet.mockResolvedValue({ data: mockProfile });

            const TestComponentWithLogout: React.FC = () => {
                const { profile, isLoading, error, isAdmin } = useProfile();
                const { logout } = useAuth();
                
                return (
                    <div>
                        <div data-testid="profile">{profile ? JSON.stringify(profile) : 'null'}</div>
                        <div data-testid="isLoading">{isLoading ? 'true' : 'false'}</div>
                        <div data-testid="error">{error || 'null'}</div>
                        <div data-testid="isAdmin">{isAdmin ? 'true' : 'false'}</div>
                        <button data-testid="logout-btn" onClick={logout}>Logout</button>
                    </div>
                );
            };

            render(
                <TestWrapper>
                    <TestComponentWithLogout />
                </TestWrapper>
            );

            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 100));
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent(JSON.stringify(mockProfile));
            });

            // Change authentication state by calling logout
            mockGetAccessToken.mockReturnValue(null);
            mockIsJwtExpired.mockReturnValue(true);
            
            const logoutButton = screen.getByTestId('logout-btn');
            await act(async () => {
                logoutButton.click();
            });

            // Wait for AuthContext to update and ProfileContext to clear profile
            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 200));
            });

            await waitFor(() => {
                expect(screen.getByTestId('profile')).toHaveTextContent('null');
            }, { timeout: 5000 });
        });
    });
});

