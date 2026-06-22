/**
 * API Service Layer
 *
 * Centralized HTTP client for all backend API calls.
 * Handles:
 * - Authentication token injection
 * - Request/response interceptors
 * - Error handling and logging
 * - Timeout management
 *
 * Usage: const response = await api.get(`/users/${userId}`);
 */

import axios, { AxiosInstance, AxiosError } from 'axios';

// Use empty baseURL for relative requests (Vite proxy will handle routing to backend)
// During development: requests to /api/v1/* are proxied to http://localhost:8080/api/v1/*
// During production: requests go directly to the same origin
const API_BASE_URL = process.env.REACT_APP_API_URL || '';

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // 30 seconds for SSE connections
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Inject JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor: Handle errors globally
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      localStorage.removeItem('authToken');
      window.location.href = '/login';
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
