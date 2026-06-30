/**
 * TypeScript Type Definitions
 *
 * Matches backend API contract: docs/user-profile-api-contract.md
 */

/**
 * User Profile interface (from backend API)
 */
export interface UserProfile {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  bio: string;
  avatarUrl: string | null;
  role: string;
  phone?: string;
  location?: string;
  createdAt: string; // ISO 8601 timestamp
  updatedAt: string; // ISO 8601 timestamp
}

/**
 * User Profile Update Request
 * Sent to backend PUT /api/v1/users/{userId}
 */
export interface UserProfileUpdatePayload {
  firstName: string;
  lastName: string;
  email: string;
  bio?: string;
  phone?: string;
  location?: string;
}

/**
 * Avatar Upload Response
 * Returned from backend POST /api/v1/users/{userId}/avatar
 */
export interface AvatarUploadResponse {
  message: string;
  avatarUrl: string;
  fileSize: number;
  uploadedAt: string; // ISO 8601 timestamp
}

/**
 * Error Response from backend
 */
export interface ErrorResponse {
  error: string;
  code: string;
  field?: string;
}

/**
 * Paginated response (for user listing)
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

/**
 * UI loading state
 */
export interface LoadingState {
  isLoading: boolean;
  error: ErrorResponse | null;
  success: string | null;
}

/**
 * Assessment Progress Event (from SSE/Polling)
 * Locked API contract for progress tracking
 */
export interface AssessmentProgressEvent {
  assessmentId: string;
  status: AssessmentStatus;
  progressPercent: number; // 0-100
  currentStep: string; // e.g., "analyzing", "extracting_skills", "generating_report"
  queuePosition: number; // Position in queue (0 if processing)
  confidence: number; // 0-1 confidence score for current analysis
  timestamp: string; // ISO 8601 timestamp
}

/**
 * Assessment Status enum
 */
export enum AssessmentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

/**
 * Skill Score result from assessment (Future: Story 2.4)
 * Currently not returned by backend, but defined for frontend readiness
 */
export interface SkillScore {
  skillName: string;
  score: number; // 0-100
  category: string; // e.g., "Backend", "Frontend", "DevOps"
}

/**
 * Assessment Result Details from backend
 * Returned when assessment is COMPLETED
 * Matches AssessmentDetailsResponse.AssessmentResultDetails from backend
 */
export interface AssessmentResultDetails {
  resultId: string; // UUID
  overallLevel: string; // NOT_DEMONSTRATED, EMERGING, PROFICIENT, ADVANCED
  allGaps: string[]; // Identified gaps in skills
  nextSteps: string[]; // Recommendations for improvement
  overallConfidence: number; // 0.0 - 1.0
  detections: Record<string, any>; // Detailed detector results
}

/**
 * Assessment Result returned after processing
 * Matches AssessmentDetailsResponse from backend
 */
export interface AssessmentResult {
  id: string; // UUID
  userId: string; // UUID
  snapshotId: string; // UUID
  status: AssessmentStatus;
  createdAt: string; // ISO 8601 timestamp
  startedAt: string | null; // ISO 8601 timestamp
  completedAt: string | null; // ISO 8601 timestamp
  errorMessage: string | null;
  result: AssessmentResultDetails | null; // Only set if status === COMPLETED
}

/**
 * Assessment submission payload
 * Note: Requires a snapshotId from a previously created repository snapshot
 */
export interface AssessmentSubmissionPayload {
  snapshotId: string; // UUID of the snapshot to assess
}
