import React from 'react';
import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';

// Create mocks using vi.hoisted() to avoid hoisting issues
let authEventHandler: ((event: { type: 'login' | 'logout'; at: number }) => void) | null = null;

const {
    mockApiPost,
    mockApiGet,
    mockApiDefaults,
    mockGetAccessToken,
    mockIsJwtExpired,
    mockClearTokens,
    mockSetTokens,
    mockBroadcastAuthEvent,
    mockSubscribeAuthEvents,
} = vi.hoisted(() => {
    const mockApiPost = vi.fn();
    const mockApiGet = vi.fn();
    const mockApiDefaults = {
        headers: {
            common: {} as Record<string, string>,
        },
    };
    const mockGetAccessToken = vi.fn();
    const mockIsJwtExpired = vi.fn();
    const mockClearTokens = vi.fn();
    const mockSetTokens = vi.fn();
    const mockBroadcastAuthEvent = vi.fn();
    const mockSubscribeAuthEvents = vi.fn((handler: (event: { type: 'login' | 'logout'; at: number }) => void) => {
        authEventHandler = handler;
        return () => {};
    });

    return {
        mockApiPost,
        mockApiGet,
        mockApiDefaults,
        mockGetAccessToken,
        mockIsJwtExpired,
        mockClearTokens,
        mockSetTokens,
        mockBroadcastAuthEvent,
        mockSubscribeAuthEvents,
    };
});

// Mock api
vi.mock('../api', () => ({
    default: {
        post: mockApiPost,
        get: mockApiGet,
        defaults: mockApiDefaults,
    },
}));

// Mock token utils
vi.mock('../utils/token', () => ({
    getAccessToken: () => mockGetAccessToken(),
    isJwtExpired: (token: string) => mockIsJwtExpired(token),
    clearTokens: () => mockClearTokens(),
    setTokens: (...args: any[]) => mockSetTokens(...args),
}));

vi.mock('../utils/authEvents', () => ({
    broadcastAuthEvent: (...args: any[]) => mockBroadcastAuthEvent(...args),
    subscribeAuthEvents: (handler: (event: { type: 'login' | 'logout'; at: number }) => void) =>
        mockSubscribeAuthEvents(handler),
}));

// Mock logger
vi.mock('../utils/logger', () => ({
    logError: vi.fn(),
    logWarn: vi.fn(),
    logInfo: vi.fn(),
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
            await login('test@example.com', 'password', { rememberDevice: false });
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
            <button data-testid="set-tokens-btn" onClick={() => setTokens('access-token')}>
                Set Tokens
            </button>
        </div>
    );
};

describe('AuthContext', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        authEventHandler = null;
        mockApiDefaults.headers.common = {};
        mockApiGet.mockResolvedValue({ data: {} });
        mockApiPost.mockResolvedValue({ data: {} });
        mockGetAccessToken.mockReturnValue(null);
        mockIsJwtExpired.mockReturnValue(true);
        mockClearTokens.mockImplementation(() => {});

        // Mock window.location.href
        delete (window as any).location;
        (window as any).location = { href: '' };
    });

    afterEach(() => {
        vi.clearAllMocks();
        cleanup();
    });

    describe('Initialization', () => {
        it('should set authenticated to false when no tokens exist', async () => {
            mockGetAccessToken.mockReturnValue(null);

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
                },
            };
            mockApiPost
                .mockResolvedValueOnce({ data: {} })
                .mockResolvedValueOnce(mockResponse);

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
                    rememberDevice: false,
                    rememberDays: undefined,
                });
            });

            await waitFor(() => {
                expect(mockSetTokens).toHaveBeenCalledWith('new-access-token');
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
                },
            };
            mockApiPost
                .mockResolvedValueOnce({ data: {} })
                .mockResolvedValueOnce(mockResponse);

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

            // logout is now async, wait for it to complete
            await waitFor(() => {
                expect(mockClearTokens).toHaveBeenCalled();
                expect(mockApiDefaults.headers.common['Authorization']).toBeUndefined();
                expect(window.location.href).toBe('/');
            });
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
                expect(mockSetTokens).toHaveBeenCalledWith('access-token');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBe('Bearer access-token');
            expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            expect(screen.getByTestId('error')).toHaveTextContent('null');
        });
    });

    describe('Cross-tab synchronization', () => {
        it('should update auth state on logout event', async () => {
            mockGetAccessToken.mockReturnValue('token');
            mockIsJwtExpired.mockReturnValue(false);

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            });

            authEventHandler?.({ type: 'logout', at: Date.now() });

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBeUndefined();
        });

        it('should refresh access token on login event', async () => {
            mockApiPost
                .mockResolvedValueOnce({ data: {} })
                .mockResolvedValueOnce({ data: { accessToken: 'new-token' } });

            render(
                <AuthProvider>
                    <TestComponent />
                </AuthProvider>
            );

            await waitFor(() => {
                expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
            });

            authEventHandler?.({ type: 'login', at: Date.now() });

            await waitFor(() => {
                expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
            });

            expect(mockApiDefaults.headers.common['Authorization']).toBe('Bearer new-token');
        });
    });

    describe('useAuth hook', () => {
        it('should throw error when used outside AuthProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });

            expect(() => {
                render(<TestComponent />);
            }).toThrow('useAuth must be used within an AuthProvider');

            consoleSpy.mockRestore();
        });
    });
});

