import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { useProfile } from './context/ProfileContext';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegistrationPage from './pages/RegistrationPage';
import VerificationPage from './pages/VerificationPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import ProfilePage from './pages/ProfilePage';
import EditProfilePage from './pages/EditProfilePage';
import AdminPage from './pages/AdminPage';
import NotFoundPage from './components/NotFoundPage';
import { CircularProgress, Box } from '@mui/material';

const LoadingScreen = () => (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
    </Box>
);

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) {
        return <LoadingScreen />;
    }

    if (!isAuthenticated) {
        return <NotFoundPage />;
    }

    return <>{children}</>;
};

const AdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();
    const { isAdmin, isLoading: isProfileLoading } = useProfile();

    if (isLoading || isProfileLoading) {
        return <LoadingScreen />;
    }

    if (!isAuthenticated || !isAdmin) {
        return <NotFoundPage />;
    }

    return <>{children}</>;
};

const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    return <>{children}</>;
};

const GuestRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) {
        return <LoadingScreen />;
    }

    if (isAuthenticated) {
        return <NotFoundPage />;
    }

    return <>{children}</>;
};

const AppRoutes: React.FC = () => {
    const { isLoading } = useAuth();

    if (isLoading) {
        return <LoadingScreen />;
    }

    return (
        <Routes>
            {/* Публичная страница */}
            <Route path="/" element={<HomePage />} />

            {/* Страницы только для гостей */}
            <Route
                path="/login"
                element={
                    <GuestRoute>
                        <LoginPage />
                    </GuestRoute>
                }
            />
            <Route
                path="/register"
                element={
                    <GuestRoute>
                        <RegistrationPage />
                    </GuestRoute>
                }
            />
            <Route
                path="/verify/*"
                element={
                    <GuestRoute>
                        <VerificationPage />
                    </GuestRoute>
                }
            />
            <Route
                path="/forgot-password"
                element={
                    <GuestRoute>
                        <ForgotPasswordPage />
                    </GuestRoute>
                }
            />
            <Route
                path="/reset-password"
                element={
                    <GuestRoute>
                        <ResetPasswordPage />
                    </GuestRoute>
                }
            />

            {/* Защищенные страницы */}
            <Route
                path="/profile/*"
                element={
                    <PrivateRoute>
                        <Routes>
                            <Route path="/" element={<ProfilePage />} />
                            <Route path="edit" element={<EditProfilePage />} />
                            <Route path="*" element={<NotFoundPage />} />
                        </Routes>
                    </PrivateRoute>
                }
            />

            {/* Страницы только для админов */}
            <Route
                path="/admin/*"
                element={
                    <AdminRoute>
                        <Routes>
                            <Route path="/" element={<AdminPage />} />
                            <Route path="*" element={<NotFoundPage />} />
                        </Routes>
                    </AdminRoute>
                }
            />

            {/* Несуществующие страницы */}
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
};

export default AppRoutes; 