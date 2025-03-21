import React, { useState, useEffect } from 'react'; // Добавили useEffect
import { useNavigate, Link } from 'react-router-dom';
import {
    Box,
    TextField,
    Button,
    Typography,
    Alert,
    Paper,
    CircularProgress,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import api from '../api';
import axios from 'axios';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
}));

interface ProfileData {
    name: string;
    email: string;
}

const EditProfilePage: React.FC = () => {
    const navigate = useNavigate();
    const [name, setName] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [message, setMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [messageType, setMessageType] = useState<'error' | 'success'>('success');


     useEffect(() => {
        let isCancelled = false;

        const fetchProfile = async () => {
            setIsLoading(true);
            try {
                const response = await api.get<ProfileData>('/api/protected/profile');

                if (!isCancelled) {
                   setName(response.data.name);

                }

            } catch (error) {
                 let message = 'Непредвиденная ошибка при загрузке профиля';
                if (!isCancelled) {
                   if (axios.isAxiosError(error)) {
                        message = error.response?.data.message || 'Ошибка при загрузке профиля';
                    }
                    setMessage(message);
                }
            }
            finally{
                if(!isCancelled)
                 setIsLoading(false);
            }

        };

        fetchProfile();

        return () => {
            isCancelled = true;
        };
    }, []);


    const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setIsLoading(true);

        if (newPassword && !currentPassword) {
            setMessage('Для изменения пароля необходимо ввести текущий пароль.');
            setMessageType('error');
            setIsLoading(false);
            return;
        }

        try {
            const response = await api.post<string>(
                '/api/protected/profile',
                {
                    name: name,
                    password: newPassword,
                    currentPassword: currentPassword,
                },
            );
            setMessage(response.data);
            setMessageType('success');
            navigate('/profile', { replace: true });

        } catch (error) {
             let message = 'Непредвиденная ошибка';
            if (axios.isAxiosError(error)) {
                message = error.response?.data.message || 'Ошибка при обновлении профиля';
            }
            setMessage(message);
            setMessageType('error');
        } finally {
            setIsLoading(false);
        }
    };

     if (isLoading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}><CircularProgress /></Box>;
    }

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    Редактирование профиля
                </Typography>
                <form onSubmit={handleUpdate} style={{ width: '100%' }}>
                    <TextField
                        label="Имя"
                        fullWidth
                        margin="normal"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                    />
                    <TextField
                        label="Текущий пароль"
                        fullWidth
                        margin="normal"
                        type="password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        required={!!newPassword}
                    />
                    <TextField
                        label="Новый пароль (если хотите изменить)"
                        fullWidth
                        margin="normal"
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                    />
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : 'Сохранить изменения'}
                    </Button>
                    <Button
                        variant="outlined"
                        fullWidth
                        component={Link}
                        to="/profile"
                    >
                        Вернуться в профиль
                    </Button>
                </form>
                {message && (
                    <Alert severity={messageType} sx={{ mt: 2, width: '100%' }}>
                        {message}
                    </Alert>
                )}
            </StyledPaper>
        </Box>
    );
};

export default EditProfilePage;