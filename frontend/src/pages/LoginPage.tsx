import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
    Box,
    TextField,
    Button,
    Typography,
    Paper,
    CircularProgress,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
}));

const LoginPage = () => {
    const { login, isLoading } = useAuth();
    const { showNotification } = useNotification();
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        
        try {
            await login(email, password);
            showNotification('Successfully logged in', 'success');
            navigate('/profile', { replace: true });
        } catch (error) {
            showNotification('Invalid email or password', 'error');
        }
    };

    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    Login
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
                        label="Password"
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
                        {isLoading ? <CircularProgress size={24} /> : 'Login'}
                    </Button>
                    <Button
                        variant="text"
                        fullWidth
                        component={Link}
                        to="/register"
                    >
                        Don't have an account? Register
                    </Button>
                    <Button
                        variant="text"
                        fullWidth
                        component={Link}
                        to="/forgot-password"
                    >
                        Forgot password?
                    </Button>
                </form>
            </StyledPaper>
        </Box>
    );
};

export default LoginPage;