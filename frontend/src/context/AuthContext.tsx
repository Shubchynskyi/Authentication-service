import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import api from '../api';
import axios from 'axios';
import { isJwtExpired, clearTokens, getAccessToken, setTokens as setAccessToken } from '../utils/token';
import { broadcastAuthEvent, subscribeAuthEvents } from '../utils/authEvents';

interface AuthContextType {
    isAuthenticated: boolean;
    login: (
        email: string,
        password: string,
        options?: { rememberDevice?: boolean; rememberDays?: number }
    ) => Promise<void>;
    logout: () => Promise<void>;
    error: string | null;
    isLoading: boolean;
    setTokens: (accessToken: string) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Check tokens on initialization
    useEffect(() => {
        let isMounted = true;

        const initAuth = async () => {
            const accessToken = getAccessToken();
            try {
                await api.get('/api/auth/csrf');
            } catch {
                // ignore
            }

            if (accessToken && !isJwtExpired(accessToken)) {
                api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                if (isMounted) {
                    setIsAuthenticated(true);
                    setIsLoading(false);
                }
                return;
            }

            try {
                const response = await api.post('/api/auth/refresh');
                const { accessToken: newAccessToken } = response.data || {};
                if (typeof newAccessToken === 'string' && newAccessToken.length > 0) {
                    setAccessToken(newAccessToken);
                    api.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;
                    if (isMounted) {
                        setIsAuthenticated(true);
                    }
                } else {
                    clearTokens();
                    delete api.defaults.headers.common['Authorization'];
                    if (isMounted) {
                        setIsAuthenticated(false);
                    }
                }
            } catch {
                clearTokens();
                delete api.defaults.headers.common['Authorization'];
                if (isMounted) {
                    setIsAuthenticated(false);
                }
            } finally {
                if (isMounted) {
                    setIsLoading(false);
                }
            }
        };

        initAuth();

        const unsubscribe = subscribeAuthEvents(async (event) => {
            if (event.type === 'logout') {
                clearTokens();
                delete api.defaults.headers.common['Authorization'];
                setIsAuthenticated(false);
                return;
            }

            const current = getAccessToken();
            if (current && !isJwtExpired(current)) {
                api.defaults.headers.common['Authorization'] = `Bearer ${current}`;
                setIsAuthenticated(true);
                return;
            }

            try {
                const response = await api.post('/api/auth/refresh');
                const { accessToken: refreshedAccessToken } = response.data || {};
                if (typeof refreshedAccessToken === 'string' && refreshedAccessToken.length > 0) {
                    setAccessToken(refreshedAccessToken);
                    api.defaults.headers.common['Authorization'] = `Bearer ${refreshedAccessToken}`;
                    setIsAuthenticated(true);
                }
            } catch {
                clearTokens();
                delete api.defaults.headers.common['Authorization'];
                setIsAuthenticated(false);
            }
        });

        return () => {
            isMounted = false;
            unsubscribe();
        };
    }, []);

    const login = useCallback(async (
        email: string,
        password: string,
        options?: { rememberDevice?: boolean; rememberDays?: number }
    ) => {
        setIsLoading(true);
        setError(null);
        try {
            let response: { data?: { accessToken?: string } } | undefined;
            try {
                response = await api.post<{ accessToken: string }>(
                    '/api/auth/login',
                    {
                        email,
                        password,
                        rememberDevice: options?.rememberDevice ?? false,
                        rememberDays: options?.rememberDays,
                    }
                );
            } catch (error) {
                // Only log critical/unexpected errors, not authentication failures
                if (axios.isAxiosError(error)) {
                    const errorData = error.response?.data;
                    if (typeof errorData === 'string') {
                        setError(errorData);
                    } else if (errorData && typeof errorData.message === 'string') {
                        setError(errorData.message);
                    } else {
                        setError('Error while logging in');
                    }

                    // Log only server errors or network failures (async import to avoid blocking)
                    if (!error.response || error.response.status >= 500) {
                        import('../utils/logger').then(({ logError }) => {
                            logError('Login request failed', error, {
                                hasResponse: !!error.response,
                                status: error.response?.status,
                            });
                        });
                    }
                } else {
                    import('../utils/logger').then(({ logError }) => {
                        logError('Unexpected login error', error);
                    });
                    setError('Unexpected error');
                }
                throw error;
            }

            const accessToken = response?.data?.accessToken;
            if (!accessToken) {
                const invalidResponseError = new Error('Invalid response from server: missing access token');
                import('../utils/logger').then(({ logError }) => {
                    logError('Unexpected login error', invalidResponseError);
                });
                setError('Unexpected error');
                throw invalidResponseError;
            }

            setAccessToken(accessToken);

            // Update API headers and auth state
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            setIsAuthenticated(true);
            broadcastAuthEvent('login');
        } finally {
            setIsLoading(false);
        }
    }, []);

    const logout = useCallback(async () => {
        // Clear local state first
        clearTokens();
        delete api.defaults.headers.common['Authorization'];
        setIsAuthenticated(false);
        broadcastAuthEvent('logout');

        // Wait for server to clear refresh token cookie
        try {
            await api.post('/api/auth/logout');
        } catch {
            // Ignore errors - cookie might already be cleared
        }

        // Redirect after cookie is cleared
        if (typeof window !== 'undefined') {
            const target = '/';
            if (typeof window.location.replace === 'function') {
                window.location.replace(target);
            } else {
                window.location.href = target;
            }
        }
    }, []);

    const setTokens = useCallback((accessToken: string) => {
        setAccessToken(accessToken);
        // Update API headers and auth state
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        setIsAuthenticated(true);
        setError(null);
        broadcastAuthEvent('login');
    }, []);

    return (
        <AuthContext.Provider value={{ isAuthenticated, login, logout, error, isLoading, setTokens }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}; 
