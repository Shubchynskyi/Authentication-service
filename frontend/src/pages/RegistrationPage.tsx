import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Link as MuiLink,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import { validatePassword } from '../utils/passwordValidation';
import { validatePasswordFlow } from '../utils/passwordChecks';
import { extractErrorMessage } from '../utils/apiError';
import PasswordHint from '../components/PasswordHint';
import PasswordFields from '../components/PasswordFields';
import api from '../api';
import FormPaper from '../components/FormPaper';

const RegistrationPage: React.FC = () => {
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const { showNotification } = useNotification();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [messageType, setMessageType] = useState<'error' | 'success'>('success');

  // Recalculate password error when language changes
  useEffect(() => {
    if (password) {
      const error = validatePassword(password, t);
      setPasswordError(error);
    }
  }, [i18n.language, password, t]);

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
    const strengthError = validatePassword(password, t);
    const passwordCheckError = validatePasswordFlow({
      password,
      confirmPassword,
      t,
      requirePassword: true,
      requireConfirm: true,
    });
    if (passwordCheckError) {
      setIsLoading(false);
      setPasswordError(strengthError || '');
      showNotification(passwordCheckError, 'error');
      return;
    }
    setPasswordError('');

    try {
      const response = await api.post<string>(
        '/api/auth/register',
        { email, name, password }
      );
      setMessage(response.data || t('auth.registerSuccess'));
      setMessageType('success');
      navigate('/verify', { state: { email } });
    } catch (err) {
      setMessageType('error');
      const message = extractErrorMessage(err, {
        fallbackMessage: t('auth.loginError.serverError'),
        transform: (raw) => {
          const lowered = raw.toLowerCase();
          if (lowered.includes('not in whitelist') || lowered.includes('whitelist')) {
            return t('auth.loginError.notInWhitelist');
          }
          return raw;
        },
      });
      setMessage(message);
      console.error('Registration error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <FormPaper elevation={3}>
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
          <PasswordFields
            passwordLabel={t('common.password')}
            confirmLabel={t('common.confirmPassword')}
            password={password}
            confirmPassword={confirmPassword}
            onPasswordChange={(value) => {
              setPassword(value);
              if (value) {
                const error = validatePassword(value, t);
                setPasswordError(error);
              } else {
                setPasswordError('');
              }
            }}
            onConfirmPasswordChange={setConfirmPassword}
            passwordError={passwordError}
            passwordHelperText={passwordError || ''}
            passwordRequired
            confirmRequired
            renderHint={<PasswordHint text={t('errors.passwordRequirements')} />}
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
      </FormPaper>
    </Box>
  );
};

export default RegistrationPage;