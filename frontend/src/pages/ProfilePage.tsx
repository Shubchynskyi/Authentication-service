import React from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  Grid,
  CircularProgress,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import AdminSection from './AdminSection';
import { useProfile } from '../context/ProfileContext';
import { useAuth } from '../context/AuthContext';

const StyledPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(4),
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  borderRadius: theme.spacing(1),
  backgroundColor: theme.palette.background.paper,
}));

const ProfilePage: React.FC = () => {
  const { profile, isLoading } = useProfile();
  const { logout } = useAuth();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!profile) {
    return null;
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
      <StyledPaper elevation={3}>
        <Typography component="h1" variant="h5" align="center" marginBottom={3}>
          My Profile
        </Typography>
        <Grid container spacing={2} sx={{ width: '100%' }}>
          <Grid item xs={12}>
            <Typography variant="body1">
              <strong>Email:</strong> {profile.email}
            </Typography>
          </Grid>
          <Grid item xs={12}>
            <Typography variant="body1">
              <strong>Name:</strong> {profile.name}
            </Typography>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="contained"
              fullWidth
              component={Link}
              to="/profile/edit"
            >
              Edit Profile
            </Button>
          </Grid>
          <Grid item xs={12}>
            <AdminSection>
              <Button
                variant="contained"
                fullWidth
                component={Link}
                to="/admin"
                color="secondary"
              >
                Admin Panel
              </Button>
            </AdminSection>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="outlined"
              fullWidth
              onClick={logout}
              color="error"
            >
              Logout
            </Button>
          </Grid>
        </Grid>
      </StyledPaper>
    </Box>
  );
};

export default ProfilePage;