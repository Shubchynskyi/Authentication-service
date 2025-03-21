import React, { ReactNode } from 'react';
import { Container, Box, AppBar, Toolbar, Typography, Button } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from './LanguageSwitcher';

interface LayoutProps {
  children: ReactNode;
  title?: string;
}

const Layout: React.FC<LayoutProps> = ({ children, title }) => {
  const { t } = useTranslation();
  
  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            {title || t('common.welcome')}
          </Typography>
          <Button color="inherit" component={RouterLink} to="/">
            {t('common.welcome')}
          </Button>
          <Button color="inherit" component={RouterLink} to="/login">
            {t('common.login')}
          </Button>
          <Button color="inherit" component={RouterLink} to="/register">
            {t('common.register')}
          </Button>
          <LanguageSwitcher />
        </Toolbar>
      </AppBar>
      <Container maxWidth="md">
        <Box sx={{ my: 4 }}>
          {children}
        </Box>
      </Container>
    </>
  );
};

export default Layout; 