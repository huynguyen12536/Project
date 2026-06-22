/**
 * Custom Hook: useAssessmentSubmission
 *
 * Wraps assessment submission logic with polling/SSE for progress.
 * Integrates with TanStack Query for server state management.
 * Handles loading states, error states, and result fetching.
 *
 * Usage:
 *   const { assessmentId, status, progress, result, error, isLoading, submit } = useAssessmentSubmission();
 *   await submit({ repositoryUrl: 'https://github.com/...' });
 */

import { useCallback, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useAssessmentStore } from '../stores/assessment-store';
import { useAssessmentProgress } from './useAssessmentProgress';
import {
  AssessmentStatus,
  AssessmentSubmissionPayload,
  AssessmentResult,
  AssessmentProgressEvent,
} from '../types';
import api from '../services/api';

interface UseAssessmentSubmissionReturn {
  assessmentId: string | null;
  status: AssessmentStatus | null;
  progress: AssessmentProgressEvent | null;
  result: AssessmentResult | null;
  error: Error | null;
  isLoading: boolean;
  isPolling: boolean;
  submit: (payload: AssessmentSubmissionPayload) => Promise<void>;
}

const SUBMISSION_ENDPOINT = '/api/v1/assessments';
const RESULT_ENDPOINT = '/api/v1/assessments/{id}/details';

export const useAssessmentSubmission = (): UseAssessmentSubmissionReturn => {
  const { currentProgress, result: storeResult, error: storeError, reset } = useAssessmentStore();

  const [assessmentId, setAssessmentId] = useState<string | null>(null);
  const [status, setStatus] = useState<AssessmentStatus | null>(null);

  // Progress tracking via SSE/polling
  const { isConnected: isPolling, error: progressError } = useAssessmentProgress(
    assessmentId,
    status
  );

  /**
   * Submit repository for assessment
   */
  const submitMutation = useMutation({
    mutationFn: async (payload: AssessmentSubmissionPayload) => {
      const response = await api.post<{ assessmentId: string; status: AssessmentStatus }>(
        SUBMISSION_ENDPOINT,
        payload
      );
      return response.data;
    },
    onSuccess: (data) => {
      setAssessmentId(data.assessmentId);
      setStatus(data.status);

      // Poll for result when processing is complete
      if (data.status === AssessmentStatus.COMPLETED) {
        resultQuery.refetch();
      }
    },
    onError: (error: any) => {
      console.error('Submission error:', error);
    },
  });

  /**
   * Fetch assessment result
   */
  const resultQuery = useQuery<AssessmentResult>({
    queryKey: ['assessmentResult', assessmentId],
    queryFn: async () => {
      if (!assessmentId) {
        throw new Error('No assessment ID');
      }
      const response = await api.get<AssessmentResult>(
        RESULT_ENDPOINT.replace('{id}', assessmentId)
      );
      return response.data;
    },
    enabled: false, // Manual trigger
    staleTime: Infinity, // Don't refetch automatically
    gcTime: 5 * 60 * 1000, // 5 minute cache
  });

  /**
   * Handle submission
   */
  const submit = useCallback(
    async (payload: AssessmentSubmissionPayload) => {
      // Reset store for new submission
      reset();
      setAssessmentId(null);
      setStatus(null);

      try {
        await submitMutation.mutateAsync(payload);
      } catch (err) {
        console.error('Submission failed:', err);
        throw err;
      }
    },
    [submitMutation, reset]
  );

  /**
   * Derive current error
   */
  const error = storeError
    ? new Error(storeError.error)
    : progressError
      ? progressError
      : submitMutation.error
        ? (submitMutation.error as Error)
        : resultQuery.error
          ? (resultQuery.error as Error)
          : null;

  return {
    assessmentId,
    status: status || currentProgress?.status || null,
    progress: currentProgress,
    result: storeResult || resultQuery.data || null,
    error,
    isLoading: submitMutation.isPending || resultQuery.isFetching,
    isPolling,
    submit,
  };
};
