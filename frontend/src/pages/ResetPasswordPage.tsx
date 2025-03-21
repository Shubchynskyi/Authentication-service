import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Box, TextField, Button, Typography, Alert, Paper, CircularProgress } from '@mui/material';
import { styled } from '@mui/material/styles';
import api from '../api';
import axios from 'axios';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
}));

const ResetPasswordPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [token, setToken] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [messageType, setMessageType] = useState<'error' | 'success'>('success');

    useEffect(() => {
        const searchParams = new URLSearchParams(location.search);
        const tokenFromUrl = searchParams.get('token');
        if (tokenFromUrl) {
            setToken(tokenFromUrl);
        } else {
            setMessage('Invalid reset link.');
            setMessageType('error')
        }
    }, [location]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (newPassword !== confirmPassword) {
            setMessage('Passwords do not match.');
            setMessageType('error')
            return;
        }

        setIsLoading(true);
        try {
            await api.post('/api/auth/reset-password', {
                token,
                newPassword,
                confirmPassword,
            });
            setMessage('Your password has been reset successfully.');
            setMessageType('success');
            navigate('/login', { replace: true });
        } catch (error) {
            let message = 'An error occurred. Please try again.';
            if (axios.isAxiosError(error)) {
                message = error.response?.data || 'An error occurred. Please try again.';
            }

            setMessage(message);
            setMessageType('error')
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    Reset Password
                </Typography>
                <form onSubmit={handleSubmit} style={{ width: '100%' }}>
                    <TextField
                        label="New Password"
                        fullWidth
                        margin="normal"
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        required
                    />
                    <TextField
                        label="Confirm Password"
                        fullWidth
                        margin="normal"
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : 'Reset Password'}
                    </Button>
                </form>
                {message && (
                    <Alert severity={messageType} sx={{ mt: 2, width: '100%' }}>
                        {message}
                    </Alert>
                )}
            </StyledPaper>
        </Box>
    );
};

export default ResetPasswordPage;