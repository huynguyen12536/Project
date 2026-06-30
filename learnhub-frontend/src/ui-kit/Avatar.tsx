import React, { useRef } from 'react';
import { cn } from '../lib/cn';

export interface AvatarProps {
  src?: string | null;
  name?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'xxl';
  /** When provided, an upload overlay appears on hover and triggers this on file select. */
  onUpload?: (file: File) => void;
  className?: string;
}

const sizeClasses = {
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-16 w-16 text-lg',
  xl: 'h-24 w-24 text-2xl',
  xxl: 'h-32 w-32 text-4xl',
};

function initials(name?: string): string {
  if (!name) return '?';
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('');
}

export const Avatar: React.FC<AvatarProps> = ({ src, name, size = 'md', onUpload, className }) => {
  const fileRef = useRef<HTMLInputElement>(null);

  return (
    <div
      className={cn(
        'group relative inline-flex items-center justify-center overflow-hidden rounded-full bg-primary-100 font-semibold text-primary-700',
        sizeClasses[size],
        className
      )}
    >
      {src ? (
        <img src={src} alt={name || 'avatar'} className="h-full w-full object-cover" />
      ) : (
        <span aria-hidden>{initials(name)}</span>
      )}

      {onUpload && (
        <>
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="absolute inset-0 flex items-center justify-center bg-black/40 text-xs text-white opacity-0 transition-opacity group-hover:opacity-100"
            aria-label="Upload avatar"
          >
            Edit
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) onUpload(f);
            }}
          />
        </>
      )}
    </div>
  );
};

export default Avatar;
