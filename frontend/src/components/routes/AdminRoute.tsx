import React, { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import api from '../../api';
import axios from 'axios';

const AdminRoute: React.FC = () => {
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);

  useEffect(() => {
    const checkAdmin = async () => {
      try {
        const response = await api.get('/api/protected/profile');
        const roles = response.data.roles;
        setIsAdmin(roles.includes('ROLE_ADMIN'));
      } catch (error) {
        setIsAdmin(false);
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
        }
      }
    };

    checkAdmin();
  }, []);
  
  if (isAdmin === null) {
    return null;
  }
  
  if (!isAdmin) {
    return <Navigate to="/login" replace />;
  }
  
  return <Outlet />;
};

export default AdminRoute;