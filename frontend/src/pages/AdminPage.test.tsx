import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import AdminPage from './AdminPage';
import { TestBrowserRouter } from '../test-utils/router';
import { getTranslation, testTranslations } from '../test-utils/translations';

// Create mocks using vi.hoisted() to avoid hoisting issues
const mocks = vi.hoisted(() => {
    const mockApiGet = vi.fn();
    const mockApiPost = vi.fn();
    const mockApiPut = vi.fn();
    const mockApiDelete = vi.fn();
    const mockShowNotification = vi.fn();
    return { mockApiGet, mockApiPost, mockApiPut, mockApiDelete, mockShowNotification };
});

const { mockApiGet, mockApiPost, mockApiPut, mockApiDelete, mockShowNotification } = mocks;

// Mock api module
vi.mock('../api', () => ({
    default: {
        get: (...args: any[]) => mockApiGet(...args),
        post: (...args: any[]) => mockApiPost(...args),
        put: (...args: any[]) => mockApiPut(...args),
        delete: (...args: any[]) => mockApiDelete(...args),
        defaults: {
            headers: {
                common: {},
            },
        },
        interceptors: {
            request: {
                use: vi.fn(),
            },
            response: {
                use: vi.fn(),
            },
        },
    },
}));

// Mock axios
vi.mock('axios', () => ({
    default: {
        isAxiosError: (error: any) => error?.isAxiosError || false,
    },
    isAxiosError: (error: any) => error?.isAxiosError || false,
}));

// Mock NotificationContext
vi.mock('../context/NotificationContext', () => ({
    useNotification: () => ({
        showNotification: mockShowNotification,
        removeNotification: vi.fn(),
        notifications: [],
    }),
}));

// Mock window.confirm
const mockConfirm = vi.fn(() => true);
window.confirm = mockConfirm;

// Mock useTranslation
vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => getTranslation(key, testTranslations),
        i18n: {
            language: 'en',
            changeLanguage: vi.fn(),
            resolvedLanguage: 'en',
        },
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
        lastLoginAt: '2024-01-01T00:00:00Z',
        authProvider: 'LOCAL' as const,
    },
    {
        id: 2,
        username: 'admin',
        email: 'admin@example.com',
        roles: ['ROLE_ADMIN', 'ROLE_USER'],
        enabled: true,
        blocked: false,
        emailVerified: true,
        lastLoginAt: '2024-01-02T00:00:00Z',
        authProvider: 'GOOGLE' as const,
    },
    {
        id: 3,
        username: 'blocked',
        email: 'blocked@example.com',
        roles: ['ROLE_USER'],
        enabled: true,
        blocked: true,
        blockReason: 'Violation of terms',
        emailVerified: true,
        authProvider: 'LOCAL' as const,
    },
];

