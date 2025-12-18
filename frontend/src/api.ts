import axios from 'axios';
import { API_URL } from './config';
import { getAccessToken, getRefreshToken, isJwtExpired, clearTokens, setTokens as persistTokens, getTokenStorageMode } from './utils/token';
import { logError } from './utils/logger';
import i18n from './i18n/i18n';

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

let isRefreshing = false;
let failedQueue: Array<{
    resolve: (value?: unknown) => void;
    reject: (reason?: any) => void;
}> = [];

const processQueue = (error: any = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve();
        }
    });
    failedQueue = [];
};

const clearAuthAndRedirect = () => {
    clearTokens();
    if (window.location.pathname !== '/login') {
        window.location.replace('/login');
    }
};

api.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        // Attach only a non-expired access token
        if (token && !isJwtExpired(token)) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        // Set Accept-Language header based on current i18n language
        const currentLanguage = (i18n.language || 'en').split('-')[0];
        config.headers['Accept-Language'] = currentLanguage;
        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // If this is a 401 error and this is not a token refresh request
        if (error.response?.status === 401 && 
            !originalRequest._retry && 
            !originalRequest.url?.includes('/api/auth/refresh')) {
            
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then(() => api(originalRequest))
                    .catch(err => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const refreshToken = getRefreshToken();
                if (!refreshToken) {
                    throw new Error('No refresh token available');
                }

                const response = await axios.post(`${API_URL}/api/auth/refresh`, {
                    refreshToken: refreshToken
                });

                const { accessToken, refreshToken: newRefreshToken } = response.data;
                persistTokens(accessToken, newRefreshToken || refreshToken, getTokenStorageMode());

                api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                processQueue();
                return api(originalRequest);
            } catch (refreshError) {
                logError('Token refresh failed', refreshError, {
                    url: originalRequest.url,
                    method: originalRequest.method,
                });
                processQueue(refreshError);
                clearAuthAndRedirect();
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        // If this is a 401 error when trying to refresh the token
        if (error.response?.status === 401 && 
            originalRequest.url?.includes('/api/auth/refresh')) {
            logError('Token refresh returned 401, clearing auth', undefined, {
                url: originalRequest.url,
            });
            clearAuthAndRedirect();
        }

        // Log critical server errors (5xx)
        if (error.response?.status >= 500) {
            logError('Server error', error, {
                url: error.config?.url,
                method: error.config?.method,
                status: error.response.status,
            });
        }

        // Log network errors
        if (!error.response && error.request) {
            logError('Network request failed', error, {
                url: error.config?.url,
                method: error.config?.method,
            });
        }

        return Promise.reject(error);
    }
);

export const checkAccess = async (resource: string): Promise<boolean> => {
    try {
        await api.get(`/api/auth/check-access/${resource}`);
        return true;
    } catch (error) {
        return false;
    }
};

// Cross-tab logout synchronization
if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
        if (e.key === 'accessToken' || e.key === 'refreshToken') {
            // Handle only token removal events and give some time for potential token re-setting
            // (for example, during OAuth2 login when tokens are temporarily cleared)
            // Note: storage event only fires in OTHER tabs, not the tab that made the change
            if (e.newValue === null && e.oldValue !== null) {
                // Increased delay to handle cases where tokens are cleared and immediately re-set
                setTimeout(() => {
                    const access = getAccessToken();
                    const refresh = getRefreshToken();
                    // Only redirect if tokens are still missing after delay
                    // This prevents false redirects during token refresh or re-authentication
                    if (!access || !refresh) {
                        // If tokens remain removed in another tab after delay, redirect to login in this tab
                        clearAuthAndRedirect();
                    }
                }, 300);
            }
        }
    });
}

export default api;
