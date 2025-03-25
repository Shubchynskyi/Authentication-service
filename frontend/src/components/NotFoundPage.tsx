import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import { styled } from '@mui/material/styles';

const StyledPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
}));

const NotFoundPage = () => {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
            <StyledPaper elevation={3}>
                <Typography variant="h4" component="h1" gutterBottom>
                    404
                </Typography>
                <Typography variant="h6" component="h2">
                    Страница не найдена
                </Typography>
            </StyledPaper>
        </Box>
    );
};

export default NotFoundPage; 