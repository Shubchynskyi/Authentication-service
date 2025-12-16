import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import api from '../api';
import axios from 'axios';
import { isJwtExpired, clearTokens, getAccessToken, getRefreshToken } from '../utils/token';

interface AuthContextType {
    isAuthenticated: boolean;
    login: (email: string, password: string) => Promise<void>;
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
        const accessToken = getAccessToken();
        const refreshToken = getRefreshToken();

        if (accessToken && !isJwtExpired(accessToken)) {
            // Valid access token exists
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            setIsAuthenticated(true);
        } else if (refreshToken) {
            // Access token expired/missing but refresh token exists
            // Token will be refreshed automatically on first API call via interceptor
            // For now, we don't set authenticated to avoid race conditions
            // The interceptor will handle token refresh and update state
            setIsAuthenticated(false);
        } else {
            // No valid tokens
            clearTokens();
            delete api.defaults.headers.common['Authorization'];
            setIsAuthenticated(false);
        }

        setIsLoading(false);

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
        return () => window.removeEventListener('storage', onStorage);
    }, []);

    const login = useCallback(async (email: string, password: string) => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await api.post<{ accessToken: string; refreshToken: string }>(
                '/api/auth/login',
                { email, password }
            );
            
            // Validate response data
            if (!response.data || !response.data.accessToken || !response.data.refreshToken) {
                throw new Error('Invalid response from server: missing tokens');
            }
            
            const { accessToken, refreshToken } = response.data;
            
            // Set tokens atomically to avoid race conditions
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            
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
        // Set new tokens (overwrites any existing values)
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        
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