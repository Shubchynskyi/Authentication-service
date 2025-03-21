import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Typography,
  Alert,
  Paper,
  Divider,
  List,
  ListItem,
  ListItemText,
  Button,
  Grid,
  ListItemButton,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import api from '../api';
import axios from 'axios';
import AdminSection from './AdminSection';


interface ProfileData {
  email: string;
  name: string;
  roles: string[];
}

interface Bot {
  id: number;
  name: string;
  description: string;
}

const StyledPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(4),
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  borderRadius: theme.spacing(1),
  backgroundColor: theme.palette.background.paper,
}));

const ProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<ProfileData>({ email: '', name: '', roles: [] });
  const [message, setMessage] = useState('');

  const [mockBots] = useState<Bot[]>([
    { id: 1, name: "Бот 1", description: "Описание бота 1" },
    { id: 2, name: "Бот 2", description: "Описание бота 2" },
    { id: 3, name: "Бот 3", description: "Описание бота 3" },
  ]);

    useEffect(() => {
        const fetchData = async() => {
            try {
                const result = await api.get<ProfileData>('/api/protected/profile');
                 setProfile(result.data);
            } catch (error) {
                let message = 'Ошибка при загрузке профиля';
                if (axios.isAxiosError(error)) {
                    message = error.response?.data.message || 'Ошибка при загрузке профиля';
                }
                setMessage(message);
            }

        }
        fetchData();
    }, []);


  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
     window.location.href = '/login';
  };

  const handleAddBot = () => {
       window.location.href ='/add-bot';
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <StyledPaper elevation={3}>
        <Typography component="h1" variant="h5" align="center" marginBottom={3}>
          Мой профиль
        </Typography>

        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
              Email:
            </Typography>
            <Typography variant="body1" noWrap>
              {profile.email}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
              Имя:
            </Typography>
             <Typography variant="body1" >
                {profile.name}
              </Typography>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="outlined"
              fullWidth
              component={Link}
              to="/edit-profile"
            >
              Редактировать профиль
            </Button>
          </Grid>
        </Grid>

        <Divider sx={{ width: '100%', my: 3 }} />

        <Typography variant='h6' align='center'>Список ботов</Typography>
        <List sx={{ width: '100%' }}>
          {mockBots.length === 0 ? (
            <ListItem>
              <ListItemText primary="Боты пока не добавлены." />
            </ListItem>
          ) : (
            mockBots.map((bot) => (
              <ListItemButton key={bot.id}>
                <ListItemText primary={bot.name} secondary={bot.description} />
              </ListItemButton>
            ))
          )}
        </List>
        <Button variant="outlined" fullWidth sx={{ mt: 2 }} onClick={handleAddBot}>
          Добавить бота
        </Button>

        <Divider sx={{ width: '100%', my: 3 }} />

         <AdminSection>
            <Box>
                <Typography variant="h6" gutterBottom>
                    Админ-панель
                </Typography>
                <Button variant="contained" component={Link} to="/admin">
                    Перейти в админ-панель
                </Button>
            </Box>
        </AdminSection>

        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1, width: '100%' }}>
          <Button onClick={handleLogout} variant="outlined" color="error">
            Выйти
          </Button>
          <Link to="/">
            <Typography variant="body2">Вернуться на главную</Typography>
          </Link>
        </Box>
      </StyledPaper>
      {message && (
        <Alert severity="error" sx={{ mt: 2, width: '100%' }}>
          {message}
        </Alert>
      )}
    </Box>
  );
};

export default ProfilePage;