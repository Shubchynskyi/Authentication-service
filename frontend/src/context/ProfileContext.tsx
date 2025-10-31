import React, { createContext, useContext, useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import api from '../api';

interface ProfileData {
    email: string;
    name: string;
    roles: string[];
    authProvider: 'LOCAL' | 'GOOGLE';
}

interface UpdateProfileData {
    name: string;
    password?: string;
    currentPassword?: string;
}

interface ProfileContextType {
    profile: ProfileData | null;
    isLoading: boolean;
    error: string | null;
    isAdmin: boolean;
    updateProfile: (data: UpdateProfileData) => Promise<void>;
}

const ProfileContext = createContext<ProfileContextType | undefined>(undefined);

export const ProfileProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated } = useAuth();
    const [profile, setProfile] = useState<ProfileData | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchProfile = async () => {
        if (!isAuthenticated) {
            setProfile(null);
            setError(null);
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        try {
            const response = await api.get<ProfileData>('/api/protected/profile');
            setProfile(response.data);
            setError(null);
        } catch (error) {
            setError('Failed to fetch profile');
            setProfile(null);
        } finally {
            setIsLoading(false);
        }
    };

    const updateProfile = async (data: UpdateProfileData) => {
        if (!isAuthenticated) {
            throw new Error('Not authenticated');
        }

        setIsLoading(true);
        try {
            await api.post('/api/protected/profile', data);
            await fetchProfile();
        } catch (error) {
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchProfile();
    }, [isAuthenticated]);

    const isAdmin = profile?.roles.includes('ROLE_ADMIN') || false;

    return (
        <ProfileContext.Provider value={{ profile, isLoading, error, isAdmin, updateProfile }}>
            {children}
        </ProfileContext.Provider>
    );
};

export const useProfile = () => {
    const context = useContext(ProfileContext);
    if (context === undefined) {
        throw new Error('useProfile must be used within a ProfileProvider');
    }
    return context;
}; 