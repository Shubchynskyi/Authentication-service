import React from 'react';
import { AppBar, Toolbar, IconButton, Box } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import { useTheme } from '../context/ThemeContext';

const Navbar: React.FC = () => {
    const { isDarkMode, toggleTheme } = useTheme();

    return (
        <AppBar position="fixed" color="primary" elevation={1}>
            <Toolbar sx={{ justifyContent: 'flex-end' }}>
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