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
  Link as MuiLink,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';

const StyledPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(4),
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  borderRadius: theme.spacing(1),
  backgroundColor: theme.palette.background.paper,
  width: '100%',
  maxWidth: 480,
}));

const RegistrationPage: React.FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { showNotification } = useNotification();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [messageType, setMessageType] = useState<'error' | 'success'>('success');

  const handleRegister = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);

    if (!email) {
      setIsLoading(false);
      showNotification(t('errors.emailRequired'), 'error');
      return;
    }
    if (!name) {
      setIsLoading(false);
      showNotification(t('errors.usernameRequired'), 'error');
      return;
    }
    if (!password) {
      setIsLoading(false);
      showNotification(t('errors.passwordRequired'), 'error');
      return;
    }

    try {
      const response = await axios.post<string>(
        'http://localhost:8080/api/auth/register',
        { email, name, password }
      );
      setMessage(response.data || t('auth.registerSuccess'));
      setMessageType('success');
      navigate('/verify', { state: { email: email } });
    } catch (err) {
      setMessageType('error');
      if (axios.isAxiosError(err)) {
        if (err.response?.data) {
          setMessage(String(err.response.data));
        } else if (err.message) {
          setMessage(err.message);
        } else {
          setMessage(t('auth.loginError.serverError'));
        }
      } else {
        setMessage(t('auth.loginError.serverError'));
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
          {t('auth.registerTitle')}
        </Typography>
        <form noValidate onSubmit={handleRegister} style={{ width: '100%' }}>
          <TextField
            label={t('common.email')}
            fullWidth
            margin="normal"
            type="email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          <TextField
            label={t('common.username')}
            fullWidth
            margin="normal"
            type="text"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
          <TextField
            label={t('common.password')}
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
            {isLoading ? <CircularProgress size={24} /> : t('auth.registerTitle')}
          </Button>

          <Button component={Link} to="/verify" variant="outlined" fullWidth sx={{ mb: 2 }}>
            {t('auth.verificationTitle')}
          </Button>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
            <MuiLink component={Link} to="/login" color="primary" underline="hover">
              {t('auth.loginTitle')}
            </MuiLink>
            <MuiLink component={Link} to="/" color="primary" underline="hover">
              {t('notFound.backHome')}
            </MuiLink>
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