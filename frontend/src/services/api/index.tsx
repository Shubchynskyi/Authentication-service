// api.tsx
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
});

export const refreshAccessToken = async () => {
  console.log("api.ts: refreshAccessToken called");
  try {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      console.log("api.ts: No refresh token found");
      return null;
    }

    const response = await api.post<{ accessToken: string; refreshToken: string }>('/api/auth/refresh', { refreshToken });
    console.log("api.ts: Token refreshed successfully:", response.data);
    localStorage.setItem('accessToken', response.data.accessToken);
    localStorage.setItem('refreshToken', response.data.refreshToken);
    return response.data.accessToken;

  } catch (error) {
    console.error("api.ts: Error refreshing token:", error);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    return null;
  }
};

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && originalRequest.url !== '/api/auth/refresh' && !originalRequest._retry) {
      originalRequest._retry = true;
      console.log("api.ts: 401 error, attempting to refresh token");
      const newAccessToken = await refreshAccessToken();

      if (newAccessToken) {
        console.log("api.ts: Token refreshed in interceptor, retrying original request"); // ЛОГ 6
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } else {
        console.log("api.ts: Token refresh failed, rejecting");
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);

export default api;