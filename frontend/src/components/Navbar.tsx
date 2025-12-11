import React from 'react';
import { AppBar, Toolbar, IconButton, Box, Typography, Button, Tooltip } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import HomeIcon from '@mui/icons-material/Home';
import LogoutIcon from '@mui/icons-material/Logout';
import { useTheme } from '../context/ThemeContext';
import { useProfile } from '../context/ProfileContext';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from './LanguageSwitcher';

const Navbar: React.FC = () => {
    const { isDarkMode, toggleTheme } = useTheme();
    const { profile } = useProfile();
    const { logout } = useAuth();
    const { t } = useTranslation();

    return (
        <AppBar position="fixed" color="primary" elevation={1}>
            <Toolbar sx={{ justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Tooltip title={t('common.home')}>
                        <IconButton component={Link} to="/" color="inherit" size="large" aria-label="Home">
                            <HomeIcon />
                        </IconButton>
                    </Tooltip>
                    {profile && (
                        <>
                            <Tooltip title={t('common.logout')}>
                                <IconButton onClick={logout} color="inherit" size="large" aria-label="Logout" sx={{ mx: 0.5 }}>
                                    <LogoutIcon />
                                </IconButton>
                            </Tooltip>
                            <Tooltip title={t('profile.goToProfile')}>
                                <Button
                                    component={Link}
                                    to="/profile"
                                    color="inherit"
                                    sx={{ textTransform: 'none' }}
                                >
                                    <Typography variant="body1">
                                        {profile.name || profile.email}
                                    </Typography>
                                </Button>
                            </Tooltip>
                        </>
                    )}
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LanguageSwitcher variant="compact" />
                    <IconButton onClick={toggleTheme} color="inherit" size="large">
                        {isDarkMode ? <Brightness7 /> : <Brightness4 />}
                    </IconButton>
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default Navbar; 