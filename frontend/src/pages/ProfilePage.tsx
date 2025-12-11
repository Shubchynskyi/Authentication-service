import React from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Grid,
  CircularProgress,
} from '@mui/material';
import AdminSection from './AdminSection';
import { useProfile } from '../context/ProfileContext';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import FormPaper from '../components/FormPaper';

const ProfilePage: React.FC = () => {
  const { profile, isLoading } = useProfile();
  const { logout } = useAuth();
  const { t } = useTranslation();

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
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
      <FormPaper elevation={3}>
        <Typography component="h1" variant="h5" align="center" marginBottom={3}>
          {t('profile.title')}
        </Typography>
        <Grid container spacing={2} sx={{ width: '100%' }}>
          <Grid item xs={12}>
            <Typography variant="body1">
              <strong>{t('common.email')}:</strong> {profile.email}
            </Typography>
          </Grid>
          <Grid item xs={12}>
            <Typography variant="body1">
              <strong>{t('common.username')}:</strong> {profile.name}
            </Typography>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="contained"
              fullWidth
              component={Link}
              to="/profile/edit"
            >
              {t('common.editProfile')}
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
                {t('profile.adminPanel')}
              </Button>
            </AdminSection>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="outlined"
              fullWidth
              component={Link}
              to="/"
            >
              {t('notFound.backHome')}
            </Button>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="outlined"
              fullWidth
              onClick={logout}
              color="error"
            >
              {t('common.logout')}
            </Button>
          </Grid>
        </Grid>
      </FormPaper>
    </Box>
  );
};

export default ProfilePage;