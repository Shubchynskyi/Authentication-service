import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate, Link, useLocation } from 'react-router-dom';
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

const VerificationPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [messageType, setMessageType] = useState<'error' | 'success'>('success');

  useEffect(() => {
    // Если email передан через state, используем его
    if (location.state?.email) {
      setEmail(location.state.email);
    }
  }, [location.state]);

  const handleVerify = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const response = await axios.post<string>(
        'http://localhost:8080/api/auth/verify',
        { email, code }
      );
      setMessage(response.data);
      setMessageType('success')
      navigate('/login');
    } catch (err) {
      setMessageType('error');
      if (axios.isAxiosError(err) && err.response?.data) {
        if (typeof err.response.data === 'string') {
          setMessage(err.response.data);
        } else if (typeof err.response.data.message === 'string') {
          setMessage(err.response.data.message);
        } else {
          setMessage('Ошибка при подтверждении');
        }
      } else {
        setMessage('Непредвиденная ошибка');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendVerification = async () => {
    setResendLoading(true);
    try {
      const response = await axios.post<string>(
        'http://localhost:8080/api/auth/resend-verification',
        { email }
      );
      setMessage(response.data);
      setMessageType('success');

    } catch (error) {
      setMessage("Не удалось отправить код повторно")
      setMessageType('error')
      console.error("Ошибка при повторной отправке кода:", error);
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <StyledPaper elevation={3}>
        <Typography component="h1" variant="h5" align="center" marginBottom={3}>
          Подтверждение Email
        </Typography>
        <form onSubmit={handleVerify} style={{ width: '100%' }}>
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
            label="Код подтверждения"
            fullWidth
            margin="normal"
            type="text"
            value={code}
            onChange={e => setCode(e.target.value)}
            required
          />
          <Button
            type="submit"
            variant="contained"
            fullWidth
            sx={{ mt: 3, mb: 2 }}
            disabled={isLoading}
          >
            {isLoading ? <CircularProgress size={24} /> : 'Подтвердить'}
          </Button>
          <Button
            onClick={handleResendVerification}
            variant="outlined"
            fullWidth
            sx={{ mb: 2 }}
            disabled={resendLoading}
          >
            {resendLoading ? <CircularProgress size={24} /> : 'Отправить код повторно'}
          </Button>

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
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

export default VerificationPage;