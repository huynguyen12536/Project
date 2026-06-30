/**
 * Custom Hook: useUserProfile
 *
 * Provides convenient interface for user profile operations.
 * Handles loading states, error handling, and side effects.
 *
 * Usage:
 *   const {
 *     profile,
 *     isLoading,
 *     error,
 *     loadProfile,
 *     updateProfile,
 *     uploadAvatar,
 *   } = useUserProfile();
 */

import { useEffect, useState, useCallback } from 'react';
import { useUser } from '../context/UserContext';
import { UserProfile, UserProfileUpdatePayload, LoadingState } from '../types';

interface UseUserProfileReturn {
  profile: UserProfile | null;
  loading: LoadingState;
  loadProfile: (userId: string) => Promise<void>;
  updateProfile: (
    userId: string,
    updates: Partial<UserProfileUpdatePayload>
  ) => Promise<void>;
  uploadAvatar: (userId: string, file: File) => Promise<string>;
  isValid: boolean;
}

export const useUserProfile = (
  autoLoadUserId?: string
): UseUserProfileReturn => {
  const { user, isLoading, error, fetchUserProfile, updateUserProfile, uploadUserAvatar } =
    useUser();

  const [loadingState, setLoadingState] = useState<LoadingState>({
    isLoading: false,
    error: null,
    success: null,
  });

  /**
   * Load user profile on mount if autoLoadUserId provided
   */
  useEffect(() => {
    if (autoLoadUserId) {
      loadProfile(autoLoadUserId);
    }
  }, [autoLoadUserId]);

  /**
   * Load user profile
   */
  const loadProfile = useCallback(async (userId: string) => {
    setLoadingState({ isLoading: true, error: null, success: null });
    try {
      await fetchUserProfile(userId);
      setLoadingState({ isLoading: false, error: null, success: null });
    } catch (err: any) {
      setLoadingState({
        isLoading: false,
        error: err,
        success: null,
      });
    }
  }, [fetchUserProfile]);

  /**
   * Update user profile
   */
  const updateProfile = useCallback(
    async (userId: string, updates: Partial<UserProfileUpdatePayload>) => {
      setLoadingState({ isLoading: true, error: null, success: null });
      try {
        await updateUserProfile(userId, updates);
        setLoadingState({
          isLoading: false,
          error: null,
          success: 'Profile updated successfully',
        });
      } catch (err: any) {
        setLoadingState({
          isLoading: false,
          error: err,
          success: null,
        });
        throw err;
      }
    },
    [updateUserProfile]
  );

  /**
   * Upload user avatar
   */
  const uploadAvatar = useCallback(
    async (userId: string, file: File): Promise<string> => {
      setLoadingState({ isLoading: true, error: null, success: null });
      try {
        const response = await uploadUserAvatar(userId, file);
        setLoadingState({
          isLoading: false,
          error: null,
          success: 'Avatar uploaded successfully',
        });
        return response.avatarUrl;
      } catch (err: any) {
        setLoadingState({
          isLoading: false,
          error: err,
          success: null,
        });
        throw err;
      }
    },
    [uploadUserAvatar]
  );

  /**
   * Validate required fields
   */
  const isValid =
    user !== null &&
    Boolean(user?.firstName?.trim()) &&
    Boolean(user?.lastName?.trim()) &&
    Boolean(user?.email?.trim());

  return {
    profile: user,
    loading: loadingState,
    loadProfile,
    updateProfile,
    uploadAvatar,
    isValid,
  };
};
