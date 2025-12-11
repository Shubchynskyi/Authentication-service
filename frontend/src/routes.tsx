import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
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
import VerifyPage from './pages/VerifyPage';
import OAuth2RedirectHandler from './pages/OAuth2RedirectHandler';
import PrivateRoute from './components/routes/PrivateRoute';
import PublicRoute from './components/routes/PublicRoute';
import AdminRoute from './components/routes/AdminRoute';

const LoadingScreen = () => (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
    </Box>
);

const AppRoutes: React.FC = () => {
    const { isLoading } = useAuth();

    if (isLoading) {
        return <LoadingScreen />;
    }

    return (
        <Routes>
            {/* Public page */}
            <Route path="/" element={<HomePage />} />
            <Route path="/oauth2/success" element={<OAuth2RedirectHandler />} />

            {/* Pages only for guests */}
            <Route
                path="/login"
                element={
                    <PublicRoute>
                        <LoginPage />
                    </PublicRoute>
                }
            />
            <Route
                path="/register"
                element={
                    <PublicRoute>
                        <RegistrationPage />
                    </PublicRoute>
                }
            />
            <Route
                path="/verify/*"
                element={
                    <PublicRoute>
                        <Routes>
                            <Route path="/" element={<VerificationPage />} />
                            <Route path="/email" element={<VerifyPage />} />
                        </Routes>
                    </PublicRoute>
                }
            />
            <Route
                path="/forgot-password"
                element={
                    <PublicRoute>
                        <ForgotPasswordPage />
                    </PublicRoute>
                }
            />
            <Route
                path="/reset-password"
                element={
                    <PublicRoute>
                        <ResetPasswordPage />
                    </PublicRoute>
                }
            />

            {/* Protected pages */}
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

            {/* Pages only for admins */}
            <Route path="/admin/*" element={<AdminRoute />}>
                <Route index element={<AdminPage />} />
                <Route path="*" element={<NotFoundPage />} />
            </Route>

            {/* Non-existent pages */}
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
};

export default AppRoutes; 