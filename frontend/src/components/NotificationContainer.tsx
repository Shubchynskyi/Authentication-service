import React from 'react';
import { Snackbar, Alert } from '@mui/material';
import { useNotification } from '../context/NotificationContext';

const NotificationContainer: React.FC = () => {
    const { notifications, removeNotification } = useNotification();

    return (
        <>
            {notifications.map((notification) => (
                <Snackbar
                    key={notification.id}
                    open={true}
                    autoHideDuration={6000}
                    onClose={() => removeNotification(notification.id)}
                    anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                >
                    <Alert
                        onClose={() => removeNotification(notification.id)}
                        severity={notification.type}
                        sx={{ width: '100%' }}
                    >
                        {notification.message}
                    </Alert>
                </Snackbar>
            ))}
        </>
    );
};

export default NotificationContainer; 