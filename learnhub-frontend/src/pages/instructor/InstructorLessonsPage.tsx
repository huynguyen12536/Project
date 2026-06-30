import React, { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
    BarChart3,
    BookOpen,
    ChevronRight,
    Clock,
    Video,
    FileText,
    NotebookPen,
    Search,
    Filter,
    MoreHorizontal,
} from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { cn } from '../../lib/cn';
import { instructorCoursesSeed, type InstructorCourse, type InstructorLecture, type InstructorLectureType } from '../../data/courseCatalog';

const sidebarItems: Array<{
    label: string;
    icon: typeof BookOpen;
    path: string;
}> = [
        { label: 'Tong quan', icon: BarChart3, path: '/instructor/dashboard' },
        { label: 'Khoa hoc cua toi', icon: BookOpen, path: '/instructor/courses' },
        { label: 'Bai hoc', icon: Clock, path: '/instructor/lessons' },
        { label: 'Hoc vien', icon: BarChart3, path: '/instructor/students' },
        { label: 'Danh gia', icon: BarChart3, path: '/instructor/reviews' },
    ];

const lectureTypeMeta: Record<
    InstructorLectureType,
    { label: string; icon: typeof Video; tone: string }
> = {
    video: { label: 'Video', icon: Video, tone: 'bg-[#EEF2FF] text-lh-blue' },
    article: { label: 'Bai viet', icon: FileText, tone: 'bg-[#FFF1F5] text-lh-pink' },
    quiz: { label: 'Quiz', icon: NotebookPen, tone: 'bg-[#F4F2F8] text-lh-purple' },
};

type LessonWithCourse = InstructorLecture & {
    courseId: string;
    courseTitle: string;
    sectionTitle: string;
};

const InstructorLessonsPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
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
        return allLessons.filter((lesson) => {
            const matchesSearch = lesson.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                lesson.courseTitle.toLowerCase().includes(searchQuery.toLowerCase());
            const matchesType = filterType === 'all' || lesson.type === filterType;
            return matchesSearch && matchesType;
        });
    }, [allLessons, searchQuery, filterType]);

    const stats = useMemo(() => {
        const videoCount = allLessons.filter(l => l.type === 'video').length;
        const articleCount = allLessons.filter(l => l.type === 'article').length;
        const quizCount = allLessons.filter(l => l.type === 'quiz').length;
        return {
            total: allLessons.length,
            video: videoCount,
            article: articleCount,
            quiz: quizCount,
        };
    }, [allLessons]);

    return (
        <div className="min-h-screen bg-[#F7F8FC]">
            <div className="mx-auto max-w-[1600px] px-6 py-6 xl:px-8">
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
                                Quan ly tat ca bai hoc
                            </h1>
                        </div>
                    </div>
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
                        <div className="grid gap-4 md:grid-cols-4">
                            {[
                                {
                                    label: 'Tong bai hoc',
                                    value: stats.total,
                                    icon: Clock,
                                    tone: 'bg-[#111827] text-white',
                                },
                                {
                                    label: 'Video',
                                    value: stats.video,
                                    icon: Video,
                                    tone: 'bg-[#EEF2FF] text-lh-blue',
                                },
                                {
                                    label: 'Bai viet',
                                    value: stats.article,
                                    icon: FileText,
                                    tone: 'bg-[#FFF1F5] text-lh-pink',
                                },
                                {
                                    label: 'Quiz',
                                    value: stats.quiz,
                                    icon: NotebookPen,
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
                                    </div>
                                );
                            })}
                        </div>

                        <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
                            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                                <div>
                                    <div className="text-sm font-medium text-[#6B7280]">Danh sach</div>
                                    <h2 className="mt-2 font-inter text-2xl font-semibold tracking-[-0.02em] text-[#111827]">
                                        Tat ca bai hoc da tai len
                                    </h2>
                                </div>
                                <div className="flex flex-wrap gap-3">
                                    <div className="relative">
                                        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
                                        <input
                                            type="text"
                                            placeholder="Tim kiem bai hoc..."
                                            value={searchQuery}
                                            onChange={(e) => setSearchQuery(e.target.value)}
                                            className="w-full rounded-xl border border-[#E5E7EB] pl-10 pr-4 py-2.5 text-sm text-[#111827] placeholder:text-[#9CA3AF] focus:border-lh-blue focus:outline-none"
                                        />
                                    </div>
                                    <div className="flex items-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3 py-2">
                                        <Filter className="h-4 w-4 text-[#6B7280]" />
                                        <select
                                            value={filterType}
                                            onChange={(e) => setFilterType(e.target.value as any)}
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
                                            transition={{ duration: 0.3, delay: index * 0.05 }}
                                            className="rounded-[20px] border border-[#ECEFF5] bg-white p-5 transition hover:border-[#D9DEF2] hover:shadow-[0_8px_24px_rgba(47,47,162,0.08)]"
                                        >
                                            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                                                <div className="flex items-start gap-4 flex-1 min-w-0">
                                                    <div className={cn('flex h-12 w-12 items-center justify-center rounded-xl flex-shrink-0', meta.tone)}>
                                                        <Icon className="h-5 w-5" />
                                                    </div>
                                                    <div className="min-w-0">
                                                        <div className="flex items-center gap-2 flex-wrap">
                                                            <h3 className="text-lg font-semibold text-[#111827] truncate">{lesson.title}</h3>
                                                            {lesson.previewable && (
                                                                <span className="rounded-full bg-[#EEF8F2] px-2.5 py-1 text-[11px] font-semibold text-[#1F7A45]">
                                                                    Preview
                                                                </span>
                                                            )}
                                                        </div>
                                                        <div className="mt-1 flex items-center gap-2 text-sm text-[#6B7280]">
                                                            <span className="truncate">{lesson.courseTitle}</span>
                                                            <span>•</span>
                                                            <span className="truncate">{lesson.sectionTitle}</span>
                                                        </div>
                                                        <div className="mt-2 flex items-center gap-4">
                                                            <span className={cn(
                                                                'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-semibold',
                                                                meta.tone
                                                            )}>
                                                                <Icon className="h-3.5 w-3.5" />
                                                                {meta.label}
                                                            </span>
                                                            <span className="text-xs text-[#9CA3AF]">{lesson.duration}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-2 flex-shrink-0">
                                                    <button className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#374151] transition hover:bg-[#F9FAFB]">
                                                        Sua
                                                    </button>
                                                    <button className="inline-flex items-center gap-2 rounded-xl border border-[#D9DEF2] px-4 py-2 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF]">
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
                                {filteredLessons.length === 0 && (
                                    <div className="flex flex-col items-center justify-center py-16 text-center">
                                        <Clock className="h-16 w-16 text-[#D1D5DB]" />
                                        <h3 className="mt-4 text-lg font-semibold text-[#111827]">Khong tim thay bai hoc</h3>
                                        <p className="mt-2 text-sm text-[#6B7280]">Vui long thu lai voi tu khoa khac</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default InstructorLessonsPage;
