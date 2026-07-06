import React, { useEffect, useState } from 'react';
import {
  ArrowLeft,
  Loader2,
  ChevronDown,
  ChevronUp,
  BookOpen,
  Play,
  FileText,
  HelpCircle,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { cn } from '../../lib/cn';
import { courseApi } from '../../services/courseApi';
import type { Course, CourseSection } from '../../types';

const statusConfig = {
  DRAFT: { label: 'Draft', color: '#B76E14', bgColor: 'bg-[#FFF5E8]' },
  PENDING_REVIEW: { label: 'Pending Review', color: '#2F2FA2', bgColor: 'bg-[#EEF2FF]' },
  PUBLISHED: { label: 'Published', color: '#1F7A45', bgColor: 'bg-[#EEF8F2]' },
  REJECTED: { label: 'Revision Needed', color: '#E11D48', bgColor: 'bg-[#FFEBE8]' },
};

const AdminCourseDetailsPage: React.FC = () => {
  const navigate = useNavigate();
  const { courseId } = useParams<{ courseId: string }>();
  const [course, setCourse] = useState<Course | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!courseId) return;
    const loadCourse = async () => {
      setIsLoading(true);
      try {
        const data = await courseApi.getCourseDetailsForAdmin(courseId);
        setCourse(data);
      } catch (e) {
        console.error('Failed to load course', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadCourse();
  }, [courseId]);

  const toggleSection = (sectionId: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(sectionId)) {
      newExpanded.delete(sectionId);
    } else {
      newExpanded.add(sectionId);
    }
    setExpandedSections(newExpanded);
  };

  if (isLoading) {
    return (
      <AdminWorkspaceLayout title="Loading...">
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-lh-blue" />
        </div>
      </AdminWorkspaceLayout>
    );
  }

  if (!course) {
    return (
      <AdminWorkspaceLayout title="Course not found">
        <div className="text-center py-12">
          <p className="text-[#6B7280]">Unable to load course details</p>
        </div>
      </AdminWorkspaceLayout>
    );
  }

  const status = statusConfig[course.status as keyof typeof statusConfig];
  const lectureCount = course.sections?.reduce(
    (total, section) => total + (section.lectures?.length || 0),
    0
  ) || 0;

  return (
    <AdminWorkspaceLayout
      title="Course Details"
      actions={
        <button
          onClick={() => navigate('/admin/courses')}
          className="inline-flex h-10 items-center gap-2 rounded-lg border border-[#D9DEF2] bg-white px-4 text-sm font-medium text-[#6B7280] transition hover:bg-[#F8FAFF]"
        >
          <ArrowLeft className="h-4 w-4" />
          Back
        </button>
      }
    >
      <div className="space-y-6">
        {/* Header */}
        <div className="rounded-xl border border-[#E5E7EB] bg-white p-6">
          <div className="space-y-4">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <h1 className="text-2xl font-semibold text-[#111827]">{course.title}</h1>
                  <span
                    className={cn('rounded-full px-3 py-1 text-sm font-semibold', status.bgColor)}
                    style={{ color: status.color }}
                  >
                    {status.label}
                  </span>
                </div>
                <p className="text-sm text-[#6B7280]">
                  Instructor: <span className="font-medium text-[#111827]">{course.instructorName || course.instructorId || 'Unknown'}</span>
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm md:grid-cols-4">
              <div>
                <div className="text-xs font-semibold text-[#9CA3AF]">Created</div>
                <div className="mt-1 text-[#111827]">
                  {new Date(course.createdAt).toLocaleDateString('vi-VN')}
                </div>
              </div>
              <div>
                <div className="text-xs font-semibold text-[#9CA3AF]">Last Updated</div>
                <div className="mt-1 text-[#111827]">
                  {new Date(course.updatedAt).toLocaleDateString('vi-VN')}
                </div>
              </div>
              <div>
                <div className="text-xs font-semibold text-[#9CA3AF]">Lessons</div>
                <div className="mt-1 text-[#111827]">{lectureCount}</div>
              </div>
              <div>
                <div className="text-xs font-semibold text-[#9CA3AF]">Duration</div>
                <div className="mt-1 text-[#111827]">
                  {Math.floor(course.totalVideoDurationSeconds / 60)}m
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            {/* Course Information */}
            <div className="rounded-xl border border-[#E5E7EB] bg-white p-6">
              <h2 className="text-lg font-semibold text-[#111827] mb-4">Course Information</h2>

              <div className="space-y-4">
                {course.thumbnailUrl && (
                  <div>
                    <p className="text-xs font-semibold text-[#9CA3AF] mb-2">Thumbnail</p>
                    <img
                      src={course.thumbnailUrl}
                      alt="Course thumbnail"
                      className="h-40 w-full object-cover rounded-lg border border-[#E5E7EB]"
                    />
                  </div>
                )}

                {course.description && (
                  <div>
                    <p className="text-xs font-semibold text-[#9CA3AF] mb-2">Description</p>
                    <p className="text-sm leading-6 text-[#4B5563] whitespace-pre-wrap">{course.description}</p>
                  </div>
                )}

                {course.categoryName && (
                  <div>
                    <p className="text-xs font-semibold text-[#9CA3AF] mb-2">Category</p>
                    <p className="text-sm text-[#111827]">{course.categoryName}</p>
                  </div>
                )}

                {course.languageName && (
                  <div>
                    <p className="text-xs font-semibold text-[#9CA3AF] mb-2">Language</p>
                    <p className="text-sm text-[#111827]">{course.languageName}</p>
                  </div>
                )}

                {course.pricing && (
                  <div>
                    <p className="text-xs font-semibold text-[#9CA3AF] mb-2">Price</p>
                    <p className="text-sm font-semibold text-[#111827]">
                      {course.pricing.priceVnd.toLocaleString('vi-VN')} VND
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Curriculum */}
            <div className="rounded-xl border border-[#E5E7EB] bg-white p-6">
              <h2 className="text-lg font-semibold text-[#111827] mb-4">Curriculum</h2>

              {course.sections && course.sections.length > 0 ? (
                <div className="space-y-2">
                  {course.sections.map((section: CourseSection) => (
                    <div key={section.id} className="border border-[#E5E7EB] rounded-lg overflow-hidden">
                      <button
                        onClick={() => toggleSection(section.id)}
                        className="w-full flex items-center justify-between p-4 hover:bg-[#F9FAFB] transition"
                      >
                        <div className="flex items-center gap-3">
                          <BookOpen className="h-5 w-5 text-lh-blue" />
                          <div className="text-left">
                            <p className="font-medium text-[#111827]">{section.title}</p>
                            <p className="text-xs text-[#6B7280] mt-1">
                              {section.lectures?.length || 0} lesson{section.lectures?.length !== 1 ? 's' : ''}
                            </p>
                          </div>
                        </div>
                        {expandedSections.has(section.id) ? (
                          <ChevronUp className="h-5 w-5 text-[#9CA3AF]" />
                        ) : (
                          <ChevronDown className="h-5 w-5 text-[#9CA3AF]" />
                        )}
                      </button>

                      {expandedSections.has(section.id) && (
                        <div className="bg-[#F9FAFB] border-t border-[#E5E7EB] p-4 space-y-3">
                          {section.lectures?.map((lecture) => {
                            const getLectureIcon = () => {
                              switch (lecture.type) {
                                case 'VIDEO':
                                  return <Play className="h-4 w-4 text-[#2563EB]" />;
                                case 'ARTICLE':
                                  return <FileText className="h-4 w-4 text-[#10B981]" />;
                                case 'QUIZ':
                                  return <HelpCircle className="h-4 w-4 text-[#F59E0B]" />;
                                default:
                                  return <BookOpen className="h-4 w-4 text-[#6B7280]" />;
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
                              <div key={lecture.id} className="border border-[#E5E7EB] rounded-lg p-3 bg-white">
                                <div className="flex items-start gap-3">
                                  {getLectureIcon()}
                                  <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium text-[#111827]">{lecture.title}</p>
                                    <div className="flex items-center gap-2 mt-2">
                                      <span className="inline-block px-2 py-1 rounded text-xs font-semibold bg-[#EEF2FF] text-[#2563EB]">
                                        {getLectureTypeLabel()}
                                      </span>
                                      {lecture.durationSeconds && (
                                        <span className="text-xs text-[#6B7280]">
                                          {Math.floor(lecture.durationSeconds / 60)}m
                                        </span>
                                      )}
                                      {lecture.isFreePreview && (
                                        <span className="inline-block px-2 py-1 rounded text-xs font-semibold bg-[#DBEAFE] text-[#0369A1]">
                                          Free Preview
                                        </span>
                                      )}
                                    </div>
                                    {lecture.content && (
                                      <p className="text-xs text-[#6B7280] mt-2 line-clamp-2">{lecture.content}</p>
                                    )}
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-[#6B7280]">No sections added yet</p>
              )}
            </div>
          </div>

          <div className="space-y-6">
            {/* Rejection Reason */}
            {course.status === 'REJECTED' && course.rejectionReason && (
              <div className="rounded-xl border border-[#FFEBE8] bg-[#FFEBE8] p-4">
                <p className="text-sm font-semibold text-[#E11D48]">Rejection Reason</p>
                <p className="text-sm text-[#E11D48] mt-2">{course.rejectionReason}</p>
              </div>
            )}

            {/* Summary */}
            <div className="rounded-xl border border-[#E5E7EB] bg-white p-6">
              <h3 className="text-sm font-semibold text-[#111827] mb-4">Summary</h3>
              <div className="space-y-3 text-sm">
                <div>
                  <span className="text-[#6B7280]">Status:</span>
                  <span className="ml-2 font-semibold text-[#111827]">{status.label}</span>
                </div>
                <div>
                  <span className="text-[#6B7280]">Instructor:</span>
                  <span className="ml-2 font-semibold text-[#111827]">{course.instructorName || course.instructorId || 'N/A'}</span>
                </div>
                <div>
                  <span className="text-[#6B7280]">Sections:</span>
                  <span className="ml-2 font-semibold text-[#111827]">{course.sections?.length || 0}</span>
                </div>
                <div>
                  <span className="text-[#6B7280]">Total Lessons:</span>
                  <span className="ml-2 font-semibold text-[#111827]">{lectureCount}</span>
                </div>
                <div>
                  <span className="text-[#6B7280]">Total Duration:</span>
                  <span className="ml-2 font-semibold text-[#111827]">
                    {Math.floor(course.totalVideoDurationSeconds / 60)} minutes
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminCourseDetailsPage;
