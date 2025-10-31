import React from 'react';
import { Navigate } from 'react-router-dom';
import { isJwtExpired } from '../../utils/token';

interface PublicRouteProps {
    children: React.ReactNode;
}

const PublicRoute: React.FC<PublicRouteProps> = ({ children }) => {
    const token = localStorage.getItem('accessToken');
    const isAuthenticated = !!token && !isJwtExpired(token);
    
    if (isAuthenticated) {
        return <Navigate to="/profile" replace />;
    }
    
    return <>{children}</>;
};

export default PublicRoute;