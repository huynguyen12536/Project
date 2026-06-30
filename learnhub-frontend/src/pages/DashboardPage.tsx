import React from 'react';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  Award,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Compass,
  Flame,
  MessageSquareText,
  PlayCircle,
  Sparkles,
  Target,
  TrendingUp,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { cn } from '../lib/cn';

type DashboardCourse = {
  id: number;
  title: string;
  instructor: string;
  progress: number;
  completedLessons: number;
  totalLessons: number;
  currentLesson: string;
  category: string;
  durationLeft: string;
  accent: 'blue' | 'pink' | 'purple' | 'mauve';
};

const enrolledCourses: DashboardCourse[] = [
  {
    id: 1,
    title: 'Lap trinh Web Full-Stack 2026',
    instructor: 'Tran Minh Quan',
    progress: 68,
    completedLessons: 12,
    totalLessons: 45,
    currentLesson: 'Xay dung API auth voi JWT va refresh flow',
    category: 'Lap trinh',
    durationLeft: '28 phut de hoan thanh bai hoc hien tai',
    accent: 'blue',
  },
  {
    id: 2,
    title: 'UI/UX Design tu A den Z',
    instructor: 'Le Thu Ha',
    progress: 42,
    completedLessons: 9,
    totalLessons: 28,
    currentLesson: 'Design system cho product dashboard',
    category: 'Thiet ke',
    durationLeft: '19 phut cho bai tiep theo',
    accent: 'pink',
  },
  {
    id: 3,
    title: 'Phan tich du lieu voi Python',
    instructor: 'Pham Duc Anh',
    progress: 83,
    completedLessons: 24,
    totalLessons: 30,
    currentLesson: 'Tien xu ly du lieu voi Pandas',
    category: 'Du lieu',
    durationLeft: '34 phut de ket thuc module',
    accent: 'purple',
  },
  {
    id: 4,
    title: 'React va Redux Toolkit',
    instructor: 'Nguyen Hoang',
    progress: 25,
    completedLessons: 5,
    totalLessons: 21,
    currentLesson: 'State management cho ung dung lon',
    category: 'Frontend',
    durationLeft: '23 phut cho bai hoc tiep theo',
    accent: 'mauve',
  },
];

const statCards = [
  {
    label: 'So gio da hoc',
    value: '126h',
    note: '+8h trong 7 ngay qua',
    icon: Clock3,
    tone: 'blue',
  },
  {
    label: 'Streak hien tai',
    value: '14 ngay',
    note: 'Chi con 1 ngay nua de lap ky luc moi',
    icon: Flame,
    tone: 'pink',
  },
  {
    label: 'Chung chi da dat',
    value: '6',
    note: '2 chung chi dang cho hoan thanh',
    icon: Award,
    tone: 'purple',
  },
] as const;

const weeklyGoals = [
  { title: 'Hoan thanh module Auth va Security', status: 'Dang dung tien do', progress: 72 },
  { title: 'Xem lai bai Redux Toolkit', status: 'Can 1 buoi hoc nua', progress: 54 },
  { title: 'Lam quiz cuoi tuan', status: 'Chua bat dau', progress: 0 },
];

const communityHighlights = [
  {
    title: 'Thread: Cach toi uu hoa project React lon',
    meta: '18 binh luan moi',
  },
  {
    title: 'Workshop online: Xay API ben vung voi Spring Boot',
    meta: 'Bat dau luc 20:00 toi nay',
  },
];

const suggestedCourses = [
  {
    id: 7,
    title: 'He thong thiet ke cho SaaS Product',
    instructor: 'Le Thu Ha',
    category: 'Thiet ke',
    summary: 'Tu wireframe den design token, quy trinh tao giao dien dong nhat cho product team.',
    price: '329.000d',
    badge: 'New',
    accent: 'pink',
  },
  {
    id: 8,
    title: 'Data Storytelling cho Product Analyst',
    instructor: 'Pham Duc Anh',
    category: 'Du lieu',
    summary: 'Bien dashboard thanh cau chuyen de bao cao hieu qua va thuyet phuc stakeholder.',
    price: '389.000d',
    badge: 'Cap nhat moi',
    accent: 'blue',
  },
  {
    id: 9,
    title: 'DevOps can ban cho backend engineer',
    instructor: 'Nguyen Hoang',
    category: 'Backend',
    summary: 'Docker, CI/CD va monitoring co ban de dua du an len moi truong that.',
    price: '449.000d',
    badge: 'Moi',
    accent: 'mauve',
  },
  {
    id: 10,
    title: 'AI Prompting thuc chien cho team van hanh',
    instructor: 'Tran Gia Bao',
    category: 'AI',
    summary: 'Ung dung prompt framework vao quy trinh hoc tap, bao cao va lam viec hang ngay.',
    price: '279.000d',
    badge: 'Ban chay',
    accent: 'purple',
  },
] as const;

