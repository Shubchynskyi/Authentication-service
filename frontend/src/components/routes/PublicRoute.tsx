import React from 'react';
import { Navigate } from 'react-router-dom';

interface PublicRouteProps {
    children: React.ReactNode;
}

const PublicRoute: React.FC<PublicRouteProps> = ({ children }) => {
    const isAuthenticated = localStorage.getItem('accessToken') !== null;
    
    if (isAuthenticated) {
        return <Navigate to="/profile" replace />;
    }
    
    return <>{children}</>;
};

export default PublicRoute;