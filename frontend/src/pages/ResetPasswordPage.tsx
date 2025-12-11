import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Box, Button, Typography, CircularProgress } from '@mui/material';
import api from '../api';
import { useNotification } from '../context/NotificationContext';
import { useTranslation } from 'react-i18next';
import { validatePassword } from '../utils/passwordValidation';
import { validatePasswordFlow } from '../utils/passwordChecks';
import { extractErrorMessage } from '../utils/apiError';
import { getQueryParam } from '../utils/queryParams';
import PasswordHint from '../components/PasswordHint';
import PasswordFields from '../components/PasswordFields';
import FormPaper from '../components/FormPaper';

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
        const tokenFromUrl = getQueryParam(location.search, 'token');
        if (tokenFromUrl) {
            setToken(tokenFromUrl);
        } else {
            showNotification(t('auth.verification.invalidLink'), 'error');
        }
    }, [location, showNotification, t]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        const strengthError = newPassword ? validatePassword(newPassword, t) : '';
        const passwordCheckError = validatePasswordFlow({
            password: newPassword,
            confirmPassword,
            t,
            requirePassword: true,
        });
        if (passwordCheckError) {
            setPasswordError(strengthError || '');
            showNotification(passwordCheckError, 'error');
            return;
        }
        setPasswordError('');

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
            const message = extractErrorMessage(error, { fallbackMessage: t('common.error') });
            showNotification(message, 'error');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <FormPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    {t('common.resetPassword')}
                </Typography>
                <form onSubmit={handleSubmit} style={{ width: '100%' }}>
                    <PasswordFields
                        passwordLabel={t('common.newPassword')}
                        confirmLabel={t('common.confirmPassword')}
                        password={newPassword}
                        confirmPassword={confirmPassword}
                        onPasswordChange={(value) => {
                            setNewPassword(value);
                            if (value) {
                                const error = validatePassword(value, t);
                                setPasswordError(error);
                            } else {
                                setPasswordError('');
                            }
                        }}
                        onConfirmPasswordChange={setConfirmPassword}
                        passwordError={passwordError}
                        passwordHelperText={passwordError || ''}
                        passwordRequired
                        confirmRequired
                        renderHint={<PasswordHint text={t('errors.passwordRequirements')} />}
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
            </FormPaper>
        </Box>
    );
};

export default ResetPasswordPage;