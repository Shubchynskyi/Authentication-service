import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { Box, Toolbar } from '@mui/material';
import { AuthProvider } from './context/AuthContext';
import { ProfileProvider } from './context/ProfileContext';
import { NotificationProvider } from './context/NotificationContext';
import { ThemeProvider } from './context/ThemeContext';
import { LayoutProvider, useLayout } from './context/LayoutContext';
import AppRoutes from './routes';
import NotificationContainer from './components/NotificationContainer';
import Navbar from './components/Navbar';
import './i18n/i18n';

const AppContent: React.FC = () => {
    const { hideNavbar } = useLayout();

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
            {!hideNavbar && <Navbar />}
            <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                {!hideNavbar && <Toolbar />}
                <NotificationContainer />
                <AppRoutes />
            </Box>
        </Box>
    );
};

const App: React.FC = () => {
    return (
        <ThemeProvider>
            <NotificationProvider>
                <AuthProvider>
                    <ProfileProvider>
                        <LayoutProvider>
                            <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
                                <AppContent />
                            </Router>
                        </LayoutProvider>
                    </ProfileProvider>
                </AuthProvider>
            </NotificationProvider>
        </ThemeProvider>
    );
};

export default App; 