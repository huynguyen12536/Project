/**
 * Phase 1A Axios HTTP client.
 *
 * - baseURL from VITE_API_URL (falls back to the Vite dev proxy at /api)
 * - JWT interceptor attaches the token from the auth store on every request
 * - 401 handling: clears auth state and redirects to /login
 *
 * This is the canonical client for the new auth/course/admin features.
 * The legacy `src/services/api.ts` remains for existing profile/assessment code.
 */

import axios, { AxiosError, AxiosInstance } from 'axios';

const API_BASE_URL =
  (import.meta.env.VITE_API_URL as string | undefined) ||
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ||
  '/api';

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Read the persisted auth token without creating an import cycle with the store.
 * The auth store persists under the key 'auth-storage' (zustand persist).
 */
function getToken(): string | null {
  try {
    const raw = localStorage.getItem('auth-storage');
    if (raw) {
      const parsed = JSON.parse(raw);
      const token = parsed?.state?.token;
      if (token) return token;
    }
  } catch {
    /* ignore parse errors */
  }
  // Fallback to a plain token key for interoperability with legacy code.
  return localStorage.getItem('authToken');
}

apiClient.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Clear persisted auth and bounce to login.
      try {
        localStorage.removeItem('auth-storage');
        localStorage.removeItem('authToken');
      } catch {
        /* ignore */
      }
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
