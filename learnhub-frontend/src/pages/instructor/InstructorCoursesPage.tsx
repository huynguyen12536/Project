import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  CheckCircle2,
  FileText,
  Plus,
  Star,
  Users,
  Video,
  NotebookPen,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import { cn } from '../../lib/cn';
import {
  instructorCoursesSeed,
  type InstructorCourse,
  type InstructorLectureType,
} from '../../data/courseCatalog';

const tabs: Array<{
  label: string;
  value: 'all' | InstructorCourse['status'];
}> = [
  { label: 'Tat ca', value: 'all' },
  { label: 'Da xuat ban', value: 'Da xuat ban' },
  { label: 'Dang cho duyet', value: 'Dang cho duyet' },
  { label: 'Dang soan thao', value: 'Dang soan thao' },
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
  const [courses, setCourses] = useState(instructorCoursesSeed);
  const [activeTab, setActiveTab] = useState<'all' | InstructorCourse['status']>('all');
  const [activeCourseId, setActiveCourseId] = useState(instructorCoursesSeed[0]?.id ?? '');
  const activeCourse = courses.find((course) => course.id === activeCourseId) ?? courses[0];

  const visibleCourses = useMemo(() => {
    return courses.filter((course) => activeTab === 'all' || course.status === activeTab);
  }, [activeTab, courses]);

  const addSection = () => {
    setCourses((current) =>
      current.map((course) => {
        if (course.id !== activeCourse.id) return course;
        return {
          ...course,
          curriculum: [
            ...course.curriculum,
            {
              id: `section-${Date.now()}`,
              title: `Chuong ${course.curriculum.length + 1} · Noi dung moi`,
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
            return {
              ...section,
              lectures: [
                ...section.lectures,
                {
                  id: `lecture-${Date.now()}`,
                  title: `${lectureTypeMeta[type].label} moi ${section.lectures.length + 1}`,
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
          <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy">
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
            {visibleCourses.map((course) => (
              <button
                key={course.id}
                onClick={() => setActiveCourseId(course.id)}
                className={cn(
                  'grid w-full gap-4 rounded-2xl border p-5 text-left transition md:grid-cols-[minmax(0,1.2fr)_0.65fr_0.55fr_0.45fr]',
                  course.id === activeCourse.id
                    ? 'border-[#D9DEF2] bg-[#F8FAFF]'
                    : 'border-[#ECEFF5] bg-white hover:border-[#D9DEF2] hover:bg-[#FBFCFE]'
                )}
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-medium text-lh-blue">
                      {course.category}
                    </span>
                    <span className={cn('rounded-full px-2.5 py-1 text-[11px] font-medium', statusTone[course.status])}>
                      {course.status}
                    </span>
                  </div>
                  <div className="mt-3 text-lg font-semibold text-[#111827]">{course.title}</div>
                  <div className="mt-2 text-sm leading-6 text-[#6B7280]">{course.subtitle}</div>
                </div>
                <div>
                  <div className="text-xs font-semibold text-[#9CA3AF]">Cap nhat</div>
                  <div className="mt-2 text-sm font-medium text-[#111827]">{course.updatedAt}</div>
                </div>
                <div>
                  <div className="text-xs font-semibold text-[#9CA3AF]">Hoc vien</div>
                  <div className="mt-2 flex items-center gap-2 text-sm font-medium text-[#111827]">
                    <Users className="h-4 w-4 text-lh-blue" />
                    {course.students}
                  </div>
                </div>
                <div>
                  <div className="text-xs font-semibold text-[#9CA3AF]">Danh gia</div>
                  <div className="mt-2 flex items-center gap-2 text-sm font-medium text-[#111827]">
                    <Star className="h-4 w-4 fill-[#F6A609] text-[#F6A609]" />
                    {course.rating}
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>

        <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="text-sm font-medium text-[#6B7280]">Builder</div>
              <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Cau truc noi dung cho {activeCourse.title}
              </h2>
            </div>
            <button
              onClick={addSection}
              className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 py-2.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
            >
              <Plus className="h-4 w-4" />
              Them chuong moi
            </button>
          </div>

          <div className="mt-6 space-y-5">
            {activeCourse.curriculum.map((section, sectionIndex) => (
              <motion.div
                key={section.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.18, ease: 'easeOut' }}
                className="rounded-2xl border border-[#ECEFF5] bg-[#FBFCFE] p-5"
              >
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="text-xs font-semibold text-[#9CA3AF]">Section {sectionIndex + 1}</div>
                    <div className="mt-2 text-lg font-semibold text-[#111827]">{section.title}</div>
                    <div className="mt-1 text-sm text-[#6B7280]">{section.lectures.length} bai giang</div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {(['video', 'article', 'quiz'] as InstructorLectureType[]).map((type) => {
                      const Icon = lectureTypeMeta[type].icon;
                      return (
                        <button
                          key={type}
                          onClick={() => addLecture(section.id, type)}
                          className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3.5 py-2 text-sm font-medium text-[#374151] transition hover:bg-[#F8FAFF]"
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
                              <div className="mt-1 text-xs text-[#6B7280]">{meta.label}</div>
                            </div>
                          </div>
                        </div>
                        <div>
                          <div className="text-xs font-semibold text-[#9CA3AF]">Thoi luong</div>
                          <div className="mt-2 text-sm font-medium text-[#111827]">{lecture.duration}</div>
                        </div>
                        <div className="flex items-center justify-end">
                          <span className="inline-flex items-center gap-1 rounded-full bg-[#EEF8F2] px-2.5 py-1 text-[11px] font-medium text-[#1F7A45]">
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            San sang
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </InstructorWorkspaceLayout>
  );
};

export default InstructorCoursesPage;
