import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import axios from 'axios';
import {
    Box,
    TextField,
    Button,
    Typography,
    Container,
    Grid,
    Alert,
} from '@mui/material';
import { useTranslation } from 'react-i18next';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMsg, setErrorMsg] = useState('');
    const { t } = useTranslation();
    const location = useLocation();

    useEffect(() => {
        if (location.state?.error) {
            setErrorMsg(location.state.error);
        }
    }, [location]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        try {
            const response = await axios.post('http://localhost:8080/api/auth/login', {
                email,
                password
            });

            if (response.data.accessToken && response.data.refreshToken) {
                localStorage.setItem('accessToken', response.data.accessToken);
                localStorage.setItem('refreshToken', response.data.refreshToken);
                window.location.href = '/';
            }
        } catch (error: any) {
            console.error('Login error:', error);
            if (error.response?.data) {
                const errorMessage = error.response.data;
                if (errorMessage.includes('User not found') || errorMessage.includes('Incorrect password')) {
                    setErrorMsg(t('auth.loginError.invalidCredentials'));
                } else if (errorMessage.includes('Account is blocked')) {
                    const blockReason = errorMessage.split('Account is blocked.')[1]?.trim();
                    setErrorMsg(t('auth.loginError.accountBlocked') + 
                        (blockReason ? `: ${blockReason}` : ''));
                } else if (errorMessage.includes('Email not verified')) {
                    setErrorMsg(t('auth.loginError.emailNotVerified'));
                } else {
                    setErrorMsg(t('auth.loginError.generalError'));
                }
            } else {
                setErrorMsg(t('auth.loginError.generalError'));
            }
        }
    };

    return (
        <Container maxWidth="xs">
            <Box
                sx={{
                    marginTop: 8,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                <Typography component="h1" variant="h5">
                    {t('auth.loginTitle')}
                </Typography>
                
                {errorMsg && (
                    <Alert severity="error" sx={{ mt: 2, mb: 2, width: '100%' }}>
                        {errorMsg}
                    </Alert>
                )}
                
                <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        label={t('common.email')}
                        name="email"
                        autoComplete="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        error={!!errorMsg}
                    />
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        name="password"
                        label={t('common.password')}
                        type="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        error={!!errorMsg}
                    />
                    <Button
                        type="submit"
                        fullWidth
                        variant="contained"
                        sx={{ mt: 3, mb: 2 }}
                    >
                        {t('common.login')}
                    </Button>
                    
                    <Button
                        fullWidth
                        variant="outlined"
                        sx={{ mb: 2 }}
                        onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                    >
                        {t('auth.loginWithGoogle')}
                    </Button>
                    
                    <Grid container>
                        <Grid item xs>
                            <Link to="/forgot-password">
                                {t('common.forgotPassword')}
                            </Link>
                        </Grid>
                        <Grid item>
                            <Link to="/register">
                                {t('common.register')}
                            </Link>
                        </Grid>
                    </Grid>
                </Box>
            </Box>
        </Container>
    );
};

export default LoginPage;