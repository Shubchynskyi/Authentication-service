// AdminSection.tsx
import React from 'react';
import { Box, CircularProgress } from '@mui/material';
import { useProfile } from '../context/ProfileContext';

interface AdminSectionProps {
    children: React.ReactNode;
}

const AdminSection: React.FC<AdminSectionProps> = ({ children }) => {
    const { isAdmin, isLoading } = useProfile();

    if (isLoading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}><CircularProgress /></Box>;
    }

    return isAdmin ? <>{children}</> : null;
};

export default AdminSection;