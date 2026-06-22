/**
 * Loading Spinner Component
 *
 * Full-screen loading indicator with message.
 * Usage: <LoadingSpinner message="Loading..." />
 */

import React from 'react';

interface LoadingSpinnerProps {
  message?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  message = 'Loading...',
}) => {
  return (
    <div className="flex items-center justify-center min-h-96">
      <div className="text-center">
        <div className="inline-block">
          <div className="animate-spin text-4xl text-primary-600 mb-4">⟳</div>
          <p className="text-gray-600 font-medium">{message}</p>
        </div>
      </div>
    </div>
  );
};
