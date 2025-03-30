import axios from 'axios';
import { API_URL } from './config';

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
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    // Используем replace вместо href для предотвращения проблем с историей браузера
    window.location.replace('/login');
};

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
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

        // Если это ошибка 401 и это не запрос на обновление токена
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
                const refreshToken = localStorage.getItem('refreshToken');
                if (!refreshToken) {
                    throw new Error('No refresh token available');
                }

                const response = await axios.post('http://localhost:8080/api/auth/refresh', {
                    refreshToken: refreshToken
                });

                const { accessToken, refreshToken: newRefreshToken } = response.data;
                localStorage.setItem('accessToken', accessToken);
                localStorage.setItem('refreshToken', newRefreshToken);

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

        // Если это ошибка 401 при попытке обновить токен
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

export default api;
