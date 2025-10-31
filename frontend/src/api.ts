import axios from 'axios';
import { API_URL } from './config';
import { getAccessToken, getRefreshToken, isJwtExpired, clearTokens } from './utils/token';

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
                localStorage.setItem('accessToken', accessToken);
                if (newRefreshToken) {
                    localStorage.setItem('refreshToken', newRefreshToken);
                }

                api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                processQueue();
                return api(originalRequest);
            } catch (refreshError) {
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
            clearAuthAndRedirect();
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
            const access = getAccessToken();
            const refresh = getRefreshToken();
            if (!access || !refresh) {
                // If tokens are removed in another tab, redirect to login in this tab
                clearAuthAndRedirect();
            }
        }
    });
}

export default api;
