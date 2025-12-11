import React from 'react';
import { vi } from 'vitest';
import { getTranslation } from './translations';

// Common mock for AuthContext
export const createMockAuthContext = (overrides = {}) => ({
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
    setTokens: vi.fn(),
    error: null,
    isLoading: false,
    ...overrides,
});

// Common mock for ProfileContext
export const createMockProfileContext = (overrides: Partial<{
    profile: { email: string; name: string; roles: string[]; authProvider: 'LOCAL' | 'GOOGLE' } | null;
    isLoading: boolean;
    isAdmin: boolean;
    updateProfile: () => Promise<void>;
}> = {}) => ({
    profile: null as { email: string; name: string; roles: string[]; authProvider: 'LOCAL' | 'GOOGLE' } | null,
    isLoading: false,
    isAdmin: false,
    updateProfile: vi.fn(),
    ...overrides,
});

// Common mock for NotificationContext
export const createMockNotificationContext = (overrides = {}) => ({
    showNotification: vi.fn(),
    removeNotification: vi.fn(),
    notifications: [],
    ...overrides,
});

// Common mock for useNavigate
export const createMockNavigate = () => vi.fn();

// Common mock for useLocation
export const createMockLocation = (overrides = {}) => ({
    pathname: '/',
    search: '',
    hash: '',
    state: null,
    key: 'default',
    ...overrides,
});

// Mock react-router-dom
export const setupRouterMocks = (config?: {
    navigate?: ReturnType<typeof createMockNavigate>;
    location?: ReturnType<typeof createMockLocation>;
}) => {
    const mockNavigate = config?.navigate || createMockNavigate();
    const mockLocation = config?.location || createMockLocation();

    vi.mock('react-router-dom', async () => {
        const actual = await vi.importActual('react-router-dom');
        return {
            ...actual,
            useNavigate: () => mockNavigate,
            useLocation: () => mockLocation,
        };
    });

    return { mockNavigate, mockLocation };
};

// Mock react-i18next
export const setupI18nMocks = (customTranslations?: Record<string, string>) => {
    const mockChangeLanguage = vi.fn();
    const mockI18n = {
        language: 'en',
        changeLanguage: mockChangeLanguage,
        resolvedLanguage: 'en',
    };

    vi.mock('react-i18next', () => ({
        useTranslation: () => ({
            t: (key: string) => getTranslation(key, customTranslations),
            i18n: mockI18n,
        }),
        initReactI18next: {
            type: '3rdParty',
            init: vi.fn(),
        },
    }));

    return { mockI18n, mockChangeLanguage };
};

// Mock api instance
export const createMockApiInstance = () => {
    const mockPost = vi.fn();
    const mockGet = vi.fn();
    const mockPut = vi.fn();
    const mockDelete = vi.fn();
    const mockPatch = vi.fn();
    const mockRequestUse = vi.fn();
    const mockResponseUse = vi.fn();

    const mockApiInstance = {
        post: mockPost,
        get: mockGet,
        put: mockPut,
        delete: mockDelete,
        patch: mockPatch,
        defaults: {
            headers: {
                common: {} as Record<string, string>,
            },
        },
        interceptors: {
            request: {
                use: mockRequestUse,
            },
            response: {
                use: mockResponseUse,
            },
        },
    };

    return {
        mockApiInstance,
        mockPost,
        mockGet,
        mockPut,
        mockDelete,
        mockPatch,
        mockRequestUse,
        mockResponseUse,
    };
};

// Setup api mocks
export const setupApiMocks = () => {
    const apiMocks = createMockApiInstance();

    vi.mock('../api', () => ({
        default: apiMocks.mockApiInstance,
    }));

    return apiMocks;
};

// Mock axios
export const setupAxiosMocks = () => {
    const mockAxiosPost = vi.fn();
    const mockAxiosGet = vi.fn();
    const mockAxiosCreate = vi.fn(() => createMockApiInstance().mockApiInstance);
    const mockIsAxiosError = vi.fn((error: any) => error?.isAxiosError || false);

    vi.mock('axios', () => ({
        default: {
            post: mockAxiosPost,
            get: mockAxiosGet,
            create: mockAxiosCreate,
            isAxiosError: mockIsAxiosError,
        },
        isAxiosError: mockIsAxiosError,
    }));

    return {
        mockAxiosPost,
        mockAxiosGet,
        mockAxiosCreate,
        mockIsAxiosError,
    };
};

