import React from 'react';
import { Link } from 'react-router-dom';
import { Box, Typography, Button } from '@mui/material';

const HomePage: React.FC = () => {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <Typography variant="h4" component="h1">
        Главная страница
      </Typography>
      <Typography variant="body1" gutterBottom>
        Добро пожаловать! Выберите нужный раздел:
      </Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        <Button variant="contained" component={Link} to="/login">
          Страница логина
        </Button>
        <Button variant="contained" component={Link} to="/register">
          Страница регистрации
        </Button>
        <Button variant="contained" component={Link} to="/verify">
          Страница подтверждения
        </Button>
        <Button variant="contained" component={Link} to="/profile">
          Мой профиль (защищено)
        </Button>
        <Button variant="contained" component={Link} to="/edit-profile">
          Редактировать профиль (защищено)
        </Button>
        <Button variant="contained" component={Link} to="/admin">
          Админ панель
        </Button>
      </Box>
    </Box>
  );
};

export default HomePage;