import React, { useEffect, useState } from 'react';
import {
  ArrowLeft,
  Loader2,
  ChevronDown,
  ChevronUp,
  BookOpen,
  Play,
  AlertCircle,
  Clock,
  Check,
  User,
  BarChart3,
  Globe,
  Zap,
  FileText,
  HelpCircle,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { cn } from '../../lib/cn';
import { courseApi } from '../../services/courseApi';
import { Button, Modal } from '../../ui-kit';
import { CourseStatus } from '../../types';
import type { Course, CourseSection } from '../../types';

const statusConfig: Record<CourseStatus, { label: string; color: string; bgColor: string }> = {
  DRAFT: { label: 'Draft', color: '#B76E14', bgColor: 'bg-[#FFF5E8]' },
  PENDING_REVIEW: { label: 'Pending Review', color: '#2F2FA2', bgColor: 'bg-[#EEF2FF]' },
  PUBLISHED: { label: 'Published', color: '#1F7A45', bgColor: 'bg-[#EEF8F2]' },
  REJECTED: { label: 'Revision Needed', color: '#E11D48', bgColor: 'bg-[#FFEBE8]' },
};

const qualityChecklist = [
  { id: 'thumbnail', label: 'Thumbnail quality', category: 'Content' },
  { id: 'description', label: 'Course description complete', category: 'Content' },
  { id: 'objectives', label: 'Learning objectives defined', category: 'Content' },
  { id: 'audio', label: 'Audio quality acceptable', category: 'Technical' },
  { id: 'video', label: 'Video quality meets standards', category: 'Technical' },
  { id: 'curriculum', label: 'Curriculum completeness', category: 'Content' },
  { id: 'quiz', label: 'Quiz validity & relevance', category: 'Content' },
  { id: 'metadata', label: 'Metadata accuracy', category: 'Metadata' },
  { id: 'copyright', label: 'Copyright & licensing', category: 'Legal' },
];

const AdminCourseReviewPage: React.FC = () => {
  const navigate = useNavigate();
  const { courseId } = useParams<{ courseId: string }>();
  const [course, setCourse] = useState<Course | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set());
  const [checkedItems, setCheckedItems] = useState<Set<string>>(new Set());
  const [moderatorNotes, setModeratorNotes] = useState('');
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    if (!courseId) return;
    const loadCourse = async () => {
      setIsLoading(true);
      try {
        const data = await courseApi.getAdminCourse(courseId);
        setCourse(data);
        // Expand first section by default
        if (data.sections && data.sections.length > 0) {
          setExpandedSections(new Set([data.sections[0].id]));
        }
      } catch (e) {
        console.error('Failed to load course', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadCourse();
  }, [courseId]);

  const handleApprove = async () => {
    if (!courseId) return;
    setIsSubmitting(true);
    try {
      const updated = await courseApi.approveCourse(courseId);
      setCourse(updated);
      setTimeout(() => navigate('/admin/courses?status=PUBLISHED'), 800);
    } catch (e) {
      console.error('Failed to approve course', e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!courseId || !rejectReason.trim()) return;
    setIsSubmitting(true);
    try {
      const updated = await courseApi.rejectCourse(courseId, {
        rejectionReason: rejectReason,
      });
      setCourse(updated);
      setShowRejectModal(false);
      setRejectReason('');
      setTimeout(() => navigate('/admin/courses?status=REJECTED'), 800);
    } catch (e) {
      console.error('Failed to reject course', e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleSection = (sectionId: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(sectionId)) {
      newExpanded.delete(sectionId);
    } else {
      newExpanded.add(sectionId);
    }
    setExpandedSections(newExpanded);
  };

  const toggleChecked = (itemId: string) => {
    const newChecked = new Set(checkedItems);
    if (newChecked.has(itemId)) {
      newChecked.delete(itemId);
    } else {
      newChecked.add(itemId);
    }
    setCheckedItems(newChecked);
  };

  if (isLoading) {
    return (
      <AdminWorkspaceLayout title="Loading...">
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-indigo-600" />
        </div>
      </AdminWorkspaceLayout>
    );
  }

  if (!course) {
    return (
      <AdminWorkspaceLayout title="Course not found">
        <div className="text-center py-12">
          <p className="text-slate-600">Unable to load course details</p>
        </div>
      </AdminWorkspaceLayout>
    );
  }

  const status = statusConfig[course.status as keyof typeof statusConfig];
  const lectureCount =
    course.sections?.reduce((total, section) => total + (section.lectures?.length || 0), 0) || 0;
  const sectionCount = course.sections?.length || 0;
  const durationMinutes = Math.floor(course.totalVideoDurationSeconds / 60);

  return (
    <AdminWorkspaceLayout
      title="Course Review"
      actions={
        <button
          onClick={() => navigate('/admin/courses')}
          className="inline-flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Courses
        </button>
      }
    >
      <div className="space-y-6">
        {/* Course Header */}
        <div className="rounded-lg border border-slate-200 bg-white overflow-hidden">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6">
            {/* Thumbnail */}
            <div className="lg:col-span-1">
              <div className="relative h-48 w-full overflow-hidden rounded-lg bg-slate-100">
                {course.thumbnailUrl ? (
                  <img
                    src={course.thumbnailUrl}
                    alt={course.title}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full items-center justify-center">
                    <BookOpen className="h-12 w-12 text-slate-300" />
                  </div>
                )}
              </div>
              <div className="mt-4">
                <span
                  className={cn('inline-block rounded-full px-3 py-1 text-sm font-semibold', status.bgColor)}
                  style={{ color: status.color }}
                >
                  {status.label}
                </span>
              </div>
            </div>

            {/* Course Info */}
            <div className="lg:col-span-2 space-y-4">
              <div>
                <h1 className="text-3xl font-bold text-slate-900">{course.title}</h1>
                <p className="mt-2 text-slate-600">{course.description}</p>
              </div>

              {/* Quick Stats */}
              <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <div className="rounded-lg bg-slate-50 p-3">
                  <div className="text-xs font-semibold text-slate-500 uppercase">Sections</div>
                  <div className="mt-1 text-2xl font-bold text-slate-900">{sectionCount}</div>
                </div>
                <div className="rounded-lg bg-slate-50 p-3">
                  <div className="text-xs font-semibold text-slate-500 uppercase">Lessons</div>
                  <div className="mt-1 text-2xl font-bold text-slate-900">{lectureCount}</div>
                </div>
                <div className="rounded-lg bg-slate-50 p-3">
                  <div className="text-xs font-semibold text-slate-500 uppercase">Duration</div>
                  <div className="mt-1 text-2xl font-bold text-slate-900">{durationMinutes}m</div>
                </div>
                <div className="rounded-lg bg-slate-50 p-3">
                  <div className="text-xs font-semibold text-slate-500 uppercase">Instructor</div>
                  <div className="mt-1 text-sm font-bold text-slate-900">
                    {course.instructorName || course.instructorId || 'N/A'}
                  </div>
                </div>
              </div>

              {/* Metadata */}
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-slate-500">Created</p>
                  <p className="font-medium text-slate-900">
                    {new Date(course.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">Last Updated</p>
                  <p className="font-medium text-slate-900">
                    {new Date(course.updatedAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Curriculum Section - Main */}
          <div className="lg:col-span-2 space-y-6">
            {/* Full Course Description */}
            <div className="rounded-lg border border-slate-200 bg-white p-6">
              <h2 className="mb-4 text-lg font-bold text-slate-900">Course Description</h2>
              <p className="whitespace-pre-wrap text-sm leading-6 text-slate-600">{course.description}</p>
            </div>

            {/* Curriculum */}
            <div className="rounded-lg border border-slate-200 bg-white p-6">
              <h2 className="mb-4 text-lg font-bold text-slate-900">
                Curriculum ({sectionCount} sections, {lectureCount} lessons)
              </h2>

              {course.sections && course.sections.length > 0 ? (
                <div className="space-y-2">
                  {course.sections.map((section: CourseSection) => (
                    <div key={section.id} className="border border-slate-200 rounded-lg overflow-hidden">
                      <button
                        onClick={() => toggleSection(section.id)}
                        className="w-full flex items-center justify-between p-4 hover:bg-slate-50 transition-colors"
                      >
                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          <BookOpen className="h-5 w-5 text-indigo-600 shrink-0" />
                          <div className="text-left min-w-0">
                            <p className="font-semibold text-slate-900 truncate">{section.title}</p>
                            <p className="text-xs text-slate-500 mt-1">
                              {section.lectures?.length || 0} lesson{section.lectures?.length !== 1 ? 's' : ''}
                            </p>
                          </div>
                        </div>
                        {expandedSections.has(section.id) ? (
                          <ChevronUp className="h-5 w-5 text-slate-400 shrink-0" />
                        ) : (
                          <ChevronDown className="h-5 w-5 text-slate-400 shrink-0" />
                        )}
                      </button>

                      {/* Lessons */}
                      {expandedSections.has(section.id) && (
                        <div className="bg-slate-50 border-t border-slate-200 p-4 space-y-3">
                          {section.lectures && section.lectures.length > 0 ? (
                            section.lectures.map((lecture) => {
                              const getLectureIcon = () => {
                                switch (lecture.type) {
                                  case 'VIDEO':
                                    return <Play className="h-4 w-4 text-indigo-600" />;
                                  case 'ARTICLE':
                                    return <FileText className="h-4 w-4 text-emerald-600" />;
                                  case 'QUIZ':
                                    return <HelpCircle className="h-4 w-4 text-amber-600" />;
                                  default:
                                    return <BookOpen className="h-4 w-4 text-slate-400" />;
                                }
                              };

                              const getLectureTypeLabel = () => {
                                const typeMap: Record<string, string> = {
                                  'VIDEO': 'Video',
                                  'ARTICLE': 'Article',
                                  'QUIZ': 'Quiz'
                                };
                                return typeMap[lecture.type] || lecture.type;
                              };

                              return (
                                <div key={lecture.id} className="border border-slate-200 rounded-lg p-3 bg-white">
                                  <div className="flex items-start gap-3">
                                    {getLectureIcon()}
                                    <div className="flex-1 min-w-0">
                                      <p className="font-medium text-slate-900 text-sm">{lecture.title}</p>
                                      <div className="flex items-center gap-2 mt-2 flex-wrap">
                                        <span className="inline-block px-2 py-1 rounded text-xs font-semibold bg-indigo-50 text-indigo-600">
                                          {getLectureTypeLabel()}
                                        </span>
                                        {lecture.durationSeconds && (
                                          <span className="text-xs text-slate-500">
                                            {Math.floor(lecture.durationSeconds / 60)}m
                                          </span>
                                        )}
                                        {lecture.isFreePreview && (
                                          <span className="inline-block px-2 py-1 rounded text-xs font-semibold bg-blue-50 text-blue-600">
                                            Free Preview
                                          </span>
                                        )}
                                      </div>
                                      {lecture.content && (
                                        <p className="text-xs text-slate-500 mt-2 line-clamp-2">{lecture.content}</p>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              );
                            })
                          ) : (
                            <div className="p-4 text-sm text-slate-500">No lessons in this section</div>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-slate-500 text-sm">No sections added yet</p>
              )}
            </div>
          </div>

          {/* Sidebar - Quality Checklist & Actions */}
          <div className="space-y-6">
            {/* Quality Checklist */}
            <div className="rounded-lg border border-slate-200 bg-white p-6">
              <h3 className="mb-4 font-bold text-slate-900">Quality Checklist</h3>
              <div className="space-y-3">
                {qualityChecklist.map((item) => (
                  <label key={item.id} className="flex items-start gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={checkedItems.has(item.id)}
                      onChange={() => toggleChecked(item.id)}
                      className="mt-1 h-4 w-4 rounded border-slate-300 text-indigo-600"
                    />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-slate-900">{item.label}</p>
                      <p className="text-xs text-slate-500">{item.category}</p>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {/* Moderator Notes */}
            <div className="rounded-lg border border-slate-200 bg-white p-6">
              <h3 className="mb-4 font-bold text-slate-900">Moderator Notes</h3>
              <textarea
                value={moderatorNotes}
                onChange={(e) => setModeratorNotes(e.target.value)}
                placeholder="Add any notes or observations about this course..."
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-500 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                rows={4}
              />
            </div>

            {/* Action Buttons */}
            <div className="flex gap-3">
              <Button
                variant="secondary"
                onClick={() => navigate('/admin/courses')}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                onClick={() => setShowRejectModal(true)}
                className="flex-1"
                style={{ backgroundColor: '#E11D48', color: 'white' }}
              >
                Send for Revision
              </Button>
              <Button
                onClick={handleApprove}
                disabled={isSubmitting}
                className="flex-1"
                style={{ backgroundColor: '#10B981', color: 'white' }}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Approving...
                  </>
                ) : (
                  <>
                    <Check className="h-4 w-4 mr-2" />
                    Approve
                  </>
                )}
              </Button>
            </div>

            {/* Rejection Reason Display */}
            {course.status === CourseStatus.REJECTED && course.rejectionReason && (
              <div className="rounded-lg border-l-2 border-rose-400 bg-rose-50 p-4">
                <p className="text-sm font-semibold text-rose-700 mb-2">Previous Rejection Reason:</p>
                <p className="text-sm text-rose-600 whitespace-pre-wrap">{course.rejectionReason}</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Reject Modal */}
      <Modal
        open={showRejectModal}
        onClose={() => {
          setShowRejectModal(false);
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
                setShowRejectModal(false);
                setRejectReason('');
              }}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={() => handleReject()}
              disabled={isSubmitting || !rejectReason.trim()}
              style={{ backgroundColor: '#E11D48', color: 'white' }}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Sending...
                </>
              ) : (
                'Send for Revision'
              )}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminWorkspaceLayout>
  );
};

export default AdminCourseReviewPage;
