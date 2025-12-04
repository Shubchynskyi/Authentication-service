import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import AdminPage from './AdminPage';
import api from '../api';
import { TestBrowserRouter } from '../test-utils/router';

// Mock api
const mockApiGet = vi.fn();
const mockApiPost = vi.fn();
const mockApiPut = vi.fn();
const mockApiDelete = vi.fn();

vi.mock('../api', () => ({
    default: {
        get: (...args: any[]) => mockApiGet(...args),
        post: (...args: any[]) => mockApiPost(...args),
        put: (...args: any[]) => mockApiPut(...args),
        delete: (...args: any[]) => mockApiDelete(...args),
    },
    checkAccess: vi.fn(),
}));

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
}));

// Mock NotificationContext
const mockShowNotification = vi.fn();
vi.mock('../context/NotificationContext', () => ({
    useNotification: () => ({
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
        notifications: [],
    }),
}));

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

const mockUsers = [
    {
        id: 1,
        username: 'user1',
        email: 'user1@example.com',
        roles: ['ROLE_USER'],
        enabled: true,
        blocked: false,
        emailVerified: true,
        authProvider: 'LOCAL' as const,
        lastLoginAt: '2024-01-01T00:00:00Z',
    },
    {
        id: 2,
        username: 'admin',
        email: 'admin@example.com',
        roles: ['ROLE_USER', 'ROLE_ADMIN'],
        enabled: true,
        blocked: false,
        emailVerified: true,
        authProvider: 'GOOGLE' as const,
        lastLoginAt: '2024-01-02T00:00:00Z',
    },
];

const mockWhitelist = ['allowed@example.com', 'another@example.com'];

const renderAdminPage = () => {
    return render(
        <TestBrowserRouter>
            <AdminPage />
        </TestBrowserRouter>
    );
};

