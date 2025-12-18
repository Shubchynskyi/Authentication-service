import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Box, Typography, Button, CircularProgress, Alert } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { getMaskedLoginSettingsPublic, getTemplate, type MaskedLoginPublicSettings } from '../services/maskedLoginService';
import MaskedLoginTemplate from '../components/MaskedLoginTemplate';

const HomePage: React.FC = () => {
    const { t } = useTranslation();
    const { isAuthenticated, isLoading: authLoading } = useAuth();
    const [maskedContent, setMaskedContent] = useState<string | null>(null);
    const [maskedSettings, setMaskedSettings] = useState<MaskedLoginPublicSettings | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if masked login is enabled (public endpoint).
        const checkMaskedLogin = async () => {
            try {
                const settings = await getMaskedLoginSettingsPublic();
                setMaskedSettings(settings);
                if (settings?.enabled && !isAuthenticated) {
                    // Load and display the template
                    const template = await getTemplate(settings.templateId);
                    setMaskedContent(template);
                } else {
                    setMaskedContent(null);
                }
            } catch (error) {
                console.error('Failed to load masked login settings:', error);
                setMaskedSettings(null);
                setMaskedContent(null);
            } finally {
                setLoading(false);
            }
        };

        if (!authLoading) {
            checkMaskedLogin();
        }
    }, [authLoading, isAuthenticated]);

    // Show loading state
    if (loading || authLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    // Show masked template if enabled
    if (!isAuthenticated && maskedContent) {
        return <MaskedLoginTemplate htmlContent={maskedContent} />;
    }

    // Show standard home page
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <Typography variant="h4" component="h1">
                {t('home.title')}
            </Typography>
            <Typography variant="body1" gutterBottom>
                {t('home.subtitle')}
            </Typography>
            <Box sx={{ width: '100%', maxWidth: 480, display: 'flex', flexDirection: 'column', gap: 1 }}>
                {isAuthenticated ? (
                    <>
                        <Button fullWidth variant="contained" component={Link} to="/profile">
                            {t('home.links.profile')}
                        </Button>
                        <Button fullWidth variant="contained" component={Link} to="/profile/edit">
                            {t('home.links.editProfile')}
                        </Button>
                        <Button fullWidth variant="contained" component={Link} to="/admin">
                            {t('home.links.adminPanel')}
                        </Button>
                    </>
                ) : (
                    <>
                        <Button fullWidth variant="contained" component={Link} to="/login">
                            {t('home.links.login')}
                        </Button>
                        <Button fullWidth variant="contained" component={Link} to="/register">
                            {t('home.links.register')}
                        </Button>
                        <Button fullWidth variant="contained" component={Link} to="/verify">
                            {t('home.links.verify')}
                        </Button>
                    </>
                )}
            </Box>

            {isAuthenticated && maskedSettings?.enabled && (
                <Box
                    sx={{
                        position: 'fixed',
                        left: 16,
                        right: 16,
                        bottom: 16,
                        display: 'flex',
                        justifyContent: 'center',
                        zIndex: (theme) => theme.zIndex.snackbar,
                    }}
                >
                    <Alert severity="info" sx={{ maxWidth: 720, width: '100%', textAlign: 'center' }}>
                        {t('maskedLogin.noticeGuestsMasked')}
                    </Alert>
                </Box>
            )}
        </Box>
    );
};

export default HomePage;