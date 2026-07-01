import React, { useMemo, useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  CheckCircle2,
  FileText,
  Plus,
  Star,
  Users,
  Video,
  NotebookPen,
  Edit3,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import { cn } from '../../lib/cn';
import { courseApi } from '../../services/courseApi';
import { CourseStatus } from '../../types';
import type { Course } from '../../types';

const statusMap: Record<CourseStatus, string> = {
  DRAFT: 'Dang soan thao',
  PENDING_REVIEW: 'Dang cho duyet',
  PUBLISHED: 'Da xuat ban',
  REJECTED: 'Tu choi',
};

const statusTone: Record<CourseStatus, string> = {
  DRAFT: 'bg-[#FFF5E8] text-[#B76E14]',
  PUBLISHED: 'bg-[#EEF8F2] text-[#1F7A45]',
  PENDING_REVIEW: 'bg-[#EEF2FF] text-lh-blue',
  REJECTED: 'bg-[#FFEBE8] text-[#E11D48]',
};

const tabs: Array<{ label: string; value: 'all' | CourseStatus }> = [
  { label: 'Tat ca', value: 'all' },
  { label: 'Da xuat ban', value: CourseStatus.PUBLISHED },
  { label: 'Dang cho duyet', value: CourseStatus.PENDING_REVIEW },
  { label: 'Dang soan thao', value: CourseStatus.DRAFT },
];

const InstructorCoursesPage: React.FC = () => {
  const navigate = useNavigate();
  const [courses, setCourses] = useState<Course[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | CourseStatus>('all');
  const [activeCourseId, setActiveCourseId] = useState<string | null>(null);

  useEffect(() => {
    const loadCourses = async () => {
      setIsLoading(true);
      try {
        const data = await courseApi.getInstructorCourses();
        setCourses(data);
        if (data.length > 0) setActiveCourseId(data[0].id);
      } catch (e) {
        console.error('Failed to load courses', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadCourses();
  }, []);

  const activeCourse = courses.find((c) => c.id === activeCourseId) ?? courses[0];

  const visibleCourses = useMemo(() => {
    return courses.filter((course) => activeTab === 'all' || course.status === activeTab);
  }, [activeTab, courses]);

  return (
    <InstructorWorkspaceLayout
      title="Khoa hoc cua toi"
      description="Quan ly danh muc khoa hoc theo trang thai xuat ban, giu luong sua curriculum va bai giang trong mot workspace gon gang."
      actions={
        <>
          <button
            onClick={() => navigate('/instructor/lessons')}
            className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
          >
            Quan ly bai giang
          </button>
          <button
            onClick={() => navigate('/instructor/courses/new')}
            className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
          >
            <Plus className="h-4 w-4" />
            Tao khoa hoc moi
          </button>
        </>
      }
    >
      <div className="space-y-6">
        <div className="flex flex-wrap gap-2">
          {tabs.map((tab) => {
            const count =
              tab.value === 'all' ? courses.length : courses.filter((course) => course.status === tab.value).length;
            const active = activeTab === tab.value;

            return (
              <button
                key={tab.value}
                onClick={() => setActiveTab(tab.value)}
                className={cn(
                  'inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition',
                  active ? 'bg-[#EEF2FF] text-lh-blue' : 'border border-[#E5E7EB] bg-white text-[#4B5563] hover:bg-[#F9FAFB]'
                )}
              >
                {tab.label}
                <span className={cn('rounded-full px-2 py-0.5 text-[11px]', active ? 'bg-white' : 'bg-[#F3F4F6]')}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div className="space-y-3">
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <div
                  key={i}
                  className="grid w-full gap-4 rounded-2xl border border-[#E5E7EB] bg-white p-5 animate-pulse"
                >
                  <div className="h-10 w-3/4 bg-[#F3F4F6] rounded-lg" />
                  <div className="h-8 w-24 bg-[#F3F4F6] rounded-lg" />
                  <div className="h-8 w-24 bg-[#F3F4F6] rounded-lg" />
                  <div className="h-8 w-24 bg-[#F3F4F6] rounded-lg" />
                </div>
              ))
            ) : (
              visibleCourses.map((course) => (
                <div
                  key={course.id}
                  className={cn(
                    'grid w-full gap-4 rounded-2xl border p-5 text-left transition md:grid-cols-[minmax(0,1.2fr)_0.65fr_0.55fr_0.45fr]',
                    course.id === activeCourseId
                      ? 'border-[#D9DEF2] bg-[#F8FAFF]'
                      : 'border-[#ECEFF5] bg-white hover:border-[#D9DEF2] hover:bg-[#FBFCFE]'
                  )}
                >
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full px-2.5 py-1 text-[11px] font-medium text-lh-blue bg-[#EEF2FF]">
                        Khoa hoc
                      </span>
                      <span
                        className={cn('rounded-full px-2.5 py-1 text-[11px] font-medium', statusTone[course.status])}
                      >
                        {statusMap[course.status]}
                      </span>
                    </div>
                    <div className="mt-3 text-lg font-semibold text-[#111827]">{course.title}</div>
                    <div className="mt-2 text-sm leading-6 text-[#6B7280]">{course.subtitle}</div>
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-[#9CA3AF]">Cap nhat</div>
                    <div className="mt-2 text-sm font-medium text-[#111827]">
                      {new Date(course.updatedAt).toLocaleDateString('vi-VN')}
                    </div>
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-[#9CA3AF]">Hoc vien</div>
                    <div className="mt-2 flex items-center gap-2 text-sm font-medium text-[#111827]">
                      <Users className="h-4 w-4 text-lh-blue" />
                      {course.studentCount}
                    </div>
                  </div>
                  <div className="flex items-end justify-end">
                    <button
                      onClick={() => navigate(`/instructor/courses/${course.id}`)}
                      className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
                    >
                      <Edit3 className="h-4 w-4" />
                      Chinh sua
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>


      </div>
    </InstructorWorkspaceLayout>
  );
};

export default InstructorCoursesPage;
