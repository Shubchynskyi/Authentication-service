import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import {
    Box,
    TextField,
    Button,
    Typography,
    Container,
    Grid,
    Link as MuiLink,
    CircularProgress,
    FormControlLabel,
    Checkbox,
    FormControl,
    InputLabel,
    Select,
    MenuItem
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import { useAuth } from '../context/AuthContext';
import { API_URL } from '../config';
import FormPaper from '../components/FormPaper';
import { getMaskedLoginSettingsPublic, getTemplate } from '../services/maskedLoginService';
import MaskedLoginTemplate from '../components/MaskedLoginTemplate';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [rememberDevice, setRememberDevice] = useState(false);
    const [rememberDays, setRememberDays] = useState<number>(15);
    const { t } = useTranslation();
    const location = useLocation();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { showNotification } = useNotification();
    const { login, isAuthenticated, isLoading: authLoading } = useAuth();
    const [maskedContent, setMaskedContent] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    const secretParam = searchParams.get('secret');

    useEffect(() => {
        if (location.state?.error) {
            showNotification(location.state.error, 'error');
        }
    }, [location, showNotification]);

    useEffect(() => {
        // If secret=true parameter is present, show login form
        if (secretParam === 'true') {
            setLoading(false);
            return;
        }

        // If user is authenticated, redirect to real home page
        if (!authLoading && isAuthenticated) {
            navigate('/', { replace: true });
            return;
        }

        // Check if masked login is enabled
        const checkMaskedLogin = async () => {
            try {
                const settings = await getMaskedLoginSettingsPublic();
                if (settings?.enabled && !isAuthenticated) {
                    // Load and display the template
                    const template = await getTemplate(settings.templateId);
                    setMaskedContent(template);
                } else {
                    setMaskedContent(null);
                }
            } catch (error) {
                console.error('Failed to load masked login settings:', error);
                setMaskedContent(null);
            } finally {
                setLoading(false);
            }
        };

        if (!authLoading) {
            checkMaskedLogin();
        }
    }, [authLoading, isAuthenticated, secretParam, navigate]);

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
            await login(email, password, { rememberDevice, rememberDays });
            const redirectTo =
                (location.state as { from?: { pathname?: string } } | undefined)?.from?.pathname || '/';
            navigate(redirectTo, { replace: true });
        } catch (error) {
            // Error is already handled in AuthContext, but we show a generic message
            // to not reveal account existence
            showNotification(t('errors.loginFailed'), 'error');
        }
    };

    // Show loading state
    if (loading || authLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    // Show masked template if enabled
    if (!isAuthenticated && maskedContent && secretParam !== 'true') {
        return <MaskedLoginTemplate htmlContent={maskedContent} />;
    }

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
                <FormPaper elevation={3}>
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

                        <FormControlLabel
                            control={
                                <Checkbox
                                    checked={rememberDevice}
                                    onChange={(e) => setRememberDevice(e.target.checked)}
                                />
                            }
                            label={t('auth.rememberDevice')}
                            sx={{ mb: 1 }}
                        />

                        {rememberDevice && (
                            <FormControl fullWidth sx={{ mb: 2 }}>
                                <InputLabel id="remember-days-label">{t('auth.rememberDays')}</InputLabel>
                                <Select
                                    labelId="remember-days-label"
                                    value={rememberDays}
                                    label={t('auth.rememberDays')}
                                    onChange={(e) => setRememberDays(Number(e.target.value))}
                                >
                                    {[15, 30, 60, 90].map((days) => (
                                        <MenuItem key={days} value={days}>
                                            {t('auth.rememberDaysOption', { days })}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        )}

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
                </FormPaper>
            </Box>
        </Container>
    );
};

export default LoginPage;
