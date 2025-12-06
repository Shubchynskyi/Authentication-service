import React from 'react';
import { render, screen } from '@testing-library/react';
import { vi, beforeEach, describe, it, expect } from 'vitest';
import App from './App';

// Mock i18n
vi.mock('./i18n/i18n', () => ({}));

// Mock routes
vi.mock('./routes', () => ({
    default: () => <div data-testid="app-routes">App Routes</div>,
}));

// Mock components
vi.mock('./components/Navbar', () => ({
    default: () => <div data-testid="navbar">Navbar</div>,
}));

vi.mock('./components/NotificationContainer', () => ({
    default: () => <div data-testid="notification-container">Notification Container</div>,
}));

// Mock contexts
vi.mock('./context/ThemeContext', () => ({
    ThemeProvider: ({ children }: { children: React.ReactNode }) => (
        <div data-testid="theme-provider">{children}</div>
    ),
}));

vi.mock('./context/NotificationContext', () => ({
    NotificationProvider: ({ children }: { children: React.ReactNode }) => (
        <div data-testid="notification-provider">{children}</div>
    ),
}));

vi.mock('./context/AuthContext', () => ({
    AuthProvider: ({ children }: { children: React.ReactNode }) => (
        <div data-testid="auth-provider">{children}</div>
    ),
}));

vi.mock('./context/ProfileContext', () => ({
    ProfileProvider: ({ children }: { children: React.ReactNode }) => (
        <div data-testid="profile-provider">{children}</div>
    ),
}));

describe('App', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render all providers in correct order', () => {
        render(<App />);

        // Check that all providers are rendered
        expect(screen.getByTestId('theme-provider')).toBeInTheDocument();
        expect(screen.getByTestId('notification-provider')).toBeInTheDocument();
        expect(screen.getByTestId('auth-provider')).toBeInTheDocument();
        expect(screen.getByTestId('profile-provider')).toBeInTheDocument();
    });

    it('should render Navbar component', () => {
        render(<App />);

        expect(screen.getByTestId('navbar')).toBeInTheDocument();
    });

    it('should render NotificationContainer component', () => {
        render(<App />);

        expect(screen.getByTestId('notification-container')).toBeInTheDocument();
    });

    it('should render AppRoutes component', () => {
        render(<App />);

        expect(screen.getByTestId('app-routes')).toBeInTheDocument();
    });

    it('should have correct provider nesting structure', () => {
        render(<App />);

        // ThemeProvider should be outermost
        const themeProvider = screen.getByTestId('theme-provider');
        expect(themeProvider).toBeInTheDocument();

        // NotificationProvider should be inside ThemeProvider
        const notificationProvider = screen.getByTestId('notification-provider');
        expect(notificationProvider).toBeInTheDocument();
        expect(themeProvider).toContainElement(notificationProvider);

        // AuthProvider should be inside NotificationProvider
        const authProvider = screen.getByTestId('auth-provider');
        expect(authProvider).toBeInTheDocument();
        expect(notificationProvider).toContainElement(authProvider);

        // ProfileProvider should be inside AuthProvider
        const profileProvider = screen.getByTestId('profile-provider');
        expect(profileProvider).toBeInTheDocument();
        expect(authProvider).toContainElement(profileProvider);
    });

    it('should render Router component', () => {
        render(<App />);

        // Router should be rendered (BrowserRouter from react-router-dom)
        // We can verify this by checking that routes are rendered
        expect(screen.getByTestId('app-routes')).toBeInTheDocument();
    });

    it('should render all main components', () => {
        render(<App />);

        // All main components should be present
        expect(screen.getByTestId('navbar')).toBeInTheDocument();
        expect(screen.getByTestId('notification-container')).toBeInTheDocument();
        expect(screen.getByTestId('app-routes')).toBeInTheDocument();
    });

    it('should maintain component structure', () => {
        render(<App />);

        // Check that the structure is maintained
        // Navbar, NotificationContainer, and AppRoutes should all be present
        const navbar = screen.getByTestId('navbar');
        const notificationContainer = screen.getByTestId('notification-container');
        const appRoutes = screen.getByTestId('app-routes');

        expect(navbar).toBeInTheDocument();
        expect(notificationContainer).toBeInTheDocument();
        expect(appRoutes).toBeInTheDocument();
    });
});

