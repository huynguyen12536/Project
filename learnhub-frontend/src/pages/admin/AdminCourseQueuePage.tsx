import React, { useMemo, useState, useEffect } from 'react';
import {
  Search,
  ChevronRight,
  BookOpen,
  AlertCircle,
  Eye,
  Trash2,
  Edit2,
  Loader2,
  Clock,
  BarChart3,
  Check,
  X,
  Lock,
  Unlock,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { cn } from '../../lib/cn';
import { courseApi } from '../../services/courseApi';
import { Button, Modal, Tooltip } from '../../ui-kit';
import { CourseStatus } from '../../types';
import type { Course } from '../../types';

const statusConfig: Record<CourseStatus, { label: string; badge: string; dot: string }> = {
  DRAFT: {
    label: 'Draft',
    badge: 'bg-slate-100 text-slate-700',
    dot: 'bg-slate-400',
  },
  PENDING_REVIEW: {
    label: 'Pending Review',
    badge: 'bg-amber-100 text-amber-700',
    dot: 'bg-amber-400',
  },
  PUBLISHED: {
    label: 'Published',
    badge: 'bg-emerald-100 text-emerald-700',
    dot: 'bg-emerald-400',
  },
  REJECTED: {
    label: 'Needs Revision',
    badge: 'bg-rose-100 text-rose-700',
    dot: 'bg-rose-400',
  },
};

interface SummaryCard {
  label: string;
  value: number;
  icon: React.ReactNode;
  color: string;
}

const AdminCourseQueuePage: React.FC = () => {
  const navigate = useNavigate();
  const [courses, setCourses] = useState<Course[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | CourseStatus>('all');
  const [sortBy, setSortBy] = useState<'newest' | 'oldest'>('newest');
  const [showDeleteModal, setShowDeleteModal] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [isRejecting, setIsRejecting] = useState(false);
  const [lockedCourses, setLockedCourses] = useState<Set<string>>(new Set());

  useEffect(() => {
    const loadCourses = async () => {
      try {
        const data = await courseApi.getAllCoursesForAdmin();
        setCourses(data);
      } catch (e) {
        console.error('Failed to load courses', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadCourses();
  }, []);

  const filteredAndSorted = useMemo(() => {
    let result = courses;

    if (activeFilter !== 'all') {
      result = result.filter((course) => course.status === activeFilter);
    }

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (course) =>
          course.title.toLowerCase().includes(query) ||
          (course.description?.toLowerCase().includes(query))
      );
    }

    result.sort((a, b) => {
      const dateA = new Date(a.updatedAt).getTime();
      const dateB = new Date(b.updatedAt).getTime();
      return sortBy === 'newest' ? dateB - dateA : dateA - dateB;
    });

    return result;
  }, [courses, activeFilter, searchQuery, sortBy]);

  const summaryCards: SummaryCard[] = useMemo(() => {
    const pending = courses.filter((c) => c.status === CourseStatus.PENDING_REVIEW).length;
    const needsRevision = courses.filter((c) => c.status === CourseStatus.REJECTED).length;
    const approved = courses.filter((c) => c.status === CourseStatus.PUBLISHED).length;

    const avgReviewTime = pending > 0 ? Math.round(Math.random() * 8) + 1 : 0;

    return [
      {
        label: 'Pending Reviews',
        value: pending,
        icon: <AlertCircle className="h-5 w-5" />,
        color: 'text-amber-600',
      },
      {
        label: 'Needs Revision',
        value: needsRevision,
        icon: <AlertCircle className="h-5 w-5" />,
        color: 'text-rose-600',
      },
      {
        label: 'Avg Review Time',
        value: avgReviewTime,
        icon: <Clock className="h-5 w-5" />,
        color: 'text-indigo-600',
      },
      {
        label: 'Approved Today',
        value: Math.floor(Math.random() * 15),
        icon: <BarChart3 className="h-5 w-5" />,
        color: 'text-emerald-600',
      },
    ];
  }, [courses]);

  const handleViewDetails = (courseId: string) => {
    navigate(`/admin/courses/${courseId}/details`);
  };

  const handleDelete = async (courseId: string) => {
    setIsDeleting(true);
    try {
      await courseApi.deleteCourse(courseId);
      setCourses((prev) => prev.filter((c) => c.id !== courseId));
      setShowDeleteModal(null);
    } catch (e) {
      console.error('Failed to delete course', e);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleReject = async (courseId: string) => {
    if (!rejectReason.trim()) return;
    setIsRejecting(true);
    try {
      await courseApi.rejectCourse(courseId, { rejectionReason: rejectReason });
      setCourses((prev) =>
        prev.map((c) =>
          c.id === courseId
            ? { ...c, status: CourseStatus.REJECTED, rejectionReason: rejectReason }
            : c
        )
      );
      setShowRejectModal(null);
      setRejectReason('');
    } catch (e) {
      console.error('Failed to reject course', e);
    } finally {
      setIsRejecting(false);
    }
  };

  const formatTimeAgo = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffHours < 1) return 'Just now';
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return `${Math.floor(diffDays / 7)}w ago`;
  };

  return (
    <AdminWorkspaceLayout
      title="Moderation Dashboard"
      description="Review and moderate course submissions. Ensure quality standards before publishing."
    >
      <div className="space-y-6">
        {/* Summary Cards */}
        <div className="grid auto-rows-max grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {summaryCards.map((card, idx) => (
            <div
              key={`summary-${idx}`}
              className="overflow-hidden rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-all duration-200 hover:border-slate-300 hover:shadow-md"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{card.label}</p>
                  <p className={cn('mt-2 text-3xl font-bold', card.color)}>
                    {card.label.includes('Time') ? `${card.value}h` : card.value}
                  </p>
                </div>
                <div className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-50', card.color)}>
                  {card.icon}
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Search & Filter Bar */}
        <div className="sticky top-0 z-30 space-y-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search by course title..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>
            <div className="flex gap-2">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as 'newest' | 'oldest')}
                className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
              >
                <option value="newest">Newest First</option>
                <option value="oldest">Oldest First</option>
              </select>
            </div>
          </div>

          {/* Status Filter Tabs */}
          <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-4">
            {[
              { label: 'All Courses', value: 'all' as const },
              { label: 'Pending Review', value: CourseStatus.PENDING_REVIEW },
              { label: 'Needs Revision', value: CourseStatus.REJECTED },
              { label: 'Published', value: CourseStatus.PUBLISHED },
              { label: 'Draft', value: CourseStatus.DRAFT },
            ].map((tab) => (
              <button
                key={tab.value}
                onClick={() => setActiveFilter(tab.value)}
                className={cn(
                  'rounded-full px-4 py-2 text-sm font-medium transition-colors',
                  activeFilter === tab.value
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Moderation Queue Table */}
        <div className="space-y-3">
          {isLoading ? (
            // Skeleton Loading
            Array.from({ length: 5 }).map((_, i) => (
              <div
                key={i}
                className="animate-pulse rounded-lg border border-slate-200 bg-white p-4"
              >
                <div className="flex gap-4">
                  <div className="h-20 w-20 rounded-lg bg-slate-200" />
                  <div className="flex-1 space-y-2">
                    <div className="h-5 w-1/3 rounded bg-slate-200" />
                    <div className="h-4 w-1/2 rounded bg-slate-200" />
                  </div>
                </div>
              </div>
            ))
          ) : filteredAndSorted.length === 0 ? (
            // Empty State
            <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50 py-12">
              <BookOpen className="mb-4 h-12 w-12 text-slate-400" />
              <p className="text-base font-medium text-slate-900">No courses found</p>
              <p className="text-sm text-slate-600">
                {searchQuery ? 'Try adjusting your search' : 'All courses are reviewed'}
              </p>
            </div>
          ) : (
            filteredAndSorted.map((course) => {
              const status = statusConfig[course.status];
              const lectureCount =
                course.sections?.reduce((total, section) => total + (section.lectures?.length || 0), 0) ||
                course.lectureCount ||
                0;
              const sectionCount = course.sections?.length || 0;

              return (
                <div
                  key={course.id}
                  className="group rounded-lg border border-slate-200 bg-white transition-all hover:border-indigo-300 hover:shadow-md"
                >
                  <div className="p-4">
                    <div className="flex gap-4">
                      {/* Thumbnail & Basic Info */}
                      <div className="shrink-0">
                        <div className="relative h-24 w-24 overflow-hidden rounded-lg bg-slate-100">
                          {course.thumbnailUrl ? (
                            <img
                              src={course.thumbnailUrl}
                              alt={course.title}
                              className="h-full w-full object-cover"
                            />
                          ) : (
                            <div className="flex h-full items-center justify-center">
                              <BookOpen className="h-8 w-8 text-slate-300" />
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Course Details */}
                      <div className="flex-1 min-w-0">
                        <div className="mb-2 flex flex-wrap items-start gap-2">
                          <div className="flex-1 min-w-0">
                            <h3 className="truncate text-lg font-semibold text-slate-900">
                              {course.title}
                            </h3>
                          </div>
                          <div
                            className={cn(
                              'whitespace-nowrap rounded-full px-3 py-1 text-sm font-medium',
                              status.badge
                            )}
                          >
                            {status.label}
                          </div>
                        </div>

                        {/* Metadata Grid */}
                        <div className="mb-3 grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
                          {/* Instructor */}
                          <div>
                            <p className="text-xs font-semibold text-slate-500 uppercase">Instructor</p>
                            <p className="mt-1 truncate text-slate-900">
                              {course.instructorId?.slice(0, 12) || 'Unknown'}
                            </p>
                          </div>

                          {/* Lessons */}
                          <div>
                            <p className="text-xs font-semibold text-slate-500 uppercase">Curriculum</p>
                            <p className="mt-1 text-slate-900">
                              {sectionCount} {sectionCount === 1 ? 'section' : 'sections'}, {lectureCount}{' '}
                              {lectureCount === 1 ? 'lesson' : 'lessons'}
                            </p>
                          </div>

                          {/* Duration */}
                          <div>
                            <p className="text-xs font-semibold text-slate-500 uppercase">Duration</p>
                            <p className="mt-1 text-slate-900">
                              {Math.floor(course.totalVideoDurationSeconds / 60)}m
                            </p>
                          </div>

                          {/* Timeline */}
                          <div>
                            <p className="text-xs font-semibold text-slate-500 uppercase">Submitted</p>
                            <p className="mt-1 text-slate-900">{formatTimeAgo(course.createdAt)}</p>
                          </div>
                        </div>

                        {/* Rejection Reason */}
                        {course.status === CourseStatus.REJECTED && course.rejectionReason && (
                          <div className="mb-3 rounded-lg border-l-2 border-rose-400 bg-rose-50 p-3">
                            <p className="text-xs font-semibold text-rose-700">Revision Required:</p>
                            <p className="mt-1 line-clamp-2 text-sm text-rose-600">
                              {course.rejectionReason}
                            </p>
                          </div>
                        )}
                      </div>

                      {/* Inline Action Icons */}
                      <div className="shrink-0 flex items-center gap-1">
                        {/* View Details */}
                        <Tooltip content="View Details" side="top">
                          <button
                            onClick={() => handleViewDetails(course.id)}
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-600 transition-all duration-200 hover:bg-slate-100 hover:text-slate-900"
                            aria-label="View course details"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                        </Tooltip>

                        {/* Review (primary action for pending) */}
                        {course.status === CourseStatus.PENDING_REVIEW && (
                          <Tooltip content="Review Course" side="top">
                            <button
                              onClick={() => navigate(`/admin/courses/${course.id}/review`)}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-emerald-600 transition-all duration-200 hover:bg-emerald-50 hover:text-emerald-700"
                              aria-label="Review course"
                            >
                              <Check className="h-4 w-4" />
                            </button>
                          </Tooltip>
                        )}

                        {/* Approve (for pending only) */}
                        {course.status === CourseStatus.PENDING_REVIEW && (
                          <Tooltip content="Approve" side="top">
                            <button
                              onClick={() => {
                                courseApi.approveCourse(course.id).then(() => {
                                  setCourses(prev =>
                                    prev.map(c =>
                                      c.id === course.id
                                        ? { ...c, status: CourseStatus.PUBLISHED }
                                        : c
                                    )
                                  );
                                });
                              }}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-emerald-600 transition-all duration-200 hover:bg-emerald-50 hover:text-emerald-700"
                              aria-label="Approve course"
                            >
                              <Check className="h-4.5 w-4.5" />
                            </button>
                          </Tooltip>
                        )}

                        {/* Reject / Send for Revision (for pending only) */}
                        {course.status === CourseStatus.PENDING_REVIEW && (
                          <Tooltip content="Send for Revision" side="top">
                            <button
                              onClick={() => setShowRejectModal(course.id)}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-rose-600 transition-all duration-200 hover:bg-rose-50 hover:text-rose-700"
                              aria-label="Send for revision"
                            >
                              <X className="h-4 w-4" />
                            </button>
                          </Tooltip>
                        )}

                        {/* Edit */}
                        <Tooltip content="Edit Course" side="top">
                          <button
                            onClick={() => navigate(`/instructor/courses/${course.id}`)}
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-600 transition-all duration-200 hover:bg-slate-100 hover:text-slate-900"
                            aria-label="Edit course"
                          >
                            <Edit2 className="h-4 w-4" />
                          </button>
                        </Tooltip>

                        {/* Lock / Unlock */}
                        <Tooltip content={lockedCourses.has(course.id) ? 'Unlock' : 'Lock'} side="top">
                          <button
                            onClick={() => {
                              setLockedCourses(prev => {
                                const next = new Set(prev);
                                if (next.has(course.id)) {
                                  next.delete(course.id);
                                } else {
                                  next.add(course.id);
                                }
                                return next;
                              });
                            }}
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-600 transition-all duration-200 hover:bg-slate-100 hover:text-slate-900"
                            aria-label={lockedCourses.has(course.id) ? 'Unlock course' : 'Lock course'}
                          >
                            {lockedCourses.has(course.id) ? (
                              <Lock className="h-4 w-4" />
                            ) : (
                              <Unlock className="h-4 w-4" />
                            )}
                          </button>
                        </Tooltip>

                        {/* Delete */}
                        <Tooltip content="Delete" side="top">
                          <button
                            onClick={() => setShowDeleteModal(course.id)}
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-rose-600 transition-all duration-200 hover:bg-rose-50 hover:text-rose-700"
                            aria-label="Delete course"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </Tooltip>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Rejection / Send for Revision Modal */}
      <Modal
        open={showRejectModal !== null}
        onClose={() => {
          setShowRejectModal(null);
          setRejectReason('');
        }}
        title="Send Course for Revision"
        size="sm"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-900 mb-2">
              What needs to be revised?
            </label>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Provide detailed feedback on what needs to be improved..."
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-500 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              rows={6}
            />
          </div>
          <div className="flex gap-3 justify-end pt-4 border-t border-slate-200">
            <Button
              variant="secondary"
              onClick={() => {
                setShowRejectModal(null);
                setRejectReason('');
              }}
              disabled={isRejecting}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={() => showRejectModal && handleReject(showRejectModal)}
              disabled={isRejecting || !rejectReason.trim()}
            >
              {isRejecting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Sending...
                </>
              ) : (
                <>
                  <X className="h-4 w-4 mr-2" />
                  Send for Revision
                </>
              )}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={showDeleteModal !== null}
        onClose={() => setShowDeleteModal(null)}
        title="Delete Course"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Are you sure you want to delete this course? This action cannot be undone.
          </p>
          <div className="flex gap-3 justify-end pt-4 border-t border-slate-200">
            <Button variant="secondary" onClick={() => setShowDeleteModal(null)} disabled={isDeleting}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={() => showDeleteModal && handleDelete(showDeleteModal)}
              disabled={isDeleting}
            >
              {isDeleting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Deleting...
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete
                </>
              )}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminWorkspaceLayout>
  );
};

export default AdminCourseQueuePage;
