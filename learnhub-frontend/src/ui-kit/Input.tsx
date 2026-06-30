import React, { useId, useState } from 'react';
import { cn } from '../lib/cn';
import { estimatePasswordStrength } from '../lib/passwordStrength';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  /** When true and type='password', renders an entropy-based strength meter. */
  showStrength?: boolean;
}

const strengthColors = [
  'bg-error-500',
  'bg-error-500',
  'bg-vibrant-amber',
  'bg-success-500',
  'bg-success-600',
];

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  (
    { label, error, hint, showStrength = false, className, id, type = 'text', value, onChange, ...props },
    ref
  ) => {
    const autoId = useId();
    const inputId = id || autoId;
    const [internal, setInternal] = useState('');

    const isPassword = type === 'password';
    const currentValue = value !== undefined ? String(value) : internal;
    const strength = showStrength && isPassword
      ? estimatePasswordStrength(currentValue)
      : null;

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      if (value === undefined) setInternal(e.target.value);
      onChange?.(e);
    };

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-gray-700">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          type={type}
          value={value !== undefined ? value : internal}
          onChange={handleChange}
          aria-invalid={Boolean(error)}
          className={cn(
            'w-full rounded-lg border px-3 py-2 text-sm text-gray-900 placeholder-gray-400',
            'focus:outline-none focus:ring-2 focus:ring-offset-0',
            error
              ? 'border-error-500 focus:ring-error-500'
              : 'border-gray-300 focus:border-primary-500 focus:ring-primary-500',
            'disabled:cursor-not-allowed disabled:bg-gray-50',
            className
          )}
          {...props}
        />
        {strength && currentValue.length > 0 && (
          <div className="mt-2" aria-live="polite">
            <div className="flex gap-1">
              {[0, 1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  className={cn(
                    'h-1.5 flex-1 rounded-full',
                    i <= strength.score ? strengthColors[strength.score] : 'bg-gray-200'
                  )}
                />
              ))}
            </div>
            <p className="mt-1 text-xs text-gray-500">
              {strength.label} · ~{strength.entropyBits} bits
            </p>
          </div>
        )}
        {error ? (
          <p className="mt-1 text-xs text-error-600">{error}</p>
        ) : hint ? (
          <p className="mt-1 text-xs text-gray-500">{hint}</p>
        ) : null}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
