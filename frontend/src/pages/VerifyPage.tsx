import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
    Box,
    Typography,
    Container,
    CircularProgress,
    Alert,
} from '@mui/material';
import api from '../services/api';  

const VerifyPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        const verifyEmail = async () => {
            try {
                const verificationToken = searchParams.get('verificationToken');
                const email = searchParams.get('email');

                if (!verificationToken || !email) {
                    setStatus('error');
                    setErrorMessage('Invalid confirmation link');
                    return;
                }

                await api.post('/api/auth/verify', {
                    email,
                    code: verificationToken
                });

                setStatus('success');
                setTimeout(() => {
                    navigate('/login', {
                        state: { message: 'Email successfully verified. Now you can login.' }
                    });
                }, 2000);
            } catch (error: any) {
                setStatus('error');
                setErrorMessage(error.response?.data || 'Error verifying email');
            }
        };

        verifyEmail();
    }, [navigate, searchParams]);

    return (
        <Container component="main" maxWidth="xs">
            <Box sx={{
                marginTop: 8,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 2
            }}>
                <Typography component="h1" variant="h5">
                    Email verification
                </Typography>

                {status === 'loading' && (
                    <>
                        <CircularProgress />
                        <Typography>
                            Verifying your email...
                        </Typography>
                    </>
                )}

                {status === 'success' && (
                    <Alert severity="success">
                        Email successfully verified! Redirecting to login page...
                    </Alert>
                )}

                {status === 'error' && (
                    <Alert severity="error">
                        {errorMessage}
                    </Alert>
                )}
            </Box>
        </Container>
    );
};

export default VerifyPage; 