import { Box, Typography, Paper, Button } from '@mui/material';
import { styled } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

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

const NotFoundPage = () => {
    const { t } = useTranslation();
    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
            <StyledPaper elevation={3}>
                <Typography variant="h4" component="h1" gutterBottom>
                    {t('notFound.title')}
                </Typography>
                <Typography variant="body1" gutterBottom>
                    {t('notFound.description')}
                </Typography>
                <Button variant="contained" component={Link} to="/" sx={{ mt: 3 }}>
                    {t('notFound.backHome')}
                </Button>
            </StyledPaper>
        </Box>
    );
};

export default NotFoundPage; 