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
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            setIsAuthenticated(true);
        } else {
            // If access token is missing/expired, don't consider authenticated
            // Optionally, we could try silent refresh here if refresh exists
            if (!refreshToken) {
                clearTokens();
            }
            setIsAuthenticated(false);
        }

        setIsLoading(false);

        // Sync auth state across tabs
        const onStorage = (e: StorageEvent) => {
            if (e.key === 'accessToken' || e.key === 'refreshToken') {
                const at = getAccessToken();
                if (at && !isJwtExpired(at)) {
                    api.defaults.headers.common['Authorization'] = `Bearer ${at}`;
                    setIsAuthenticated(true);
                } else {
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
            const { accessToken, refreshToken } = response.data;
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            setIsAuthenticated(true);
        } catch (error) {
            if (axios.isAxiosError(error)) {
                console.log('Auth error:', error.response?.data);
                const errorData = error.response?.data;
                if (typeof errorData === 'string') {
                    setError(errorData);
                } else if (errorData && typeof errorData.message === 'string') {
                    setError(errorData.message);
                } else {
                    setError('Error	whileиloggingеinу');
                }
            } else {
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
        window.location.href = '/';
    }, []);

    const setTokens = useCallback((accessToken: string, refreshToken: string) => {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        setIsAuthenticated(true);
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