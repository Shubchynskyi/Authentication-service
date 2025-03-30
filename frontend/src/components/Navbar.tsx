import React from 'react';
import { AppBar, Toolbar, IconButton, Box, Typography, Button } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import { useTheme } from '../context/ThemeContext';
import { useProfile } from '../context/ProfileContext';
import { Link } from 'react-router-dom';

const Navbar: React.FC = () => {
    const { isDarkMode, toggleTheme } = useTheme();
    const { profile } = useProfile();

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
                <Box>
                    <IconButton onClick={toggleTheme} color="inherit" size="large">
                        {isDarkMode ? <Brightness7 /> : <Brightness4 />}
                    </IconButton>
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default Navbar; 