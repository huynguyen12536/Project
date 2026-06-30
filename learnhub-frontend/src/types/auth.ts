/**
 * Auth & Domain TypeScript Type Definitions (Phase 1A)
 *
 * These complement the existing user/assessment types in ./index.ts
 */

export type UserRole = 'student' | 'instructor' | 'admin';

/**
 * Authenticated user (subset used across auth flows).
 * Note: the richer profile shape lives in ./index.ts as `UserProfile`.
 */
export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  avatarUrl?: string;
  bio?: string;
}

export interface AuthResponse {
  userId: string;
  token: string;
  refreshToken: string;
  expiresIn: number;
  user?: AuthUser;
}

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface SignupPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface VerifyEmailResponse {
  verified: boolean;
  message: string;
}

/* ----------------------------- Courses ----------------------------- */

export interface Course {
  id: string;
  title: string;
  description: string;
  instructorName: string;
  thumbnailUrl?: string;
  enrolled?: boolean;
  progress?: number; // 0-100
  lessonCount?: number;
  tags?: string[];
}

export interface Lesson {
  id: string;
  courseId: string;
  title: string;
  order: number;
  durationSeconds?: number;
  completed?: boolean;
}

/* ----------------------------- Admin ------------------------------- */

export interface AdminUserRow {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  status: 'active' | 'suspended' | 'pending';
  createdAt: string;
}

export interface SystemHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  uptimeSeconds: number;
  components: Record<string, { status: string; details?: Record<string, unknown> }>;
}

export interface AuditLogEntry {
  id: string;
  actor: string;
  action: string;
  target?: string;
  timestamp: string;
}

/* --------------------------- Analytics ----------------------------- */

export interface StudentKPI {
  coursesEnrolled: number;
  lessonsCompleted: number;
  avgScore: number;
  streakDays: number;
}

export interface InstructorKPI {
  totalStudents: number;
  activeCourses: number;
  avgCompletionRate: number;
  avgRating: number;
}

export interface HeatmapCell {
  lessonId: string;
  lessonTitle: string;
  engagement: number; // 0-1
}

/* ----------------------------- Notes ------------------------------- */

export interface StudentNote {
  id: string;
  lessonId?: string;
  courseId?: string;
  title: string;
  content: string; // rich-text HTML
  createdAt: string;
  updatedAt: string;
}

/* ----------------------------- Toasts ------------------------------ */

export type ToastVariant = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: string;
  message: string;
  variant: ToastVariant;
  duration?: number;
}
