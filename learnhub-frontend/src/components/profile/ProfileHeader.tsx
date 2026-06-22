/**
 * Profile Header Component
 *
 * Displays user avatar, name, and basic profile information.
 * Located in left column of profile page (sticky).
 *
 * Features:
 * - Large avatar display (200x200px)
 * - User full name
 * - Username handle
 * - Bio/description
 * - Created/updated timestamps
 */

import React from 'react';
import { UserProfile } from '../../types';
import { formatDate } from '../../utils/dateFormatter';

interface ProfileHeaderProps {
  profile: UserProfile;
}

export const ProfileHeader: React.FC<ProfileHeaderProps> = ({ profile }) => {
  return (
    <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
      {/* Avatar Container */}
      <div className="flex flex-col items-center mb-6">
        <div className="relative">
          <img
            src={profile.avatarUrl || 'https://via.placeholder.com/200'}
            alt={profile.firstName}
            className="w-32 h-32 rounded-full object-cover border-4 border-primary-500"
          />
          <div className="absolute bottom-0 right-0 w-8 h-8 bg-success-500 rounded-full border-4 border-white"></div>
        </div>
      </div>

      {/* User Information */}
      <div className="text-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">
          {profile.firstName} {profile.lastName}
        </h1>
        <p className="text-gray-600 text-sm mb-4">@{profile.username}</p>

        {/* Bio */}
        {profile.bio && (
          <p className="text-gray-700 text-sm leading-relaxed mb-6 italic">
            "{profile.bio}"
          </p>
        )}

        {/* Email */}
        <div className="flex items-center justify-center gap-2 mb-4">
          <span className="text-sm text-gray-600">📧</span>
          <a
            href={`mailto:${profile.email}`}
            className="text-sm text-primary-600 hover:text-primary-700 truncate"
          >
            {profile.email}
          </a>
        </div>
      </div>

      {/* Metadata */}
      <div className="border-t border-gray-200 pt-4">
        <div className="space-y-2 text-xs text-gray-600">
          <div className="flex justify-between">
            <span>Member since</span>
            <span>{formatDate(profile.createdAt)}</span>
          </div>
          <div className="flex justify-between">
            <span>Last updated</span>
            <span>{formatDate(profile.updatedAt)}</span>
          </div>
        </div>
      </div>

      {/* Status Badge */}
      <div className="mt-4 flex justify-center">
        <span className="inline-flex items-center gap-2 px-3 py-1 bg-success-50 text-success-600 rounded-full text-xs font-medium">
          <span className="inline-block w-2 h-2 bg-success-500 rounded-full"></span>
          Active
        </span>
      </div>
    </div>
  );
};
