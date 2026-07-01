/**
 * Profile Page
 *
 * Main page for user profile management.
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
    return (
      <div className="mx-auto max-w-[1200px] px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-1">
            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6">
              <div className="flex flex-col items-center">
                <Skeleton variant="circular" width={128} height={128} />
                <Skeleton variant="text" width="70%" height={38} className="mt-4" />
                <Skeleton variant="text" width="50%" height={24} />
                <Skeleton variant="text" width="90%" height={24} className="mt-3" />
              </div>
            </div>
            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6">
              <Skeleton variant="rounded" height={170} />
            </div>
          </div>
          <div className="lg:col-span-2">
            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-8">
              <Skeleton variant="text" width="32%" height={40} />
              <div className="mt-6 grid gap-6 sm:grid-cols-2">
                <Skeleton variant="rounded" height={70} />
                <Skeleton variant="rounded" height={70} />
                <Skeleton variant="rounded" height={70} className="sm:col-span-2" />
                <Skeleton variant="rounded" height={140} className="sm:col-span-2" />
              </div>
              <Skeleton variant="rounded" height={56} className="mt-8" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (loading.error && !profile) {
    return (
      <div className="mx-auto max-w-[1200px] px-4 py-10 sm:px-6 lg:px-8">
        <AlertBox
          type="error"
          title="Lỗi tải hồ sơ"
          message={loading.error.error}
        />
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="mx-auto max-w-[1200px] px-4 py-10 sm:px-6 lg:px-8">
        <AlertBox
          type="error"
          title="Không tìm thấy hồ sơ"
          message="Không thể tải hồ sơ người dùng. Vui lòng thử lại."
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-10 sm:px-6 lg:px-8">
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
        {/* Left Column */}
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

        {/* Right Column */}
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
      </div>

      {/* Success Message */}
      {loading.success && (
        <div className="mt-6">
          <AlertBox
            type="success"
            title="Thành công"
            message={loading.success}
          />
        </div>
      )}
    </div>
  );
};

export default ProfilePage;
