// AdminSection.tsx
import React, { useState, useEffect } from 'react';
import api from '../api';
import { CircularProgress, Box } from '@mui/material';
 import axios from 'axios';

interface AdminSectionProps {
    children: React.ReactNode;
}

const AdminSection: React.FC<AdminSectionProps> = ({ children }) => {
    const [isAdmin, setIsAdmin] = useState<boolean | null>(null);

    useEffect(() => {
        let isCancelled = false;

        const checkAdmin = async () => {
            try {
                const response = await api.get('/api/protected/profile');
                const userRoles: string[] = response.data.roles;

                if (!isCancelled) {
                    setIsAdmin(userRoles.includes('ROLE_ADMIN'));
                }
            } catch (error) {
                if (!isCancelled) {
                    setIsAdmin(false);
                    if (axios.isAxiosError(error) && error.response?.status === 401) {

                    }
                }
            }
        };
        checkAdmin();
        return () => { isCancelled = true; }
    }, []);

    if (isAdmin === null) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}><CircularProgress /></Box>;
    }

    return isAdmin ? <>{children}</> : null;
};

export default AdminSection;