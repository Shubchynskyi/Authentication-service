import React, { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from '../../context/AuthContext';
import { checkAccess } from '../../api';
import NotFoundPage from '../NotFoundPage';

const AdminRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const [hasAccess, setHasAccess] = useState<boolean | null>(null);

  useEffect(() => {
    const run = async () => {
      if (!isAuthenticated) {
        setHasAccess(false);
        return;
      }
      try {
        const canAccess = await checkAccess('admin-panel');
        setHasAccess(canAccess);
      } catch {
        setHasAccess(false);
      }
    };

    run();
  }, [isAuthenticated]);
  
  if (isLoading || hasAccess === null) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!hasAccess) {
    return <NotFoundPage />;
  }
  
  return <Outlet />;
};

export default AdminRoute;