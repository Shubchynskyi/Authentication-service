import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
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

const VerificationPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const { showNotification } = useNotification();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);

  useEffect(() => {
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
      showNotification(response.data || t('auth.verificationSuccess'), 'success');
      navigate('/login');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const msg = String(err.response?.data || t('auth.verification.error'));
        showNotification(msg, 'error');
      } else {
        showNotification(t('auth.verification.error'), 'error');
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
      showNotification(response.data || t('auth.verification.codeResent'), 'success');
    } catch (error) {
      showNotification(t('auth.verification.resendError'), 'error');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
      <StyledPaper elevation={3}>
        <Typography component="h1" variant="h5" align="center" marginBottom={3}>
          {t('auth.verificationTitle')}
        </Typography>
        <form onSubmit={handleVerify} style={{ width: '100%' }}>
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
            label={t('auth.verificationCode')}
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
            {isLoading ? <CircularProgress size={24} /> : t('auth.verify')}
          </Button>
          <Button
            onClick={handleResendVerification}
            variant="outlined"
            fullWidth
            sx={{ mb: 2 }}
            disabled={resendLoading}
          >
            {resendLoading ? <CircularProgress size={24} /> : t('auth.resendCode')}
          </Button>

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
            <MuiLink component={Link} to="/" color="primary" underline="hover">
              {t('notFound.backHome')}
            </MuiLink>
          </Box>
        </form>
      </StyledPaper>
    </Box>
  );
};

export default VerificationPage;