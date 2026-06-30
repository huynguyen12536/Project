import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  BarChart3,
  BookOpen,
  ChevronRight,
  Clock,
  FileText,
  GraduationCap,
  LayoutGrid,
  MessageSquareText,
  NotebookPen,
  Plus,
  Star,
  Video,
  Wallet,
  Users,
} from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { cn } from '../../lib/cn';
import {
  instructorCoursesSeed,
  type InstructorCourse,
  type InstructorLectureType,
} from '../../data/courseCatalog';

const sidebarItems: Array<{
  label: string;
  icon: typeof BookOpen;
  path: string;
}> = [
    { label: 'Tong quan', icon: BarChart3, path: '/instructor/dashboard' },
    { label: 'Khoa hoc cua toi', icon: BookOpen, path: '/instructor/courses' },
    { label: 'Bai hoc', icon: Clock, path: '/instructor/lessons' },
    { label: 'Hoc vien', icon: Users, path: '/instructor/students' },
    { label: 'Danh gia', icon: Star, path: '/instructor/reviews' },
  ];

const statusTone: Record<InstructorCourse['status'], string> = {
  'Dang soan thao': 'bg-[#FFF5E8] text-[#B76E14]',
  'Da xuat ban': 'bg-[#EEF8F2] text-[#1F7A45]',
  'Dang cho duyet': 'bg-[#EEF2FF] text-lh-blue',
};

const lectureTypeMeta: Record<
  InstructorLectureType,
  { label: string; icon: typeof Video; tone: string }
> = {
  video: { label: 'Video', icon: Video, tone: 'bg-[#EEF2FF] text-lh-blue' },
  article: { label: 'Bai viet', icon: FileText, tone: 'bg-[#FFF1F5] text-lh-pink' },
  quiz: { label: 'Quiz', icon: NotebookPen, tone: 'bg-[#F4F2F8] text-lh-purple' },
};

const InstructorCoursesPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [courses, setCourses] = useState(instructorCoursesSeed);
  const [activeCourseId, setActiveCourseId] = useState(instructorCoursesSeed[0]?.id ?? '');
  const activeCourse = courses.find((course) => course.id === activeCourseId) ?? courses[0];

  const stats = useMemo(() => {
    return {
      revenue: '287.1tr',
      students: '13.5K',
      rating: '4.83',
    };
  }, []);

  const addSection = () => {
    setCourses((current) =>
      current.map((course) => {
        if (course.id !== activeCourse.id) return course;

        const nextSection = course.curriculum.length + 1;
        return {
          ...course,
          curriculum: [
            ...course.curriculum,
            {
              id: `section-${Date.now()}`,
              title: `Chuong ${nextSection} · Noi dung moi`,
              lectures: [],
            },
          ],
        };
      })
    );
  };

  const addLecture = (sectionId: string, type: InstructorLectureType) => {
    setCourses((current) =>
      current.map((course) => {
        if (course.id !== activeCourse.id) return course;

        return {
          ...course,
          curriculum: course.curriculum.map((section) => {
            if (section.id !== sectionId) return section;

            const nextLecture = section.lectures.length + 1;
            return {
              ...section,
              lectures: [
                ...section.lectures,
                {
                  id: `lecture-${Date.now()}-${nextLecture}`,
                  title: `${lectureTypeMeta[type].label} moi ${nextLecture}`,
                  duration: type === 'quiz' ? '10 cau hoi' : '08:00',
                  type,
                },
              ],
            };
          }),
        };
      })
    );
  };

  return (
    <div className="min-h-screen bg-[#F7F8FC]">
      <div className="mx-auto max-w-[1600px] px-4 py-6 sm:px-6 xl:px-8">
        <div className="mb-6 flex items-center justify-between gap-4 rounded-[28px] border border-[#E7E9F2] bg-white px-6 py-5 shadow-[0_12px_32px_rgba(21,22,46,0.05)]">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/dashboard')}
              className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] px-3.5 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]"
            >
              <ChevronRight className="h-4 w-4 rotate-180" />
              Quay lai Dashboard
            </button>
            <div>
              <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Instructor studio</div>
              <h1 className="mt-1 font-inter text-3xl font-semibold tracking-[-0.03em] text-[#111827]">
                Quan ly khoa hoc va noi dung giang day
              </h1>
            </div>
          </div>
          <button className="inline-flex h-12 items-center justify-center gap-2 rounded-xl bg-lh-blue px-5 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(47,47,162,0.22)] transition hover:bg-lh-navy">
            <Plus className="h-4.5 w-4.5" />
            Tao khoa hoc moi
          </button>
        </div>

        <div className="grid gap-6 xl:grid-cols-[260px_minmax(0,1fr)]">
          <aside className="rounded-[28px] border border-[#E7E9F2] bg-white p-4 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
            <div className="mb-4 px-3 py-2">
              <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Giang vien</div>
              <div className="mt-2 font-inter text-xl font-semibold text-[#111827]">Studio LearnHub</div>
            </div>
            <div className="space-y-1.5">
              {sidebarItems.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname === item.path;
                return (
                  <button
                    key={item.label}
                    onClick={() => navigate(item.path)}
                    className={cn(
                      'flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-semibold transition',
                      isActive
                        ? 'bg-[#111827] text-white shadow-[0_10px_20px_rgba(17,24,39,0.12)]'
                        : 'text-[#4B5563] hover:bg-[#F8FAFF] hover:text-[#111827]'
                    )}
                  >
                    <Icon className="h-4.5 w-4.5" />
                    {item.label}
                  </button>
                );
              })}
            </div>
          </aside>

          <div className="min-w-0 space-y-6">
            <div className="grid gap-4 md:grid-cols-3">
              {[
                {
                  label: 'Tong doanh thu',
                  value: stats.revenue,
                  note: '+12.4tr trong 30 ngay qua',
                  icon: Wallet,
                  tone: 'bg-[#EEF2FF] text-lh-blue',
                },
                {
                  label: 'Tong hoc vien',
                  value: stats.students,
                  note: '1.238 hoc vien dang hoc tuan nay',
                  icon: Users,
                  tone: 'bg-[#FFF1F5] text-lh-pink',
                },
                {
                  label: 'Xep hang trung binh',
                  value: stats.rating,
                  note: 'Duoc tinh tren toan bo khoa hoc da xuat ban',
                  icon: Star,
                  tone: 'bg-[#F4F2F8] text-lh-purple',
                },
              ].map((stat) => {
                const Icon = stat.icon;
                return (
                  <div
                    key={stat.label}
                    className="rounded-[24px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="text-sm font-medium text-[#6B7280]">{stat.label}</div>
                        <div className="mt-3 font-inter text-[32px] font-semibold tracking-[-0.03em] text-[#111827]">
                          {stat.value}
                        </div>
                      </div>
                      <div className={cn('rounded-2xl p-3', stat.tone)}>
                        <Icon className="h-5 w-5" />
                      </div>
                    </div>
                    <div className="mt-4 text-sm leading-6 text-[#6B7280]">{stat.note}</div>
                  </div>
                );
              })}
            </div>

            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <div className="text-sm font-medium text-[#6B7280]">Khoa hoc cua toi</div>
                  <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                    Danh sach khoa hoc dang van hanh
                  </h2>
                </div>
                <button className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] px-4 py-2.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                  <GraduationCap className="h-4 w-4" />
                  Xem trang ban khoa hoc
                </button>
              </div>

              <div className="mt-6 space-y-3">
                {courses.map((course) => (
                  <button
                    key={course.id}
                    onClick={() => setActiveCourseId(course.id)}
                    className={cn(
                      'grid w-full gap-4 rounded-2xl border p-4 text-left transition md:grid-cols-[minmax(0,1.3fr)_0.65fr_0.65fr_0.55fr]',
                      course.id === activeCourse.id
                        ? 'border-[#D9DEF2] bg-[#F8FAFF] shadow-[0_10px_24px_rgba(47,47,162,0.06)]'
                        : 'border-[#ECEFF5] bg-white hover:border-[#D9DEF2]'
                    )}
                  >
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-semibold text-lh-blue">
                          {course.category}
                        </span>
                        <span className={cn('rounded-full px-2.5 py-1 text-[11px] font-semibold', statusTone[course.status])}>
                          {course.status}
                        </span>
                      </div>
                      <div className="mt-3 text-lg font-semibold text-[#111827]">{course.title}</div>
                      <div className="mt-2 text-sm leading-6 text-[#6B7280]">{course.subtitle}</div>
                    </div>
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Cap nhat</div>
                      <div className="mt-2 text-sm font-medium text-[#111827]">{course.updatedAt}</div>
                    </div>
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Hoc vien</div>
                      <div className="mt-2 text-sm font-medium text-[#111827]">{course.students}</div>
                    </div>
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Doanh thu</div>
                      <div className="mt-2 text-sm font-medium text-[#111827]">{course.revenue}</div>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <div className="text-sm font-medium text-[#6B7280]">Course builder</div>
                  <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                    Xay dung chuong va bai hoc cho {activeCourse.title}
                  </h2>
                  <p className="mt-2 max-w-3xl text-sm leading-6 text-[#6B7280]">
                    Giao dien nay uu tien cach to chuc noi dung ro rang de giang vien them bai moi ma khong bi roi boi qua nhieu thao tac.
                  </p>
                </div>
                <div className="flex flex-wrap gap-3">
                  <button
                    onClick={addSection}
                    className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 py-2.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
                  >
                    <Plus className="h-4 w-4" />
                    Them chuong moi
                  </button>
                </div>
              </div>

              <div className="mt-6 space-y-5">
                {activeCourse.curriculum.map((section, sectionIndex) => (
                  <motion.div
                    key={section.id}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.18, ease: 'easeOut' }}
                    className="rounded-[24px] border border-[#ECEFF5] bg-[#FBFCFE] p-5"
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                          Section {sectionIndex + 1}
                        </div>
                        <div className="mt-2 text-lg font-semibold text-[#111827]">{section.title}</div>
                        <div className="mt-1 text-sm text-[#6B7280]">{section.lectures.length} bai hoc</div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {(['video', 'article', 'quiz'] as InstructorLectureType[]).map((type) => {
                          const Icon = lectureTypeMeta[type].icon;
                          return (
                            <button
                              key={type}
                              onClick={() => addLecture(section.id, type)}
                              className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3.5 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F8FAFF]"
                            >
                              <Icon className="h-4 w-4" />
                              Them {lectureTypeMeta[type].label.toLowerCase()}
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    <div className="mt-5 space-y-3">
                      {section.lectures.map((lecture, lectureIndex) => {
                        const meta = lectureTypeMeta[lecture.type];
                        const Icon = meta.icon;

                        return (
                          <div
                            key={lecture.id}
                            className="grid gap-3 rounded-2xl border border-[#E5E7EB] bg-white p-4 lg:grid-cols-[minmax(0,1fr)_160px_140px]"
                          >
                            <div className="min-w-0">
                              <div className="flex items-center gap-3">
                                <div className={cn('rounded-xl p-2.5', meta.tone)}>
                                  <Icon className="h-4.5 w-4.5" />
                                </div>
                                <div className="min-w-0">
                                  <div className="truncate text-sm font-semibold text-[#111827]">
                                    Bai {lectureIndex + 1}. {lecture.title}
                                  </div>
                                  <div className="mt-1 text-xs text-[#6B7280]">
                                    {meta.label}
                                    {lecture.previewable ? ' · Co preview' : ''}
                                  </div>
                                </div>
                              </div>
                            </div>
                            <div>
                              <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Thoi luong</div>
                              <div className="mt-2 text-sm font-medium text-[#111827]">{lecture.duration}</div>
                            </div>
                            <div className="flex items-center justify-end gap-2">
                              <button className="rounded-xl border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]">
                                Sua
                              </button>
                              <button className="rounded-xl border border-[#D9DEF2] px-3 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                                Noi dung
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>

            <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
              <div className="text-sm font-medium text-[#6B7280]">Huong dan tiep theo</div>
              <div className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Lam studio de giang vien de tho hon
              </div>
              <div className="mt-4 grid gap-4 md:grid-cols-3">
                {[
                  {
                    icon: BookOpen,
                    title: 'Cau truc ro rang',
                    text: 'Chuong va bai hoc tach rach roi giup giang vien de quan ly va hoc vien de theo doi.',
                  },
                  {
                    icon: MessageSquareText,
                    title: 'Trang thai minh bach',
                    text: 'Mau trang thai giup thay ngay khoa hoc dang draft, da publish hay cho duyet.',
                  },
                  {
                    icon: LayoutGrid,
                    title: 'San sang mo rong',
                    text: 'Khung builder nay co the gan them keo tha bang dnd-kit o buoc tiep theo ma khong can viet lai layout.',
                  },
                ].map((item) => {
                  const Icon = item.icon;
                  return (
                    <div key={item.title} className="rounded-2xl border border-[#ECEFF5] bg-[#FBFCFE] p-5">
                      <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#EEF2FF] text-lh-blue">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div className="mt-4 text-base font-semibold text-[#111827]">{item.title}</div>
                      <div className="mt-2 text-sm leading-6 text-[#6B7280]">{item.text}</div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InstructorCoursesPage;
