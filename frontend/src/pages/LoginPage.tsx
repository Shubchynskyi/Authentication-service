import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
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
import { useAuth } from '../context/AuthContext';
import { API_URL } from '../config';

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
    const navigate = useNavigate();
    const { showNotification } = useNotification();
    const { login } = useAuth();

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
            await login(email, password);
            // Small delay to ensure tokens are saved and state is updated
            setTimeout(() => {
                navigate('/', { replace: true });
            }, 50);
        } catch (error) {
            // Error is already handled in AuthContext, but we show a generic message
            // to not reveal account existence
            showNotification(t('errors.loginFailed'), 'error');
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