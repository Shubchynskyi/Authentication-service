import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
    Box,
    TextField,
    Button,
    Typography,
    Paper,
    CircularProgress,
    Alert,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useProfile } from '../context/ProfileContext';
import { useNotification } from '../context/NotificationContext';
import { useTranslation } from 'react-i18next';
import { validatePassword } from '../utils/passwordValidation';
import PasswordHint from '../components/PasswordHint';
import { validatePasswordFlow } from '../utils/passwordChecks';
import { extractErrorMessage } from '../utils/apiError';
import PasswordFields from '../components/PasswordFields';

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

const EditProfilePage: React.FC = () => {
    const navigate = useNavigate();
    const { profile, updateProfile, isLoading } = useProfile();
    const { showNotification } = useNotification();
    const { t, i18n } = useTranslation();
    
    const [name, setName] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [newPasswordError, setNewPasswordError] = useState('');

    // Recalculate password error when language changes
    useEffect(() => {
        if (newPassword) {
            const error = validatePassword(newPassword, t);
            setNewPasswordError(error);
        }
    }, [i18n.language, newPassword, t]);

    useEffect(() => {
        if (profile?.name) {
            setName(profile.name);
        }
    }, [profile]);

    const handleUpdate = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (newPassword) {
            const strengthError = validatePassword(newPassword, t);
            const passwordCheckError = validatePasswordFlow({
                password: newPassword,
                confirmPassword,
                currentPassword,
                t,
                requireConfirm: true,
                requireCurrent: true,
            });
            if (passwordCheckError) {
                setNewPasswordError(strengthError || '');
                showNotification(passwordCheckError, 'error');
                return;
            }
            setNewPasswordError('');
        }

        try {
            await updateProfile({
                name,
                password: newPassword,
                currentPassword: currentPassword,
            });
            showNotification(t('profile.notifications.updateSuccess'), 'success');
            navigate('/profile', { replace: true });
        } catch (error) {
            const message = extractErrorMessage(error, {
                fallbackMessage: t('profile.notifications.updateError'),
                transform: (raw) => {
                    const lowered = raw.toLowerCase();
                    if (lowered.includes('incorrect current password') || lowered.includes('wrong current password')) {
                        return t('profile.notifications.incorrectCurrentPassword');
                    }
                    // For other cases, keep previous behavior: show generic updateError
                    return t('profile.notifications.updateError');
                },
            });
            showNotification(message, 'error');
        }
    };

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    const isGoogleUser = profile?.authProvider === 'GOOGLE';

    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    {t('profile.editTitle')}
                </Typography>
                <form onSubmit={handleUpdate} style={{ width: '100%' }} noValidate>
                    <TextField
                        label={t('common.name')}
                        fullWidth
                        margin="normal"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                        InputLabelProps={{ required: false }}
                    />
                    {isGoogleUser ? (
                        <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
                            {t('profile.googleAuthInfo')}
                        </Alert>
                    ) : (
                        <>
                            <TextField
                                label={t('common.currentPassword')}
                                fullWidth
                                margin="normal"
                                type="password"
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                required={!!newPassword}
                            />
                            <PasswordFields
                                passwordLabel={`${t('common.newPassword')} (${t('common.optional')})`}
                                confirmLabel={t('common.confirmPassword')}
                                password={newPassword}
                                confirmPassword={confirmPassword}
                                onPasswordChange={(value) => {
                                    setNewPassword(value);
                                    if (value) {
                                        const error = validatePassword(value, t);
                                        setNewPasswordError(error);
                                    } else {
                                        setNewPasswordError('');
                                    }
                                }}
                                onConfirmPasswordChange={setConfirmPassword}
                                passwordError={newPasswordError}
                                passwordHelperText={newPasswordError || ''}
                                confirmRequired={!!newPassword}
                                renderHint={<PasswordHint text={t('errors.passwordRequirements')} />}
                            />
                        </>
                    )}
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : t('common.save')}
                    </Button>
                    <Button
                        variant="outlined"
                        fullWidth
                        component={Link}
                        to="/"
                        sx={{ mb: 2 }}
                    >
                        {t('notFound.backHome')}
                    </Button>
                    <Button
                        variant="outlined"
                        fullWidth
                        component={Link}
                        to="/profile"
                    >
                        {t('profile.backToProfile')}
                    </Button>
                </form>
            </StyledPaper>
        </Box>
    );
};

export default EditProfilePage;