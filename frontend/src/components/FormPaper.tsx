import { Paper } from '@mui/material';
import { styled } from '@mui/material/styles';

// Shared Paper wrapper for auth/profile forms to keep layout consistent.
const FormPaper = styled(Paper)(({ theme }) => ({
    padding: theme.spacing(4),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    borderRadius: theme.spacing(1),
    backgroundColor: theme.palette.background.paper,
    width: '100%',
    maxWidth: 480,
}));

export default FormPaper;

