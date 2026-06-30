import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  BarChart3,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Heart,
  ShoppingCart,
} from 'lucide-react';
import { cn } from '../../lib/cn';
import type { CatalogCourse } from '../../data/courseCatalog';

type Position = {
  top: number;
  left: number;
  side: 'left' | 'right';
};

interface CoursePreviewPopoverProps {
  course: CatalogCourse;
  anchorRef: React.RefObject<HTMLElement>;
  onNavigate: (courseId: number) => void;
}

const PREVIEW_WIDTH = 360;
const PREVIEW_HEIGHT = 356;
const VIEWPORT_GAP = 16;
const OPEN_DELAY = 300;
const CLOSE_DELAY = 120;

export const CoursePreviewPopover: React.FC<CoursePreviewPopoverProps> = ({
  course,
  anchorRef,
  onNavigate,
}) => {
  const [visible, setVisible] = useState(false);
  const [hoveringCard, setHoveringCard] = useState(false);
  const [hoveringPopover, setHoveringPopover] = useState(false);
  const [position, setPosition] = useState<Position>({ top: 0, left: 0, side: 'right' });
  const openTimerRef = useRef<number | null>(null);
  const closeTimerRef = useRef<number | null>(null);

  const updatePosition = useMemo(
    () => () => {
      const rect = anchorRef.current?.getBoundingClientRect();
      if (!rect) return;

      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      const preferredRight = rect.right + VIEWPORT_GAP + PREVIEW_WIDTH <= viewportWidth - VIEWPORT_GAP;
      const side = preferredRight ? 'right' : 'left';
      const desiredLeft =
        side === 'right'
          ? rect.right + VIEWPORT_GAP
          : rect.left - PREVIEW_WIDTH - VIEWPORT_GAP;
      const desiredTop = rect.top - 16;

      setPosition({
        side,
        left: Math.max(VIEWPORT_GAP, Math.min(desiredLeft, viewportWidth - PREVIEW_WIDTH - VIEWPORT_GAP)),
        top: Math.max(VIEWPORT_GAP, Math.min(desiredTop, viewportHeight - PREVIEW_HEIGHT - VIEWPORT_GAP)),
      });
    },
    [anchorRef]
  );

  const clearOpenTimer = () => {
    if (openTimerRef.current) {
      window.clearTimeout(openTimerRef.current);
      openTimerRef.current = null;
    }
  };

  const clearCloseTimer = () => {
    if (closeTimerRef.current) {
      window.clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  };

  const scheduleOpen = () => {
    clearOpenTimer();
    clearCloseTimer();
    openTimerRef.current = window.setTimeout(() => {
      updatePosition();
      setVisible(true);
    }, OPEN_DELAY);
  };

  const scheduleClose = () => {
    clearOpenTimer();
    clearCloseTimer();
    closeTimerRef.current = window.setTimeout(() => {
      setVisible(false);
    }, CLOSE_DELAY);
  };

  useEffect(() => {
    const anchor = anchorRef.current;
    if (!anchor) return;

    const handleEnter = () => {
      setHoveringCard(true);
      scheduleOpen();
    };

    const handleLeave = () => {
      setHoveringCard(false);
      scheduleClose();
    };

    anchor.addEventListener('mouseenter', handleEnter);
    anchor.addEventListener('mouseleave', handleLeave);

    return () => {
      anchor.removeEventListener('mouseenter', handleEnter);
      anchor.removeEventListener('mouseleave', handleLeave);
    };
  }, [anchorRef, updatePosition]);

  useEffect(() => {
    if (!visible) return;

    const handleReposition = () => updatePosition();
    window.addEventListener('scroll', handleReposition, true);
    window.addEventListener('resize', handleReposition);

    return () => {
      window.removeEventListener('scroll', handleReposition, true);
      window.removeEventListener('resize', handleReposition);
    };
  }, [visible, updatePosition]);

  useEffect(() => {
    if (hoveringCard || hoveringPopover) {
      clearCloseTimer();
      return;
    }

    if (visible) {
      scheduleClose();
    }
  }, [hoveringCard, hoveringPopover, visible]);

  useEffect(() => {
    return () => {
      clearOpenTimer();
      clearCloseTimer();
    };
  }, []);

  if (typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <AnimatePresence>
      {visible ? (
        <motion.div
          initial={{ opacity: 0, y: 12, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 8, scale: 0.98 }}
          transition={{ duration: 0.18, ease: 'easeOut' }}
          onMouseEnter={() => {
            setHoveringPopover(true);
            clearCloseTimer();
          }}
          onMouseLeave={() => {
            setHoveringPopover(false);
            scheduleClose();
          }}
          className="fixed z-[90] w-[360px] rounded-lg border border-[#ECEEF5] bg-white p-5 shadow-[0_30px_70px_rgba(21,22,46,0.18)]"
          style={{ left: position.left, top: position.top }}
        >
          <div
            className={cn(
              'absolute top-10 h-4 w-4 rotate-45 border-[#ECEEF5] bg-white',
              position.side === 'right'
                ? '-left-2 border-b border-l'
                : '-right-2 border-r border-t'
            )}
          />

          <button onClick={() => onNavigate(course.id)} className="w-full text-left">
            <div className="flex items-start justify-between gap-3">
              <h3 className="text-lg font-bold leading-7 text-[#111827]">{course.title}</h3>
              <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-semibold text-lh-blue">
                {course.badge}
              </span>
            </div>
          </button>

          <div className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-xs font-medium text-[#6B7280]">
            <div className="inline-flex items-center gap-1.5">
              <CalendarDays className="h-3.5 w-3.5 text-lh-blue" />
              {course.updatedLabel}
            </div>
            <div className="inline-flex items-center gap-1.5">
              <Clock3 className="h-3.5 w-3.5 text-lh-blue" />
              {course.totalHours}
            </div>
            <div className="inline-flex items-center gap-1.5">
              <BarChart3 className="h-3.5 w-3.5 text-lh-blue" />
              {course.level}
            </div>
          </div>

          <p className="mt-4 line-clamp-3 text-sm leading-6 text-[#4B5563]">{course.summary}</p>

          <div className="mt-5 space-y-2.5">
            {course.highlights.slice(0, 4).map((item) => (
              <div key={item} className="flex items-start gap-2.5 text-sm leading-6 text-[#374151]">
                <CheckCircle2 className="mt-0.5 h-4.5 w-4.5 flex-none text-[#1F7A45]" />
                <span>{item}</span>
              </div>
            ))}
          </div>

          <div className="mt-5 flex items-center gap-3">
            <button
              onClick={() => onNavigate(course.id)}
              className="inline-flex h-11 flex-1 items-center justify-center gap-2 rounded-lg bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
            >
              <ShoppingCart className="h-4 w-4" />
              Them vao gio hang
            </button>
            <button
              aria-label="Yeu thich khoa hoc"
              className="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-[#D9DEF2] text-lh-blue transition hover:bg-[#F8FAFF]"
            >
              <Heart className="h-4.5 w-4.5" />
            </button>
          </div>
        </motion.div>
      ) : null}
    </AnimatePresence>,
    document.body
  );
};

export default CoursePreviewPopover;
