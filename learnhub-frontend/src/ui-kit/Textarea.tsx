import React, { useId, useLayoutEffect, useRef, useState } from 'react';
import { cn } from '../lib/cn';

export interface TextareaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  autoResize?: boolean;
  showCount?: boolean;
}

export const Textarea: React.FC<TextareaProps> = ({
  label,
  error,
  autoResize = true,
  showCount = false,
  maxLength,
  className,
  id,
  value,
  onChange,
  ...props
}) => {
  const autoId = useId();
  const textareaId = id || autoId;
  const ref = useRef<HTMLTextAreaElement>(null);
  const [internal, setInternal] = useState('');
  const current = value !== undefined ? String(value) : internal;

  useLayoutEffect(() => {
    if (autoResize && ref.current) {
      ref.current.style.height = 'auto';
      ref.current.style.height = `${ref.current.scrollHeight}px`;
    }
  }, [current, autoResize]);

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (value === undefined) setInternal(e.target.value);
    onChange?.(e);
  };

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={textareaId} className="mb-1 block text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <textarea
        ref={ref}
        id={textareaId}
        value={value !== undefined ? value : internal}
        onChange={handleChange}
        maxLength={maxLength}
        aria-invalid={Boolean(error)}
        className={cn(
          'w-full resize-none rounded-lg border px-3 py-2 text-sm text-gray-900 placeholder-gray-400',
          'focus:outline-none focus:ring-2',
          error
            ? 'border-error-500 focus:ring-error-500'
            : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500',
          className
        )}
        rows={props.rows ?? 3}
        {...props}
      />
      <div className="mt-1 flex items-center justify-between">
        {error ? <p className="text-xs text-error-600">{error}</p> : <span />}
        {showCount && (
          <p className="text-xs text-gray-400">
            {current.length}
            {maxLength ? ` / ${maxLength}` : ''}
          </p>
        )}
      </div>
    </div>
  );
};

export default Textarea;
