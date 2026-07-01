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

/**
 * Course Status enum
 */
export enum CourseStatus {
  DRAFT = 'DRAFT',
  PENDING_REVIEW = 'PENDING_REVIEW',
  PUBLISHED = 'PUBLISHED',
  REJECTED = 'REJECTED'
}

/**
 * Lecture Type enum
 */
export enum LectureType {
  VIDEO = 'VIDEO',
  ARTICLE = 'ARTICLE',
  QUIZ = 'QUIZ'
}

export enum UploadAssetType {
  COURSE_VIDEO = 'COURSE_VIDEO',
}

/**
 * Course Category type
 */
export interface CourseCategory {
  id: string;
  name: string;
  slug: string;
  description?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Subcategory type
 */
export interface CourseSubcategory {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  slug: string;
  description?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Level type
 */
export interface CourseLevel {
  id: string;
  code: string;
  label: string;
  description?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Language type
 */
export interface CourseLanguage {
  id: string;
  code: string;
  label: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Tag type
 */
export interface CourseTag {
  id: string;
  name: string;
  slug: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Lecture type
 */
export interface CourseLecture {
  id: string;
  sectionId: string;
  courseId: string;
  title: string;
  type: LectureType;
  content?: string;
  videoUrl?: string;
  durationSeconds?: number;
  displayOrder: number;
  isFreePreview: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Section type
 */
export interface CourseSection {
  id: string;
  courseId: string;
  title: string;
  displayOrder: number;
  lectures: CourseLecture[];
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Pricing type
 */
export interface CoursePricing {
  id: string;
  courseId: string;
  priceVnd: number;
  currency: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course Coupon type
 */
export interface CourseCoupon {
  id: string;
  courseId: string;
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  maxUses?: number;
  usedCount: number;
  validFrom?: string;
  validUntil?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Course type
 */
export interface Course {
  id: string;
  instructorId: string;
  title: string;
  subtitle?: string;
  description?: string;
  thumbnailUrl?: string;
  promoVideoUrl?: string;
  categoryId?: string;
  subcategoryId?: string;
  levelId?: string;
  languageId?: string;
  status: CourseStatus;
  totalVideoDurationSeconds: number;
  lectureCount: number;
  studentCount: number;
  averageRating?: number;
  rejectionReason?: string;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  sections?: CourseSection[];
  pricing?: CoursePricing;
  coupons?: CourseCoupon[];
}

/**
 * Course Taxonomy Bundle
 */
export interface CourseTaxonomyBundle {
  categories: CourseCategory[];
  subcategories: CourseSubcategory[];
  levels: CourseLevel[];
  languages: CourseLanguage[];
  tags: CourseTag[];
}

/**
 * Create Course Request
 */
export interface CreateCourseRequest {
  title: string;
  subtitle?: string;
  description?: string;
  thumbnailUrl?: string;
  promoVideoUrl?: string;
  categoryId?: string;
  subcategoryId?: string;
  levelId?: string;
  languageId?: string;
}

/**
 * Update Course Request
 */
export interface UpdateCourseRequest extends Partial<CreateCourseRequest> {
}

/**
 * Create Section Request
 */
export interface CreateSectionRequest {
  title: string;
  displayOrder: number;
}

/**
 * Update Section Request
 */
export interface UpdateSectionRequest extends Partial<CreateSectionRequest> {
}

/**
 * Create Lecture Request
 */
export interface CreateLectureRequest {
  title: string;
  type: LectureType;
  content?: string;
  videoUrl?: string;
  durationSeconds?: number;
  displayOrder: number;
  isFreePreview?: boolean;
}

/**
 * Update Lecture Request
 */
export interface UpdateLectureRequest extends Partial<CreateLectureRequest> {
}

export interface StartMultipartUploadRequest {
  fileName: string;
  contentType: string;
  size: number;
  assetType: UploadAssetType;
  courseId: string;
  lectureId?: string;
}

export interface UploadedPartSummary {
  partNumber: number;
  eTag: string;
  size: number;
}

export interface MultipartUploadSession {
  sessionId: string;
  uploadId: string;
  objectKey: string;
  bucket: string;
  assetType: string;
  fileName: string;
  contentType: string;
  size: number;
  chunkSizeBytes: number;
  maxConcurrency: number;
  maxRetries: number;
  expiresAt: string;
  uploadedParts: UploadedPartSummary[];
}

export interface PresignedPart {
  partNumber: number;
  url: string;
}

export interface MultipartUploadPresignResponse {
  uploadId: string;
  objectKey: string;
  parts: PresignedPart[];
}

export interface CompleteMultipartUploadRequestPayload {
  uploadId: string;
  objectKey: string;
  parts: Array<{
    partNumber: number;
    eTag: string;
  }>;
  durationSeconds?: number;
}

export interface CompleteMultipartUploadResponse {
  assetId: string;
  sessionId: string;
  uploadId: string;
  objectKey: string;
  bucket: string;
  publicUrl: string;
  contentType: string;
  size: number;
  durationSeconds?: number;
  completedAt: string;
}

export interface MultipartUploadDraft {
  contextKey: string;
  fileFingerprint: string;
  fileName: string;
  size: number;
  contentType: string;
  assetType: UploadAssetType;
  courseId: string;
  lectureId?: string;
  uploadId: string;
  objectKey: string;
  chunkSizeBytes: number;
  createdAt: string;
  updatedAt: string;
}

export type MultipartUploadPhase =
  | 'idle'
  | 'starting'
  | 'uploading'
  | 'completing'
  | 'completed'
  | 'failed'
  | 'cancelled';

export interface MultipartUploadProgressSnapshot {
  phase: MultipartUploadPhase;
  fileName: string;
  totalBytes: number;
  uploadedBytes: number;
  overallProgress: number;
  uploadedParts: number[];
  partProgress: Record<number, number>;
  uploadId?: string;
  objectKey?: string;
  message?: string;
}

/**
 * Create Coupon Request
 */
export interface CreateCouponRequest {
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  maxUses?: number;
  validFrom?: string;
  validUntil?: string;
}

/**
 * Update Coupon Request
 */
export interface UpdateCouponRequest extends Partial<CreateCouponRequest> {
  isActive?: boolean;
}

/**
 * Create Pricing Request
 */
export interface CreatePricingRequest {
  priceVnd: number;
}

/**
 * Update Pricing Request
 */
export interface UpdatePricingRequest extends Partial<CreatePricingRequest> {
  isActive?: boolean;
}

/**
 * Submit for Review Request
 */
export interface SubmitCourseReviewRequest {
}

/**
 * Approve Course Request
 */
export interface ApproveCourseRequest {
}

/**
 * Reject Course Request
 */
export interface RejectCourseRequest {
  rejectionReason: string;
}
