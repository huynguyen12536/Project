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
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { AlertBox } from '../components/common/AlertBox';

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
    return <LoadingSpinner message="Loading profile..." />;
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
