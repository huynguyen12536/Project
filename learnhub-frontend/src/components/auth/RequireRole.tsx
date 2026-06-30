import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import type { UserRole } from '../../types/auth';
import { getRoleHomePath } from '../../lib/roleRouting';

interface RequireRoleProps {
  /** Roles được phép truy cập route này */
  roles: UserRole[];
  children: React.ReactNode;
  /**
   * Nếu user đã đăng nhập nhưng không đủ quyền → redirect về đây.
   * Mặc định là '/dashboard'.
   */
  redirectTo?: string;
}

/**
 * Bảo vệ route theo role.
 * - Chưa đăng nhập → /login
 * - Đã đăng nhập nhưng sai role → redirectTo (mặc định /dashboard)
 */
const RequireRole: React.FC<RequireRoleProps> = ({
  roles,
  children,
  redirectTo,
}) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const role = useAuthStore((s) => s.role);
  const location = useLocation();

  if (!isAuthenticated) {
    const redirect = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />;
  }

  if (!role || !roles.includes(role)) {
    return <Navigate to={redirectTo ?? getRoleHomePath(role)} replace />;
  }

  return <>{children}</>;
};

export default RequireRole;
