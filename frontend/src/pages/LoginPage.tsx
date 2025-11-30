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
    Link as MuiLink,
    Paper
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import { API_BASE_URL, API_URL } from '../config';

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

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const { t } = useTranslation();
    const location = useLocation();
    const { showNotification } = useNotification();

    useEffect(() => {
        if (location.state?.error) {
            showNotification(location.state.error, 'error');
        }
    }, [location, showNotification]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!email) {
            showNotification(t('errors.emailRequired'), 'error');
            return;
        }
        if (!password) {
            showNotification(t('errors.passwordRequired'), 'error');
            return;
        }

        try {
            const response = await axios.post(`${API_BASE_URL}/auth/login`, {
                email,
                password
            });
            const { accessToken, refreshToken } = response.data;
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            window.location.href = '/';
        } catch (error) {
            if (axios.isAxiosError(error)) {
                // Temporarily disabled - errorMessage variable not used
                // let errorMessage = t('errors.loginFailed');
                // if (error.response?.data) {
                //     if (typeof error.response.data === 'string') {
                //         errorMessage = error.response.data;
                //     } else if (error.response.data.message) {
                //         errorMessage = error.response.data.message;
                //     } else if (error.response.data.error) {
                //         errorMessage = error.response.data.error;
                //     }
                // }
                // Always show generic message to not reveal account existence
                showNotification(t('errors.loginFailed'), 'error');
            } else {
                showNotification(t('errors.loginFailed'), 'error');
            }
        }
    };

    return (
        <Container component="main" maxWidth="xs">
            <Box
                sx={{
                    marginTop: 8,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                <StyledPaper elevation={3}>
                    <Typography component="h1" variant="h5">
                        {t('auth.loginTitle')}
                    </Typography>

                    <Box component="form" noValidate onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label={t('common.email')}
                            name="email"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
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
                            onClick={() => window.location.href = `${API_URL}/oauth2/authorization/google`}
                        >
                            {t('auth.loginWithGoogle')}
                        </Button>

                        <Grid container alignItems="center" justifyContent="space-between" sx={{ mt: 1 }}>
                            <Grid item>
                                <MuiLink component={Link} to="/forgot-password" color="primary" underline="hover">
                                    {t('common.forgotPassword')}
                                </MuiLink>
                            </Grid>
                            <Grid item>
                                <MuiLink component={Link} to="/register" color="primary" underline="hover">
                                    {t('common.register')}
                                </MuiLink>
                            </Grid>
                        </Grid>
                    </Box>
                </StyledPaper>
            </Box>
        </Container>
    );
};

export default LoginPage;