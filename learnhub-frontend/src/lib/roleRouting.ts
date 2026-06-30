import type { UserRole } from '../types/auth';

export function normalizeRole(role?: string | null): UserRole {
  const value = role?.toLowerCase();
  if (value === 'admin') return 'admin';
  if (value === 'instructor') return 'instructor';
  if (value === 'student' || value === 'learner') return 'student';
  return 'student';
}

export function getRoleHomePath(role?: UserRole | string | null): string {
  const normalized = normalizeRole(role);
  if (normalized === 'instructor' || normalized === 'admin') {
    return '/instructor/dashboard';
  }
  return '/dashboard';
}

export function canAccessInstructorStudio(role?: UserRole | string | null): boolean {
  const normalized = normalizeRole(role);
  return normalized === 'instructor' || normalized === 'admin';
}
