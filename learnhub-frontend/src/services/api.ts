import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/api/v1';

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const isUnauthorized = error.response?.status === 401;
    const url = originalRequest?.url ?? '';
    const isAuthRecoveryRequest =
      url.includes('/auth/login') ||
      url.includes('/auth/refresh') ||
      url.includes('/auth/logout');

    if (isUnauthorized && originalRequest && !originalRequest._retry && !isAuthRecoveryRequest) {
      originalRequest._retry = true;

      try {
        await refreshClient.post('/auth/refresh');
        return api(originalRequest);
      } catch {
        try {
          localStorage.removeItem('auth-storage');
        } catch {
          /* ignore localStorage failures */
        }

        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }

    if (isUnauthorized && isAuthRecoveryRequest) {
      try {
        localStorage.removeItem('auth-storage');
      } catch {
        /* ignore localStorage failures */
      }
    }

    console.error('API Error:', {
      status: error.response?.status,
      message: error.message,
      data: error.response?.data,
    });

    return Promise.reject(error);
  }
);

export default api;
