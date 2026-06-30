import React, { useRef, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ChevronDown,
  Clock3,
  Heart,
  Layers3,
  Star,
  Users,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { CoursePreviewPopover } from '../components/courses/CoursePreviewPopover';
import { catalogCourses } from '../data/courseCatalog';

const categories = [
  { name: 'Lap trinh', count: '12.400' },
  { name: 'Thiet ke', count: '8.100' },
  { name: 'Kinh doanh', count: '6.700' },
  { name: 'Marketing', count: '5.300' },
  { name: 'Du lieu & AI', count: '4.900' },
  { name: 'Nhiep anh', count: '3.200' },
];

const CatalogCourseCard: React.FC<{
  course: (typeof catalogCourses)[number];
  onNavigate: (courseId: number) => void;
}> = ({ course, onNavigate }) => {
  const cardRef = useRef<HTMLDivElement>(null);
  const [liked, setLiked] = useState(false);

  return (
    <>
      <div
        ref={cardRef}
        className="group relative rounded-2xl border border-lh-border bg-white transition duration-200 hover:-translate-y-1 hover:border-[#D9DEF2] hover:shadow-[0_18px_40px_rgba(21,22,46,0.08)]"
      >
        <div
          role="button"
          tabIndex={0}
          onClick={() => onNavigate(course.id)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              onNavigate(course.id);
            }
          }}
          className="w-full cursor-pointer text-left"
        >
          <div className="relative h-40 overflow-hidden rounded-t-2xl" style={{ backgroundColor: course.color }}>
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.18),_transparent_48%)]" />
            <div className="absolute left-4 top-4 inline-flex items-center rounded-full bg-white/15 px-2.5 py-1 text-[11px] font-semibold text-white backdrop-blur-sm">
              {course.badge}
            </div>
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-white/18 backdrop-blur-sm">
                <div className="ml-1 h-0 w-0 border-b-[8px] border-l-[12px] border-t-[8px] border-b-transparent border-l-white border-t-transparent" />
              </div>
            </div>
          </div>

          <div className="p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="line-clamp-2 text-lg font-bold leading-7 text-[#111827]">{course.title}</div>
                <div className="mt-2 text-sm text-lh-muted">
                  {course.instructor} · {course.category}
                </div>
              </div>
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  setLiked((current) => !current);
                }}
                className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#E5E7EB] text-[#9CA3AF] transition hover:bg-[#F9FAFB]"
              >
                <Heart className={liked ? 'h-4.5 w-4.5 fill-[#F64C72] text-[#F64C72]' : 'h-4.5 w-4.5'} />
              </button>
            </div>

            <div className="mt-4 flex flex-wrap gap-3 text-xs font-medium text-[#6B7280]">
              <div className="inline-flex items-center gap-1.5">
                <Layers3 className="h-3.5 w-3.5 text-lh-blue" />
                {course.level}
              </div>
              <div className="inline-flex items-center gap-1.5">
                <Clock3 className="h-3.5 w-3.5 text-lh-blue" />
                {course.totalHours}
              </div>
            </div>

            <div className="mt-4 flex items-center gap-1.5">
              <span className="text-sm font-bold text-amber-700">{course.rating}</span>
              <Star className="h-4 w-4 fill-[#F6A609] text-[#F6A609]" />
              <span className="text-xs text-lh-muted">({course.reviews})</span>
            </div>

            <p className="mt-4 line-clamp-2 text-sm leading-6 text-[#4B5563]">{course.summary}</p>

            <div className="mt-5 flex items-end justify-between gap-4">
              <div className="flex items-center gap-2">
                <span className="text-2xl font-black tracking-tight text-lh-navy">{course.price}</span>
                <span className="text-sm text-lh-muted line-through">{course.oldPrice}</span>
              </div>
              <div className="inline-flex items-center gap-1.5 text-xs font-medium text-[#6B7280]">
                <Users className="h-3.5 w-3.5 text-lh-blue" />
                {course.reviews}
              </div>
            </div>
          </div>
        </div>
      </div>

      <CoursePreviewPopover course={course} anchorRef={cardRef} onNavigate={onNavigate} />
    </>
  );
};

const CourseCatalogPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#F7F8FC]">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="text-xs font-semibold text-lh-muted">
          <button onClick={() => navigate('/')} className="transition hover:text-lh-navy">
            Trang chu
          </button>{' '}
          › <span className="text-lh-navy">Tat ca khoa hoc</span>
        </div>

        <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="font-inter text-4xl font-semibold tracking-[-0.03em] text-[#111827]">
              Kham pha khoa hoc phu hop cho muc tieu tiep theo
            </h1>
            <p className="mt-3 max-w-3xl text-base leading-7 text-[#6B7280]">
              Re preview khi hover de xem nhanh noi dung, muc tieu va muc do phu hop truoc khi vao trang chi tiet.
            </p>
          </div>
          <div className="inline-flex items-center gap-2 rounded-2xl border border-[#E5E7EB] bg-white px-4 py-3 text-sm font-medium text-[#4B5563]">
            <span>{catalogCourses.length} khoa hoc dang mo ban</span>
          </div>
        </div>

        <div className="mt-8 grid gap-8 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="space-y-5">
            <div className="rounded-2xl border border-[#E7E9F2] bg-white p-5 shadow-[0_10px_30px_rgba(21,22,46,0.04)]">
              <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Danh muc</div>
              <div className="mt-4 space-y-3">
                {categories.map((category) => (
                  <label
                    key={category.name}
                    className="flex cursor-pointer items-center gap-3 rounded-xl px-2 py-1.5 text-sm font-medium text-[#111827] transition hover:bg-[#F8FAFF]"
                  >
                    <input type="checkbox" className="h-4 w-4 rounded border-[#D1D5DB] text-lh-blue focus:ring-lh-blue" />
                    <span>{category.name}</span>
                    <span className="ml-auto text-xs text-lh-muted">{category.count}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="rounded-2xl border border-[#E7E9F2] bg-white p-5 shadow-[0_10px_30px_rgba(21,22,46,0.04)]">
              <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Trinh do</div>
              <div className="mt-4 space-y-3">
                {['Tat ca trinh do', 'Co ban', 'Trung cap', 'Nang cao'].map((level) => (
                  <label
                    key={level}
                    className="flex cursor-pointer items-center gap-3 rounded-xl px-2 py-1.5 text-sm font-medium text-[#111827] transition hover:bg-[#F8FAFF]"
                  >
                    <input type="radio" name="level" className="h-4 w-4 border-[#D1D5DB] text-lh-blue focus:ring-lh-blue" />
                    {level}
                  </label>
                ))}
              </div>
            </div>

            <div className="rounded-2xl border border-[#E7E9F2] bg-white p-5 shadow-[0_10px_30px_rgba(21,22,46,0.04)]">
              <div className="text-xs font-bold uppercase tracking-[0.16em] text-lh-muted">Danh gia</div>
              <div className="mt-4 space-y-3">
                {['4.5 tro len', '4.0 tro len'].map((label) => (
                  <label
                    key={label}
                    className="flex cursor-pointer items-center gap-3 rounded-xl px-2 py-1.5 text-sm font-medium text-[#111827] transition hover:bg-[#F8FAFF]"
                  >
                    <input type="radio" name="rating" className="h-4 w-4 border-[#D1D5DB] text-lh-blue focus:ring-lh-blue" />
                    <span className="inline-flex items-center gap-1">
                      <Star className="h-3.5 w-3.5 fill-[#F6A609] text-[#F6A609]" />
                      {label}
                    </span>
                  </label>
                ))}
              </div>
            </div>
          </aside>

          <div className="min-w-0">
            <div className="mb-6 flex flex-col gap-3 rounded-2xl border border-[#E7E9F2] bg-white px-5 py-4 shadow-[0_10px_30px_rgba(21,22,46,0.04)] sm:flex-row sm:items-center sm:justify-between">
              <div className="text-sm font-medium text-[#6B7280]">
                Hover vao tung the khoa hoc de xem nhanh noi dung va them vao gio hang.
              </div>
              <button className="inline-flex items-center gap-2 rounded-xl border border-[#E5E7EB] px-4 py-2.5 text-sm font-semibold text-[#111827] transition hover:bg-[#F9FAFB]">
                Pho bien nhat
                <ChevronDown className="h-4 w-4" />
              </button>
            </div>

            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
              {catalogCourses.map((course, index) => (
                <motion.div
                  key={course.id}
                  initial={{ opacity: 0, y: 18 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.2, delay: index * 0.04, ease: 'easeOut' }}
                >
                  <CatalogCourseCard course={course} onNavigate={(courseId) => navigate(`/courses/${courseId}`)} />
                </motion.div>
              ))}
            </div>

            <div className="mt-10 flex justify-center gap-2">
              <button className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-sm font-semibold text-lh-muted">
                1
              </button>
              <button className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-[#D9DEF2] bg-[#EEF2FF] text-sm font-semibold text-lh-blue">
                2
              </button>
              <button className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-[#E5E7EB] bg-white text-sm font-semibold text-lh-muted">
                3
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CourseCatalogPage;
