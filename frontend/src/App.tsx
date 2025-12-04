import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { Box, Toolbar } from '@mui/material';
import { AuthProvider } from './context/AuthContext';
import { ProfileProvider } from './context/ProfileContext';
import { NotificationProvider } from './context/NotificationContext';
import { ThemeProvider } from './context/ThemeContext';
import AppRoutes from './routes';
import NotificationContainer from './components/NotificationContainer';
import Navbar from './components/Navbar';
import './i18n/i18n';

const App: React.FC = () => {
    return (
        <ThemeProvider>
            <NotificationProvider>
                <AuthProvider>
                    <ProfileProvider>
                        <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
                            <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                                <Navbar />
                                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                                    <Toolbar />
                                    <NotificationContainer />
                                    <AppRoutes />
                                </Box>
                            </Box>
                        </Router>
                    </ProfileProvider>
                </AuthProvider>
            </NotificationProvider>
        </ThemeProvider>
    );
};

export default App; 