import React, { useEffect, useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { Box, Typography, Button, CircularProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { getMaskedLoginSettingsPublic, getTemplate } from '../services/maskedLoginService';
import MaskedLoginTemplate from '../components/MaskedLoginTemplate';

const HomePage: React.FC = () => {
    const { t } = useTranslation();
    const { isAuthenticated, isLoading: authLoading } = useAuth();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [maskedContent, setMaskedContent] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    const secretParam = searchParams.get('secret');

    useEffect(() => {
        // If secret=true parameter is present, show standard page
        if (secretParam === 'true') {
            setLoading(false);
            return;
        }

        // If user is authenticated, redirect to profile
        if (!authLoading && isAuthenticated) {
            navigate('/profile', { replace: true });
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

    // Show loading state
    if (loading || authLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    // Show masked template if enabled
    if (maskedContent && !secretParam) {
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
                <Button fullWidth variant="contained" component={Link} to="/login">
                    {t('home.links.login')}
                </Button>
                <Button fullWidth variant="contained" component={Link} to="/register">
                    {t('home.links.register')}
                </Button>
                <Button fullWidth variant="contained" component={Link} to="/verify">
                    {t('home.links.verify')}
                </Button>
                <Button fullWidth variant="contained" component={Link} to="/profile">
                    {t('home.links.profile')}
                </Button>
                <Button fullWidth variant="contained" component={Link} to="/profile/edit">
                    {t('home.links.editProfile')}
                </Button>
                <Button fullWidth variant="contained" component={Link} to="/admin">
                    {t('home.links.adminPanel')}
                </Button>
            </Box>
        </Box>
    );
};

export default HomePage;