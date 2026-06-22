/**
 * User Context & Provider
 *
 * Manages global user profile state using React Context API.
 * Provides centralized access to user data across the application.
 *
 * Usage:
 *   const { user, loading, updateProfile, uploadAvatar } = useUser();
 */

import React, { createContext, useState, useCallback, ReactNode } from 'react';
import api from '../services/api';
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
    updates: UserProfileUpdatePayload
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

export const UserProvider: React.FC<UserProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);

  /**
   * Fetch user profile from backend
   */
  const fetchUserProfile = useCallback(async (userId: string) => {
    setIsLoading(true);
    setError(null);

    try {
      const { data } = await api.get<UserProfile>(`/users/${userId}`);
      setUser(data);
    } catch (err: any) {
      const errorResponse: ErrorResponse = {
        error: err.response?.data?.error || 'Failed to fetch profile',
        code: err.response?.data?.code || 'FETCH_ERROR',
      };
      setError(errorResponse);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Update user profile
   */
  const updateUserProfile = useCallback(
    async (userId: string, updates: UserProfileUpdatePayload) => {
      setIsLoading(true);
      setError(null);

      try {
        const { data } = await api.put<UserProfile>(
          `/users/${userId}`,
          updates
        );
        setUser(data);
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
    []
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

        const { data } = await api.post<AvatarUploadResponse>(
          `/users/${userId}/avatar`,
          formData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
            },
          }
        );

        // Update user avatar URL in state
        if (user) {
          setUser({ ...user, avatarUrl: data.avatarUrl });
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
