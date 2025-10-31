import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { isJwtExpired } from '../../utils/token';

interface PrivateRouteProps {
    children: React.ReactNode;
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ children }) => {
    const location = useLocation();
    
    const token = localStorage.getItem('accessToken');
    const isAuthenticated = !!token && !isJwtExpired(token);
    
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }
    
    return <>{children}</>;
};

export default PrivateRoute;