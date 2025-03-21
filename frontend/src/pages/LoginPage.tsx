import React, { useState } from 'react';
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

const LoginPage = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            const response = await api.post<{ accessToken: string; refreshToken: string }>(
                '/api/auth/login',
                { email, password }
            );
            localStorage.setItem('accessToken', response.data.accessToken);
            localStorage.setItem('refreshToken', response.data.refreshToken);
            navigate('/profile', { replace: true });

        } catch (error) {
            let message = 'Непредвиденная ошибка';
            if (axios.isAxiosError(error)) {
                if (error.response?.status === 401) {
                    message = 'Неверный email или пароль';
                } else {
                    message = error.response?.data.message || 'Ошибка при авторизации';
                }
            }
            setErrorMessage(message)
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    Добро пожаловать!
                </Typography>
                <Typography variant="h6" align="center" marginBottom={3}>
                    Вход в аккаунт
                </Typography>
                <form onSubmit={handleLogin} style={{ width: '100%' }}>
                    <TextField
                        label="Email"
                        fullWidth
                        margin="normal"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <TextField
                        label="Пароль"
                        fullWidth
                        margin="normal"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : 'Войти'}
                    </Button>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                        <Button component={Link} to="/register" variant="outlined">
                            Регистрация
                        </Button>
                        <Button component={Link} to="/forgot-password" variant="outlined">
                            Забыли пароль?
                        </Button>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
                        <Link to="/">
                            <Typography variant="body2">Вернуться на главную</Typography>
                        </Link>
                    </Box>
                </form>
            </StyledPaper>
            {errorMessage && (
                <Alert severity="error" sx={{ mt: 2, width: '100%' }}>
                    {errorMessage}
                </Alert>
            )}
        </Box>
    );
};

export default LoginPage;