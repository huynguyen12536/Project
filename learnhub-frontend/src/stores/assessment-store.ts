/**
 * Zustand Store: assessmentStore
 *
 * Manages assessment UI state:
 * - Current progress event
 * - Assessment result
 * - Errors during processing
 * - Reset between submissions
 *
 * Usage:
 *   const { setProgress, setResult, setError, reset } = useAssessmentStore();
 */

import { create } from 'zustand';
import {
  AssessmentProgressEvent,
  AssessmentResult,
  ErrorResponse,
} from '../types';

interface AssessmentState {
  // Progress tracking
  currentProgress: AssessmentProgressEvent | null;
  progressHistory: AssessmentProgressEvent[];

  // Result
  result: AssessmentResult | null;

  // Error
  error: ErrorResponse | null;

  // Actions
  setProgress: (progress: AssessmentProgressEvent) => void;
  setResult: (result: AssessmentResult) => void;
  setError: (error: ErrorResponse | null) => void;
  reset: () => void;
}

const initialState = {
  currentProgress: null,
  progressHistory: [],
  result: null,
  error: null,
};

export const useAssessmentStore = create<AssessmentState>((set) => ({
  ...initialState,

  setProgress: (progress: AssessmentProgressEvent) =>
    set((state) => ({
      currentProgress: progress,
      progressHistory: [...state.progressHistory, progress],
      error: null, // Clear error on new progress
    })),

  setResult: (result: AssessmentResult) =>
    set({
      result,
      error: null,
    }),

  setError: (error: ErrorResponse | null) =>
    set({
      error,
    }),

  reset: () =>
    set({
      ...initialState,
    }),
}));
