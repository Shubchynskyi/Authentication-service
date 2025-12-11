import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
    Box,
    Typography,
    Container,
    CircularProgress
} from '@mui/material';
import api from '../api';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import FormPaper from '../components/FormPaper';

const VerifyPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
    const { t } = useTranslation();
    const { showNotification } = useNotification();

    useEffect(() => {
        const verifyEmail = async () => {
            try {
                const verificationToken = searchParams.get('verificationToken');
                const email = searchParams.get('email');

                if (!verificationToken || !email) {
                    setStatus('error');
                    showNotification(t('auth.verification.invalidLink'), 'error');
                    return;
                }

                await api.post('/api/auth/verify', {
                    email,
                    code: verificationToken
                });

                setStatus('success');
                showNotification(t('auth.verification.successRedirect'), 'success');
                setTimeout(() => {
                    navigate('/login', {
                        state: { message: t('auth.verificationSuccess') }
                    });
                }, 1500);
            } catch (error: any) {
                setStatus('error');
                showNotification(t('auth.verification.error'), 'error');
            }
        };

        verifyEmail();
    }, [navigate, searchParams, showNotification, t]);

    return (
        <Container component="main" maxWidth="sm">
            <Box sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
            }}>
                <FormPaper elevation={3}>
                    <Typography component="h1" variant="h5" gutterBottom>
                        {t('auth.verificationTitle')}
                    </Typography>
                    {status === 'loading' && (
                        <>
                            <CircularProgress />
                            <Typography sx={{ mt: 2 }}>
                                {t('auth.verification.verifying')}
                            </Typography>
                        </>
                    )}
                </FormPaper>
            </Box>
        </Container>
    );
};

export default VerifyPage; 