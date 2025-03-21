// ForgotPasswordPage.tsx
import React, { useState } from 'react';
import axios from 'axios';
import { Box, TextField, Button, Typography, Alert, Paper, CircularProgress } from '@mui/material';
import { styled } from '@mui/material/styles';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
}));

const ForgotPasswordPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [messageType, setMessageType] = useState<'error' | 'success'>('success');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await axios.post('http://localhost:8080/api/auth/forgot-password', { email });
            setMessage('If an account with that email exists, a password reset link has been sent.');
            setMessageType('success')
        } catch {
             setMessageType('error')
            setMessage('An error occurred. Please try again.');

        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <StyledPaper elevation={3}>
                <Typography component="h1" variant="h5" align="center" marginBottom={3}>
                    Forgot Password
                </Typography>
                <Typography variant="body1" align="center" marginBottom={3}>
                    Enter your email address and we'll send you a link to reset your password.
                </Typography>
                <form onSubmit={handleSubmit} style={{ width: '100%' }}>
                    <TextField
                        label="Email"
                        fullWidth
                        margin="normal"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoading}
                    >
                        {isLoading ? <CircularProgress size={24} /> : 'Reset Password'}
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

export default ForgotPasswordPage;