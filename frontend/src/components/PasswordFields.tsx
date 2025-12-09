import React from 'react';
import { TextField } from '@mui/material';

interface PasswordFieldsProps {
  passwordLabel: string;
  confirmLabel: string;
  password: string;
  confirmPassword: string;
  onPasswordChange: (value: string) => void;
  onConfirmPasswordChange: (value: string) => void;
  passwordError?: string;
  passwordHelperText?: string;
  passwordRequired?: boolean;
  confirmRequired?: boolean;
  renderHint?: React.ReactNode;
}

const PasswordFields: React.FC<PasswordFieldsProps> = ({
  passwordLabel,
  confirmLabel,
  password,
  confirmPassword,
  onPasswordChange,
  onConfirmPasswordChange,
  passwordError,
  passwordHelperText,
  passwordRequired = false,
  confirmRequired = false,
  renderHint,
}) => {
  return (
    <>
      <TextField
        label={passwordLabel}
        fullWidth
        margin="normal"
        type="password"
        value={password}
        onChange={(e) => onPasswordChange(e.target.value)}
        error={!!passwordError}
        helperText={passwordHelperText || ''}
        required={passwordRequired}
      />
      {renderHint}
      <TextField
        label={confirmLabel}
        fullWidth
        margin="normal"
        type="password"
        value={confirmPassword}
        onChange={(e) => onConfirmPasswordChange(e.target.value)}
        required={confirmRequired}
      />
    </>
  );
};

export default PasswordFields;

