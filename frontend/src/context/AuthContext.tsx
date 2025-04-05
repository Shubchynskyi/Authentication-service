import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import api from '../api';
import axios from 'axios';

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
        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');
        if (accessToken && refreshToken) {
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            setIsAuthenticated(true);
        }
        setIsLoading(false);
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
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        delete api.defaults.headers.common['Authorization'];
        setIsAuthenticated(false);
        window.location.href = '/login';
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