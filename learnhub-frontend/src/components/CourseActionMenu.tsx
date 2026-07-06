import React, { useState } from 'react';
import { useFloating, useClick, useDismiss, useRole, useInteractions, offset, flip, shift } from '@floating-ui/react';
import {
  Eye,
  Edit2,
  Trash2,
  Check,
  X,
} from 'lucide-react';
import { cn } from '../lib/cn';

export interface CourseActionItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  color?: string;
  onClick: () => void;
  divider?: boolean;
}

interface CourseActionMenuProps {
  courseId: string;
  items: CourseActionItem[];
  onClose?: () => void;
}

const CourseActionMenu: React.FC<CourseActionMenuProps> = ({ courseId, items, onClose }) => {
  const [isOpen, setIsOpen] = useState(false);

  const { refs, floatingStyles, context } = useFloating({
    open: isOpen,
    onOpenChange: setIsOpen,
    middleware: [
      offset(8),
      flip({ padding: 8 }),
      shift({ padding: 8 }),
    ],
  });

  const click = useClick(context);
  const dismiss = useDismiss(context);
  const role = useRole(context);

  const { getReferenceProps, getFloatingProps } = useInteractions([
    click,
    dismiss,
    role,
  ]);

  const handleItemClick = (item: CourseActionItem) => {
    item.onClick();
    setIsOpen(false);
    onClose?.();
  };

  return (
    <>
      <button
        ref={refs.setReference}
        {...getReferenceProps()}
        title="Actions"
        className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 transition-all duration-200 hover:border-slate-300 hover:bg-slate-50 hover:text-slate-700"
        aria-label="Course actions menu"
        aria-expanded={isOpen}
        aria-haspopup="menu"
      >
        <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <circle cx="12" cy="5" r="1" fill="currentColor" />
          <circle cx="12" cy="12" r="1" fill="currentColor" />
          <circle cx="12" cy="19" r="1" fill="currentColor" />
        </svg>
      </button>

      {isOpen && (
        <div
          ref={refs.setFloating}
          style={floatingStyles}
          {...getFloatingProps()}
          className="z-50 w-52 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg"
          role="menu"
          aria-label="Course actions"
        >
          {items.map((item, index) => (
            <React.Fragment key={item.id}>
              {item.divider && index > 0 && (
                <div className="border-t border-slate-100" role="separator" />
              )}
              {!item.divider && (
                <button
                  onClick={() => handleItemClick(item)}
                  className={cn(
                    'flex w-full items-center gap-3 px-4 py-3 text-left text-sm font-medium transition-colors duration-150',
                    item.color === 'text-emerald-600 hover:bg-emerald-50'
                      ? 'text-emerald-600 hover:bg-emerald-50'
                      : item.color === 'text-rose-600 hover:bg-rose-50'
                        ? 'text-rose-600 hover:bg-rose-50'
                        : 'text-slate-700 hover:bg-slate-50',
                  )}
                  role="menuitem"
                >
                  <span className="flex h-4 w-4 items-center justify-center">
                    {item.icon}
                  </span>
                  {item.label}
                </button>
              )}
            </React.Fragment>
          ))}
        </div>
      )}
    </>
  );
};

export default CourseActionMenu;
