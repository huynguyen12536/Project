import React from 'react';
import { cn } from '../lib/cn';

export interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  label?: string;
}

const sizeClasses = { sm: 'h-4 w-4', md: 'h-6 w-6', lg: 'h-10 w-10' };

export const Spinner: React.FC<SpinnerProps> = ({ size = 'md', className, label }) => (
  <div className="inline-flex items-center gap-2" role="status" aria-live="polite">
    <span
      className={cn(
        'animate-spin rounded-full border-2 border-primary-200 border-t-primary-600',
        sizeClasses[size],
        className
      )}
    />
    {label && <span className="text-sm text-gray-500">{label}</span>}
    <span className="sr-only">Loading</span>
  </div>
);

export interface SkeletonCardProps {
  className?: string;
  lines?: number;
}

export const SkeletonCard: React.FC<SkeletonCardProps> = ({ className, lines = 3 }) => (
  <div className={cn('animate-pulse rounded-xl border border-gray-100 p-4', className)}>
    <div className="mb-4 h-32 w-full rounded-lg bg-gray-200" />
    {Array.from({ length: lines }).map((_, i) => (
      <div
        key={i}
        className={cn('mb-2 h-3 rounded bg-gray-200', i === lines - 1 ? 'w-2/3' : 'w-full')}
      />
    ))}
  </div>
);

export default Spinner;
