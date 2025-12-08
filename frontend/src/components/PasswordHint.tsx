import React from 'react';
import { Box, Typography } from '@mui/material';

interface PasswordHintProps {
  text: string;
}

/**
 * Reusable block for showing password requirements with fixed height
 * and subtle border/background so multi-line translations fit without layout shift.
 */
const PasswordHint: React.FC<PasswordHintProps> = ({ text }) => (
  <Box
    sx={{
      mt: 1,
      p: 1.25,
      borderRadius: 1,
      border: '1px solid',
      borderColor: 'divider',
      backgroundColor: 'background.default',
      minHeight: 120, // fits up to 4+ lines for long translations
      display: 'flex',
      alignItems: 'center',
    }}
  >
    <Typography
      variant="body2"
      color="text.secondary"
      sx={{ lineHeight: 1.5, wordBreak: 'break-word' }}
    >
      {text}
    </Typography>
  </Box>
);

export default PasswordHint;

