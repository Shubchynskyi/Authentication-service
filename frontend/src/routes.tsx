import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { checkAccess } from './api';
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

const LoadingScreen = () => (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
    </Box>
);

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingScreen />;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    return <>{children}</>;
};

const AdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated, isLoading } = useAuth();
    const [hasAccess, setHasAccess] = useState(false);
    const [isCheckingAccess, setIsCheckingAccess] = useState(true);

    useEffect(() => {
        const checkAdminAccess = async () => {
            if (!isAuthenticated) {
                setHasAccess(false);
                setIsCheckingAccess(false);
                return;
            }

            try {
                const canAccess = await checkAccess('admin-panel');
                setHasAccess(canAccess);
            } catch (error) {
                setHasAccess(false);
            } finally {
                setIsCheckingAccess(false);
            }
        };

        checkAdminAccess();
    }, [isAuthenticated]);

    if (isLoading || isCheckingAccess) {
        return <LoadingScreen />;
    }

    if (!isAuthenticated || !hasAccess) {
        return <NotFoundPage />;
    }

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
            {/* Public page */}
            <Route path="/" element={<HomePage />} />

            {/* Pages only for guests */}
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
                        <Routes>
                            <Route path="/" element={<VerificationPage />} />
                            <Route path="/email" element={<VerifyPage />} />
                        </Routes>
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

            {/* Non-existent pages */}
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
};

export default AppRoutes; 