import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
  CircularProgress,
  Link as MuiLink,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import api from '../api';
import FormPaper from '../components/FormPaper';

const VerificationPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const { showNotification } = useNotification();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  useEffect(() => {
    if (location.state?.email) {
      setEmail(location.state.email);
    }
  }, [location.state]);

  const handleVerify = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const response = await api.post<string>(
        '/api/auth/verify',
        { email, code }
      );
      showNotification(response.data || t('auth.verificationSuccess'), 'success');
      navigate('/login');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        let msg = t('auth.verification.error');
        if (err.response?.data) {
          const data = err.response.data;
          if (typeof data === 'string') {
            msg = data;
          } else if (data.message) {
            msg = data.message;
          } else if (data.error) {
            msg = data.error;
          }
        }
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
      const response = await api.post<string>(
        '/api/auth/resend-verification',
        { email }
      );
      showNotification(response.data || t('auth.verification.codeResent'), 'success');
    } catch (error) {
      let msg = t('auth.verification.resendError');
      let retryAfterSeconds = 60;
      const maybeResponse = axios.isAxiosError(error) ? error.response : (error as any)?.response;
      if (maybeResponse?.data) {
        const data = maybeResponse.data as any;
        if (typeof data === 'string') {
          msg = data;
        } else if (data.message) {
          msg = data.message;
        } else if (data.error) {
          msg = data.error;
        }
        if (typeof data.retryAfterSeconds === 'number') {
          retryAfterSeconds = data.retryAfterSeconds;
        }
      }
      const retryAfterHeader = maybeResponse?.headers?.['retry-after'];
      if (retryAfterHeader && !Number.isNaN(Number(retryAfterHeader))) {
        retryAfterSeconds = Number(retryAfterHeader);
      }
      if (maybeResponse?.status === 429) {
        setResendCooldown(retryAfterSeconds);
        msg = `${msg} (${retryAfterSeconds}s)`;
      }
      showNotification(msg, 'error');
    } finally {
      setResendLoading(false);
    }
  };

  useEffect(() => {
    if (resendCooldown <= 0) {
      return;
    }
    const timer = setInterval(() => {
      setResendCooldown(prev => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
      <FormPaper elevation={3}>
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
            disabled={resendLoading || resendCooldown > 0}
          >
            {resendLoading ? (
              <CircularProgress size={24} />
            ) : resendCooldown > 0 ? (
              `${t('auth.resendCode')} (${resendCooldown}s)`
            ) : (
              t('auth.resendCode')
            )}
          </Button>

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
            <MuiLink component={Link} to="/" color="primary" underline="hover">
              {t('notFound.backHome')}
            </MuiLink>
          </Box>
        </form>
      </FormPaper>
    </Box>
  );
};

export default VerificationPage;