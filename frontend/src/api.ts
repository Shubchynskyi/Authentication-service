import axios from 'axios';
import {API_URL} from './config';
import {clearTokens, getAccessToken, isJwtExpired, setTokens} from './utils/token';
import {logError} from './utils/logger';
import i18n from './i18n/i18n';
import {subscribeAuthEvents} from './utils/authEvents';

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
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

const getXsrfToken = () => {
    if (typeof document === 'undefined') return null;
    const raw = document.cookie
        .split('; ')
        .find((row) => row.startsWith('XSRF-TOKEN='));
    if (!raw) return null;
    const value = raw.split('=')[1];
    return value ? decodeURIComponent(value) : null;
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
        config.headers['Accept-Language'] = (i18n.language || 'en').split('-')[0];

        const xsrfToken = getXsrfToken();
        if (xsrfToken) {
            config.headers['X-XSRF-TOKEN'] = xsrfToken;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // If this is a 401 error, and this is not a token refresh request
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
                if (!getXsrfToken()) {
                    try {
                        await api.get('/api/auth/csrf');
                    } catch {
                        // ignore
                    }
                }
                const response = await api.post('/api/auth/refresh');
                const { accessToken } = response.data;
                if (!accessToken) {
                    throw new Error('Missing access token on refresh');
                }
                setTokens(accessToken);

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

        // If this is a 401/403 error when trying to refresh the token
        // Don't redirect or log - this is normal when no session exists
        // AuthContext will handle setting isAuthenticated = false
        if ((error.response?.status === 401 || error.response?.status === 403) &&
            originalRequest.url?.includes('/api/auth/refresh')) {
            clearTokens();
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

if (typeof window !== 'undefined') {
    subscribeAuthEvents((event) => {
        if (event.type === 'logout') {
            clearAuthAndRedirect();
        }
    });
}

export default api;