// Mock token utilities with configurable module path
export const setupTokenMocks = (modulePath = '../utils/token') => {
    const mockIsJwtExpired = vi.fn();
    const mockIsValidJwtFormat = vi.fn();
    const mockGetAccessToken = vi.fn();
    const mockGetRefreshToken = vi.fn();
    const mockClearTokens = vi.fn();

    vi.mock(modulePath, () => ({
        isJwtExpired: mockIsJwtExpired,
        isValidJwtFormat: mockIsValidJwtFormat,
        getAccessToken: mockGetAccessToken,
        getRefreshToken: mockGetRefreshToken,
        clearTokens: mockClearTokens,
    }));

    return {
        mockIsJwtExpired,
        mockIsValidJwtFormat,
        mockGetAccessToken,
        mockGetRefreshToken,
        mockClearTokens,
    };
};

// Mock NotificationContext with configurable module path
export const setupNotificationMocks = (
    modulePath = '../context/NotificationContext',
    overrides: {
        notifications?: Array<{ id: string; message: string; type: string }>;
        showNotification?: ReturnType<typeof vi.fn>;
        removeNotification?: ReturnType<typeof vi.fn>;
    } = {}
) => {
    const mockNotifications = overrides.notifications || [];
    const mockShowNotification = overrides.showNotification || vi.fn();
    const mockRemoveNotification = overrides.removeNotification || vi.fn();

    vi.mock(modulePath, () => ({
        NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
        useNotification: () => ({
            notifications: mockNotifications,
            showNotification: mockShowNotification,
            removeNotification: mockRemoveNotification,
        }),
    }));

    return {
        mockNotifications,
        mockShowNotification,
        mockRemoveNotification,
    };
};

// Setup mocks for contexts
export const setupContextMocks = (config: {
    auth?: ReturnType<typeof createMockAuthContext>;
    profile?: ReturnType<typeof createMockProfileContext>;
    notification?: ReturnType<typeof createMockNotificationContext>;
}) => {
    const mockAuth = config.auth || createMockAuthContext();
    const mockProfile = config.profile || createMockProfileContext();
    const mockNotification = config.notification || createMockNotificationContext();

    vi.mock('../context/AuthContext', () => ({
        AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
        useAuth: () => mockAuth,
    }));

    vi.mock('../context/ProfileContext', () => ({
        ProfileProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
        useProfile: () => mockProfile,
    }));

    vi.mock('../context/NotificationContext', () => ({
        NotificationProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
        useNotification: () => mockNotification,
    }));

    return {
        mockAuth,
        mockProfile,
        mockNotification,
    };
};

// Universal function to setup all common mocks
export const setupCommonMocks = (config?: {
    auth?: ReturnType<typeof createMockAuthContext>;
    profile?: ReturnType<typeof createMockProfileContext>;
    notification?: ReturnType<typeof createMockNotificationContext>;
    navigate?: ReturnType<typeof createMockNavigate>;
    location?: ReturnType<typeof createMockLocation>;
    translations?: Record<string, string>;
    api?: boolean;
    axios?: boolean;
}) => {
    const contextMocks = setupContextMocks({
        auth: config?.auth,
        profile: config?.profile,
        notification: config?.notification,
    });

    const routerMocks = setupRouterMocks({
        navigate: config?.navigate,
        location: config?.location,
    });

    const i18nMocks = setupI18nMocks(config?.translations);

    let apiMocks = null;
    if (config?.api !== false) {
        apiMocks = setupApiMocks();
    }

    let axiosMocks = null;
    if (config?.axios === true) {
        axiosMocks = setupAxiosMocks();
    }

    return {
        ...contextMocks,
        ...routerMocks,
        ...i18nMocks,
        apiMocks,
        axiosMocks,
    };
};

