/**
 * Zustand auth store (persisted to localStorage under 'auth-storage').
 *
 * Holds the authenticated user and derived role.
 * Access/refresh tokens are stored in HttpOnly cookies managed by the backend.
 */

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse, AuthUser, UserRole } from '../types/auth';

interface AuthState {
  user: AuthUser | null;
  token: string | null;
  refreshToken: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;

  login: (payload: AuthResponse & { user?: AuthUser }) => void;
  setUser: (user: AuthUser) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      refreshToken: null,
      role: null,
      isAuthenticated: false,

      login: (payload) =>
        set({
          token: payload.token ?? null,
          refreshToken: payload.refreshToken ?? null,
          user: payload.user ?? null,
          role: payload.user?.role ?? null,
          isAuthenticated: Boolean(payload.user?.id || payload.userId),
        }),

      setUser: (user) =>
        set({
          user,
          role: user.role,
        }),

      logout: () =>
        set({
          user: null,
          token: null,
          refreshToken: null,
          role: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'auth-storage',
      // Persist everything needed to rehydrate an authenticated session.
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        role: state.role,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
