import React, { useState, useEffect, useCallback } from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Tooltip,
    TablePagination,
    Tab,
    Tabs,
    List,
    ListItem,
    ListItemText,
    ListItemSecondaryAction,
    Typography,
    FormControlLabel,
    Checkbox
} from '@mui/material';
import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import api from '../api';
import axios from 'axios';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../context/NotificationContext';
import AddIcon from '@mui/icons-material/Add';
import GoogleIcon from '@mui/icons-material/Google';
import PersonIcon from '@mui/icons-material/Person';

interface User {
    id: number;
    username: string;
    email: string;
    roles: string[];
    enabled: boolean;
    blocked: boolean;
    emailVerified: boolean;
    blockReason?: string;
    lastLoginAt?: string;
    authProvider?: 'LOCAL' | 'GOOGLE';
}

interface BlacklistEntry {
    email: string;
    reason?: string | null;
}

interface WhitelistEntry {
    email: string;
    reason?: string | null;
}

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

function TabPanel(props: TabPanelProps) {
    const { children, value, index, ...other } = props;
    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            {...other}
        >
            {value === index && (
                <Box sx={{ p: 3 }}>
                    {children}
                </Box>
            )}
        </div>
    );
}

const AdminPage = () => {
    const { t } = useTranslation();
    const { showNotification } = useNotification();
    const [tabValue, setTabValue] = useState(0);
    const [users, setUsers] = useState<User[]>([]);
    const [whitelist, setWhitelist] = useState<WhitelistEntry[]>([]);
    const [blacklist, setBlacklist] = useState<BlacklistEntry[]>([]);
    const [accessMode, setAccessMode] = useState<'WHITELIST' | 'BLACKLIST'>('WHITELIST');
    const [totalElements, setTotalElements] = useState(0);
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [openDialog, setOpenDialog] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [newEmail, setNewEmail] = useState('');
    const [newBlacklistEmail, setNewBlacklistEmail] = useState('');
    const [whitelistReason, setWhitelistReason] = useState('');
    const [blacklistReason, setBlacklistReason] = useState('');
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        roles: ['ROLE_USER'] as string[],
        enabled: true,
        blocked: false,
        blockReason: ''
    });
    const [confirmActionDialog, setConfirmActionDialog] = useState(false);
    const [confirmActionPassword, setConfirmActionPassword] = useState('');
    const [confirmBlacklistDialog, setConfirmBlacklistDialog] = useState(false);
    const [pendingAction, setPendingAction] = useState<{
        type: 'DELETE_USER' | 'ADD_WHITELIST' | 'REMOVE_WHITELIST' | 'ADD_BLACKLIST' | 'REMOVE_BLACKLIST';
        data: any;
        reason?: string;
    } | null>(null);

    const [confirmAdminDialog, setConfirmAdminDialog] = useState(false);
    const [adminPassword, setAdminPassword] = useState('');
    const [changeModeDialog, setChangeModeDialog] = useState(false);
    const [modeChangePassword, setModeChangePassword] = useState('');
    const [modeChangeOtp, setModeChangeOtp] = useState('');
    const [modeChangeReason, setModeChangeReason] = useState('');
    const [otpRequested, setOtpRequested] = useState(false);

    // Limit for block reason length
    const MAX_BLOCK_REASON_LENGTH = 200; // Maximum symbols for block reason
    const accessModeLabel = accessMode === 'WHITELIST' ? t('admin.accessMode.whitelist') : t('admin.accessMode.blacklist');
    const accessModeChipColor: 'success' | 'error' = accessMode === 'WHITELIST' ? 'success' : 'error';

    const fetchWhitelist = async () => {
        try {
            const response = await api.get('/api/admin/whitelist');
            const data = Array.isArray(response.data) ? response.data : [];
            const normalized = data.map((entry: any) => ({
                email: typeof entry === 'string' ? entry : entry?.email || '',
                reason: typeof entry === 'object' ? entry?.reason : null
            })).filter((entry: any) => !!entry.email);
            setWhitelist(normalized);
        } catch (error) {
            console.error('Error fetching whitelist:', error);
            setWhitelist([]); // Set empty array on error
        }
    };

    const fetchBlacklist = async () => {
        try {
            const response = await api.get('/api/admin/blacklist');
            const data = Array.isArray(response.data) ? response.data : [];
            const normalized = data.map((entry: any) => ({
                email: entry.email || entry,
                reason: entry.reason || null
            }));
            setBlacklist(normalized);
        } catch (error) {
            console.error('Error fetching blacklist:', error);
            setBlacklist([]);
        }
    };

    const fetchAccessMode = async () => {
        try {
            const response = await api.get('/api/admin/access-mode');
            if (response.data && response.data.mode) {
                setAccessMode(response.data.mode);
            }
        } catch (error) {
            console.error('Error fetching access mode:', error);
        }
    };

    const fetchUsers = useCallback(async () => {
        try {
            const response = await api.get('/api/admin/users', {
                params: {
                    page,
                    size: rowsPerPage
                }
            });
            const content = Array.isArray(response.data?.content) ? response.data.content : [];
            const total = typeof response.data?.totalElements === 'number'
                ? response.data.totalElements
                : content.length;
            setUsers(content);
            setTotalElements(total);
        } catch (error) {
            console.error('Error fetching users:', error);
            showNotification(t('common.error'), 'error');
        }
    }, [page, rowsPerPage, showNotification, t]);

    useEffect(() => {
        fetchWhitelist();
        fetchBlacklist();
        fetchAccessMode();
    }, []);

    useEffect(() => {
        fetchUsers();
    }, [page, rowsPerPage, fetchUsers]);

    const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    const handleChangePage = (_event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleOpenDialog = (user?: User) => {
        if (user) {
            setSelectedUser(user);
            setFormData({
                username: user.username,
                email: user.email,
                roles: user.roles,
                enabled: user.enabled,
                blocked: user.blocked,
                blockReason: user.blockReason || ''
            });
        } else {
            setSelectedUser(null);
            setFormData({
                username: '',
                email: '',
                roles: ['ROLE_USER'],
                enabled: true,
                blocked: false,
                blockReason: ''
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setSelectedUser(null);
        setFormData({
            username: '',
            email: '',
            roles: ['ROLE_USER'],
            enabled: true,
            blocked: false,
            blockReason: ''
        });
    };

    const handleRoleChange = (isAdmin: boolean) => {
        if (isAdmin && !formData.roles.includes('ROLE_ADMIN')) {
            setConfirmAdminDialog(true);
        } else {
            setFormData({
                ...formData,
                roles: ['ROLE_USER']
            });
        }
    };

    const handleConfirmAdmin = async () => {
        try {
            await api.post('/api/admin/verify-admin', { password: adminPassword });
            const nextRoles = Array.from(new Set([...(formData.roles || []), 'ROLE_ADMIN', 'ROLE_USER']));
            setFormData({ ...formData, roles: nextRoles });
            setConfirmAdminDialog(false);
            setAdminPassword('');
            showNotification(t('admin.notifications.adminGranted'), 'success');
        } catch (error) {
            setAdminPassword('');
            showNotification(t('admin.errors.invalidAdminPassword'), 'error');
        }
    };

    const handleSubmit = async () => {
        try {
            const trimmedEmail = formData.email.trim().toLowerCase();
            const blacklistedEntry = blacklist.find(
                (entry) => entry.email.toLowerCase() === trimmedEmail
            );
            if (!selectedUser && blacklistedEntry) {
                const reasonPart = blacklistedEntry.reason
                    ? `\n${t('admin.blacklist.reason')}: ${blacklistedEntry.reason}`
                    : '';
                const warning = `${t('admin.blacklist.userCreateWarning')}${reasonPart}\n${t('admin.blacklist.userWillBeRemovedFromList')}`;
                const confirmed = window.confirm(warning);
                if (!confirmed) {
                    return;
                }
            }

            const url = selectedUser
                ? `/api/admin/users/${selectedUser.id}`
                : '/api/admin/users';
            
            const requestData = {
                username: formData.username,
                email: formData.email,
                roles: formData.roles,
                isBlocked: formData.blocked,
                blockReason: formData.blocked ? formData.blockReason : null
            };
            
            const method = selectedUser ? 'put' : 'post';
            const response = await api[method](url, requestData);

            if (response.status === 200 || response.status === 201) {
                if (selectedUser) {
                    const prev = selectedUser.roles || [];
                    const next = Array.from(new Set(formData.roles || []));
                    const rolesChanged = prev.length !== next.length || !prev.every(r => next.includes(r));
                    if (rolesChanged) {
                        await api.put(`/api/admin/users/${selectedUser.id}/roles`, { roles: next });
                    }
                }

                handleCloseDialog();
                fetchUsers();
                showNotification(t('admin.notifications.userSaved'), 'success');
            }
        } catch (error) {
            if (axios.isAxiosError(error)) {
                const status = error.response?.status;
                const data = String(error.response?.data || '').toLowerCase();
                if (status === 409 || data.includes('already') || data.includes('exists')) {
                    showNotification(t('admin.errors.emailAlreadyExists'), 'error');
                } else if (status === 400 && data.includes('email')) {
                    showNotification(t('errors.invalidEmail'), 'error');
                } else {
                    showNotification(t('common.error'), 'error');
                }
            } else {
                showNotification(t('common.error'), 'error');
            }
        }
    };

    const handleConfirmAction = async () => {
        try {
            await api.post('/api/admin/verify-admin', { password: confirmActionPassword });
            
            switch (pendingAction?.type) {
                case 'DELETE_USER':
                    await api.delete(`/api/admin/users/${pendingAction.data}`);
                    fetchUsers();
                    showNotification(t('admin.notifications.userDeleted'), 'success');
                    break;
                case 'ADD_WHITELIST':
                    try {
                        await api.post('/api/admin/whitelist/add', null, {
                            params: { 
                                email: pendingAction.data,
                                reason: pendingAction.reason ?? ''
                            }
                        });
                        setNewEmail('');
                        setWhitelistReason('');
                        fetchWhitelist();
                        fetchUsers();
                        showNotification(t('admin.whitelist.added'), 'success');
                    } catch (addErr) {
                        if (axios.isAxiosError(addErr)) {
                            const status = addErr.response?.status;
                            const data = String(addErr.response?.data || '').toLowerCase();
                            if (data.includes('already') || data.includes('exists')) {
                                showNotification(t('admin.whitelist.emailAlreadyExists'), 'error');
                            } else if (status === 400 && (data.includes('email') || data.includes('invalid'))) {
                                showNotification(t('admin.whitelist.invalidEmail'), 'error');
                            } else {
                                showNotification(t('common.error'), 'error');
                            }
                        } else {
                            showNotification(t('common.error'), 'error');
                        }
                    }
                    break;
                case 'REMOVE_WHITELIST':
                    await api.delete('/api/admin/whitelist/remove', {
                        params: { 
                            email: pendingAction.data,
                            reason: pendingAction.reason ?? ''
                        }
                    });
                    fetchWhitelist();
                    fetchUsers();
                    setWhitelistReason('');
                    showNotification(t('admin.whitelist.removed'), 'success');
                    break;
                case 'ADD_BLACKLIST':
                    try {
                        const response = await api.post('/api/admin/blacklist/add', null, {
                            params: { 
                                email: pendingAction.data,
                                reason: pendingAction.reason || ''
                            }
                        });
                        setNewBlacklistEmail('');
                        setBlacklistReason('');
                        fetchBlacklist();
                        const serverMessage = typeof response.data === 'string'
                            ? response.data
                            : response.data?.message;
                        const userBlocked = typeof response.data === 'object' && !!response.data?.userBlocked;
                        showNotification(serverMessage || t('admin.blacklist.added'), 'success');
                        if (userBlocked) {
                            showNotification(t('admin.blacklist.userBlockedNow'), 'warning');
                        }
                        // Refresh users to reflect potential blocks
                        fetchUsers();
                    } catch (addErr) {
                        if (axios.isAxiosError(addErr)) {
                            const status = addErr.response?.status;
                            const data = String(addErr.response?.data || '').toLowerCase();
                            if (data.includes('already') || data.includes('exists')) {
                                showNotification(t('admin.blacklist.emailAlreadyExists'), 'error');
                            } else if (status === 400 && (data.includes('email') || data.includes('invalid'))) {
                                showNotification(t('admin.blacklist.invalidEmail'), 'error');
                            } else {
                                showNotification(t('common.error'), 'error');
                            }
                        } else {
                            showNotification(t('common.error'), 'error');
                        }
                    }
                    break;
                case 'REMOVE_BLACKLIST':
                    await api.delete('/api/admin/blacklist/remove', {
                        params: { 
                            email: pendingAction.data,
                            reason: pendingAction.reason || ''
                        }
                    });
                    fetchBlacklist();
                    fetchUsers();
                    setBlacklistReason('');
                    showNotification(t('admin.blacklist.removed'), 'success');
                    break;
            }
            
            setConfirmActionDialog(false);
            setConfirmActionPassword('');
            setPendingAction(null);
        } catch (error) {
            if (axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) {
                showNotification(t('admin.errors.invalidAdminPassword'), 'error');
            } else {
                showNotification(t('common.error'), 'error');
            }
            setConfirmActionDialog(false);
            setConfirmActionPassword('');
        }
    };

    const handleDelete = (userId: number) => {
        setPendingAction({ type: 'DELETE_USER', data: userId });
        setConfirmActionDialog(true);
    };

    const handleAddToWhitelist = () => {
        if (!newEmail) return;
        if (!newEmail.includes('@')) {
            showNotification(t('admin.whitelist.invalidEmail'), 'error');
            return;
        }
        setPendingAction({ type: 'ADD_WHITELIST', data: newEmail, reason: whitelistReason });
        setConfirmActionDialog(true);
    };

    const handleRemoveFromWhitelist = (email: string) => {
        setPendingAction({ type: 'REMOVE_WHITELIST', data: email, reason: '' });
        setConfirmActionDialog(true);
    };

    const handleAddToBlacklist = () => {
        if (!newBlacklistEmail) return;
        if (!newBlacklistEmail.includes('@')) {
            showNotification(t('admin.blacklist.invalidEmail'), 'error');
            return;
        }
        setPendingAction({ type: 'ADD_BLACKLIST', data: newBlacklistEmail, reason: blacklistReason });
        setConfirmBlacklistDialog(true);
    };

    const handleConfirmBlacklistWarning = () => {
        setConfirmBlacklistDialog(false);
        setConfirmActionDialog(true);
    };

    const handleRemoveFromBlacklist = (email: string) => {
        setPendingAction({ type: 'REMOVE_BLACKLIST', data: email, reason: '' });
        setConfirmActionDialog(true);
    };

    const handleRequestOtp = async () => {
        try {
            await api.post('/api/admin/access-mode/request-otp');
            setOtpRequested(true);
            showNotification(t('admin.accessMode.otpSent'), 'success');
        } catch (error) {
            showNotification(t('common.error'), 'error');
        }
    };

    const handleChangeMode = async () => {
        if (!modeChangePassword || !modeChangeOtp || !modeChangeReason) {
            showNotification(t('admin.accessMode.reasonRequired'), 'error');
            return;
        }
        try {
            const newMode = accessMode === 'WHITELIST' ? 'BLACKLIST' : 'WHITELIST';
            await api.post('/api/admin/access-mode/change', {
                mode: newMode,
                password: modeChangePassword,
                otpCode: modeChangeOtp,
                reason: modeChangeReason
            });
            setChangeModeDialog(false);
            setModeChangePassword('');
            setModeChangeOtp('');
            setModeChangeReason('');
            setOtpRequested(false);
            fetchAccessMode();
            showNotification(t('admin.accessMode.modeChanged'), 'success');
        } catch (error) {
            if (axios.isAxiosError(error)) {
                const data = String(error.response?.data || '').toLowerCase();
                if (data.includes('otp') || data.includes('invalid')) {
                    showNotification(t('admin.accessMode.invalidOtp'), 'error');
                } else if (data.includes('password')) {
                    showNotification(t('admin.errors.invalidAdminPassword'), 'error');
                } else {
                    showNotification(t('common.error'), 'error');
                }
            } else {
                showNotification(t('common.error'), 'error');
            }
        }
    };

    return (
        <Box sx={{ width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'flex-start', mb: 2 }}>
                <Chip
                    label={`${t('admin.accessMode.currentMode')}: ${accessModeLabel}`}
                    color={accessModeChipColor}
                    sx={{ fontWeight: 600 }}
                />
            </Box>
            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                <Tabs value={tabValue} onChange={handleTabChange}>
                    <Tab label={t('admin.users')} />
                    <Tab label={t('admin.whitelistTab')} />
                    <Tab label={t('admin.blacklistTab')} />
                    <Tab label={t('admin.accessControlTab')} />
                </Tabs>
            </Box>

            <TabPanel value={tabValue} index={0}>
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1 }}>
                    <Button
                        variant="contained"
                        onClick={() => handleOpenDialog()}
                        sx={{ width: 240 }}
                    >
                        {t('admin.addUser')}
                    </Button>
                </Box>

                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>{t('common.id')}</TableCell>
                                <TableCell>{t('common.username')}</TableCell>
                                <TableCell>{t('common.email')}</TableCell>
                                <TableCell>{t('admin.lastLogin')}</TableCell>
                                <TableCell>{t('admin.source')}</TableCell>
                                <TableCell>{t('admin.status')}</TableCell>
                                <TableCell>{t('admin.actions')}</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {users.map((user) => (
                                <TableRow key={user.id}>
                                    <TableCell>{user.id}</TableCell>
                                    <TableCell>{user.username} {user.roles.includes('ROLE_ADMIN') && (
  <span
    title="Administrator"
    aria-label="Administrator"
    style={{
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: 18,
      height: 18,
      marginLeft: 6,
      borderRadius: '50%',
      backgroundColor: '#e53935',
      color: '#ffffff',
      fontWeight: 'bold',
      fontSize: 12,
      lineHeight: '18px'
    }}
  >A</span>
)}</TableCell>
                                    <TableCell>{user.email}</TableCell>
                                    <TableCell>
                                        {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString(undefined, {
                                            year: 'numeric', month: '2-digit', day: '2-digit',
                                            hour: '2-digit', minute: '2-digit'
                                        }) : '—'}
                                    </TableCell>
                                    <TableCell>
                                        {user.authProvider === 'GOOGLE' ? (
                                            <Tooltip title={t('admin.sourceGoogle')}><GoogleIcon fontSize="small" /></Tooltip>
                                        ) : (
                                            <Tooltip title={t('admin.sourceLocal')}><PersonIcon fontSize="small" /></Tooltip>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        {user.blocked ? (
                                            <React.Fragment>
                                                <span style={{ fontWeight: 'bold', color: 'red' }}>{t('admin.statusBlocked')}</span>
                                                {user.blockReason && (
                                                    <div style={{ fontSize: '0.92em', color: '#cc3535', marginTop: 2, fontWeight: 'bold' }}>
                                                        {user.blockReason}
                                                    </div>
                                                )}
                                            </React.Fragment>
                                        ) :
                                            !user.enabled ? t('admin.statusDisabled')
                                                : !user.emailVerified ? t('admin.statusNotVerified')
                                                    : t('admin.statusActive')}
                                    </TableCell>
                                    <TableCell>
                                        <Tooltip title={t('admin.edit')}>
                                            <IconButton aria-label={t('admin.edit')} onClick={() => handleOpenDialog(user)}>
                                                <EditIcon />
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title={t('admin.delete')}>
                                            <IconButton aria-label={t('admin.delete')} onClick={() => handleDelete(user.id)}>
                                                <DeleteIcon />
                                            </IconButton>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                    {(totalElements > rowsPerPage) && (
                        <TablePagination
                            component="div"
                            count={totalElements}
                            page={page}
                            onPageChange={handleChangePage}
                            rowsPerPage={rowsPerPage}
                            onRowsPerPageChange={handleChangeRowsPerPage}
                            rowsPerPageOptions={[5, 10, 25]}
                            labelDisplayedRows={({ from, to, count }) => `${from}–${to} ${t('common.of')} ${count}`}
                            labelRowsPerPage={t('common.rowsPerPage')}
                        />
                    )}
                </TableContainer>
            </TabPanel>

            <TabPanel value={tabValue} index={1}>
                <Box sx={{ maxWidth: 600, mx: 'auto' }}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 2 }}>
                        <TextField
                            fullWidth
                            label={t('admin.whitelistEmailLabel')}
                            value={newEmail}
                            onChange={(e) => setNewEmail(e.target.value)}
                        />
                        <TextField
                            fullWidth
                            label={t('admin.reasonLabel')}
                            value={whitelistReason}
                            onChange={(e) => setWhitelistReason(e.target.value)}
                            multiline
                            rows={2}
                        />
                        <Button
                            variant="contained"
                            onClick={handleAddToWhitelist}
                            disabled={!newEmail}
                            startIcon={<AddIcon />}
                            sx={{ width: 180, alignSelf: 'flex-start' }}
                        >
                            {t('admin.whitelistAdd')}
                        </Button>
                    </Box>

                    <List>
                        {(Array.isArray(whitelist) ? whitelist : []).map((entry) => (
                            <ListItem key={entry.email}>
                                <ListItemText
                                    primary={entry.email}
                                    secondary={entry.reason || undefined}
                                    secondaryTypographyProps={{ sx: { whiteSpace: 'pre-wrap' } }}
                                />
                                <ListItemSecondaryAction>
                                    <IconButton
                                        edge="end"
                                        aria-label={t('admin.delete')}
                                        onClick={() => handleRemoveFromWhitelist(entry.email)}
                                    >
                                        <DeleteIcon />
                                    </IconButton>
                                </ListItemSecondaryAction>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </TabPanel>

            <TabPanel value={tabValue} index={2}>
                <Box sx={{ maxWidth: 600, mx: 'auto' }}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 2 }}>
                        <TextField
                            fullWidth
                            label={t('admin.whitelistEmailLabel')}
                            value={newBlacklistEmail}
                            onChange={(e) => setNewBlacklistEmail(e.target.value)}
                        />
                        <TextField
                            fullWidth
                            label={t('admin.reasonLabel')}
                            value={blacklistReason}
                            onChange={(e) => setBlacklistReason(e.target.value)}
                            multiline
                            rows={2}
                        />
                        <Button
                            variant="contained"
                            onClick={handleAddToBlacklist}
                            disabled={!newBlacklistEmail}
                            startIcon={<AddIcon />}
                            sx={{ width: 180, alignSelf: 'flex-start' }}
                        >
                            {t('admin.whitelistAdd')}
                        </Button>
                    </Box>

                    <List>
                        {(Array.isArray(blacklist) ? blacklist : []).map((entry) => (
                            <ListItem key={entry.email}>
                                <ListItemText
                                    primary={entry.email}
                                    secondary={entry.reason || undefined}
                                    secondaryTypographyProps={{ sx: { whiteSpace: 'pre-wrap' } }}
                                />
                                <ListItemSecondaryAction>
                                    <IconButton
                                        edge="end"
                                        aria-label={t('admin.delete')}
                                        onClick={() => handleRemoveFromBlacklist(entry.email)}
                                    >
                                        <DeleteIcon />
                                    </IconButton>
                                </ListItemSecondaryAction>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </TabPanel>

            <TabPanel value={tabValue} index={3}>
                <Box sx={{ maxWidth: 800, mx: 'auto' }}>
                    <Paper sx={{ p: 3, mb: 3 }}>
                        <Typography variant="h6" gutterBottom>
                            {t('admin.accessMode.title')}
                        </Typography>
                        <Box sx={{ display: 'flex', justifyContent: 'flex-start', mb: 1 }}>
                            <Chip
                                label={`${t('admin.accessMode.currentMode')}: ${accessModeLabel}`}
                                color={accessModeChipColor}
                                sx={{ fontWeight: 600 }}
                            />
                        </Box>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>
                            {accessMode === 'WHITELIST' 
                                ? t('admin.accessMode.whitelistDescription')
                                : t('admin.accessMode.blacklistDescription')}
                        </Typography>
                        <Button
                            variant="contained"
                            color="primary"
                            onClick={() => setChangeModeDialog(true)}
                        >
                            {t('admin.accessMode.changeMode')}
                        </Button>
                    </Paper>
                </Box>
            </TabPanel>

            <Dialog open={openDialog} onClose={handleCloseDialog}>
                <DialogTitle>
                    {selectedUser ? t('admin.editUser') : t('admin.addUser')}
                </DialogTitle>
                <DialogContent>
                    <TextField
                        margin="normal"
                        fullWidth
                        label={t('common.username')}
                        value={formData.username}
                        onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    />
                    <TextField
                        margin="normal"
                        fullWidth
                        label={t('common.email')}
                        value={formData.email}
                        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    />
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={formData.roles.includes('ROLE_ADMIN')}
                                onChange={(e) => handleRoleChange(e.target.checked)}
                            />
                        }
                        label={t('admin.adminLabel')}
                    />
                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={formData.blocked}
                                onChange={(e) => setFormData({ 
                                    ...formData, 
                                    blocked: e.target.checked,
                                    blockReason: e.target.checked ? formData.blockReason : ''
                                })}
                            />
                        }
                        label={t('admin.blockedLabel')}
                    />
                    {formData.blocked && (
                        <TextField
                            margin="normal"
                            fullWidth
                            label={t('admin.blockReasonLabel')}
                            value={formData.blockReason}
                            onChange={(e) => {
                                const value = e.target.value;
                                setFormData({ ...formData, blockReason: value.slice(0, MAX_BLOCK_REASON_LENGTH) });
                            }}
                            required
                            error={formData.blocked && !formData.blockReason}
                            helperText={formData.blocked && !formData.blockReason ? t('admin.blockReasonRequired') : `${formData.blockReason.length}/${MAX_BLOCK_REASON_LENGTH}`}
                            inputProps={{ maxLength: MAX_BLOCK_REASON_LENGTH }}
                        />
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDialog}>{t('common.cancel')}</Button>
                    <Button 
                        onClick={handleSubmit}
                        disabled={formData.blocked && !formData.blockReason}
                    >
                        {t('common.save')}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={confirmActionDialog} onClose={() => {
                setConfirmActionDialog(false);
                setConfirmActionPassword('');
                setPendingAction(null);
                setWhitelistReason('');
                setBlacklistReason('');
            }}>
                <DialogTitle>{t('admin.confirmActionTitle')}</DialogTitle>
                <DialogContent>
                    <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <Typography gutterBottom>
                            {t('admin.confirmActionPrompt')}
                        </Typography>
                        {pendingAction?.reason && (
                            <Typography variant="body2" color="text.secondary">
                                <strong>{t('admin.reasonLabel')}:</strong> {pendingAction.reason}
                            </Typography>
                        )}
                        <TextField
                            type="password"
                            label={t('admin.adminPasswordLabel')}
                            value={confirmActionPassword}
                            onChange={(e) => setConfirmActionPassword(e.target.value)}
                            fullWidth
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => {
                        setConfirmActionDialog(false);
                        setConfirmActionPassword('');
                        setPendingAction(null);
                        setWhitelistReason('');
                        setBlacklistReason('');
                    }}>{t('common.cancel')}</Button>
                    <Button onClick={handleConfirmAction} variant="contained">
                        {t('common.submit')}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={confirmBlacklistDialog} onClose={() => {
                setConfirmBlacklistDialog(false);
                setPendingAction(null);
            }}>
                <DialogTitle>{t('admin.blacklist.warningTitle')}</DialogTitle>
                <DialogContent>
                    <Box sx={{ pt: 2 }}>
                        <Typography
                            gutterBottom
                            color="warning.main"
                            fontWeight="bold"
                            sx={{ textAlign: 'center', whiteSpace: 'pre-line' }}
                        >
                            ⚠️ {t('admin.blacklist.warningMessage')}
                        </Typography>
                        <Typography variant="body2" sx={{ mt: 2 }}>
                            {t('admin.blacklist.warningDetails')}
                        </Typography>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => {
                        setConfirmBlacklistDialog(false);
                        setPendingAction(null);
                        setNewBlacklistEmail('');
                        setBlacklistReason('');
                    }}>{t('common.cancel')}</Button>
                    <Button onClick={handleConfirmBlacklistWarning} variant="contained" color="warning">
                        {t('common.confirm')}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={confirmAdminDialog} onClose={() => setConfirmAdminDialog(false)}>
                <DialogTitle>{t('admin.assignAdminTitle')}</DialogTitle>
                <DialogContent>
                    <Box sx={{ pt: 2 }}>
                        <Typography gutterBottom>
                            {t('admin.assignAdminPrompt')}
                        </Typography>
                        <TextField
                            type="password"
                            label={t('admin.adminPasswordLabel')}
                            value={adminPassword}
                            onChange={(e) => setAdminPassword(e.target.value)}
                            fullWidth
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmAdminDialog(false)}>{t('common.cancel')}</Button>
                    <Button onClick={handleConfirmAdmin} variant="contained">
                        {t('common.submit')}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={changeModeDialog} onClose={() => {
                setChangeModeDialog(false);
                setModeChangePassword('');
                setModeChangeOtp('');
                setModeChangeReason('');
                setOtpRequested(false);
            }}>
                <DialogTitle>{t('admin.accessMode.changeModeTitle')}</DialogTitle>
                <DialogContent>
                    <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <Typography gutterBottom>
                            {t('admin.accessMode.changeModePrompt')}
                        </Typography>
                        {!otpRequested && (
                            <Button
                                variant="outlined"
                                onClick={handleRequestOtp}
                                sx={{ alignSelf: 'flex-start' }}
                            >
                                {t('admin.accessMode.requestOtp')}
                            </Button>
                        )}
                        {otpRequested && (
                            <Typography variant="body2" color="success.main">
                                {t('admin.accessMode.otpSent')}
                            </Typography>
                        )}
                        <TextField
                            type="password"
                            label={t('admin.adminPasswordLabel')}
                            value={modeChangePassword}
                            onChange={(e) => setModeChangePassword(e.target.value)}
                            fullWidth
                            required
                        />
                        <TextField
                            label={t('admin.accessMode.otpLabel')}
                            value={modeChangeOtp}
                            onChange={(e) => setModeChangeOtp(e.target.value)}
                            fullWidth
                            required
                            disabled={!otpRequested}
                        />
                        <TextField
                            label={t('admin.accessMode.reasonLabel')}
                            value={modeChangeReason}
                            onChange={(e) => setModeChangeReason(e.target.value)}
                            fullWidth
                            required
                            multiline
                            rows={3}
                            error={!modeChangeReason}
                            helperText={!modeChangeReason ? t('admin.accessMode.reasonRequired') : ''}
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => {
                        setChangeModeDialog(false);
                        setModeChangePassword('');
                        setModeChangeOtp('');
                        setModeChangeReason('');
                        setOtpRequested(false);
                    }}>{t('common.cancel')}</Button>
                    <Button 
                        onClick={handleChangeMode} 
                        variant="contained"
                        disabled={!modeChangePassword || !modeChangeOtp || !modeChangeReason || !otpRequested}
                    >
                        {t('common.submit')}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AdminPage;