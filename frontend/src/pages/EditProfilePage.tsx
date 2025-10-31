import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
    Box,
    TextField,
    Button,
    Typography,
    Paper,
    CircularProgress,
    Alert,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useProfile } from '../context/ProfileContext';
import { useNotification } from '../context/NotificationContext';
import axios from 'axios';
import { useTranslation } from 'react-i18next';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
    width: '100%',
    maxWidth: 480,
}));

const EditProfilePage: React.FC = () => {
    const navigate = useNavigate();
    const { profile, updateProfile, isLoading } = useProfile();
    const { showNotification } = useNotification();
    const { t } = useTranslation();
    
    const [name, setName] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');

    useEffect(() => {
        if (profile?.name) {
            setName(profile.name);
        }
    }, [profile]);

    const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (newPassword && !currentPassword) {
            showNotification(t('profile.notifications.currentPasswordRequired'), 'error');
            return;
        }

        try {
            await updateProfile({
                name,
                password: newPassword,
                currentPassword: currentPassword,
            });
            showNotification(t('profile.notifications.updateSuccess'), 'success');
            navigate('/profile', { replace: true });
        } catch (error) {
            if (axios.isAxiosError(error)) {
                const msg = String(error.response?.data || '').toLowerCase();
                if (msg.includes('incorrect current password') || msg.includes('wrong current password')) {
                    showNotification(t('profile.notifications.incorrectCurrentPassword'), 'error');
                } else {
                    showNotification(t('profile.notifications.updateError'), 'error');
                }
            } else {
                showNotification(t('profile.notifications.updateError'), 'error');
            }
        }
    };

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    const isGoogleUser = profile?.authProvider === 'GOOGLE';

    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    Edit Profile
                </Typography>
                <form onSubmit={handleUpdate} style={{ width: '100%' }}>
                    <TextField
                        label="Name"
                        fullWidth
                        margin="normal"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                    />
                    {isGoogleUser ? (
                        <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
                            {t('profile.googleAuthInfo')}
                        </Alert>
                    ) : (
                        <>
                            <TextField
                                label="Current Password"
                                fullWidth
                                margin="normal"
                                type="password"
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                required={!!newPassword}
                            />
                            <TextField
                                label="New Password (optional)"
                                fullWidth
                                margin="normal"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                            />
                        </>
                    )}
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : t('common.save')}
                    </Button>
                    <Button
                        variant="outlined"
                        fullWidth
                        component={Link}
                        to="/"
                        sx={{ mb: 2 }}
                    >
                        {t('notFound.backHome')}
                    </Button>
                    <Button
                        variant="outlined"
                        fullWidth
                        component={Link}
                        to="/profile"
                    >
                        {t('profile.backToProfile')}
                    </Button>
                </form>
            </StyledPaper>
        </Box>
    );
};

export default EditProfilePage;