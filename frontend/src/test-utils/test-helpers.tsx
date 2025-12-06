import React from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { vi, beforeEach, afterEach } from 'vitest';
import { TestBrowserRouter, TestMemoryRouter } from './router';
import { MemoryRouterProps } from 'react-router-dom';

// Common test data constants
export const mockUser = {
    id: 1,
    email: 'test@example.com',
    name: 'Test User',
    username: 'testuser',
    roles: ['ROLE_USER'],
    enabled: true,
    blocked: false,
    emailVerified: true,
    authProvider: 'LOCAL' as const,
};

export const mockAdminUser = {
    ...mockUser,
    id: 2,
    email: 'admin@example.com',
    name: 'Admin User',
    roles: ['ROLE_ADMIN', 'ROLE_USER'],
};

export const mockGoogleUser = {
    ...mockUser,
    id: 3,
    email: 'google@example.com',
    name: 'Google User',
    authProvider: 'GOOGLE' as const,
};

// Common cleanup function
export const setupTestCleanup = () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
    });
};

// Universal render function for pages with BrowserRouter
export const renderWithRouter = (
    component: React.ReactElement,
    options?: Omit<RenderOptions, 'wrapper'>
) => {
    return render(
        <TestBrowserRouter>
            {component}
        </TestBrowserRouter>,
        options
    );
};

// Universal render function for pages with MemoryRouter
export const renderWithMemoryRouter = (
    component: React.ReactElement,
    routerProps?: Omit<MemoryRouterProps, 'children'>,
    options?: Omit<RenderOptions, 'wrapper'>
) => {
    return render(
        <TestMemoryRouter {...routerProps}>
            {component}
        </TestMemoryRouter>,
        options
    );
};

// Helper to create render function for specific page
export const createPageRenderer = <T extends React.ComponentType<any>>(
    PageComponent: T,
    routerType: 'browser' | 'memory' = 'browser'
) => {
    return (props?: React.ComponentProps<T>, routerProps?: Omit<MemoryRouterProps, 'children'>) => {
        const component = <PageComponent {...(props || {} as React.ComponentProps<T>)} />;
        
        if (routerType === 'memory') {
            return renderWithMemoryRouter(component, routerProps);
        }
        
        return renderWithRouter(component);
    };
};

// Helper to wait for async operations with default timeout
export const waitForAsync = async (callback: () => void | Promise<void>, timeout = 5000) => {
    const { waitFor } = await import('@testing-library/react');
    return waitFor(callback, { timeout });
};

// Helper to create mock function with common return values
export const createMockFn = <T extends (...args: any[]) => any>(
    returnValue?: ReturnType<T>
) => {
    const fn = vi.fn();
    if (returnValue !== undefined) {
        fn.mockReturnValue(returnValue);
    }
    return fn;
};

// Helper to create mock async function
export const createMockAsyncFn = <T extends (...args: any[]) => Promise<any>>(
    returnValue?: Awaited<ReturnType<T>>
) => {
    const fn = vi.fn();
    if (returnValue !== undefined) {
        fn.mockResolvedValue(returnValue);
    }
    return fn;
};

// Helper to create mock rejected function
export const createMockRejectedFn = (error: any) => {
    const fn = vi.fn();
    fn.mockRejectedValue(error);
    return fn;
};

// Helper to create axios error mock
export const createAxiosError = (message: string, status = 400) => ({
    isAxiosError: true,
    response: {
        status,
        data: message,
    },
    message,
});

// Helper to clear all mocks and storage
export const clearAllTestData = () => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
};