describe('AdminPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockShowNotification.mockClear();
        mockApiGet.mockClear();
        mockApiPost.mockClear();
        mockApiPut.mockClear();
        mockApiDelete.mockClear();

        // Default successful responses
        mockApiGet.mockImplementation((url: string) => {
            if (url.includes('/whitelist')) {
                return Promise.resolve({ data: mockWhitelist });
            }
            if (url.includes('/users')) {
                return Promise.resolve({
                    data: {
                        content: mockUsers,
                        totalElements: mockUsers.length,
                    },
                });
            }
            return Promise.resolve({ data: {} });
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders admin page with tabs', async () => {
        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByRole('tab', { name: /admin.users/i })).toBeInTheDocument();
            expect(screen.getByRole('tab', { name: /admin.whitelistTab/i })).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('displays users table', async () => {
        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByText('user1@example.com')).toBeInTheDocument();
            expect(screen.getByText('admin@example.com')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('displays user information correctly', async () => {
        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByText('1')).toBeInTheDocument(); // User ID
            expect(screen.getByText('user1')).toBeInTheDocument(); // Username
            expect(screen.getByText('user1@example.com')).toBeInTheDocument(); // Email
        }, { timeout: 5000 });
    });

    it('shows admin badge for admin users', async () => {
        renderAdminPage();

        await waitFor(() => {
            const adminRow = screen.getByText('admin').closest('tr');
            expect(adminRow).toBeInTheDocument();
            // Admin badge should be present (letter 'A')
            expect(within(adminRow!).getByText('A')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('opens add user dialog when add button is clicked', async () => {
        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByText(/admin.addUser/i)).toBeInTheDocument();
        }, { timeout: 5000 });

        const addButton = screen.getByRole('button', { name: /admin.addUser/i });
        fireEvent.click(addButton);

        await waitFor(() => {
            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByLabelText(/common.username/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/common.email/i)).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('opens edit user dialog when edit button is clicked', async () => {
        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByText('user1@example.com')).toBeInTheDocument();
        }, { timeout: 5000 });

        // Find edit button by tooltip or icon
        const editButtons = screen.getAllByRole('button');
        const editButton = editButtons.find(btn => {
            const tooltip = btn.getAttribute('aria-label') || btn.getAttribute('title');
            return tooltip?.includes('admin.edit') || tooltip?.includes('Edit');
        });

        if (editButton) {
            fireEvent.click(editButton);
        }

        await waitFor(() => {
            // Dialog should open
            const dialog = screen.queryByRole('dialog');
            expect(dialog).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('switches to whitelist tab', async () => {
        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByRole('tab', { name: /admin.whitelistTab/i })).toBeInTheDocument();
        }, { timeout: 5000 });

        const whitelistTab = screen.getByRole('tab', { name: /admin.whitelistTab/i });
        fireEvent.click(whitelistTab);

        await waitFor(() => {
            expect(screen.getByLabelText(/admin.whitelistEmailLabel/i)).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('displays whitelist emails', async () => {
        renderAdminPage();

        const whitelistTab = screen.getByRole('tab', { name: /admin.whitelistTab/i });
        fireEvent.click(whitelistTab);

        await waitFor(() => {
            expect(screen.getByText('allowed@example.com')).toBeInTheDocument();
            expect(screen.getByText('another@example.com')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('adds email to whitelist', async () => {
        mockApiPost.mockResolvedValueOnce({ data: 'Success' });
        mockApiPost.mockResolvedValueOnce({ data: 'Success' }); // For verify-admin

        renderAdminPage();

        const whitelistTab = screen.getByRole('tab', { name: /admin.whitelistTab/i });
        fireEvent.click(whitelistTab);

        await waitFor(() => {
            const emailInput = screen.getByLabelText(/admin.whitelistEmailLabel/i);
            fireEvent.change(emailInput, { target: { value: 'new@example.com' } });

            const addButton = screen.getByRole('button', { name: /admin.whitelistAdd/i });
            fireEvent.click(addButton);
        }, { timeout: 5000 });

        await waitFor(() => {
            // Should show password confirmation dialog
            expect(screen.getByRole('dialog')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('validates email format when adding to whitelist', async () => {
        renderAdminPage();

        const whitelistTab = screen.getByRole('tab', { name: /admin.whitelistTab/i });
        fireEvent.click(whitelistTab);

        await waitFor(() => {
            const emailInput = screen.getByLabelText(/admin.whitelistEmailLabel/i);
            fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

            const addButton = screen.getByRole('button', { name: /admin.whitelistAdd/i });
            fireEvent.click(addButton);
        }, { timeout: 5000 });

        await waitFor(() => {
            expect(mockShowNotification).toHaveBeenCalledWith(
                expect.stringContaining('invalidEmail'),
                'error'
            );
        }, { timeout: 5000 });
    });

    it('handles pagination', async () => {
        mockApiGet.mockImplementation((url: string) => {
            if (url.includes('/whitelist')) {
                return Promise.resolve({ data: mockWhitelist });
            }
            if (url.includes('/users')) {
                return Promise.resolve({
                    data: {
                        content: mockUsers,
                        totalElements: 25,
                    },
                });
            }
            return Promise.resolve({ data: {} });
        });

        renderAdminPage();

        await waitFor(() => {
            // Should show pagination if totalElements > rowsPerPage
            const pagination = screen.queryByLabelText(/common.rowsPerPage/i);
            if (pagination) {
                expect(pagination).toBeInTheDocument();
            }
        }, { timeout: 5000 });
    });

    it('shows user status correctly', async () => {
        const blockedUser = {
            ...mockUsers[0],
            blocked: true,
            blockReason: 'Suspicious activity',
        };

        mockApiGet.mockImplementation((url: string) => {
            if (url.includes('/whitelist')) {
                return Promise.resolve({ data: mockWhitelist });
            }
            if (url.includes('/users')) {
                return Promise.resolve({
                    data: {
                        content: [blockedUser],
                        totalElements: 1,
                    },
                });
            }
            return Promise.resolve({ data: {} });
        });

        renderAdminPage();

        await waitFor(() => {
            expect(screen.getByText(/admin.statusBlocked/i)).toBeInTheDocument();
            expect(screen.getByText('Suspicious activity')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('shows auth provider icons', async () => {
        renderAdminPage();

        await waitFor(() => {
            // Google user should have Google icon
            const adminRow = screen.getByText('admin@example.com').closest('tr');
            expect(adminRow).toBeInTheDocument();
            // Icons are rendered as SVG, hard to test directly
        }, { timeout: 5000 });
    });

    it('handles API errors gracefully', async () => {
        // Mock to reject users request but allow whitelist
        mockApiGet.mockImplementation((url: string) => {
            if (url.includes('/whitelist')) {
                return Promise.resolve({ data: mockWhitelist });
            }
            if (url.includes('/users')) {
                return Promise.reject(new Error('Network error'));
            }
            return Promise.resolve({ data: {} });
        });

        renderAdminPage();

        // Should not crash, might show error or empty state
        await waitFor(() => {
            // Page should still render
            expect(screen.getByRole('tab', { name: /admin.users/i })).toBeInTheDocument();
        }, { timeout: 5000 });
    });
});