const accentMap = {
  blue: {
    tint: 'bg-[#EEF2FF]',
    text: 'text-[#3B5CCC]',
    line: 'bg-[#4B63F0]',
    chip: 'bg-[#EEF2FF] text-[#3B5CCC]',
  },
  pink: {
    tint: 'bg-[#FFF1F5]',
    text: 'text-[#D9447A]',
    line: 'bg-[#F06495]',
    chip: 'bg-[#FFF1F5] text-[#D9447A]',
  },
  purple: {
    tint: 'bg-[#F4F1FF]',
    text: 'text-[#6D50C9]',
    line: 'bg-[#7C63E8]',
    chip: 'bg-[#F4F1FF] text-[#6D50C9]',
  },
  mauve: {
    tint: 'bg-[#F4F2F8]',
    text: 'text-[#7C6C96]',
    line: 'bg-[#8C7BA8]',
    chip: 'bg-[#F4F2F8] text-[#7C6C96]',
  },
} as const;

const cardMotion = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
};

function getGreetingByHour(date: Date): string {
  const hour = date.getHours();
  if (hour < 12) return 'Chao buoi sang';
  if (hour < 18) return 'Chao buoi chieu';
  return 'Chao buoi toi';
}

function getDisplayName(name?: string, email?: string): string {
  if (name && name.trim().length > 0) {
    return name.trim();
  }
  if (email) {
    return email.split('@')[0];
  }
  return 'ban';
}

const SurfaceCard: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className }) => (
  <div
    className={cn(
      'rounded-2xl border border-[#E9ECF3] bg-white p-6 shadow-[0_1px_2px_rgba(16,24,40,0.02),0_10px_24px_rgba(16,24,40,0.04)]',
      className
    )}
  >
    {children}
  </div>
);

