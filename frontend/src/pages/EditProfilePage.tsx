import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
    Box,
    TextField,
    Button,
    Typography,
    Paper,
    CircularProgress,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useProfile } from '../context/ProfileContext';
import { useNotification } from '../context/NotificationContext';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
}));

const EditProfilePage: React.FC = () => {
    const navigate = useNavigate();
    const { profile, updateProfile, isLoading } = useProfile();
    const { showNotification } = useNotification();
    
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
            showNotification('Current password is required to change password', 'error');
            return;
        }

        try {
            await updateProfile({
                name,
                password: newPassword,
                currentPassword: currentPassword,
            });
            showNotification('Profile updated successfully', 'success');
            navigate('/profile', { replace: true });
        } catch (error) {
            showNotification('Error updating profile', 'error');
        }
    };

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
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
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : 'Save Changes'}
                    </Button>
                    <Button
                        variant="outlined"
                        fullWidth
                        component={Link}
                        to="/profile"
                    >
                        Back to Profile
                    </Button>
                </form>
            </StyledPaper>
        </Box>
    );
};

export default EditProfilePage;