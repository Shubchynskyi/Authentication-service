import React, { useState } from 'react';
import axios from 'axios';
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

const StyledPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(4),
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  borderRadius: theme.spacing(1),
  backgroundColor: theme.palette.background.paper,
}));

const RegistrationPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [messageType, setMessageType] = useState<'error' | 'success'>('success');

  const handleRegister = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const response = await axios.post<string>(
        'http://localhost:8080/api/auth/register',
        { email, name, password }
      );
      setMessage(response.data);
      setMessageType('success');
      navigate('/verify', { state: { email: email } });

    } catch (err) {
      setMessageType('error');
      if (axios.isAxiosError(err)) {
        if (err.response?.data) {
          setMessage(err.response.data);
        } else if (err.message) {
          setMessage(err.message);
        } else {
          setMessage('Registration error occurred');
        }
      } else {
        setMessage('Unexpected error during registration');
      }
      console.error('Registration error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <StyledPaper elevation={3}>
        <Typography component="h1" variant="h5" align="center" marginBottom={3}>
          Регистрация
        </Typography>
        <form onSubmit={handleRegister} style={{ width: '100%' }}>
          <TextField
            label="Email"
            fullWidth
            margin="normal"
            type="email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          <TextField
            label="Имя"
            fullWidth
            margin="normal"
            type="text"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
          <TextField
            label="Пароль"
            fullWidth
            margin="normal"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
          <Button
            type="submit"
            variant="contained"
            fullWidth
            sx={{ mt: 3, mb: 2 }}
            disabled={isLoading}
          >
            {isLoading ? <CircularProgress size={24} /> : 'Зарегистрироваться'}
          </Button>

        <Button component={Link} to="/verify" variant="outlined" fullWidth sx={{ mb: 2 }}>
             Активировать аккаунт
        </Button>


          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
            <Link to="/login">
              <Typography variant="body2">Уже есть аккаунт?</Typography>
            </Link>
            <Link to="/">
              <Typography variant="body2">Вернуться на главную</Typography>
            </Link>
          </Box>
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

export default RegistrationPage;