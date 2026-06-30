import React, { useId } from 'react';
import { cn } from '../lib/cn';

interface BaseToggleProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
}

export const Checkbox: React.FC<BaseToggleProps> = ({ label, className, id, ...props }) => {
  const autoId = useId();
  const cbId = id || autoId;
  return (
    <label htmlFor={cbId} className="inline-flex cursor-pointer items-center gap-2">
      <input
        id={cbId}
        type="checkbox"
        className={cn(
          'h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500',
          className
        )}
        {...props}
      />
      {label && <span className="text-sm text-gray-700">{label}</span>}
    </label>
  );
};

export const Radio: React.FC<BaseToggleProps> = ({ label, className, id, ...props }) => {
  const autoId = useId();
  const rId = id || autoId;
  return (
    <label htmlFor={rId} className="inline-flex cursor-pointer items-center gap-2">
      <input
        id={rId}
        type="radio"
        className={cn(
          'h-4 w-4 border-gray-300 text-primary-600 focus:ring-primary-500',
          className
        )}
        {...props}
      />
      {label && <span className="text-sm text-gray-700">{label}</span>}
    </label>
  );
};

export interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
}

export const Toggle: React.FC<ToggleProps> = ({ checked, onChange, label, disabled }) => {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={cn(
          'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
          checked ? 'bg-primary-600' : 'bg-gray-300',
          disabled && 'opacity-50'
        )}
      >
        <span
          className={cn(
            'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
            checked ? 'translate-x-6' : 'translate-x-1'
          )}
        />
      </button>
      {label && <span className="text-sm text-gray-700">{label}</span>}
    </label>
  );
};

export default Checkbox;
