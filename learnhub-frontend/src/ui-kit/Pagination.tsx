import React from 'react';
import { cn } from '../lib/cn';

export interface PaginationProps {
  currentPage: number; // 1-based
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

function pageRange(current: number, total: number): (number | '…')[] {
  const delta = 1;
  const range: (number | '…')[] = [];
  const left = Math.max(2, current - delta);
  const right = Math.min(total - 1, current + delta);

  range.push(1);
  if (left > 2) range.push('…');
  for (let i = left; i <= right; i++) range.push(i);
  if (right < total - 1) range.push('…');
  if (total > 1) range.push(total);
  return range;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className,
}) => {
  if (totalPages <= 1) return null;
  const pages = pageRange(currentPage, totalPages);

  const btn =
    'inline-flex h-8 min-w-8 items-center justify-center rounded-md px-2 text-sm transition-colors';

  return (
    <nav className={cn('flex items-center gap-1', className)} aria-label="Pagination">
      <button
        className={cn(btn, 'text-gray-600 hover:bg-gray-100 disabled:opacity-40')}
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        aria-label="Previous page"
      >
        ‹
      </button>
      {pages.map((p, i) =>
        p === '…' ? (
          <span key={`gap-${i}`} className="px-1 text-gray-400">
            …
          </span>
        ) : (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            aria-current={p === currentPage ? 'page' : undefined}
            className={cn(
              btn,
              p === currentPage
                ? 'bg-primary-600 text-white'
                : 'text-gray-700 hover:bg-gray-100'
            )}
          >
            {p}
          </button>
        )
      )}
      <button
        className={cn(btn, 'text-gray-600 hover:bg-gray-100 disabled:opacity-40')}
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        aria-label="Next page"
      >
        ›
      </button>
    </nav>
  );
};

export default Pagination;
