import type {FC} from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import RegistrationPage from '../pages/RegistrationPage';
import VerificationPage from '../pages/VerificationPage';
import ProfilePage from '../pages/ProfilePage';
import EditProfilePage from '../pages/EditProfilePage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import ResetPasswordPage from '../pages/ResetPasswordPage';
import AdminPage from '../pages/AdminPage';
import PrivateRoute from '../components/routes/PrivateRoute';
import PublicRoute from '../components/routes/PublicRoute';
import AdminRoute from '../components/routes/AdminRoute';
import { ProfileProvider } from '../context/ProfileContext';

const App: FC = () => {
    return (
        <ProfileProvider>
            <Router>
                <Routes>
                    <Route path="/" element={<HomePage />} />

                    <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
                    <Route path="/register" element={<PublicRoute><RegistrationPage /></PublicRoute>} />
                    <Route path="/verify" element={<PublicRoute><VerificationPage /></PublicRoute>} />
                    <Route path="/forgot-password" element={<PublicRoute><ForgotPasswordPage /></PublicRoute>} />
                    <Route path="/reset-password" element={<PublicRoute><ResetPasswordPage /></PublicRoute>} />

                    <Route path="/profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
                    <Route path="/edit-profile" element={<PrivateRoute><EditProfilePage /></PrivateRoute>} />

                    <Route path="/admin/*" element={<AdminRoute />}>
                        <Route index element={<AdminPage />} /> 
                        {/*<Route path="users" element={<AdminUsersPage />} />*/}  {/* /admin/users */}
                        {/*<Route path="settings" element={<AdminSettingsPage />} />*/}  {/* /admin/settings */}
                    </Route>

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </Router>
        </ProfileProvider>
    );
};

export default App;