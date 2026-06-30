import React, { useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clock3,
  FileText,
  LayoutPanelLeft,
  Menu,
  MessageSquareText,
  PlayCircle,
  Star,
  X,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { cn } from '../lib/cn';

type Lesson = {
  id: string;
  title: string;
  duration: string;
  completed: boolean;
  summary: string;
};

type Chapter = {
  id: string;
  title: string;
  duration: string;
  lessons: Lesson[];
};

type PlayerTab = 'overview' | 'qa' | 'notes' | 'resources';

const courseData = {
  id: '1',
  title: 'Lap trinh Web Full-Stack 2026',
  progress: 35,
  instructor: 'Tran Minh Quan',
  chapters: [
    {
      id: 'chapter-1',
      title: 'Khoi dong va set up du an',
      duration: '4 bai · 42 phut',
      lessons: [
        {
          id: 'lesson-1',
          title: 'Tong quan lo trinh khoa hoc',
          duration: '06:12',
          completed: true,
          summary: 'Xac dinh cach khoa hoc duoc chia module, output dau ra va cach dat tien do hoc tap thuc te.',
        },
        {
          id: 'lesson-2',
          title: 'Cai dat Node, VS Code va bo cong cu',
          duration: '11:40',
          completed: true,
          summary: 'Thiet lap moi truong phat trien nhat quan giua frontend, backend va database.',
        },
        {
          id: 'lesson-3',
          title: 'Tao monorepo va cau truc folder',
          duration: '09:25',
          completed: false,
          summary: 'To chuc source theo monorepo de mo rong he thong hoc tap, auth va dashboard ve sau.',
        },
        {
          id: 'lesson-4',
          title: 'Git workflow cho team nho',
          duration: '14:18',
          completed: false,
          summary: 'Dinh nghia nhanh branch strategy, commit flow va cach review de giu toc do phat trien.',
        },
      ],
    },
    {
      id: 'chapter-2',
      title: 'Frontend voi React',
      duration: '6 bai · 2 gio 18 phut',
      lessons: [
        {
          id: 'lesson-5',
          title: 'Routing va layout thong nhat',
          duration: '17:08',
          completed: false,
          summary: 'Xay khung giao dien co route cong khai, route hoc vien va route hoc tap toi gian.',
        },
        {
          id: 'lesson-6',
          title: 'Quan ly state dang nhap',
          duration: '21:44',
          completed: false,
          summary: 'Ket noi auth store voi dieu huong va session de nguoi hoc khong bi mat mach hoc.',
        },
      ],
    },
    {
      id: 'chapter-3',
      title: 'Backend, auth va thanh toan',
      duration: '8 bai · 3 gio 05 phut',
      lessons: [
        {
          id: 'lesson-7',
          title: 'JWT, refresh flow va session',
          duration: '24:10',
          completed: false,
          summary: 'Thiet ke luong truy cap an toan cho web app co dashboard va player.',
        },
        {
          id: 'lesson-8',
          title: 'Don hang khoa hoc va cap quyen hoc',
          duration: '19:40',
          completed: false,
          summary: 'Dong bo trang thai mua hang voi quyen truy cap khoa hoc sau thanh toan.',
        },
      ],
    },
  ] as Chapter[],
};

const tabItems: { key: PlayerTab; label: string; icon: typeof BookOpen }[] = [
  { key: 'overview', label: 'Tong quan', icon: BookOpen },
  { key: 'qa', label: 'Hoi & dap', icon: MessageSquareText },
  { key: 'notes', label: 'Ghi chu', icon: FileText },
  { key: 'resources', label: 'Tai lieu', icon: LayoutPanelLeft },
];

function flattenLessons(chapters: Chapter[]): Lesson[] {
  return chapters.flatMap((chapter) => chapter.lessons);
}

const CoursePlayerPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const course = courseData.id === id || !id ? courseData : { ...courseData, id: id ?? courseData.id };

  const allLessons = useMemo(() => flattenLessons(course.chapters), [course.chapters]);
  const initialLesson = allLessons.find((lesson) => !lesson.completed) ?? allLessons[0];
  const [activeLessonId, setActiveLessonId] = useState(initialLesson.id);
  const [openChapters, setOpenChapters] = useState<string[]>([course.chapters[0]?.id ?? '']);
  const [activeTab, setActiveTab] = useState<PlayerTab>('overview');
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const activeLesson = allLessons.find((lesson) => lesson.id === activeLessonId) ?? allLessons[0];
  const totalLessons = allLessons.length;
  const completedLessons = allLessons.filter((lesson) => lesson.completed).length;

  const activeChapter = course.chapters.find((chapter) =>
    chapter.lessons.some((lesson) => lesson.id === activeLesson.id)
  );

  const toggleChapter = (chapterId: string) => {
    setOpenChapters((current) =>
      current.includes(chapterId) ? current.filter((id) => id !== chapterId) : [...current, chapterId]
    );
  };

  const handleSelectLesson = (lessonId: string, chapterId: string) => {
    setActiveLessonId(lessonId);
    setSidebarOpen(false);
    setOpenChapters((current) => (current.includes(chapterId) ? current : [...current, chapterId]));
  };

  const renderTabContent = () => {
    if (activeTab === 'overview') {
      return (
        <div className="grid gap-4 lg:grid-cols-[1.35fr_0.95fr]">
          <div className="rounded-2xl border border-[#E8ECF5] bg-white p-5">
            <div className="text-sm font-semibold text-[#111827]">Noi dung bai hoc</div>
            <p className="mt-3 text-sm leading-7 text-[#4B5563]">{activeLesson.summary}</p>
            <div className="mt-5 grid gap-3 sm:grid-cols-3">
              <div className="rounded-xl bg-[#F8FAFF] p-4">
                <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Chuong</div>
                <div className="mt-2 text-sm font-semibold text-[#111827]">{activeChapter?.title}</div>
              </div>
              <div className="rounded-xl bg-[#F8FAFF] p-4">
                <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Thoi luong</div>
                <div className="mt-2 text-sm font-semibold text-[#111827]">{activeLesson.duration}</div>
              </div>
              <div className="rounded-xl bg-[#F8FAFF] p-4">
                <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">Tien do</div>
                <div className="mt-2 text-sm font-semibold text-[#111827]">{completedLessons}/{totalLessons} bai</div>
              </div>
            </div>
          </div>
          <div className="rounded-2xl border border-[#E8ECF5] bg-white p-5">
            <div className="text-sm font-semibold text-[#111827]">Muc tieu sau bai nay</div>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-[#4B5563]">
              <li className="flex gap-3">
                <CheckCircle2 className="mt-0.5 h-4.5 w-4.5 flex-none text-lh-blue" />
                Nhan biet duoc tac dong cua bai hoc hien tai trong tong the lo trinh.
              </li>
              <li className="flex gap-3">
                <CheckCircle2 className="mt-0.5 h-4.5 w-4.5 flex-none text-lh-blue" />
                Chot duoc mot output nho de tiep tuc bai ke tiep ma khong bi dut mach.
              </li>
              <li className="flex gap-3">
                <CheckCircle2 className="mt-0.5 h-4.5 w-4.5 flex-none text-lh-blue" />
                Luu duoc ghi chu va cau hoi de hoc lai nhanh vao cuoi tuan.
              </li>
            </ul>
          </div>
        </div>
      );
    }

    if (activeTab === 'qa') {
      return (
        <div className="rounded-2xl border border-[#E8ECF5] bg-white p-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="text-sm font-semibold text-[#111827]">Hoi & dap cua bai hoc</div>
              <p className="mt-2 text-sm text-[#6B7280]">
                Tap trung vao cac cau hoi lien quan truc tiep den bai dang hoc de giu dong hoc tap lien mach.
              </p>
            </div>
            <button className="rounded-xl border border-[#D9DEF2] px-4 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
              Dat cau hoi
            </button>
          </div>
          <div className="mt-5 space-y-4">
            {[
              'Khi nao nen tach auth service thanh module rieng?',
              'Tai sao route hoc tap nen tach khoi route marketing?',
            ].map((question) => (
              <div key={question} className="rounded-xl border border-[#EEF1F5] bg-[#FBFCFE] p-4">
                <div className="text-sm font-semibold text-[#111827]">{question}</div>
                <div className="mt-2 text-sm leading-6 text-[#6B7280]">
                  Day la thread mau de hoc vien dat van de dung ngu canh bai hoc hien tai.
                </div>
              </div>
            ))}
          </div>
        </div>
      );
    }

    if (activeTab === 'notes') {
      return (
        <div className="rounded-2xl border border-[#E8ECF5] bg-white p-5">
          <div className="text-sm font-semibold text-[#111827]">Ghi chu ca nhan</div>
          <textarea
            className="mt-4 min-h-[180px] w-full rounded-2xl border border-[#E5E7EB] bg-[#FBFCFE] p-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
            placeholder="Tom tat nhanh y chinh, doan code can nho, hoac viec ban muon thu lai sau bai nay..."
            defaultValue={`- Can tach route marketing va route learning.\n- Header cua player phai gon de giam xao nhang.\n- Bai hoc hien tai: ${activeLesson.title}`}
          />
        </div>
      );
    }

    return (
      <div className="rounded-2xl border border-[#E8ECF5] bg-white p-5">
        <div className="text-sm font-semibold text-[#111827]">Tai lieu di kem</div>
        <div className="mt-4 space-y-3">
          {[
            'Slide tom tat bai hoc.pdf',
            'Source code module hien tai.zip',
            'Checklist review truoc khi sang bai tiep theo.md',
          ].map((file) => (
            <button
              key={file}
              className="flex w-full items-center justify-between rounded-xl border border-[#EEF1F5] bg-[#FBFCFE] px-4 py-3 text-left transition hover:border-[#D9DEF2]"
            >
              <div className="flex items-center gap-3">
                <FileText className="h-4.5 w-4.5 text-lh-blue" />
                <span className="text-sm font-medium text-[#111827]">{file}</span>
              </div>
              <ChevronRight className="h-4 w-4 text-[#9CA3AF]" />
            </button>
          ))}
        </div>
      </div>
    );
  };

  const sidebarContent = (
    <div className="flex h-full flex-col">
      <div className="border-b border-[#E8ECF5] px-5 py-4">
        <div className="text-sm font-semibold text-[#111827]">Noi dung khoa hoc</div>
        <div className="mt-2 flex items-center justify-between text-xs text-[#6B7280]">
          <span>{course.chapters.length} chuong</span>
          <span>{totalLessons} bai hoc</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-3">
        <div className="space-y-2">
          {course.chapters.map((chapter, chapterIndex) => {
            const isOpen = openChapters.includes(chapter.id);

            return (
              <div key={chapter.id} className="overflow-hidden rounded-2xl border border-[#E8ECF5] bg-white">
                <button
                  onClick={() => toggleChapter(chapter.id)}
                  className="flex w-full items-center justify-between gap-3 px-4 py-4 text-left transition hover:bg-[#FBFCFE]"
                >
                  <div className="min-w-0">
                    <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                      Chuong {chapterIndex + 1}
                    </div>
                    <div className="mt-1 text-sm font-semibold text-[#111827]">{chapter.title}</div>
                    <div className="mt-1 text-xs text-[#6B7280]">{chapter.duration}</div>
                  </div>
                  <ChevronDown
                    className={cn('h-4 w-4 flex-none text-[#9CA3AF] transition-transform', isOpen && 'rotate-180')}
                  />
                </button>

                <AnimatePresence initial={false}>
                  {isOpen ? (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.22, ease: 'easeOut' }}
                      className="overflow-hidden border-t border-[#EEF1F5]"
                    >
                      <div className="space-y-1 p-2">
                        {chapter.lessons.map((lesson) => {
                          const isActive = lesson.id === activeLessonId;

                          return (
                            <button
                              key={lesson.id}
                              onClick={() => handleSelectLesson(lesson.id, chapter.id)}
                              className={cn(
                                'flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition',
                                isActive ? 'bg-[#EEF2FF]' : 'hover:bg-[#FBFCFE]'
                              )}
                            >
                              <div
                                className={cn(
                                  'flex h-5 w-5 flex-none items-center justify-center rounded-full border',
                                  lesson.completed
                                    ? 'border-[#BFE3CD] bg-[#EEF8F2] text-[#1F7A45]'
                                    : isActive
                                      ? 'border-[#B9C6FF] bg-white text-lh-blue'
                                      : 'border-[#D1D5DB] bg-white text-transparent'
                                )}
                              >
                                <CheckCircle2 className="h-3.5 w-3.5" />
                              </div>
                              <div className="min-w-0 flex-1">
                                <div className={cn('line-clamp-2 text-sm font-medium', isActive ? 'text-lh-blue' : 'text-[#111827]')}>
                                  {lesson.title}
                                </div>
                                <div className="mt-1 flex items-center gap-2 text-xs text-[#6B7280]">
                                  <Clock3 className="h-3.5 w-3.5" />
                                  {lesson.duration}
                                </div>
                              </div>
                            </button>
                          );
                        })}
                      </div>
                    </motion.div>
                  ) : null}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#F5F7FB] text-[#111827]">
      <header className="sticky top-0 z-40 border-b border-[#E8ECF5] bg-white/95 backdrop-blur-sm">
        <div className="mx-auto flex min-h-[72px] max-w-[1600px] items-center gap-4 px-4 sm:px-6 xl:px-8">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <button
              onClick={() => navigate('/dashboard')}
              className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3.5 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]"
            >
              <ArrowLeft className="h-4 w-4" />
              <span className="hidden sm:inline">Quay lai Dashboard</span>
            </button>
            <button
              onClick={() => navigate('/dashboard')}
              className="hidden items-center gap-3 lg:flex"
            >
              <div className="flex h-[34px] w-[34px] items-center justify-center rounded-xl bg-lh-navy shadow-[0_10px_20px_rgba(36,37,130,0.18)]">
                <div className="ml-1 h-0 w-0 border-b-[7px] border-l-[11px] border-t-[7px] border-b-transparent border-l-lh-pink border-t-transparent" />
              </div>
              <span className="font-inter text-lg font-black tracking-[-0.03em] text-lh-navy">LearnHub</span>
            </button>
          </div>

          <div className="hidden min-w-0 flex-[1.1] items-center justify-center xl:flex">
            <div className="truncate text-center font-inter text-base font-semibold tracking-[-0.02em] text-[#111827]">
              {course.title}
            </div>
          </div>

          <div className="ml-auto flex items-center gap-3">
            <div className="hidden items-center gap-3 lg:flex">
              <div className="w-[180px]">
                <div className="mb-2 flex items-center justify-between text-xs font-semibold text-[#6B7280]">
                  <span>{course.progress}% hoan thanh</span>
                  <span>{completedLessons}/{totalLessons} bai</span>
                </div>
                <div className="h-2 rounded-full bg-[#E5E7EB]">
                  <div className="h-2 rounded-full bg-lh-blue" style={{ width: `${course.progress}%` }} />
                </div>
              </div>
              <button className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
                <Star className="h-4 w-4" />
                Viet danh gia
              </button>
            </div>

            <button
              onClick={() => setSidebarOpen(true)}
              className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-[#374151] transition hover:bg-[#F9FAFB] xl:hidden"
              aria-label="Mo noi dung khoa hoc"
            >
              <Menu className="h-4.5 w-4.5" />
            </button>
          </div>
        </div>

        <div className="border-t border-[#F1F3F7] px-4 py-3 xl:hidden">
          <div className="mx-auto max-w-[1600px]">
            <div className="truncate text-sm font-semibold text-[#111827]">{course.title}</div>
            <div className="mt-2 flex items-center justify-between gap-3 text-xs font-semibold text-[#6B7280]">
              <span>{course.progress}% hoan thanh</span>
              <span>{completedLessons}/{totalLessons} bai</span>
            </div>
            <div className="mt-2 h-2 rounded-full bg-[#E5E7EB]">
              <div className="h-2 rounded-full bg-lh-blue" style={{ width: `${course.progress}%` }} />
            </div>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-[1600px] gap-6 px-4 py-6 sm:px-6 xl:px-8">
        <main className="min-w-0 flex-1 xl:basis-[70%]">
          <section className="overflow-hidden rounded-[28px] border border-[#E8ECF5] bg-white shadow-[0_1px_2px_rgba(16,24,40,0.02),0_16px_32px_rgba(16,24,40,0.06)]">
            <div className="relative aspect-video bg-[#0F172A]">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(75,99,240,0.32),_transparent_48%),linear-gradient(135deg,#0f172a_0%,#141b34_45%,#1f2856_100%)]" />
              <div className="absolute inset-0 flex flex-col justify-between p-6 sm:p-8">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-white/85 backdrop-blur-sm">
                      <PlayCircle className="h-3.5 w-3.5" />
                      Dang hoc
                    </div>
                    <div className="mt-4 max-w-3xl font-inter text-2xl font-semibold tracking-[-0.02em] text-white sm:text-3xl">
                      {activeLesson.title}
                    </div>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-white/72">{activeLesson.summary}</p>
                  </div>
                  <div className="hidden rounded-2xl border border-white/10 bg-white/8 px-4 py-3 text-right text-white/80 backdrop-blur-sm sm:block">
                    <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-white/50">Giang vien</div>
                    <div className="mt-2 text-sm font-semibold text-white">{course.instructor}</div>
                  </div>
                </div>

                <div>
                  <div className="mb-4 flex items-center justify-between text-xs font-semibold text-white/70">
                    <span>{activeChapter?.title}</span>
                    <span>{activeLesson.duration}</span>
                  </div>
                  <div className="h-1.5 rounded-full bg-white/15">
                    <div className="h-1.5 rounded-full bg-lh-pink" style={{ width: '38%' }} />
                  </div>
                  <div className="mt-4 flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3 text-white/75">
                      <button className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-white/10 backdrop-blur-sm">
                        <PlayCircle className="h-5 w-5" />
                      </button>
                      <div>
                        <div className="text-sm font-semibold text-white">Tiep tuc bai hoc</div>
                        <div className="mt-1 text-xs text-white/60">Trang thai player duoc thiet ke de tap trung vao mot bai hoc tai mot thoi diem.</div>
                      </div>
                    </div>
                    <button className="hidden rounded-xl border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/15 sm:inline-flex">
                      Danh dau da hoc
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div className="border-t border-[#EEF1F5] bg-[#FBFCFE]">
              <div className="flex overflow-x-auto px-3 pt-3 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                {tabItems.map((tab) => {
                  const Icon = tab.icon;
                  const active = activeTab === tab.key;

                  return (
                    <button
                      key={tab.key}
                      onClick={() => setActiveTab(tab.key)}
                      className={cn(
                        'inline-flex items-center gap-2 rounded-t-2xl px-4 py-3 text-sm font-semibold transition',
                        active ? 'bg-white text-lh-blue' : 'text-[#6B7280] hover:text-[#111827]'
                      )}
                    >
                      <Icon className="h-4 w-4" />
                      {tab.label}
                    </button>
                  );
                })}
              </div>
              <div className="border-t border-[#EEF1F5] p-4 sm:p-5">{renderTabContent()}</div>
            </div>
          </section>
        </main>

        <aside className="hidden h-[calc(100vh-120px)] min-w-[340px] max-w-[380px] flex-col overflow-hidden rounded-[28px] border border-[#E8ECF5] bg-[#FCFDFE] shadow-[0_1px_2px_rgba(16,24,40,0.02),0_16px_32px_rgba(16,24,40,0.06)] xl:flex xl:basis-[30%]">
          {sidebarContent}
        </aside>
      </div>

      <AnimatePresence>
        {sidebarOpen ? (
          <>
            <motion.div
              className="fixed inset-0 z-40 bg-[#111827]/35 xl:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSidebarOpen(false)}
            />
            <motion.aside
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ duration: 0.24, ease: 'easeOut' }}
              className="fixed right-0 top-0 z-50 h-full w-full max-w-[420px] overflow-hidden border-l border-[#E8ECF5] bg-[#FCFDFE] shadow-[0_20px_40px_rgba(16,24,40,0.18)] xl:hidden"
            >
              <div className="flex items-center justify-between border-b border-[#E8ECF5] px-5 py-4">
                <div>
                  <div className="text-sm font-semibold text-[#111827]">Noi dung khoa hoc</div>
                  <div className="mt-1 text-xs text-[#6B7280]">{course.title}</div>
                </div>
                <button
                  onClick={() => setSidebarOpen(false)}
                  className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-[#374151]"
                >
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>
              <div className="h-[calc(100%-73px)]">{sidebarContent}</div>
            </motion.aside>
          </>
        ) : null}
      </AnimatePresence>
    </div>
  );
};

export default CoursePlayerPage;
