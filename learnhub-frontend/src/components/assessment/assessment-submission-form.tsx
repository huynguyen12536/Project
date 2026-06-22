/**
 * Component: AssessmentSubmissionForm
 *
 * Simple form to submit repository URL for assessment.
 * - Input field for GitHub repository URL
 * - Validation: URL format check
 * - Submit button with loading state
 * - Error display
 *
 * Usage:
 *   const { submit, isLoading, error } = useAssessmentSubmission();
 *   <AssessmentSubmissionForm onSubmit={submit} isLoading={isLoading} error={error} />
 */

import React, { useState, useCallback } from 'react';

export interface AssessmentSubmissionFormProps {
  onSubmit: (snapshotId: string) => Promise<void>;
  isLoading?: boolean;
  error?: Error | null;
  disabled?: boolean;
}

export const AssessmentSubmissionForm: React.FC<AssessmentSubmissionFormProps> = ({
  onSubmit,
  isLoading = false,
  error = null,
  disabled = false,
}) => {
  const [snapshotId, setSnapshotId] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  /**
   * Validate UUID format for snapshot ID
   */
  const isValidUUID = useCallback((uuid: string): boolean => {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }, []);

  /**
   * Handle form submission
   */
  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();

      // Validate
      if (!snapshotId.trim()) {
        setValidationError('Snapshot ID is required');
        return;
      }

      if (!isValidUUID(snapshotId)) {
        setValidationError('Please enter a valid snapshot ID (UUID format)');
        return;
      }

      setValidationError(null);

      try {
        await onSubmit(snapshotId);
      } catch (err) {
        console.error('Submission error:', err);
      }
    },
    [snapshotId, isValidUUID, onSubmit]
  );

  /**
   * Handle input change
   */
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setSnapshotId(e.target.value);
    if (validationError) {
      setValidationError(null);
    }
  }, [validationError]);

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-md mx-auto p-6 bg-white rounded-lg shadow">
      <div className="space-y-4">
        <div>
          <label
            htmlFor="snapshot-id"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Snapshot ID
          </label>
          <input
            id="snapshot-id"
            type="text"
            value={snapshotId}
            onChange={handleInputChange}
            placeholder="e.g., 123e4567-e89b-12d3-a456-426614174000"
            disabled={isLoading || disabled}
            className={`w-full px-4 py-2 border rounded-md focus:outline-none focus:ring-2 transition ${
              validationError || error
                ? 'border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:ring-blue-500'
            } ${isLoading || disabled ? 'bg-gray-50 cursor-not-allowed' : 'bg-white'}`}
          />
        </div>

        {/* Validation Error */}
        {validationError && (
          <div className="text-sm text-red-600 flex items-center gap-2">
            <span className="text-lg">!</span>
            {validationError}
          </div>
        )}

        {/* API Error */}
        {error && (
          <div className="text-sm text-red-600 flex items-center gap-2 p-3 bg-red-50 rounded">
            <span className="text-lg">⚠</span>
            {error.message}
          </div>
        )}

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isLoading || disabled}
          className={`w-full py-2 px-4 rounded-md font-medium transition ${
            isLoading || disabled
              ? 'bg-gray-300 text-gray-600 cursor-not-allowed'
              : 'bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800'
          }`}
        >
          {isLoading ? (
            <span className="flex items-center justify-center gap-2">
              <span className="inline-block animate-spin">⟳</span>
              Assessing Snapshot...
            </span>
          ) : (
            'Submit for Assessment'
          )}
        </button>
      </div>
    </form>
  );
};
