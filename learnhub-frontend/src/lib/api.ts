import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';

const configuredApiBaseUrl =
  (import.meta.env.VITE_API_URL as string | undefined) ||
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ||
  '/api';

const API_BASE_URL = configuredApiBaseUrl.replace(/\/$/, '').endsWith('/api')
  ? configuredApiBaseUrl.replace(/\/$/, '')
  : `${configuredApiBaseUrl.replace(/\/$/, '')}/api`;

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const isUnauthorized = error.response?.status === 401;
    const url = originalRequest?.url ?? '';
    const isAuthRecoveryRequest =
      url.includes('/v1/auth/login') ||
      url.includes('/v1/auth/refresh') ||
      url.includes('/v1/auth/logout');

    if (isUnauthorized && originalRequest && !originalRequest._retry && !isAuthRecoveryRequest) {
      originalRequest._retry = true;

      try {
        await refreshClient.post('/v1/auth/refresh');
        return apiClient(originalRequest);
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

    return Promise.reject(error);
  }
);

export default apiClient;
