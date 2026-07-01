/**
 * User Context & Provider
 *
 * Manages global user profile state using React Context API.
 * Provides centralized access to user data across the application.
 *
 * Usage:
 *   const { user, loading, updateProfile, uploadAvatar } = useUser();
 */

import React, { createContext, useState, useCallback, ReactNode, useEffect } from 'react';
import api from '../services/api';
import { useAuthStore } from '../stores/authStore';
import {
  UserProfile,
  UserProfileUpdatePayload,
  AvatarUploadResponse,
  ErrorResponse,
} from '../types';

interface UserContextType {
  user: UserProfile | null;
  isLoading: boolean;
  error: ErrorResponse | null;
  fetchUserProfile: (userId: string) => Promise<void>;
  updateUserProfile: (
    userId: string,
    updates: Partial<UserProfileUpdatePayload>
  ) => Promise<UserProfile>;
  uploadUserAvatar: (
    userId: string,
    file: File
  ) => Promise<AvatarUploadResponse>;
  clearError: () => void;
}

export const UserContext = createContext<UserContextType | undefined>(undefined);

interface UserProviderProps {
  children: ReactNode;
}

const withAvatarVersion = (avatarUrl?: string | null, version?: string | null) => {
  if (!avatarUrl) {
    return null;
  }

  if (!version) {
    return avatarUrl;
  }

  const separator = avatarUrl.includes('?') ? '&' : '?';
  return `${avatarUrl}${separator}v=${encodeURIComponent(version)}`;
};

const normalizeProfile = (profile: UserProfile): UserProfile => ({
  ...profile,
  avatarUrl: withAvatarVersion(profile.avatarUrl, profile.updatedAt),
});

export const UserProvider: React.FC<UserProviderProps> = ({ children }) => {
  const authUser = useAuthStore(state => state.user);
  const authRole = useAuthStore(state => state.role);

  const [user, setUser] = useState<UserProfile | null>(
    authUser ? {
      id: authUser.id,
      username: authUser.email,
      email: authUser.email,
      firstName: authUser.firstName,
      lastName: authUser.lastName,
      bio: authUser.bio || '',
      avatarUrl: authUser.avatarUrl || null,
      role: authRole || 'student',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      phone: '',
      location: ''
    } : null
  );

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);

  // Initialize user from authStore on mount
  useEffect(() => {
    if (authUser && !user) {
      setUser({
        id: authUser.id,
        username: authUser.email,
        email: authUser.email,
        firstName: authUser.firstName,
        lastName: authUser.lastName,
        bio: authUser.bio || '',
        avatarUrl: authUser.avatarUrl || null,
        role: authRole || 'student',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        phone: '',
        location: ''
      });
    }
  }, [authUser, authRole, user]);

  /**
   * Fetch user profile from backend
   */
  const fetchUserProfile = useCallback(async (userId: string) => {
    // If userId is 'me' or 'current' and we have authUser, use authUser's id
    const idToFetch = ((userId === 'me' || userId === 'current') && authUser?.id)
      ? authUser.id
      : userId;

    setIsLoading(true);
    setError(null);

    try {
      const { data } = await api.get<UserProfile>(`/users/${idToFetch}`);
      setUser(normalizeProfile(data));
    } catch (err: any) {
      // If API fails, use mock data for demonstration
      if (authUser) {
        setUser({
          id: authUser.id,
          username: authUser.email,
          email: authUser.email,
          firstName: authUser.firstName,
          lastName: authUser.lastName,
          bio: authUser.bio || '',
          avatarUrl: authUser.avatarUrl || null,
          role: authRole || 'student',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          phone: '',
          location: ''
        });
      } else {
        const errorResponse: ErrorResponse = {
          error: err.response?.data?.error || 'Failed to fetch profile',
          code: err.response?.data?.code || 'FETCH_ERROR',
        };
        setError(errorResponse);
      }
    } finally {
      setIsLoading(false);
    }
  }, [authUser, authRole]);

  /**
   * Update user profile
   */
  const updateUserProfile = useCallback(
    async (userId: string, updates: Partial<UserProfileUpdatePayload>) => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await api.patch<UserProfile>(`/users/${userId}`, updates);
        const data = normalizeProfile(response.data);
        setUser(data);

        // Update authStore to keep data consistent
        const setAuthUser = useAuthStore.getState().setUser;
        if (data && setAuthUser) {
          setAuthUser({
            id: data.id,
            email: data.email,
            firstName: data.firstName,
            lastName: data.lastName,
            role: (data.role as any) || 'student',
            avatarUrl: data.avatarUrl,
            bio: data.bio
          });
        }

        return data;
      } catch (err: any) {
        const errorResponse: ErrorResponse = {
          error: err.response?.data?.error || 'Failed to update profile',
          code: err.response?.data?.code || 'UPDATE_ERROR',
          field: err.response?.data?.field,
        };
        setError(errorResponse);
        throw errorResponse;
      } finally {
        setIsLoading(false);
      }
    },
    [user]
  );

  /**
   * Upload user avatar
   */
  const uploadUserAvatar = useCallback(
    async (userId: string, file: File) => {
      setIsLoading(true);
      setError(null);

      try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await api.post<AvatarUploadResponse>(
          `/users/${userId}/avatar`,
          formData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
            },
          }
        );
        const data = response.data;

        // Update user avatar URL in state
        if (user) {
          const updatedUser = {
            ...user,
            avatarUrl: withAvatarVersion(data.avatarUrl, data.uploadedAt),
            updatedAt: data.uploadedAt,
          };
          setUser(updatedUser);

          // Update authStore
          const setAuthUser = useAuthStore.getState().setUser;
          if (setAuthUser) {
            setAuthUser({
              id: updatedUser.id,
              email: updatedUser.email,
              firstName: updatedUser.firstName,
              lastName: updatedUser.lastName,
              role: (updatedUser.role as any) || 'student',
              avatarUrl: updatedUser.avatarUrl,
              bio: updatedUser.bio
            });
          }
        }

        return data;
      } catch (err: any) {
        const errorResponse: ErrorResponse = {
          error: err.response?.data?.error || 'Failed to upload avatar',
          code: err.response?.data?.code || 'UPLOAD_ERROR',
        };
        setError(errorResponse);
        throw errorResponse;
      } finally {
        setIsLoading(false);
      }
    },
    [user]
  );

  /**
   * Clear error state
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const value: UserContextType = {
    user,
    isLoading,
    error,
    fetchUserProfile,
    updateUserProfile,
    uploadUserAvatar,
    clearError,
  };

  return (
    <UserContext.Provider value={value}>{children}</UserContext.Provider>
  );
};

/**
 * Custom hook for using UserContext
 */
export const useUser = (): UserContextType => {
  const context = React.useContext(UserContext);
  if (!context) {
    throw new Error('useUser must be used within UserProvider');
  }
  return context;
};
