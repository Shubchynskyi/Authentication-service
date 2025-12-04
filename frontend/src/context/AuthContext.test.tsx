import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';
import api from '../api';
import * as tokenUtils from '../utils/token';

// Create mocks using vi.hoisted() to avoid hoisting issues
const { mockApiPost, mockApiDefaults, mockGetAccessToken, mockGetRefreshToken, mockIsJwtExpired, mockClearTokens } = vi.hoisted(() => {
    const mockApiPost = vi.fn();
    const mockApiDefaults = {
        headers: {
            common: {} as Record<string, string>,
        },
    };
    const mockGetAccessToken = vi.fn();
    const mockGetRefreshToken = vi.fn();
    const mockIsJwtExpired = vi.fn();
    const mockClearTokens = vi.fn();
    
    return { mockApiPost, mockApiDefaults, mockGetAccessToken, mockGetRefreshToken, mockIsJwtExpired, mockClearTokens };
});

// Mock api
vi.mock('../api', () => ({
    default: {
        post: mockApiPost,
        defaults: mockApiDefaults,
        get: vi.fn(),
    },
}));

// Mock token utils
vi.mock('../utils/token', () => ({
    getAccessToken: () => mockGetAccessToken(),
    getRefreshToken: () => mockGetRefreshToken(),
    isJwtExpired: (token: string) => mockIsJwtExpired(token),
    clearTokens: () => mockClearTokens(),
}));

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
}));

// Test component that uses AuthContext
const TestComponent: React.FC = () => {
    const { isAuthenticated, login, logout, error, isLoading, setTokens } = useAuth();
    
    const handleLogin = async () => {
        try {
            await login('test@example.com', 'password');
        } catch (err) {
            // Error is handled by AuthContext, just catch to prevent unhandled rejection
        }
    };
    
    return (
        <div>
            <div data-testid="isAuthenticated">{isAuthenticated ? 'true' : 'false'}</div>
            <div data-testid="isLoading">{isLoading ? 'true' : 'false'}</div>
            <div data-testid="error">{error || 'null'}</div>
            <button data-testid="login-btn" onClick={handleLogin}>
                Login
            </button>
            <button data-testid="logout-btn" onClick={logout}>
                Logout
            </button>
            <button data-testid="set-tokens-btn" onClick={() => setTokens('access-token', 'refresh-token')}>
                Set Tokens
            </button>
        </div>
    );
};

