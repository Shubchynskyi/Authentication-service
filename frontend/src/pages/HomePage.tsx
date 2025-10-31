import React from 'react';
import { Link } from 'react-router-dom';
import { Box, Typography, Button } from '@mui/material';
import { useTranslation } from 'react-i18next';

const HomePage: React.FC = () => {
    const { t } = useTranslation();
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