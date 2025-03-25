import React, { createContext, useContext, useState, useCallback } from 'react';
import { AlertColor } from '@mui/material';

interface Notification {
    message: string;
    type: AlertColor;
    id: number;
}

interface NotificationContextType {
    notifications: Notification[];
    showNotification: (message: string, type: AlertColor) => void;
    removeNotification: (id: number) => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [nextId, setNextId] = useState(1);

    const showNotification = useCallback((message: string, type: AlertColor) => {
        const id = nextId;
        setNextId(prev => prev + 1);
        setNotifications(prev => [...prev, { message, type, id }]);
    }, [nextId]);

    const removeNotification = useCallback((id: number) => {
        setNotifications(prev => prev.filter(notification => notification.id !== id));
    }, []);

    return (
        <NotificationContext.Provider value={{ notifications, showNotification, removeNotification }}>
            {children}
        </NotificationContext.Provider>
    );
};

export const useNotification = () => {
    const context = useContext(NotificationContext);
    if (context === undefined) {
        throw new Error('useNotification must be used within a NotificationProvider');
    }
    return context;
}; 