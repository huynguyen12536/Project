/**
 * Custom Hook: useAssessmentProgress
 *
 * Subscribes to assessment progress updates via SSE or polling.
 * - Attempts SSE connection when assessmentId + status === PROCESSING
 * - Falls back to polling if SSE fails or is unavailable
 * - Updates Zustand store with progress events
 * - Handles network errors gracefully
 * - Unsubscribes on unmount
 *
 * Usage:
 *   const { isConnected, error, progress } = useAssessmentProgress(assessmentId, status);
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { useAssessmentStore } from '../stores/assessment-store';
import { AssessmentStatus, AssessmentProgressEvent } from '../types';
import api from '../services/api';

interface UseAssessmentProgressReturn {
  isConnected: boolean; // SSE or polling active
  error: Error | null;
  progress: AssessmentProgressEvent | null;
}

const SSE_ENDPOINT = '/api/v1/assessments/{id}/progress';
const POLLING_ENDPOINT = '/api/v1/assessments/{id}/progress/poll';
const POLLING_INTERVAL_MS = 2000; // 2 second polling interval
const SSE_TIMEOUT_MS = 5000; // Timeout SSE connection after 5s of inactivity

export const useAssessmentProgress = (
  assessmentId: string | null,
  status: AssessmentStatus | null
): UseAssessmentProgressReturn => {
  const { setProgress } = useAssessmentStore();

  const [isConnected, setIsConnected] = useState(false);
  const [error, setErrorState] = useState<Error | null>(null);
  const [progress, setProgressState] = useState<AssessmentProgressEvent | null>(null);

  const eventSourceRef = useRef<EventSource | null>(null);
  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const sseTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastEventTimeRef = useRef<number>(Date.now());
  const isMountedRef = useRef(true);

  /**
   * Attempt to connect via SSE
   */
  const connectSSE = useCallback(() => {
    if (!assessmentId || status !== AssessmentStatus.PROCESSING) return;

    try {
      const url = `${SSE_ENDPOINT.replace('{id}', assessmentId)}`;

      // EventSource relies on the browser sending the auth cookies automatically on same-origin requests.
      const eventSource = new EventSource(url);

      eventSource.addEventListener('progress', (event: MessageEvent) => {
        if (!isMountedRef.current) return;

        lastEventTimeRef.current = Date.now();
        if (sseTimeoutRef.current) {
          clearTimeout(sseTimeoutRef.current);
        }

        try {
          const progressEvent: AssessmentProgressEvent = JSON.parse(event.data);
          setProgressState(progressEvent);
          setProgress(progressEvent);
          setErrorState(null);
          setIsConnected(true);
        } catch (parseError) {
          console.error('Failed to parse SSE message:', parseError);
        }

        // Restart timeout for next event
        sseTimeoutRef.current = setTimeout(() => {
          if (!isMountedRef.current) return;
          console.warn('SSE timeout - no events received in 5s, falling back to polling');
          disconnectSSE();
          startPolling();
        }, SSE_TIMEOUT_MS) as unknown as NodeJS.Timeout;
      });

      eventSource.addEventListener('error', () => {
        if (!isMountedRef.current) return;
        console.error('SSE connection error');
        disconnectSSE();
        startPolling();
      });

      eventSourceRef.current = eventSource;
      setIsConnected(true);
      setErrorState(null);

      // Set initial timeout
      sseTimeoutRef.current = setTimeout(() => {
        if (!isMountedRef.current) return;
        console.warn('SSE startup timeout - falling back to polling');
        disconnectSSE();
        startPolling();
      }, SSE_TIMEOUT_MS) as unknown as NodeJS.Timeout;
    } catch (err) {
      console.error('Failed to connect SSE:', err);
      startPolling();
    }
  }, [assessmentId, status, setProgress]);

  /**
   * Disconnect SSE
   */
  const disconnectSSE = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    if (sseTimeoutRef.current !== null) {
      clearTimeout(sseTimeoutRef.current);
      sseTimeoutRef.current = null;
    }
  }, []);

  /**
   * Fallback polling
   */
  const startPolling = useCallback(() => {
    if (!assessmentId || status !== AssessmentStatus.PROCESSING) return;

    const pollOnce = async () => {
      if (!isMountedRef.current) return;

      try {
        const response = await api.get<AssessmentProgressEvent>(
          POLLING_ENDPOINT.replace('{id}', assessmentId)
        );

        if (!isMountedRef.current) return;

        setProgressState(response.data);
        setProgress(response.data);
        setErrorState(null);
        setIsConnected(true);
      } catch (err: any) {
        if (!isMountedRef.current) return;

        // Handle specific error codes
        if (err.response?.status === 404) {
          setErrorState(new Error('Assessment not found'));
          setErrorState({ error: 'Assessment not found', code: 'ASSESSMENT_NOT_FOUND' } as any);
        } else if (err.response?.status === 409) {
          setErrorState(new Error('Rate limit exceeded'));
          setErrorState({ error: 'Rate limit exceeded', code: 'RATE_LIMIT' } as any);
        } else {
          console.error('Polling error:', err);
          setErrorState(err);
        }
        setIsConnected(false);
      }
    };

    pollOnce();
    pollingIntervalRef.current = setInterval(pollOnce, POLLING_INTERVAL_MS);
  }, [assessmentId, status, setProgress]);

  /**
   * Stop polling
   */
  const stopPolling = useCallback(() => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
  }, []);

  /**
   * Effect: Connect/disconnect based on assessmentId and status
   */
  useEffect(() => {
    isMountedRef.current = true;

    // Only subscribe if processing
    if (assessmentId && status === AssessmentStatus.PROCESSING) {
      connectSSE();
    } else {
      disconnectSSE();
      stopPolling();
      setIsConnected(false);
    }

    return () => {
      isMountedRef.current = false;
      disconnectSSE();
      stopPolling();
    };
  }, [assessmentId, status, connectSSE, disconnectSSE, stopPolling]);

  return {
    isConnected,
    error,
    progress,
  };
};
