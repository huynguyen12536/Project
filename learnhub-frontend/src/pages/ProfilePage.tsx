/**
 * Profile Page
 *
 * Main page for user profile management.
 * Composition of:
 * - ProfileHeader (avatar + name)
 * - ProfileForm (editable fields)
 * - AvatarUploadZone (avatar management)
 *
 * Integrates with UserContext for state management.
 */

import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useUserProfile } from '../hooks/useUserProfile';
import { ProfileHeader } from '../components/profile/ProfileHeader';
import { ProfileForm } from '../components/profile/ProfileForm';
import { AvatarUploadZone } from '../components/profile/AvatarUploadZone';
import { AlertBox } from '../components/common/AlertBox';
import Skeleton from '@mui/material/Skeleton';

export const ProfilePage: React.FC = () => {
  const { userId = 'me' } = useParams();
  const {
    profile,
    loading,
    loadProfile,
    updateProfile,
    uploadAvatar,
  } = useUserProfile();

  // Load profile on mount
  useEffect(() => {
    if (userId) {
      loadProfile(userId === 'me' ? 'current' : userId);
    }
  }, [userId, loadProfile]);

  if (loading.isLoading && !profile) {
    return <ProfileSkeleton />;
  }

  if (loading.error && !profile) {
    return (
      <AlertBox
        type="error"
        title="Error Loading Profile"
        message={loading.error.error}
      />
    );
  }

  if (!profile) {
    return (
      <AlertBox
        type="error"
        title="Profile Not Found"
        message="Unable to load user profile. Please try again."
      />
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
      {/* Left Column: Avatar */}
      <div className="lg:col-span-1">
        <div className="sticky top-8 space-y-6">
          <ProfileHeader profile={profile} />
          <AvatarUploadZone
            userId={profile.id}
            currentAvatarUrl={profile.avatarUrl}
            isLoading={loading.isLoading}
            onUpload={uploadAvatar}
            onError={(error) => console.error('Upload error:', error)}
          />
        </div>
      </div>

      {/* Right Column: Profile Form */}
      <div className="lg:col-span-2">
        <ProfileForm
          profile={profile}
          isLoading={loading.isLoading}
          error={loading.error}
          onSubmit={(updates) => updateProfile(profile.id, updates)}
          onSuccess={(message) => {
            console.log('Profile updated:', message);
          }}
          onError={(error) => {
            console.error('Update error:', error);
          }}
        />
      </div>

      {/* Success Message */}
      {loading.success && (
        <AlertBox
          type="success"
          title="Success"
          message={loading.success}
        />
      )}
    </div>
  );
};

const ProfileSkeleton = () => (
  <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
    <div className="space-y-6 lg:col-span-1 lg:sticky lg:top-8">
      <div className="rounded-lg border border-lh-border bg-white p-6 shadow-clay-sm">
        <div className="flex flex-col items-center">
          <Skeleton variant="circular" width={128} height={128} />
          <Skeleton variant="text" width="70%" height={38} className="mt-4" />
          <Skeleton variant="text" width="46%" height={24} />
          <Skeleton variant="text" width="84%" height={24} className="mt-3" />
        </div>
      </div>
      <div className="rounded-lg border border-lh-border bg-white p-6 shadow-clay-sm">
        <Skeleton variant="rounded" height={170} />
      </div>
    </div>
    <div className="lg:col-span-2 rounded-lg border border-lh-border bg-white p-6 shadow-clay-sm">
      <Skeleton variant="text" width="32%" height={40} />
      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <Skeleton variant="rounded" height={54} />
        <Skeleton variant="rounded" height={54} />
        <Skeleton variant="rounded" height={54} className="sm:col-span-2" />
        <Skeleton variant="rounded" height={120} className="sm:col-span-2" />
      </div>
      <Skeleton variant="rounded" height={50} className="mt-6" />
    </div>
  </div>
);