const ProgressTrack: React.FC<{ value: number; accent: keyof typeof accentMap; thin?: boolean }> = ({
  value,
  accent,
  thin = false,
}) => (
  <div className={cn('w-full rounded-full bg-[#ECEFF5]', thin ? 'h-1.5' : 'h-2')}>
    <div
      className={cn('h-full rounded-full transition-all duration-500', accentMap[accent].line)}
      style={{ width: `${Math.max(0, Math.min(100, value))}%` }}
    />
  </div>
);

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);

  const now = new Date();
  const userName = getDisplayName(
    [user?.firstName, user?.lastName].filter(Boolean).join(' '),
    user?.email
  );
  const greeting = getGreetingByHour(now);
  const formattedDate = new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(now);
  const featuredCourse = enrolledCourses[0];
  const featuredAccent = accentMap[featuredCourse.accent];

  return (
    <div className="min-h-full bg-[#F9FAFB]">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        <motion.section
          variants={cardMotion}
          initial="initial"
          animate="animate"
          transition={{ duration: 0.22, ease: 'easeOut' }}
          className="mb-6"
        >
          <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full border border-[#E8EBF2] bg-white px-3 py-1 text-xs font-semibold text-lh-muted">
                <Sparkles className="h-3.5 w-3.5 text-lh-blue" />
                Hoc tap ca nhan hoa
              </div>
              <h1 className="mt-4 font-inter text-[28px] font-semibold tracking-[-0.02em] text-[#111827] sm:text-[32px]">
                {greeting}, {userName}
              </h1>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-[#6B7280]">
                Hom nay ban muon hoc them dieu gi moi? Mot buoi hoc ngan va lien tuc se giup ban giu nhip tien bo on dinh hon.
              </p>
            </div>
            <div className="inline-flex items-center gap-2 rounded-full border border-[#E8EBF2] bg-white px-4 py-2 text-sm font-medium text-[#4B5563]">
              <CalendarDays className="h-4 w-4 text-lh-blue" />
              {formattedDate}
            </div>
          </div>
        </motion.section>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.65fr)_340px]">
          <div className="space-y-6">
            <motion.section
              variants={cardMotion}
              initial="initial"
              animate="animate"
              transition={{ duration: 0.26, delay: 0.04, ease: 'easeOut' }}
            >
              <SurfaceCard className="p-0 overflow-hidden">
                <div className="grid gap-0 lg:grid-cols-[1.45fr_0.75fr]">
                  <div className="p-6 sm:p-7">
                    <div className="flex flex-wrap items-center gap-3">
                      <span className={cn('inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold', featuredAccent.chip)}>
                        <PlayCircle className="h-3.5 w-3.5" />
                        Tiep tuc hoc
                      </span>
                      <span className="text-sm text-[#6B7280]">{featuredCourse.category}</span>
                    </div>

                    <h2 className="mt-5 max-w-2xl font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827] sm:text-[28px]">
                      {featuredCourse.title}
                    </h2>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-[#6B7280]">
                      Ban dang hoc toi bai: {featuredCourse.currentLesson}. He thong giu san diem tiep noi de ban quay lai ngay ma khong can tim lai.
                    </p>

                    <div className="mt-6 grid gap-4 sm:grid-cols-3">
                      <div className="rounded-xl border border-[#EEF1F5] bg-[#FBFCFE] p-4">
                        <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                          Tien do
                        </div>
                        <div className="mt-2 font-inter text-3xl font-semibold text-[#111827]">
                          {featuredCourse.progress}%
                        </div>
                      </div>
                      <div className="rounded-xl border border-[#EEF1F5] bg-[#FBFCFE] p-4">
                        <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                          Bai da hoc
                        </div>
                        <div className="mt-2 font-inter text-3xl font-semibold text-[#111827]">
                          {featuredCourse.completedLessons}/{featuredCourse.totalLessons}
                        </div>
                      </div>
                      <div className="rounded-xl border border-[#EEF1F5] bg-[#FBFCFE] p-4">
                        <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                          Thoi gian tiep theo
                        </div>
                        <div className="mt-2 text-sm font-medium leading-6 text-[#111827]">
                          {featuredCourse.durationLeft}
                        </div>
                      </div>
                    </div>

                    <div className="mt-6">
                      <ProgressTrack value={featuredCourse.progress} accent={featuredCourse.accent} />
                    </div>

                    <div className="mt-6 flex flex-wrap gap-3">
                      <button
                        onClick={() => navigate(`/learn/courses/${featuredCourse.id}`)}
                        className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#111827] px-5 text-sm font-semibold text-white transition hover:bg-[#1F2937]"
                      >
                        Hoc tiep
                        <ArrowRight className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => navigate('/courses')}
                        className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-5 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]"
                      >
                        Xem tat ca khoa hoc
                      </button>
                    </div>
                  </div>

                  <div className="border-l border-[#EEF1F5] bg-[#FCFCFD] p-6 sm:p-7">
                    <div className="rounded-2xl border border-[#EEF1F5] bg-white p-5">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                            Bai hoc hien tai
                          </div>
                          <div className="mt-2 text-sm font-medium text-[#6B7280]">{featuredCourse.instructor}</div>
                        </div>
                        <div className={cn('rounded-xl p-3', featuredAccent.tint)}>
                          <BookOpen className={cn('h-5 w-5', featuredAccent.text)} />
                        </div>
                      </div>

                      <div className="mt-5 rounded-2xl border border-[#EEF1F5] bg-[#FAFBFC] p-4">
                        <div className={cn('inline-flex rounded-full px-3 py-1 text-xs font-semibold', featuredAccent.chip)}>
                          Dang hoc
                        </div>
                        <div className="mt-4 text-lg font-semibold leading-7 text-[#111827]">
                          {featuredCourse.currentLesson}
                        </div>
                        <div className="mt-4 flex items-center gap-2 text-sm text-[#6B7280]">
                          <CheckCircle2 className="h-4 w-4 text-lh-blue" />
                          Danh dau ghi chu trong bai nay de on lai cuoi tuan.
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </SurfaceCard>
            </motion.section>

            <motion.section
              variants={cardMotion}
              initial="initial"
              animate="animate"
              transition={{ duration: 0.26, delay: 0.07, ease: 'easeOut' }}
              className="grid gap-4 sm:grid-cols-3"
            >
              {statCards.map((stat) => {
                const Icon = stat.icon;
                const tone = accentMap[stat.tone];

                return (
                  <SurfaceCard key={stat.label}>
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="text-sm font-medium text-[#6B7280]">{stat.label}</div>
                        <div className="mt-3 font-inter text-[30px] font-semibold tracking-[-0.02em] text-[#111827]">
                          {stat.value}
                        </div>
                      </div>
                      <div className={cn('rounded-xl p-3', tone.tint)}>
                        <Icon className={cn('h-5 w-5', tone.text)} />
                      </div>
                    </div>
                    <div className="mt-4 text-sm leading-6 text-[#6B7280]">{stat.note}</div>
                  </SurfaceCard>
                );
              })}
            </motion.section>

            <motion.section
              id="my-courses"
              variants={cardMotion}
              initial="initial"
              animate="animate"
              transition={{ duration: 0.3, delay: 0.1, ease: 'easeOut' }}
            >
              <SurfaceCard>
                <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">Khoa hoc cua toi</div>
                    <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                      Tiep tuc tien do hoc tap
                    </h2>
                  </div>
                  <button
                    onClick={() => navigate('/courses')}
                    className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 py-2.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
                  >
                    Mo catalog
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  {enrolledCourses.map((course, index) => {
                    const accent = accentMap[course.accent];

                    return (
                      <motion.button
                        key={course.id}
                        whileHover={{ y: -2 }}
                        whileTap={{ scale: 0.995 }}
                        onClick={() => navigate(`/learn/courses/${course.id}`)}
                        className="flex items-start gap-4 rounded-2xl border border-[#EAECEF] bg-white p-4 text-left transition hover:border-[#D9DEE8] hover:shadow-[0_8px_18px_rgba(16,24,40,0.05)]"
                      >
                        <div className={cn('flex h-14 w-14 flex-none items-center justify-center rounded-2xl', accent.tint)}>
                          <span className={cn('text-sm font-semibold', accent.text)}>{String(index + 1).padStart(2, '0')}</span>
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className={cn('inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold', accent.chip)}>
                              {course.category}
                            </span>
                            <span className="text-xs text-[#9CA3AF]">{course.instructor}</span>
                          </div>
                          <div className="mt-3 line-clamp-2 text-base font-semibold leading-6 text-[#111827]">
                            {course.title}
                          </div>
                          <div className="mt-2 line-clamp-1 text-sm text-[#6B7280]">{course.currentLesson}</div>
                          <div className="mt-4">
                            <ProgressTrack value={course.progress} accent={course.accent} thin />
                          </div>
                          <div className="mt-3 flex items-center justify-between gap-3 text-xs text-[#6B7280]">
                            <span>{course.completedLessons}/{course.totalLessons} bai</span>
                            <span>{course.progress}%</span>
                          </div>
                        </div>
                      </motion.button>
                    );
                  })}
                </div>
              </SurfaceCard>
            </motion.section>
          </div>

          <div className="space-y-6">
            <motion.aside
              id="roadmap"
              variants={cardMotion}
              initial="initial"
              animate="animate"
              transition={{ duration: 0.3, delay: 0.12, ease: 'easeOut' }}
            >
              <SurfaceCard>
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">Roadmap</div>
                    <h3 className="mt-2 font-inter text-xl font-semibold tracking-[-0.02em] text-[#111827]">
                      Muc tieu trong tuan
                    </h3>
                  </div>
                  <div className="rounded-xl bg-[#EEF2FF] p-3">
                    <Target className="h-5 w-5 text-lh-blue" />
                  </div>
                </div>
                <div className="mt-5 space-y-4">
                  {weeklyGoals.map((goal) => (
                    <div key={goal.title} className="rounded-2xl border border-[#EEF1F5] bg-[#FBFCFE] p-4">
                      <div className="text-sm font-semibold text-[#111827]">{goal.title}</div>
                      <div className="mt-2 text-sm text-[#6B7280]">{goal.status}</div>
                      <div className="mt-4">
                        <ProgressTrack
                          value={goal.progress}
                          accent={goal.progress > 60 ? 'blue' : goal.progress > 20 ? 'pink' : 'mauve'}
                          thin
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </SurfaceCard>
            </motion.aside>

            <motion.aside
              id="community"
              variants={cardMotion}
              initial="initial"
              animate="animate"
              transition={{ duration: 0.3, delay: 0.16, ease: 'easeOut' }}
            >
              <SurfaceCard>
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">Community Pulse</div>
                    <h3 className="mt-2 font-inter text-xl font-semibold tracking-[-0.02em] text-[#111827]">
                      Cap nhat cong dong
                    </h3>
                  </div>
                  <div className="rounded-xl bg-[#FFF1F5] p-3">
                    <MessageSquareText className="h-5 w-5 text-lh-pink" />
                  </div>
                </div>
                <div className="mt-5 space-y-4">
                  {communityHighlights.map((item) => (
                    <div key={item.title} className="rounded-2xl border border-[#EEF1F5] bg-white p-4">
                      <div className="text-sm font-semibold leading-6 text-[#111827]">{item.title}</div>
                      <div className="mt-2 flex items-center gap-2 text-sm text-[#6B7280]">
                        <TrendingUp className="h-4 w-4 text-lh-blue" />
                        {item.meta}
                      </div>
                    </div>
                  ))}
                  <div className="rounded-2xl border border-[#EEF1F5] bg-[#FBFCFE] p-4">
                    <div className="text-sm font-semibold text-[#111827]">De xuat tiep theo</div>
                    <p className="mt-2 text-sm leading-6 text-[#6B7280]">
                      Sau khi hoan thanh khoa Full-Stack, ban nen hoc tiep module Deploy va Monitoring de co mot project thuc te hoan chinh hon.
                    </p>
                  </div>
                </div>
              </SurfaceCard>
            </motion.aside>
          </div>
        </div>

        <motion.section
          variants={cardMotion}
          initial="initial"
          animate="animate"
          transition={{ duration: 0.32, delay: 0.2, ease: 'easeOut' }}
          className="mt-6"
        >
          <SurfaceCard>
            <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <div className="text-sm font-medium text-[#6B7280]">Kham pha cac ky nang moi</div>
                <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                  Goi y khoa hoc phu hop voi muc tieu tiep theo
                </h2>
                <p className="mt-2 max-w-3xl text-sm leading-6 text-[#6B7280]">
                  Day la cac khoa hoc chua mua, duoc goi y de ban mo rong ky nang tu dashboard ma khong phai quay ve mot trang khac.
                </p>
              </div>
              <button
                onClick={() => navigate('/courses')}
                className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-4 py-2.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
              >
                <Compass className="h-4 w-4" />
                Xem tat ca khoa hoc
              </button>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {suggestedCourses.map((course) => {
                const accent = accentMap[course.accent];

                return (
                  <motion.div
                    key={course.id}
                    whileHover={{ y: -3 }}
                    className="rounded-2xl border border-[#EAECEF] bg-white p-5 shadow-[0_1px_2px_rgba(16,24,40,0.02),0_10px_24px_rgba(16,24,40,0.04)]"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className={cn('rounded-xl p-3', accent.tint)}>
                        <Compass className={cn('h-5 w-5', accent.text)} />
                      </div>
                      <span className={cn('rounded-full px-2.5 py-1 text-[11px] font-semibold', accent.chip)}>
                        {course.badge}
                      </span>
                    </div>

                    <div className="mt-4">
                      <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9CA3AF]">
                        {course.category}
                      </div>
                      <div className="mt-2 min-h-[56px] text-lg font-semibold leading-7 text-[#111827]">
                        {course.title}
                      </div>
                      <div className="mt-2 text-sm text-[#6B7280]">{course.instructor}</div>
                    </div>

                    <p className="mt-4 min-h-[72px] text-sm leading-6 text-[#6B7280]">{course.summary}</p>

                    <div className="mt-5 flex items-center justify-between gap-3">
                      <div className="text-base font-semibold text-[#111827]">{course.price}</div>
                      <button
                        onClick={() => navigate(`/courses/${course.id}`)}
                        className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-3.5 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]"
                      >
                        Xem chi tiet
                        <ArrowRight className="h-4 w-4" />
                      </button>
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </SurfaceCard>
        </motion.section>
      </div>
    </div>
  );
};

export default DashboardPage;
