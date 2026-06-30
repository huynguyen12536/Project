/**
 * Profile Header Component
 *
 * Displays user avatar, name, role and basic profile information.
 * Located in left column of profile page (sticky).
 */

import React from 'react';
import { UserProfile } from '../../types';
import { formatDate } from '../../utils/dateFormatter';
import { Avatar } from '../../ui-kit/Avatar';
import {
  Calendar,
  Mail,
  ShieldCheck,
  User,
  GraduationCap,
  Cog
} from 'lucide-react';

interface ProfileHeaderProps {
  profile: UserProfile;
}

// Function to normalize role for display
const normalizeRoleForDisplay = (role?: string) => {
  const normalized = role?.toLowerCase();
  if (normalized === 'instructor') return { label: 'Giảng viên', icon: GraduationCap, tone: 'bg-[#EEF2FF] text-lh-blue' };
  if (normalized === 'admin') return { label: 'Quản trị viên', icon: ShieldCheck, tone: 'bg-[#FFF5E8] text-[#B76E14]' };
  if (normalized === 'learner' || normalized === 'student') return { label: 'Học viên', icon: User, tone: 'bg-[#F4F2F8] text-lh-purple' };
  return { label: role || 'Học viên', icon: User, tone: 'bg-[#F4F2F8] text-lh-purple' };
};

export const ProfileHeader: React.FC<ProfileHeaderProps> = ({ profile }) => {
  const roleInfo = normalizeRoleForDisplay(profile.role);
  const RoleIcon = roleInfo.icon;
  const fullName = `${profile.firstName} ${profile.lastName}`.trim();
  const displayName = fullName || profile.email || 'User';

  return (
    <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
      {/* Avatar Container */}
      <div className="flex flex-col items-center mb-6">
        <Avatar
          src={profile.avatarUrl}
          name={displayName}
          size="xxl"
          className="border-4 border-white shadow-[0_8px_24px_rgba(21,22,46,0.08)]"
        />
        {/* Role Badge */}
        <div className="mt-4 flex items-center gap-2">
          <div className={roleInfo.tone + ' flex items-center gap-2 rounded-full px-4 py-2'}>
            <RoleIcon className="h-4 w-4" />
            <span className="text-xs font-bold uppercase tracking-[0.1em]">
              {roleInfo.label}
            </span>
          </div>
        </div>
      </div>

      {/* User Information */}
      <div className="text-center mb-6">
        <h1 className="text-2xl font-bold tracking-[-0.03em] text-[#111827] mb-1">
          {displayName}
        </h1>
        <p className="text-sm text-[#6B7280] mb-4">@{profile.username}</p>

        {/* Bio */}
        {profile.bio && (
          <p className="text-sm leading-relaxed text-[#6B7280] mb-6">
            {profile.bio}
          </p>
        )}

        {/* Email */}
        <div className="flex items-center justify-center gap-2 mb-3">
          <Mail className="h-4 w-4 text-[#9CA3AF]" />
          <span className="text-sm text-[#6B7280] break-all">
            {profile.email}
          </span>
        </div>
      </div>

      {/* Metadata */}
      <div className="border-t border-[#E7E9F2] pt-4 space-y-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-sm text-[#6B7280]">
            <Calendar className="h-4 w-4 text-[#9CA3AF]" />
            <span>Tham gia từ</span>
          </div>
          <span className="text-sm font-medium text-[#111827]">
            {formatDate(profile.createdAt)}
          </span>
        </div>
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-sm text-[#6B7280]">
            <Cog className="h-4 w-4 text-[#9CA3AF]" />
            <span>Cập nhật lần cuối</span>
          </div>
          <span className="text-sm font-medium text-[#111827]">
            {formatDate(profile.updatedAt)}
          </span>
        </div>
      </div>
    </div>
  );
};
