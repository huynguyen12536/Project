/**
 * Alert Box Component
 *
 * Displays alerts (success, error, info, warning).
 * Usage:
 *   <AlertBox type="success" title="Success" message="Profile updated" />
 *   <AlertBox type="error" title="Error" message="Something went wrong" />
 */

import React from 'react';

type AlertType = 'success' | 'error' | 'info' | 'warning';

interface AlertBoxProps {
  type: AlertType;
  title: string;
  message: string;
  onClose?: () => void;
}

const alertConfig = {
  success: {
    bg: 'bg-success-50',
    border: 'border-success-300',
    title: 'text-success-900',
    message: 'text-success-700',
    icon: '✓',
  },
  error: {
    bg: 'bg-error-50',
    border: 'border-error-300',
    title: 'text-error-900',
    message: 'text-error-700',
    icon: '✕',
  },
  info: {
    bg: 'bg-blue-50',
    border: 'border-blue-300',
    title: 'text-blue-900',
    message: 'text-blue-700',
    icon: 'ℹ',
  },
  warning: {
    bg: 'bg-yellow-50',
    border: 'border-yellow-300',
    title: 'text-yellow-900',
    message: 'text-yellow-700',
    icon: '⚠',
  },
};

export const AlertBox: React.FC<AlertBoxProps> = ({
  type,
  title,
  message,
  onClose,
}) => {
  const config = alertConfig[type];

  return (
    <div className={`rounded-lg border ${config.bg} ${config.border} p-6`}>
      <div className="flex gap-4">
        <div className={`text-2xl flex-shrink-0 ${config.title}`}>
          {config.icon}
        </div>
        <div className="flex-1">
          <h3 className={`font-semibold ${config.title}`}>{title}</h3>
          <p className={`text-sm mt-1 ${config.message}`}>{message}</p>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className={`text-xl flex-shrink-0 ${config.title} hover:opacity-70 transition`}
          >
            ✕
          </button>
        )}
      </div>
    </div>
  );
};