describe('AuthContext', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        mockApiDefaults.headers.common = {};
        mockGetAccessToken.mockReturnValue(null);
        mockGetRefreshToken.mockReturnValue(null);
        mockIsJwtExpired.mockReturnValue(true);
        mockClearTokens.mockImplementation(() => {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
        });
        
        // Mock window.location.href
        delete (window as any).location;
        (window as any).location = { href: '' };
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });

    describe('Initialization', () => {
        it('should set authenticated to false when no tokens exist', async () => {
            mockGetAccessToken.mockReturnValue(null);
            mockGetRefreshToken.mockReturnValue(null);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            expect(mockClearTokens).toHaveBeenCalled();
        });

        it('should set authenticated to true when valid access token exists', async () => {
            const validToken = 'valid.access.token';
            mockGetAccessToken.mockReturnValue(validToken);
            mockGetRefreshToken.mockReturnValue(null);
            mockIsJwtExpired.mockReturnValue(false);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            expect(mockApiDefaults.headers.common['Authorization']).toBe('Bearer valid.access.token');
        });

        it('should set authenticated to false when access token is expired', async () => {
            const expiredToken = 'expired.access.token';
            mockGetAccessToken.mockReturnValue(expiredToken);
            mockGetRefreshToken.mockReturnValue('refresh-token');
            mockIsJwtExpired.mockReturnValue(true);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
        });

        it('should clear tokens when no valid tokens exist', async () => {
            mockGetAccessToken.mockReturnValue(null);
            mockGetRefreshToken.mockReturnValue(null);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            expect(mockClearTokens).toHaveBeenCalled();
        });
    });

    describe('login', () => {
        it('should successfully login and set tokens', async () => {
            const mockResponse = {
                data: {
                    accessToken: 'new-access-token',
                    refreshToken: 'new-refresh-token',
                },
            };
            mockApiPost.mockResolvedValue(mockResponse);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const loginButton = screen.getByTestId('login-btn');
            loginButton.click();

            await waitFor(() => {
                expect(mockApiPost).toHaveBeenCalledWith('/api/auth/login', {
                    email: 'test@example.com',
                    password: 'password',
                });
            });

            await waitFor(() => {
                expect(localStorage.getItem('accessToken')).toBe('new-access-token');
                expect(localStorage.getItem('refreshToken')).toBe('new-refresh-token');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBe('Bearer new-access-token');
            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
        });

        it('should handle login error with string error message', async () => {
            const mockError = {
                isAxiosError: true,
                response: {
                    data: 'Invalid credentials',
                },
            };
            mockApiPost.mockRejectedValue(mockError);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const loginButton = screen.getByTestId('login-btn');
            
            // Click and wait for error to be handled
            loginButton.click();

            await waitFor(() => {
                expect(screen.getByTestId('error')).toHaveTextContent('Invalid credentials');
            }, { timeout: 5000 });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
        });

        it('should handle login error with object error message', async () => {
            const mockError = {
                isAxiosError: true,
                response: {
                    data: {
                        message: 'Email not found',
                    },
                },
            };
            mockApiPost.mockRejectedValue(mockError);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const loginButton = screen.getByTestId('login-btn');
            
            // Click and wait for error to be handled
            loginButton.click();

            await waitFor(() => {
                expect(screen.getByTestId('error')).toHaveTextContent('Email not found');
            }, { timeout: 5000 });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
        });

        it('should handle login error with missing tokens in response', async () => {
            const mockResponse = {
                data: {
                    accessToken: 'token',
                    // Missing refreshToken
                },
            };
            mockApiPost.mockResolvedValue(mockResponse);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const loginButton = screen.getByTestId('login-btn');
            
            // The error is thrown but not caught as axios error, so it becomes "Unexpected error"
            loginButton.click();

            await waitFor(() => {
                expect(screen.getByTestId('error')).toHaveTextContent('Unexpected error');
            }, { timeout: 5000 });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
        });

        it('should handle unexpected error', async () => {
            const mockError = new Error('Network error');
            mockApiPost.mockRejectedValue(mockError);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const loginButton = screen.getByTestId('login-btn');
            
            // Click and wait for error to be handled
            loginButton.click();

            await waitFor(() => {
                expect(screen.getByTestId('error')).toHaveTextContent('Unexpected error');
            }, { timeout: 5000 });

            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
        });
    });

    describe('logout', () => {
        it('should clear tokens and redirect on logout', async () => {
            localStorage.setItem('accessToken', 'token');
            localStorage.setItem('refreshToken', 'refresh');
            mockApiDefaults.headers.common['Authorization'] = 'Bearer token';

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const logoutButton = screen.getByTestId('logout-btn');
            logoutButton.click();

            expect(mockClearTokens).toHaveBeenCalled();
            expect(mockApiDefaults.headers.common['Authorization']).toBeUndefined();
            expect(window.location.href).toBe('/');
        });
    });

    describe('setTokens', () => {
        it('should set tokens and update auth state', async () => {
            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            const setTokensButton = screen.getByTestId('set-tokens-btn');
            setTokensButton.click();

            await waitFor(() => {
                expect(localStorage.getItem('accessToken')).toBe('access-token');
                expect(localStorage.getItem('refreshToken')).toBe('refresh-token');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBe('Bearer access-token');
            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            expect(screen.getByTestId('error')).toHaveTextContent('null');
        });
    });

    describe('Cross-tab synchronization', () => {
        it('should update auth state when token is added in another tab', async () => {
            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            // Simulate storage event from another tab
            mockGetAccessToken.mockReturnValue('new-token');
            mockGetRefreshToken.mockReturnValue('new-refresh');
            mockIsJwtExpired.mockReturnValue(false);

            const storageEvent = new StorageEvent('storage', {
                key: 'accessToken',
                oldValue: null,
                newValue: 'new-token',
            });
            window.dispatchEvent(storageEvent);

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBe('Bearer new-token');
        });

        it('should update auth state when token is removed in another tab', async () => {
            mockGetAccessToken.mockReturnValue('token');
            mockGetRefreshToken.mockReturnValue('refresh');
            mockIsJwtExpired.mockReturnValue(false);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            });

            // Simulate token removal in another tab
            mockGetAccessToken.mockReturnValue(null);
            mockGetRefreshToken.mockReturnValue(null);

            const storageEvent = new StorageEvent('storage', {
                key: 'accessToken',
                oldValue: 'token',
                newValue: null,
            });
            window.dispatchEvent(storageEvent);

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBeUndefined();
        });

        it('should handle expired access token with refresh token', async () => {
            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            // Simulate expired access token but refresh token exists
            mockGetAccessToken.mockReturnValue('expired-token');
            mockGetRefreshToken.mockReturnValue('refresh-token');
            mockIsJwtExpired.mockReturnValue(true);

            const storageEvent = new StorageEvent('storage', {
                key: 'accessToken',
                oldValue: 'token',
                newValue: 'expired-token',
            });
            window.dispatchEvent(storageEvent);

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBeUndefined();
        });
    });

    describe('useAuth hook', () => {
        it('should throw error when used outside AuthProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            expect(() => {
                render(<TestComponent />);
            }).toThrow('useAuth must be used within an AuthProvider');

            consoleSpy.mockRestore();
        });
    });
});

