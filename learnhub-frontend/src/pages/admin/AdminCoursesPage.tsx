import React, { useMemo, useState, useEffect } from 'react';
import {
  ArrowUpRight,
  BookCopy,
  CircleDashed,
  ShieldCheck,
  Sparkles,
  CheckCircle2,
  XCircle,
  Loader2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import { cn } from '../../lib/cn';
import { courseApi } from '../../services/courseApi';
import { Button, Input, Textarea } from '../../ui-kit';
import { CourseStatus } from '../../types';
import type { Course } from '../../types';

const courseTabs: Array<{
  label: string;
  value: 'all' | CourseStatus;
}> = [
  { label: 'Tat ca', value: 'all' },
  { label: 'Cho duyet', value: CourseStatus.PENDING_REVIEW },
  { label: 'Can sua', value: CourseStatus.REJECTED },
  { label: 'Da xuat ban', value: CourseStatus.PUBLISHED },
];

const statusMap: Record<CourseStatus, string> = {
  DRAFT: 'Dang soan thao',
  PENDING_REVIEW: 'Cho duyet',
  PUBLISHED: 'Da xuat ban',
  REJECTED: 'Tu choi',
};

const statusTone: Record<CourseStatus, string> = {
  DRAFT: 'bg-[#FFF5E8] text-[#B76E14]',
  PENDING_REVIEW: 'bg-[#EEF2FF] text-lh-blue',
  PUBLISHED: 'bg-[#EEF8F2] text-[#1F7A45]',
  REJECTED: 'bg-[#FFEBE8] text-[#E11D48]',
};

const AdminCoursesPage: React.FC = () => {
  const navigate = useNavigate();
  const [courses, setCourses] = useState<Course[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | CourseStatus>('all');
  const [selectedCourseId, setSelectedCourseId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    const loadCourses = async () => {
      setIsLoading(true);
      try {
        const data = await courseApi.getPendingCourses();
        setCourses(data);
        if (data.length > 0) setSelectedCourseId(data[0].id);
      } catch (e) {
        console.error('Failed to load courses', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadCourses();
  }, []);

  const visibleCourses = useMemo(() => {
    return courses.filter(
      (course) => activeTab === 'all' || course.status === activeTab
    );
  }, [activeTab, courses]);

  const selectedCourse = courses.find((c) => c.id === selectedCourseId);

  const handleApprove = async () => {
    if (!selectedCourseId) return;
    setIsSubmitting(true);
    try {
      const updated = await courseApi.approveCourse(selectedCourseId);
      setCourses((prev) =>
        prev.map((c) => (c.id === selectedCourseId ? updated : c))
      );
    } catch (e) {
      console.error('Failed to approve course', e);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!selectedCourseId || !rejectReason.trim()) return;
    setIsSubmitting(true);
    try {
      const updated = await courseApi.rejectCourse(selectedCourseId, {
        rejectionReason: rejectReason,
      });
      setCourses((prev) =>
        prev.map((c) => (c.id === selectedCourseId ? updated : c))
      );
      setRejectReason('');
    } catch (e) {
      console.error('Failed to reject course', e);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AdminWorkspaceLayout
      title="Dieu phoi noi dung khoa hoc"
      description="Review queue xuat ban, theo doi cac khoa hoc can sua va giu chat luong catalog thong nhat truoc khi mo ban."
      actions={
        <button
          onClick={() => navigate('/admin/taxonomy')}
          className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
        >
          <ShieldCheck className="h-4 w-4" />
          Mo taxonomy editor
        </button>
      }
    >
      <div className="space-y-6">
        <div className="flex flex-wrap gap-2">
          {courseTabs.map((tab) => (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              className={cn(
                'rounded-full px-4 py-2 text-sm font-medium transition',
                activeTab === tab.value
                  ? 'bg-[#111827] text-white'
                  : 'bg-[#F3F4F6] text-[#4B5563] hover:bg-[#E5E7EB]'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_340px]">
          <div className="space-y-4">
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <div
                  key={i}
                  className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm animate-pulse"
                >
                  <div className="h-8 w-2/3 bg-[#F3F4F6] rounded-lg" />
                  <div className="h-4 w-1/2 bg-[#F3F4F6] rounded-lg mt-3" />
                </div>
              ))
            ) : visibleCourses.length === 0 ? (
              <div className="rounded-2xl border border-[#E5E7EB] bg-white p-10 shadow-sm text-center">
                <div className="text-sm text-[#6B7280]">
                  Khong co khoa hoc nao trong queue hien tai
                </div>
              </div>
            ) : (
              visibleCourses.map((course) => (
                <div
                  key={course.id}
                  onClick={() => setSelectedCourseId(course.id)}
                  className={cn(
                    'rounded-2xl border bg-white p-6 shadow-sm cursor-pointer transition',
                    course.id === selectedCourseId
                      ? 'border-[#D9DEF2] bg-[#F8FAFF]'
                      : 'border-[#E5E7EB] hover:border-[#D9DEF2] hover:bg-[#FBFCFE]'
                  )}
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={cn(
                            'rounded-full px-2.5 py-1 text-[11px] font-semibold',
                            statusTone[course.status]
                          )}
                        >
                          {statusMap[course.status]}
                        </span>
                        <span className="rounded-full bg-[#F3F4F6] px-2.5 py-1 text-[11px] font-semibold text-[#4B5563]">
                          Khoa hoc
                        </span>
                      </div>
                      <h2 className="mt-3 text-xl font-semibold tracking-[-0.02em] text-[#111827]">
                        {course.title}
                      </h2>
                      <div className="mt-2 text-sm text-[#6B7280]">
                        Cap nhat {new Date(course.updatedAt).toLocaleDateString('vi-VN')}
                      </div>
                    </div>

                    <button className="inline-flex h-10 items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-3.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                      Mo chi tiet
                      <ArrowUpRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="space-y-4">
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
                <BookCopy className="h-4 w-4 text-lh-blue" />
                Moderation policy
              </div>
              <div className="mt-4 space-y-3 text-sm leading-6 text-[#4B5563]">
                <p>1. Uu tien review khoa hoc moi trong 24 gio.</p>
                <p>
                  2. Bat buoc co preview, thumbnail dung guideline va 1 lesson mo ta ro ket qua dau ra.
                </p>
                <p>
                  3. Danh sach &quot;Can sua&quot; phai co comment cu the de giang vien xu ly trong 1 vong lap.
                </p>
              </div>
            </div>

            {selectedCourse ? (
              <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
                <h3 className="text-lg font-semibold text-[#111827]">
                  {selectedCourse.title}
                </h3>
                {selectedCourse.rejectionReason && (
                  <div className="mt-3 rounded-2xl bg-[#FFEBE8] p-4">
                    <div className="flex items-center gap-2 text-sm font-semibold text-[#E11D48]">
                      <XCircle className="h-4 w-4" />
                      Ly do tu choi
                    </div>
                    <p className="mt-2 text-sm text-[#E11D48]">
                      {selectedCourse.rejectionReason}
                    </p>
                  </div>
                )}

                {selectedCourse.status === CourseStatus.PENDING_REVIEW && (
                  <div className="mt-4 space-y-4">
                    <div>
                      <label className="mb-2 block text-sm font-semibold text-[#111827]">
                        Ly do tu choi (neu can)
                      </label>
                      <Textarea
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                        placeholder="Nhap ly do cu the de giang vien sua..."
                        rows={4}
                      />
                    </div>
                    <div className="flex gap-3">
                      <Button
                        onClick={handleApprove}
                        disabled={isSubmitting}
                      >
                        {isSubmitting ? (
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        ) : (
                          <CheckCircle2 className="h-4 w-4 mr-2" />
                        )}
                        Duyet
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={handleReject}
                        disabled={isSubmitting || !rejectReason.trim()}
                        className="border-[#E5E7EB] text-[#E11D48] hover:bg-[#FFEBE8]"
                      >
                        {isSubmitting ? (
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        ) : (
                          <XCircle className="h-4 w-4 mr-2" />
                        )}
                        Tu choi
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
                <div className="text-sm text-[#6B7280] text-center">
                  Chon khoa hoc de xem chi tiet
                </div>
              </div>
            )}

            <div className="rounded-2xl border border-dashed border-[#D9DEF2] bg-[#EEF2FF]/40 p-6 shadow-sm">
              <div className="flex items-center gap-3 text-sm font-medium text-lh-blue">
                <CircleDashed className="h-4 w-4" />
                Ghi chu van hanh
              </div>
              <p className="mt-3 text-sm leading-6 text-[#4B5563]">
                Khi chat luong catalog khong dong deu, completion va conversion se giam rat nhanh. Queue moderation nay can duoc du tri nhu mot lane rieng.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminCoursesPage;
