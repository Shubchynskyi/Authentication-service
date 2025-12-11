// ForgotPasswordPage.tsx
import React, { useEffect, useMemo, useState } from 'react';
import { Box, TextField, Button, Typography, CircularProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import api from '../api';
import FormPaper from '../components/FormPaper';

const COOLDOWN_MINUTES = Number(import.meta.env.VITE_PASSWORD_RESET_COOLDOWN_MINUTES ?? 10);
const COOLDOWN_MS = COOLDOWN_MINUTES * 60 * 1000;

const ForgotPasswordPage: React.FC = () => {
    const { t } = useTranslation();
    const { showNotification } = useNotification();
    const [email, setEmail] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [cooldownMs, setCooldownMs] = useState(0);

    useEffect(() => {
        if (cooldownMs <= 0) {
            return;
        }
        const timerId = window.setInterval(() => {
            setCooldownMs((prev) => Math.max(0, prev - 1000));
        }, 1000);
        return () => window.clearInterval(timerId);
    }, [cooldownMs]);

    const formattedCooldown = useMemo(() => {
        const totalSeconds = Math.ceil(cooldownMs / 1000);
        const minutes = Math.floor(totalSeconds / 60)
            .toString()
            .padStart(2, '0');
        const seconds = (totalSeconds % 60).toString().padStart(2, '0');
        return `${minutes}:${seconds}`;
    }, [cooldownMs]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await api.post('/api/auth/forgot-password', { email });
            showNotification(t('auth.forgotPasswordSuccessGeneric'), 'success');
            setCooldownMs(COOLDOWN_MS);
        } catch {
            showNotification(t('common.error'), 'error');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <FormPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    {t('auth.forgotPasswordTitle')}
                </Typography>
                <Typography variant="body1" align="center" marginBottom={3}>
                    {t('auth.forgotPasswordDescription')}
                </Typography>
                <form onSubmit={handleSubmit} style={{ width: '100%' }}>
                    <TextField
                        label={t('common.email')}
                        fullWidth
                        margin="normal"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading || cooldownMs > 0}
                    >
                        {isLoading ? (
                            <CircularProgress size={24} />
                        ) : cooldownMs > 0 ? (
                            `${t('common.resetPassword')} (${formattedCooldown})`
                        ) : (
                            t('common.resetPassword')
                        )}
                    </Button>
                    {cooldownMs > 0 && (
                        <Typography variant="body2" color="text.secondary" textAlign="center">
                            {t('auth.forgotPasswordCooldownActive', { time: formattedCooldown })}
                        </Typography>
                    )}
                </form>
            </FormPaper>
        </Box>
    );
};

export default ForgotPasswordPage;