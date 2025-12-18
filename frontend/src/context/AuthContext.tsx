import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import api from '../api';
import axios from 'axios';
import { API_URL } from '../config';
import { isJwtExpired, clearTokens, getAccessToken, getRefreshToken, setTokens as persistTokens, getTokenStorageMode } from '../utils/token';

interface AuthContextType {
    isAuthenticated: boolean;
    login: (
        email: string,
        password: string,
        options?: { rememberDevice?: boolean; rememberDays?: number }
    ) => Promise<void>;
    logout: () => void;
    error: string | null;
    isLoading: boolean;
    setTokens: (accessToken: string, refreshToken: string) => void;
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
            const refreshToken = getRefreshToken();

            if (accessToken && !isJwtExpired(accessToken)) {
                // Valid access token exists
                api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                if (isMounted) {
                    setIsAuthenticated(true);
                    setIsLoading(false);
                }
                return;
            }

            if (refreshToken) {
                // Access token expired/missing but refresh token exists: refresh on init to avoid false logout.
                try {
                    const response = await axios.post(`${API_URL}/api/auth/refresh`, {
                        refreshToken,
                    });

                    const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data || {};
                    if (typeof newAccessToken === 'string' && newAccessToken.length > 0) {
                        persistTokens(newAccessToken, typeof newRefreshToken === 'string' && newRefreshToken.length > 0 ? newRefreshToken : refreshToken, getTokenStorageMode());
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

                return;
            }

            // No valid tokens
            clearTokens();
            delete api.defaults.headers.common['Authorization'];
            if (isMounted) {
                setIsAuthenticated(false);
                setIsLoading(false);
            }
        };

        initAuth();

        // Sync auth state across tabs
        const onStorage = (e: StorageEvent) => {
            if (e.key === 'accessToken' || e.key === 'refreshToken') {
                const at = getAccessToken();
                const rt = getRefreshToken();
                if (at && !isJwtExpired(at)) {
                    api.defaults.headers.common['Authorization'] = `Bearer ${at}`;
                    setIsAuthenticated(true);
                } else if (!rt) {
                    // No refresh token, clear everything
                    delete api.defaults.headers.common['Authorization'];
                    setIsAuthenticated(false);
                } else {
                    // Access token expired but refresh token exists
                    // Don't change state, let interceptor handle it
                    delete api.defaults.headers.common['Authorization'];
                    setIsAuthenticated(false);
                }
            }
        };
        window.addEventListener('storage', onStorage);
        return () => {
            isMounted = false;
            window.removeEventListener('storage', onStorage);
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
            const response = await api.post<{ accessToken: string; refreshToken: string }>(
                '/api/auth/login',
                {
                    email,
                    password,
                    rememberDevice: options?.rememberDevice ?? false,
                    rememberDays: options?.rememberDays,
                }
            );
            
            // Validate response data
            if (!response.data || !response.data.accessToken || !response.data.refreshToken) {
                throw new Error('Invalid response from server: missing tokens');
            }
            
            const { accessToken, refreshToken } = response.data;
            
            // Persist tokens using the currently selected storage mode.
            persistTokens(accessToken, refreshToken, getTokenStorageMode());
            
            // Update API headers and auth state
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            setIsAuthenticated(true);
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
        } finally {
            setIsLoading(false);
        }
    }, []);

    const logout = useCallback(() => {
        clearTokens();
        delete api.defaults.headers.common['Authorization'];
        setIsAuthenticated(false);
        if (typeof window !== 'undefined') {
            const target = '/';
            if (typeof window.location.replace === 'function') {
                window.location.replace(target);
            } else {
                window.location.href = target;
            }
        }
    }, []);

    const setTokens = useCallback((accessToken: string, refreshToken: string) => {
        // Set new tokens (overwrites any existing values) using the currently selected storage mode.
        persistTokens(accessToken, refreshToken, getTokenStorageMode());
        
        // Update API headers and auth state
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        setIsAuthenticated(true);
        setError(null);
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