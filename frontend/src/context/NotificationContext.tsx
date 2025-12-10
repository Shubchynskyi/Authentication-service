import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
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
    const nextIdRef = useRef(1);

    // Stable callback: use ref for incremental id to avoid changing reference on each call
    const showNotification = useCallback((message: string, type: AlertColor) => {
        const id = nextIdRef.current++;
        setNotifications(prev => [...prev, { message, type, id }]);
    }, []);

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