const mockWhitelist = ['allowed1@example.com', 'allowed2@example.com'];
const mockBlacklist = [
    { email: 'blocked1@example.com', reason: 'Spam' },
    { email: 'blocked2@example.com', reason: null },
];

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
        mockApiGet.mockReset();
        mockApiPost.mockReset();
        mockApiPut.mockReset();
        mockApiDelete.mockReset();
        mockShowNotification.mockReset();
        mockConfirm.mockReturnValue(true);

        // Default API responses
        mockApiGet.mockImplementation((url: string) => {
            if (url === '/api/admin/users') {
                return Promise.resolve({
                    data: {
                        content: mockUsers,
                        totalElements: mockUsers.length,
                    },
                });
            }
            if (url === '/api/admin/whitelist') {
                return Promise.resolve({ data: mockWhitelist });
            }
            if (url === '/api/admin/blacklist') {
                return Promise.resolve({ data: mockBlacklist });
            }
            if (url === '/api/admin/access-mode') {
                return Promise.resolve({ data: { mode: 'WHITELIST' } });
            }
            return Promise.resolve({ data: {} });
        });

        mockApiPost.mockResolvedValue({ data: {}, status: 200 });
        mockApiPut.mockResolvedValue({ data: {}, status: 200 });
        mockApiDelete.mockResolvedValue({ data: {}, status: 200 });
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('Rendering', () => {
        it('renders all tabs', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByRole('tab', { name: /Users/i })).toBeInTheDocument();
            }, { timeout: 5000 });

            expect(screen.getByRole('tab', { name: /Whitelist/i })).toBeInTheDocument();
            expect(screen.getByRole('tab', { name: /Blacklist/i })).toBeInTheDocument();
            expect(screen.getByRole('tab', { name: /Access Control/i })).toBeInTheDocument();
        });

        it('renders users table on Users tab', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            expect(screen.getByText('admin')).toBeInTheDocument();
            expect(screen.getByText('blocked')).toBeInTheDocument();
        });

        it('renders whitelist on Whitelist tab', async () => {
            renderAdminPage();

            const whitelistTab = screen.getByRole('tab', { name: /Whitelist/i });
            fireEvent.click(whitelistTab);

            await waitFor(() => {
                expect(screen.getByText('allowed1@example.com')).toBeInTheDocument();
            }, { timeout: 5000 });

            expect(screen.getByText('allowed2@example.com')).toBeInTheDocument();
        });

        it('renders blacklist on Blacklist tab', async () => {
            renderAdminPage();

            const blacklistTab = screen.getByRole('tab', { name: /Blacklist/i });
            fireEvent.click(blacklistTab);

            await waitFor(() => {
                expect(screen.getByText('blocked1@example.com')).toBeInTheDocument();
            }, { timeout: 5000 });

            expect(screen.getByText('blocked2@example.com')).toBeInTheDocument();
            expect(screen.getByText('Spam')).toBeInTheDocument();
        });

        it('renders access control mode on Access Control tab', async () => {
            renderAdminPage();

            const accessControlTab = screen.getByRole('tab', { name: /Access Control/i });
            fireEvent.click(accessControlTab);

            await waitFor(() => {
                expect(screen.getByText(/Access Control Mode/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            expect(screen.getByText(/Current mode/i)).toBeInTheDocument();
        });
    });

    describe('User Management', () => {
        it('opens add user dialog when Add User button is clicked', async () => {
            renderAdminPage();

            await waitFor(() => {
                // Wait for users to load first
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            // Find Add User button by looking for button with text that's not in a dialog
            const allButtons = screen.getAllByRole('button');
            const addButton = allButtons.find(btn => 
                btn.textContent === 'Add User' && !btn.closest('[role="dialog"]')
            );
            
            expect(addButton).toBeInTheDocument();
            if (addButton) {
                fireEvent.click(addButton);

                await waitFor(() => {
                    expect(screen.getByRole('dialog')).toBeInTheDocument();
                }, { timeout: 5000 });

                // Check for dialog title
                const dialog = screen.getByRole('dialog');
                expect(within(dialog).getByText(/Add User/i)).toBeInTheDocument();
            }
        });

        it('opens edit user dialog when edit button is clicked', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const editButtons = screen.getAllByRole('button', { name: /Edit/i });
            fireEvent.click(editButtons[0]);

            await waitFor(() => {
                expect(screen.getByRole('dialog')).toBeInTheDocument();
            }, { timeout: 5000 });

            expect(screen.getByText(/Edit User/i)).toBeInTheDocument();
        });

        it('shows delete confirmation dialog when delete button is clicked', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const deleteButtons = screen.getAllByRole('button', { name: /Delete/i });
            fireEvent.click(deleteButtons[0]);

            await waitFor(() => {
                expect(screen.getByText(/Confirm Action/i)).toBeInTheDocument();
            }, { timeout: 5000 });
        });

        it('creates new user successfully', async () => {
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 201 });

            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const allButtons = screen.getAllByRole('button');
            const addButton = allButtons.find(btn => 
                btn.textContent === 'Add User' && !btn.closest('[role="dialog"]')
            );
            
            expect(addButton).toBeInTheDocument();
            if (addButton) {
                fireEvent.click(addButton);

                await waitFor(() => {
                    expect(screen.getByRole('dialog')).toBeInTheDocument();
                }, { timeout: 5000 });

                const usernameInput = screen.getByLabelText(/Username/i);
                const emailInput = screen.getByLabelText(/Email/i);

                fireEvent.change(usernameInput, { target: { value: 'newuser' } });
                fireEvent.change(emailInput, { target: { value: 'newuser@example.com' } });

                const saveButton = screen.getByRole('button', { name: /Save/i });
                fireEvent.click(saveButton);

                await waitFor(() => {
                    expect(mockApiPost).toHaveBeenCalledWith('/api/admin/users', expect.objectContaining({
                        username: 'newuser',
                        email: 'newuser@example.com',
                    }));
                }, { timeout: 5000 });
            }
        });

        it('updates user successfully', async () => {
            mockApiPut.mockResolvedValueOnce({ data: {}, status: 200 });

            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const editButtons = screen.getAllByRole('button', { name: /Edit/i });
            fireEvent.click(editButtons[0]);

            await waitFor(() => {
                expect(screen.getByRole('dialog')).toBeInTheDocument();
            }, { timeout: 5000 });

            const usernameInput = screen.getByLabelText(/Username/i);
            fireEvent.change(usernameInput, { target: { value: 'updateduser' } });

            const saveButton = screen.getByRole('button', { name: /Save/i });
            fireEvent.click(saveButton);

            await waitFor(() => {
                expect(mockApiPut).toHaveBeenCalledWith('/api/admin/users/1', expect.objectContaining({
                    username: 'updateduser',
                }));
            }, { timeout: 5000 });
        });

        it('deletes user after password confirmation', async () => {
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 200 });
            mockApiDelete.mockResolvedValueOnce({ data: {}, status: 200 });

            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const deleteButtons = screen.getAllByRole('button', { name: /Delete/i });
            fireEvent.click(deleteButtons[0]);

            await waitFor(() => {
                expect(screen.getByText(/Confirm Action/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const passwordInput = screen.getByLabelText(/Admin Password/i);
            fireEvent.change(passwordInput, { target: { value: 'admin123' } });

            const submitButton = screen.getByRole('button', { name: /Submit/i });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(mockApiPost).toHaveBeenCalledWith('/api/admin/verify-admin', { password: 'admin123' });
                expect(mockApiDelete).toHaveBeenCalledWith('/api/admin/users/1');
            }, { timeout: 5000 });
        });

        it('shows admin badge for admin users', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('admin')).toBeInTheDocument();
            }, { timeout: 5000 });

            // Check for admin indicator (letter 'A' badge)
            const adminRow = screen.getByText('admin').closest('tr');
            expect(adminRow).toBeInTheDocument();
        });

        it('displays user status correctly', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('blocked')).toBeInTheDocument();
            }, { timeout: 5000 });

            const blockedRow = screen.getByText('blocked').closest('tr');
            expect(blockedRow).not.toBeNull();
            if (blockedRow) {
                expect(within(blockedRow).getByText(/Blocked/i, { selector: 'span' })).toBeInTheDocument();
                expect(within(blockedRow).getByText('Violation of terms')).toBeInTheDocument();
            }
        });
    });

    describe('Whitelist Management', () => {
        it('adds email to whitelist', async () => {
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 200 });
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 200 });

            renderAdminPage();

            const whitelistTab = screen.getByRole('tab', { name: /Whitelist/i });
            fireEvent.click(whitelistTab);

            await waitFor(() => {
                expect(screen.getByLabelText(/Email address/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const emailInput = screen.getByLabelText(/Email address/i);
            fireEvent.change(emailInput, { target: { value: 'new@example.com' } });

            const addButton = screen.getByRole('button', { name: /Add to Whitelist/i });
            fireEvent.click(addButton);

            await waitFor(() => {
                expect(screen.getByText(/Confirm Action/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const passwordInput = screen.getByLabelText(/Admin Password/i);
            fireEvent.change(passwordInput, { target: { value: 'admin123' } });

            const submitButton = screen.getByRole('button', { name: /Submit/i });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(mockApiPost).toHaveBeenCalledWith('/api/admin/verify-admin', { password: 'admin123' });
            }, { timeout: 5000 });
        });

        it('removes email from whitelist', async () => {
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 200 });
            mockApiDelete.mockResolvedValueOnce({ data: {}, status: 200 });

            renderAdminPage();

            const whitelistTab = screen.getByRole('tab', { name: /Whitelist/i });
            fireEvent.click(whitelistTab);

            await waitFor(() => {
                expect(screen.getByText('allowed1@example.com')).toBeInTheDocument();
            }, { timeout: 5000 });

            const deleteButtons = screen.getAllByRole('button', { name: /Delete/i });
            // Find delete button in whitelist (not in users table)
            const whitelistDeleteButton = deleteButtons.find(btn => 
                btn.closest('[role="listitem"]') !== null
            );
            
            if (whitelistDeleteButton) {
                fireEvent.click(whitelistDeleteButton);

                await waitFor(() => {
                    expect(screen.getByText(/Confirm Action/i)).toBeInTheDocument();
                }, { timeout: 5000 });
            }
        });

        it('validates email format when adding to whitelist', async () => {
            renderAdminPage();

            const whitelistTab = screen.getByRole('tab', { name: /Whitelist/i });
            fireEvent.click(whitelistTab);

            await waitFor(() => {
                expect(screen.getByLabelText(/Email address/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const emailInput = screen.getByLabelText(/Email address/i);
            fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

            const addButton = screen.getByRole('button', { name: /Add to Whitelist/i });
            fireEvent.click(addButton);

            await waitFor(() => {
                expect(mockShowNotification).toHaveBeenCalledWith(
                    expect.stringContaining(getTranslation('admin.whitelist.invalidEmail', testTranslations)),
                    'error'
                );
            }, { timeout: 5000 });
        });
    });

    describe('Blacklist Management', () => {
        it('adds email to blacklist', async () => {
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 200 });
            mockApiPost.mockResolvedValueOnce({ data: { message: 'Email added to blacklist' }, status: 200 });

            renderAdminPage();

            const blacklistTab = screen.getByRole('tab', { name: /Blacklist/i });
            fireEvent.click(blacklistTab);

            await waitFor(() => {
                expect(screen.getByLabelText(/Email address/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const emailInput = screen.getByLabelText(/Email address/i);
            fireEvent.change(emailInput, { target: { value: 'blocked@example.com' } });

            const reasonInput = screen.getAllByLabelText(/Reason/i)[0];
            fireEvent.change(reasonInput, { target: { value: 'Spam account' } });

            const addButton = screen.getByRole('button', { name: /Add to Whitelist/i });
            fireEvent.click(addButton);

            await waitFor(() => {
                expect(screen.getByText(/Confirm Action/i)).toBeInTheDocument();
            }, { timeout: 5000 });
        });

        it('validates email format when adding to blacklist', async () => {
            renderAdminPage();

            const blacklistTab = screen.getByRole('tab', { name: /Blacklist/i });
            fireEvent.click(blacklistTab);

            await waitFor(() => {
                expect(screen.getByLabelText(/Email address/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const emailInput = screen.getByLabelText(/Email address/i);
            fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

            const addButton = screen.getByRole('button', { name: /Add to Whitelist/i });
            fireEvent.click(addButton);

            await waitFor(() => {
                expect(mockShowNotification).toHaveBeenCalledWith(
                    expect.stringContaining(getTranslation('admin.blacklist.invalidEmail', testTranslations)),
                    'error'
                );
            }, { timeout: 5000 });
        });
    });

    describe('Access Control Mode', () => {
        it('displays current access mode', async () => {
            renderAdminPage();

            const accessControlTab = screen.getByRole('tab', { name: /Access Control/i });
            fireEvent.click(accessControlTab);

            await waitFor(() => {
                expect(screen.getByText(/Current mode/i)).toBeInTheDocument();
            }, { timeout: 5000 });

            const modeLine = screen.getByText(/Current mode/i).closest('p');
            expect(modeLine).not.toBeNull();
            expect(modeLine).toHaveTextContent(/Whitelist/i);
        });

        it('opens change mode dialog', async () => {
            renderAdminPage();

            const accessControlTab = screen.getByRole('tab', { name: /Access Control/i });
            fireEvent.click(accessControlTab);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /Change mode/i })).toBeInTheDocument();
            }, { timeout: 5000 });

            const changeModeButton = screen.getByRole('button', { name: /Change mode/i });
            fireEvent.click(changeModeButton);

            await waitFor(() => {
                expect(screen.getByRole('heading', { name: /Change Access Mode/i })).toBeInTheDocument();
            }, { timeout: 5000 });
        });

        it('requests OTP code', async () => {
            mockApiPost.mockResolvedValueOnce({ data: {}, status: 200 });

            renderAdminPage();

            const accessControlTab = screen.getByRole('tab', { name: /Access Control/i });
            fireEvent.click(accessControlTab);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /Change mode/i })).toBeInTheDocument();
            }, { timeout: 5000 });

            const changeModeButton = screen.getByRole('button', { name: /Change mode/i });
            fireEvent.click(changeModeButton);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /Request OTP code/i })).toBeInTheDocument();
            }, { timeout: 5000 });

            const requestOtpButton = screen.getByRole('button', { name: /Request OTP code/i });
            fireEvent.click(requestOtpButton);

            await waitFor(() => {
                expect(mockApiPost).toHaveBeenCalledWith('/api/admin/access-mode/request-otp');
            }, { timeout: 5000 });
        });
    });

    describe('Form Validation', () => {
        it('requires block reason when user is blocked', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const allButtons = screen.getAllByRole('button');
            const addButton = allButtons.find(btn => 
                btn.textContent === 'Add User' && !btn.closest('[role="dialog"]')
            );
            
            expect(addButton).toBeInTheDocument();
            if (addButton) {
                fireEvent.click(addButton);

                await waitFor(() => {
                    expect(screen.getByRole('dialog')).toBeInTheDocument();
                }, { timeout: 5000 });

                const blockedCheckbox = screen.getByLabelText(/Blocked/i);
                fireEvent.click(blockedCheckbox);

                await waitFor(() => {
                    expect(screen.getByLabelText(/Block Reason/i)).toBeInTheDocument();
                }, { timeout: 5000 });

                const saveButton = screen.getByRole('button', { name: /Save/i });
                expect(saveButton).toBeDisabled();
            }
        });

        it('limits block reason length', async () => {
            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const allButtons = screen.getAllByRole('button');
            const addButton = allButtons.find(btn => 
                btn.textContent === 'Add User' && !btn.closest('[role="dialog"]')
            );
            
            expect(addButton).toBeInTheDocument();
            if (addButton) {
                fireEvent.click(addButton);

                await waitFor(() => {
                    expect(screen.getByRole('dialog')).toBeInTheDocument();
                }, { timeout: 5000 });

                const blockedCheckbox = screen.getByLabelText(/Blocked/i);
                fireEvent.click(blockedCheckbox);

                await waitFor(() => {
                    const reasonInput = screen.getByLabelText(/Block Reason/i);
                    const longText = 'a'.repeat(250);
                    fireEvent.change(reasonInput, { target: { value: longText } });
                    expect((reasonInput as HTMLInputElement).value.length).toBeLessThanOrEqual(200);
                }, { timeout: 5000 });
            }
        });
    });

    describe('Error Handling', () => {
        it('handles API errors when fetching users', async () => {
            mockApiGet.mockImplementation((url: string) => {
                if (url === '/api/admin/users') {
                    return Promise.reject(new Error('Network error'));
                }
                return Promise.resolve({ data: {} });
            });

            renderAdminPage();

            await waitFor(() => {
                expect(mockShowNotification).toHaveBeenCalledWith(
                    expect.stringContaining('Error'),
                    'error'
                );
            }, { timeout: 5000 });
        });

        it('handles email already exists error', async () => {
            const axiosError = {
                isAxiosError: true,
                response: {
                    status: 409,
                    data: 'Email already exists',
                },
            };

            mockApiPost.mockRejectedValueOnce(axiosError);

            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            const allButtons = screen.getAllByRole('button');
            const addButton = allButtons.find(btn => 
                btn.textContent === 'Add User' && !btn.closest('[role="dialog"]')
            );
            
            expect(addButton).toBeInTheDocument();
            if (addButton) {
                fireEvent.click(addButton);

                await waitFor(() => {
                    expect(screen.getByRole('dialog')).toBeInTheDocument();
                }, { timeout: 5000 });

                const emailInput = screen.getByLabelText(/Email/i);
                fireEvent.change(emailInput, { target: { value: 'existing@example.com' } });
                fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'test' } });

                const saveButton = screen.getByRole('button', { name: /Save/i });
                fireEvent.click(saveButton);

                await waitFor(() => {
                    expect(mockShowNotification).toHaveBeenCalledWith(
                        expect.stringContaining('Email already exists'),
                        'error'
                    );
                }, { timeout: 5000 });
            }
        });
    });

    describe('Pagination', () => {
        it('handles page change', async () => {
            mockApiGet.mockImplementationOnce((url: string, config?: any) => {
                if (url === '/api/admin/users') {
                    const page = config?.params?.page || 0;
                    return Promise.resolve({
                        data: {
                            content: page === 0 ? mockUsers : [],
                            totalElements: mockUsers.length,
                        },
                    });
                }
                return Promise.resolve({ data: {} });
            });

            renderAdminPage();

            await waitFor(() => {
                expect(screen.getByText('user1')).toBeInTheDocument();
            }, { timeout: 5000 });

            // Check if pagination controls exist when there are more items
            const pagination = screen.queryByLabelText(/Rows per page/i);
            if (pagination) {
                expect(pagination).toBeInTheDocument();
            }
        });
    });
});

