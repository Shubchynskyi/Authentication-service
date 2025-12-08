import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Box, TextField, Button, Typography, Paper, CircularProgress } from '@mui/material';
import { styled } from '@mui/material/styles';
import api from '../api';
import axios from 'axios';
import { useNotification } from '../context/NotificationContext';
import { useTranslation } from 'react-i18next';
import { validatePassword } from '../utils/passwordValidation';
import PasswordHint from '../components/PasswordHint';

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

const ResetPasswordPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { showNotification } = useNotification();
    const { t, i18n } = useTranslation();
    const [token, setToken] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // Recalculate password error when language changes
    useEffect(() => {
        if (newPassword) {
            const error = validatePassword(newPassword, t);
            setPasswordError(error);
        }
    }, [i18n.language, newPassword, t]);

    useEffect(() => {
        const searchParams = new URLSearchParams(location.search);
        const tokenFromUrl = searchParams.get('token');
        if (tokenFromUrl) {
            setToken(tokenFromUrl);
        } else {
            showNotification(t('auth.verification.invalidLink'), 'error');
        }
    }, [location, showNotification, t]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!newPassword) {
            showNotification(t('errors.passwordRequired'), 'error');
            return;
        }

        const passwordValidationError = validatePassword(newPassword, t);
        if (passwordValidationError) {
            setPasswordError(passwordValidationError);
            showNotification(passwordValidationError, 'error');
            return;
        }
        setPasswordError('');

        if (newPassword !== confirmPassword) {
            showNotification(t('errors.passwordMismatch'), 'error');
            return;
        }

        setIsLoading(true);
        try {
            await api.post('/api/auth/reset-password', {
                token,
                newPassword,
                confirmPassword,
            });
            showNotification(t('auth.passwordResetSuccess'), 'success');
            navigate('/login', { replace: true });
        } catch (error) {
            let message = t('common.error');
            if (axios.isAxiosError(error) && error.response?.data) {
                const data = error.response.data;
                // Handle both object response {error, message} and string response
                if (typeof data === 'object' && data.message) {
                    message = data.message;
                } else if (typeof data === 'string') {
                    message = data;
                }
            }
            showNotification(message, 'error');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    {t('common.resetPassword')}
                </Typography>
                <form onSubmit={handleSubmit} style={{ width: '100%' }}>
                    <TextField
                        label={t('common.newPassword')}
                        fullWidth
                        margin="normal"
                        type="password"
                        value={newPassword}
                        onChange={(e) => {
                            setNewPassword(e.target.value);
                            if (e.target.value) {
                                const error = validatePassword(e.target.value, t);
                                setPasswordError(error);
                            } else {
                                setPasswordError('');
                            }
                        }}
                        error={!!passwordError}
                        helperText={passwordError || ''}
                        required
                    />
                    <PasswordHint text={t('errors.passwordRequirements')} />
                    <TextField
                        label={t('common.confirmPassword')}
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
                        {isLoading ? <CircularProgress size={24} /> : t('common.resetPassword')}
                    </Button>
                </form>
            </StyledPaper>
        </Box>
    );
};

export default ResetPasswordPage;