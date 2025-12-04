import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { NotificationProvider, useNotification } from './NotificationContext';
import { AlertColor } from '@mui/material';
import { setupTestCleanup } from '../test-utils/test-helpers';

// Test component that uses NotificationContext
const TestComponent: React.FC = () => {
    const { notifications, showNotification, removeNotification } = useNotification();

    return (
        <div>
            <div data-testid="notifications-count">{notifications.length}</div>
            <div data-testid="notifications">
                {notifications.map((notif) => (
                    <div key={notif.id} data-testid={`notification-${notif.id}`}>
                        <span data-testid={`notification-${notif.id}-message`}>{notif.message}</span>
                        <span data-testid={`notification-${notif.id}-type`}>{notif.type}</span>
                        <button
                            data-testid={`remove-${notif.id}`}
                            onClick={() => removeNotification(notif.id)}
                        >
                            Remove
                        </button>
                    </div>
                ))}
            </div>
            <button
                data-testid="show-success"
                onClick={() => showNotification('Success message', 'success')}
            >
                Show Success
            </button>
            <button
                data-testid="show-error"
                onClick={() => showNotification('Error message', 'error')}
            >
                Show Error
            </button>
            <button
                data-testid="show-warning"
                onClick={() => showNotification('Warning message', 'warning')}
            >
                Show Warning
            </button>
            <button
                data-testid="show-info"
                onClick={() => showNotification('Info message', 'info')}
            >
                Show Info
            </button>
        </div>
    );
};

describe('NotificationContext', () => {
    setupTestCleanup();

    describe('showNotification', () => {
        it('should add a notification to the list', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('0');

            const showButton = screen.getByTestId('show-success');
            fireEvent.click(showButton);

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');
            expect(screen.getByTestId('notification-1-message')).toHaveTextContent('Success message');
            expect(screen.getByTestId('notification-1-type')).toHaveTextContent('success');
        });

        it('should add multiple notifications with unique IDs', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));
            fireEvent.click(screen.getByTestId('show-warning'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('3');
            expect(screen.getByTestId('notification-1-message')).toHaveTextContent('Success message');
            expect(screen.getByTestId('notification-2-message')).toHaveTextContent('Error message');
            expect(screen.getByTestId('notification-3-message')).toHaveTextContent('Warning message');
        });

        it('should support different notification types', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            const types: AlertColor[] = ['success', 'error', 'warning', 'info'];

            types.forEach((type) => {
                const button = screen.getByTestId(`show-${type}`);
                fireEvent.click(button);
            });

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('4');

            types.forEach((type, index) => {
                const id = index + 1;
                expect(screen.getByTestId(`notification-${id}-type`)).toHaveTextContent(type);
            });
        });

        it('should increment ID for each new notification', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            // Add first notification
            fireEvent.click(screen.getByTestId('show-success'));
            expect(screen.getByTestId('notification-1')).toBeInTheDocument();

            // Add second notification
            fireEvent.click(screen.getByTestId('show-error'));
            expect(screen.getByTestId('notification-2')).toBeInTheDocument();

            // Add third notification
            fireEvent.click(screen.getByTestId('show-info'));
            expect(screen.getByTestId('notification-3')).toBeInTheDocument();
        });
    });

    describe('removeNotification', () => {
        it('should remove a notification by ID', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));
            fireEvent.click(screen.getByTestId('show-warning'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('3');

            const removeButton = screen.getByTestId('remove-2');
            fireEvent.click(removeButton);

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('2');
            expect(screen.queryByTestId('notification-2')).not.toBeInTheDocument();
            expect(screen.getByTestId('notification-1')).toBeInTheDocument();
            expect(screen.getByTestId('notification-3')).toBeInTheDocument();
        });

        it('should remove the first notification', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('2');

            const removeButton = screen.getByTestId('remove-1');
            fireEvent.click(removeButton);

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');
            expect(screen.queryByTestId('notification-1')).not.toBeInTheDocument();
            expect(screen.getByTestId('notification-2')).toBeInTheDocument();
        });

        it('should remove the last notification', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('2');

            const removeButton = screen.getByTestId('remove-2');
            fireEvent.click(removeButton);

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');
            expect(screen.getByTestId('notification-1')).toBeInTheDocument();
            expect(screen.queryByTestId('notification-2')).not.toBeInTheDocument();
        });

        it('should handle removing non-existent notification gracefully', () => {
            const TestComponentWithRemove = () => {
                const { notifications, showNotification, removeNotification } = useNotification();
                return (
                    <div>
                        <div data-testid="notifications-count">{notifications.length}</div>
                        <button
                            data-testid="show-success"
                            onClick={() => showNotification('Success message', 'success')}
                        >
                            Show Success
                        </button>
                        <button
                            data-testid="remove-999"
                            onClick={() => removeNotification(999)}
                        >
                            Remove 999
                        </button>
                    </div>
                );
            };

            render(
                <NotificationProvider>
                    <TestComponentWithRemove />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');

            // Try to remove non-existent notification
            fireEvent.click(screen.getByTestId('remove-999'));

            // Should still have the original notification
            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');
        });

        it('should remove all notifications one by one', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));
            fireEvent.click(screen.getByTestId('show-warning'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('3');

            fireEvent.click(screen.getByTestId('remove-1'));
            expect(screen.getByTestId('notifications-count')).toHaveTextContent('2');

            fireEvent.click(screen.getByTestId('remove-2'));
            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');

            fireEvent.click(screen.getByTestId('remove-3'));
            expect(screen.getByTestId('notifications-count')).toHaveTextContent('0');
        });
    });

    describe('useNotification hook', () => {
        it('should throw error when used outside NotificationProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            const TestComponentWithoutProvider = () => {
                useNotification();
                return <div>Test</div>;
            };

            expect(() => {
                render(<TestComponentWithoutProvider />);
            }).toThrow('useNotification must be used within a NotificationProvider');

            consoleSpy.mockRestore();
        });
    });

    describe('Notification state management', () => {
        it('should maintain notification order', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));
            fireEvent.click(screen.getByTestId('show-warning'));

            const notifications = screen.getAllByTestId(/^notification-\d+$/);
            expect(notifications).toHaveLength(3);
            expect(notifications[0]).toHaveAttribute('data-testid', 'notification-1');
            expect(notifications[1]).toHaveAttribute('data-testid', 'notification-2');
            expect(notifications[2]).toHaveAttribute('data-testid', 'notification-3');
        });

        it('should allow adding notifications after removal', () => {
            render(
                <NotificationProvider>
                    <TestComponent />
                </NotificationProvider>
            );

            fireEvent.click(screen.getByTestId('show-success'));
            fireEvent.click(screen.getByTestId('show-error'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('2');

            fireEvent.click(screen.getByTestId('remove-1'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('1');

            fireEvent.click(screen.getByTestId('show-info'));

            expect(screen.getByTestId('notifications-count')).toHaveTextContent('2');
            expect(screen.getByTestId('notification-2')).toBeInTheDocument();
            expect(screen.getByTestId('notification-3')).toBeInTheDocument();
        });
    });
});

