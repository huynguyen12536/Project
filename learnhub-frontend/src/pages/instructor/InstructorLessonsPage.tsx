import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Clock3,
  FileText,
  Filter,
  MoreHorizontal,
  NotebookPen,
  Search,
  Video,
} from 'lucide-react';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import { cn } from '../../lib/cn';
import {
  instructorCoursesSeed,
  type InstructorLecture,
  type InstructorLectureType,
} from '../../data/courseCatalog';

type LessonWithCourse = InstructorLecture & {
  courseId: string;
  courseTitle: string;
  sectionTitle: string;
};

const lectureTypeMeta: Record<
  InstructorLectureType,
  { label: string; icon: typeof Video; tone: string }
> = {
  video: { label: 'Video', icon: Video, tone: 'bg-[#EEF2FF] text-lh-blue' },
  article: { label: 'Bai viet', icon: FileText, tone: 'bg-[#FFF1F5] text-lh-pink' },
  quiz: { label: 'Quiz', icon: NotebookPen, tone: 'bg-[#F4F2F8] text-lh-purple' },
};

const InstructorLessonsPage: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<InstructorLectureType | 'all'>('all');

  const allLessons = useMemo(() => {
    const lessons: LessonWithCourse[] = [];
    instructorCoursesSeed.forEach((course) => {
      course.curriculum.forEach((section) => {
        section.lectures.forEach((lecture) => {
          lessons.push({
            ...lecture,
            courseId: course.id,
            courseTitle: course.title,
            sectionTitle: section.title,
          });
        });
      });
    });
    return lessons;
  }, []);

  const filteredLessons = useMemo(() => {
    const normalized = searchQuery.trim().toLowerCase();
    return allLessons.filter((lesson) => {
      const matchesSearch =
        normalized.length === 0 ||
        lesson.title.toLowerCase().includes(normalized) ||
        lesson.courseTitle.toLowerCase().includes(normalized);
      const matchesType = filterType === 'all' || lesson.type === filterType;
      return matchesSearch && matchesType;
    });
  }, [allLessons, filterType, searchQuery]);

  const stats = useMemo(() => {
    return {
      total: allLessons.length,
      video: allLessons.filter((lesson) => lesson.type === 'video').length,
      article: allLessons.filter((lesson) => lesson.type === 'article').length,
      quiz: allLessons.filter((lesson) => lesson.type === 'quiz').length,
    };
  }, [allLessons]);

  return (
    <InstructorWorkspaceLayout
      title="Bai giang"
      description="Danh sach tat ca video, bai viet va quiz da duoc dua vao khoa hoc. Giang vien co the tim nhanh, loc nhanh va mo dung bai can chinh sua."
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-4">
          {[
            { label: 'Tong bai giang', value: stats.total, icon: Clock3, tone: 'bg-[#111827] text-white' },
            { label: 'Video', value: stats.video, icon: Video, tone: 'bg-[#EEF2FF] text-lh-blue' },
            { label: 'Bai viet', value: stats.article, icon: FileText, tone: 'bg-[#FFF1F5] text-lh-pink' },
            { label: 'Quiz', value: stats.quiz, icon: NotebookPen, tone: 'bg-[#F4F2F8] text-lh-purple' },
          ].map((stat) => {
            const Icon = stat.icon;
            return (
              <div
                key={stat.label}
                className="rounded-[24px] border border-[#E7E9F2] bg-white p-5 shadow-[0_12px_32px_rgba(21,22,46,0.04)]"
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">{stat.label}</div>
                    <div className="mt-3 font-inter text-[30px] font-semibold tracking-[-0.03em] text-[#111827]">
                      {stat.value}
                    </div>
                  </div>
                  <div className={cn('rounded-2xl p-3', stat.tone)}>
                    <Icon className="h-5 w-5" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="text-sm font-medium text-[#6B7280]">Danh sach</div>
              <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                Tat ca bai giang da dang
              </h2>
            </div>
            <div className="flex flex-wrap gap-3">
              <div className="relative w-full sm:w-[320px]">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
                <input
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Tim bai giang theo ten bai hoac khoa hoc..."
                  className="w-full rounded-xl border border-[#E5E7EB] bg-white py-2.5 pl-9 pr-3 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                />
              </div>
              <div className="flex items-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3 py-2">
                <Filter className="h-4 w-4 text-[#6B7280]" />
                <select
                  value={filterType}
                  onChange={(event) => setFilterType(event.target.value as InstructorLectureType | 'all')}
                  className="bg-transparent text-sm font-medium text-[#6B7280] outline-none"
                >
                  <option value="all">Tat ca</option>
                  <option value="video">Video</option>
                  <option value="article">Bai viet</option>
                  <option value="quiz">Quiz</option>
                </select>
              </div>
            </div>
          </div>

          <div className="mt-6 space-y-4">
            {filteredLessons.map((lesson, index) => {
              const meta = lectureTypeMeta[lesson.type];
              const Icon = meta.icon;
              return (
                <motion.div
                  key={lesson.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.18, delay: index * 0.03, ease: 'easeOut' }}
                  className="rounded-[20px] border border-[#ECEFF5] bg-white p-5 transition hover:border-[#D9DEF2] hover:shadow-[0_8px_24px_rgba(47,47,162,0.08)]"
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-start gap-4">
                        <div className={cn('flex h-12 w-12 flex-none items-center justify-center rounded-xl', meta.tone)}>
                          <Icon className="h-5 w-5" />
                        </div>
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="truncate text-lg font-semibold text-[#111827]">{lesson.title}</h3>
                            <span className={cn('rounded-full px-2.5 py-1 text-[11px] font-semibold', meta.tone)}>
                              {meta.label}
                            </span>
                          </div>
                          <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-[#6B7280]">
                            <span>{lesson.courseTitle}</span>
                            <span>•</span>
                            <span>{lesson.sectionTitle}</span>
                            <span>•</span>
                            <span>{lesson.duration}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button className="rounded-xl border border-[#E5E7EB] px-3 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]">
                        Sua
                      </button>
                      <button className="rounded-xl border border-[#D9DEF2] px-3 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                        Xem
                      </button>
                      <button className="inline-flex items-center justify-center rounded-xl border border-[#E5E7EB] p-2 text-[#6B7280] transition hover:bg-[#F9FAFB]">
                        <MoreHorizontal className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </motion.div>
              );
            })}

            {filteredLessons.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 text-center">
                <Clock3 className="h-16 w-16 text-[#D1D5DB]" />
                <h3 className="mt-4 text-lg font-semibold text-[#111827]">Khong tim thay bai giang</h3>
                <p className="mt-2 text-sm text-[#6B7280]">Thu doi tu khoa hoac bo loc de xem nhieu ket qua hon.</p>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </InstructorWorkspaceLayout>
  );
};

export default InstructorLessonsPage;
