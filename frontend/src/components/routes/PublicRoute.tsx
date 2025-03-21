import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';

interface PublicRouteProps {
    children: React.ReactNode;
}

const PublicRoute: React.FC<PublicRouteProps> = ({ children }) => {
    const location = useLocation();
    
    const isAuthenticated = localStorage.getItem('token') !== null;
    
    if (isAuthenticated) {
        const from = location.state?.from?.pathname || '/';
        return <Navigate to={from} replace />;
    }
    
    return <>{children}</>;
};

export default PublicRoute;