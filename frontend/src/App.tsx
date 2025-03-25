import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { Box } from '@mui/material';
import { AuthProvider } from './context/AuthContext';
import { ProfileProvider } from './context/ProfileContext';
import { NotificationProvider } from './context/NotificationContext';
import { ThemeProvider } from './context/ThemeContext';
import AppRoutes from './routes';
import NotificationContainer from './components/NotificationContainer';
import Navbar from './components/Navbar';

const App: React.FC = () => {
    return (
        <ThemeProvider>
            <NotificationProvider>
                <AuthProvider>
                    <ProfileProvider>
                        <Router>
                            <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
                                <Navbar />
                                <Box sx={{ pt: 8, flex: 1 }}>
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