import React from 'react';
import { cn } from '../lib/cn';

export type BadgeVariant = 'default' | 'success' | 'error' | 'warning' | 'info';

export interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

const variantClasses: Record<BadgeVariant, string> = {
  default: 'bg-gray-100 text-gray-700',
  success: 'bg-success-50 text-success-600',
  error: 'bg-error-50 text-error-600',
  warning: 'bg-amber-50 text-amber-700',
  info: 'bg-primary-50 text-primary-700',
};

export const Badge: React.FC<BadgeProps> = ({ variant = 'default', children, className }) => (
  <span
    className={cn(
      'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
      variantClasses[variant],
      className
    )}
  >
    {children}
  </span>
);

export default Badge;
