import React, { useState, useEffect } from 'react';
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
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    List,
    ListItem,
    ListItemText,
    ListItemSecondaryAction,
    Typography,
    FormGroup,
    FormControlLabel,
    Checkbox
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useAuth } from '../context/AuthContext';
import api from '../api';

interface User {
    id: number;
    username: string;
    email: string;
    roles: string[];
    enabled: boolean;
    blocked: boolean;
    emailVerified: boolean;
    blockReason?: string;
}

interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
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
    const { token } = useAuth();
    const [tabValue, setTabValue] = useState(0);
    const [users, setUsers] = useState<User[]>([]);
    const [roles, setRoles] = useState<string[]>([]);
    const [whitelist, setWhitelist] = useState<string[]>([]);
    const [totalElements, setTotalElements] = useState(0);
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [openDialog, setOpenDialog] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [newEmail, setNewEmail] = useState('');
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        roles: ['ROLE_USER'] as string[],
        enabled: true,
        blocked: false,
        blockReason: ''
    });
    const [confirmAdminDialog, setConfirmAdminDialog] = useState(false);
    const [adminPassword, setAdminPassword] = useState('');
    const [roleMapping] = useState({
        'ROLE_USER': 'User',
        'ROLE_ADMIN': 'Admin'
    });
    const [confirmActionDialog, setConfirmActionDialog] = useState(false);
    const [confirmActionPassword, setConfirmActionPassword] = useState('');
    const [pendingAction, setPendingAction] = useState<{
        type: 'DELETE_USER' | 'ADD_WHITELIST' | 'REMOVE_WHITELIST';
        data: any;
    } | null>(null);

    const fetchRoles = async () => {
        try {
            const response = await api.get('/api/admin/roles');
            setRoles(response.data);
        } catch (error) {
            console.error('Error fetching roles:', error);
        }
    };

    const fetchWhitelist = async () => {
        try {
            const response = await api.get('/api/admin/whitelist');
            setWhitelist(response.data);
        } catch (error) {
            console.error('Error fetching whitelist:', error);
        }
    };

    const fetchUsers = async () => {
        try {
            const response = await api.get('/api/admin/users', {
                params: {
                    page,
                    size: rowsPerPage
                }
            });
            setUsers(response.data.content);
            setTotalElements(response.data.totalElements);
        } catch (error) {
            console.error('Error fetching users:', error);
        }
    };

    useEffect(() => {
        fetchRoles();
        fetchWhitelist();
        fetchUsers();
    }, []);

    const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    const handleChangePage = (event: unknown, newPage: number) => {
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
                roles: isAdmin 
                    ? ['ROLE_USER', 'ROLE_ADMIN']
                    : ['ROLE_USER']
            });
        }
    };

    const handleConfirmAdmin = async () => {
        try {
            await api.post('/api/admin/verify-admin', { password: adminPassword });
            
            setFormData({ 
                ...formData, 
                roles: [...formData.roles, 'ROLE_ADMIN'] 
            });
            setConfirmAdminDialog(false);
            setAdminPassword('');
        } catch (error) {
            console.error('Error verifying admin:', error);
            alert('Неверный пароль администратора');
        }
    };

    const handleSubmit = async () => {
        try {
            const url = selectedUser
                ? `/api/admin/users/${selectedUser.id}`
                : '/api/admin/users';
            
            const method = selectedUser ? 'put' : 'post';
            const response = await api[method](url, formData);

            if (response.status === 200 || response.status === 201) {
                handleCloseDialog();
                fetchUsers();
            }
        } catch (error) {
            console.error('Error saving user:', error);
            alert(error.response?.data?.message || 'Ошибка при сохранении пользователя');
        }
    };

    const handleConfirmAction = async () => {
        try {
            await api.post('/api/admin/verify-admin', { password: confirmActionPassword });
            
            switch (pendingAction?.type) {
                case 'DELETE_USER':
                    await api.delete(`/api/admin/users/${pendingAction.data}`);
                    fetchUsers();
                    break;
                case 'ADD_WHITELIST':
                    await api.post('/api/admin/whitelist/add', null, {
                        params: { email: pendingAction.data }
                    });
                    setNewEmail('');
                    fetchWhitelist();
                    break;
                case 'REMOVE_WHITELIST':
                    await api.delete('/api/admin/whitelist/remove', {
                        params: { email: pendingAction.data }
                    });
                    fetchWhitelist();
                    break;
            }
            
            setConfirmActionDialog(false);
            setConfirmActionPassword('');
            setPendingAction(null);
        } catch (error) {
            console.error('Error during action:', error);
            alert('Неверный пароль администратора');
        }
    };

    const handleDelete = (userId: number) => {
        setPendingAction({ type: 'DELETE_USER', data: userId });
        setConfirmActionDialog(true);
    };

    const handleAddToWhitelist = () => {
        if (!newEmail) return;
        setPendingAction({ type: 'ADD_WHITELIST', data: newEmail });
        setConfirmActionDialog(true);
    };

    const handleRemoveFromWhitelist = (email: string) => {
        setPendingAction({ type: 'REMOVE_WHITELIST', data: email });
        setConfirmActionDialog(true);
    };

    return (
        <Box sx={{ width: '100%' }}>
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                <Tabs value={tabValue} onChange={handleTabChange}>
                    <Tab label="Пользователи" />
                    <Tab label="Белый список" />
                </Tabs>
            </Box>

            <TabPanel value={tabValue} index={0}>
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                    <Button
                        variant="contained"
                        onClick={() => handleOpenDialog()}
                    >
                        Добавить пользователя
                    </Button>
                </Box>

                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>ID</TableCell>
                                <TableCell>Имя пользователя</TableCell>
                                <TableCell>Email</TableCell>
                                <TableCell>Роли</TableCell>
                                <TableCell>Статус</TableCell>
                                <TableCell>Действия</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {users.map((user) => (
                                <TableRow key={user.id}>
                                    <TableCell>{user.id}</TableCell>
                                    <TableCell>{user.username}</TableCell>
                                    <TableCell>{user.email}</TableCell>
                                    <TableCell>{user.roles.join(', ')}</TableCell>
                                    <TableCell>
                                        {user.blocked ? 'Заблокирован' : 
                                         !user.enabled ? 'Отключен' :
                                         !user.emailVerified ? 'Не подтвержден' :
                                         'Активен'}
                                    </TableCell>
                                    <TableCell>
                                        <Tooltip title="Редактировать">
                                            <IconButton onClick={() => handleOpenDialog(user)}>
                                                <EditIcon />
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title="Удалить">
                                            <IconButton onClick={() => handleDelete(user.id)}>
                                                <DeleteIcon />
                                            </IconButton>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                    <TablePagination
                        component="div"
                        count={totalElements}
                        page={page}
                        onPageChange={handleChangePage}
                        rowsPerPage={rowsPerPage}
                        onRowsPerPageChange={handleChangeRowsPerPage}
                        rowsPerPageOptions={[5, 10, 25]}
                    />
                </TableContainer>
            </TabPanel>

            <TabPanel value={tabValue} index={1}>
                <Box sx={{ maxWidth: 600, mx: 'auto' }}>
                    <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                        <TextField
                            fullWidth
                            label="Email адрес"
                            value={newEmail}
                            onChange={(e) => setNewEmail(e.target.value)}
                        />
                        <Button
                            variant="contained"
                            onClick={handleAddToWhitelist}
                            disabled={!newEmail}
                        >
                            Добавить
                        </Button>
                    </Box>

                    <List>
                        {whitelist.map((email) => (
                            <ListItem key={email}>
                                <ListItemText primary={email} />
                                <ListItemSecondaryAction>
                                    <IconButton
                                        edge="end"
                                        onClick={() => handleRemoveFromWhitelist(email)}
                                    >
                                        <DeleteIcon />
                                    </IconButton>
                                </ListItemSecondaryAction>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </TabPanel>

            <Dialog open={openDialog} onClose={handleCloseDialog}>
                <DialogTitle>
                    {selectedUser ? 'Редактировать пользователя' : 'Добавить пользователя'}
                </DialogTitle>
                <DialogContent>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
                        <TextField
                            label="Имя пользователя"
                            value={formData.username}
                            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                            fullWidth
                        />
                        <TextField
                            label="Email"
                            value={formData.email}
                            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                            fullWidth
                        />
                        <FormControlLabel
                            control={
                                <Checkbox
                                    checked={formData.roles.includes('ROLE_ADMIN')}
                                    onChange={(e) => handleRoleChange(e.target.checked)}
                                />
                            }
                            label="Администратор"
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
                            label="Заблокирован"
                        />
                        {formData.blocked && (
                            <TextField
                                label="Причина блокировки"
                                value={formData.blockReason}
                                onChange={(e) => setFormData({ ...formData, blockReason: e.target.value })}
                                fullWidth
                                multiline
                                rows={2}
                            />
                        )}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDialog}>Отмена</Button>
                    <Button onClick={handleSubmit} variant="contained">
                        {selectedUser ? 'Сохранить' : 'Добавить'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={confirmAdminDialog} onClose={() => setConfirmAdminDialog(false)}>
                <DialogTitle>Подтверждение прав администратора</DialogTitle>
                <DialogContent>
                    <Box sx={{ pt: 2 }}>
                        <Typography gutterBottom>
                            Для назначения прав администратора введите ваш пароль:
                        </Typography>
                        <TextField
                            type="password"
                            label="Пароль администратора"
                            value={adminPassword}
                            onChange={(e) => setAdminPassword(e.target.value)}
                            fullWidth
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmAdminDialog(false)}>Отмена</Button>
                    <Button onClick={handleConfirmAdmin} variant="contained">
                        Подтвердить
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={confirmActionDialog} onClose={() => setConfirmActionDialog(false)}>
                <DialogTitle>Подтверждение действия</DialogTitle>
                <DialogContent>
                    <Box sx={{ pt: 2 }}>
                        <Typography gutterBottom>
                            Для подтверждения действия введите ваш пароль:
                        </Typography>
                        <TextField
                            type="password"
                            label="Пароль администратора"
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
                    }}>Отмена</Button>
                    <Button onClick={handleConfirmAction} variant="contained">
                        Подтвердить
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AdminPage;