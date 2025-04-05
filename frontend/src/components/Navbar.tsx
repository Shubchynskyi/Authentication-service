import React from 'react';
import { AppBar, Toolbar, IconButton, Box, Typography, Button, Select, MenuItem, SelectChangeEvent } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import { useTheme } from '../context/ThemeContext';
import { useProfile } from '../context/ProfileContext';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { availableLanguages } from '../i18n/i18n';

const Navbar: React.FC = () => {
    const { isDarkMode, toggleTheme } = useTheme();
    const { profile } = useProfile();
    const { i18n } = useTranslation();

    const handleLanguageChange = (event: SelectChangeEvent<string>) => {
        const language = event.target.value;
        i18n.changeLanguage(language);
        localStorage.setItem('language', language);
    };

    return (
        <AppBar position="fixed" color="primary" elevation={1}>
            <Toolbar sx={{ justifyContent: 'space-between' }}>
                <Box>
                    {profile && (
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
                    )}
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Select
                        value={i18n.language}
                        onChange={handleLanguageChange}
                        size="small"
                        sx={{
                            color: 'inherit',
                            '& .MuiSelect-icon': { color: 'inherit' },
                            '& .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
                            '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
                        }}
                    >
                        {Object.entries(availableLanguages).map(([code, name]) => (
                            <MenuItem key={code} value={code}>
                                {name}
                            </MenuItem>
                        ))}
                    </Select>
                    <IconButton onClick={toggleTheme} color="inherit" size="large">
                        {isDarkMode ? <Brightness7 /> : <Brightness4 />}
                    </IconButton>
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default Navbar; 