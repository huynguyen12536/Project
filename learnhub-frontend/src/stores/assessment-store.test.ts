/**
 * Tests for assessmentStore (Zustand)
 *
 * Verifies:
 * - State mutations (setProgress, setResult, setError)
 * - Reset functionality
 * - Progress history accumulation
 */

import { renderHook, act } from '@testing-library/react';
import { useAssessmentStore } from './assessment-store';
import { AssessmentStatus, SkillScore } from '../types';

describe('useAssessmentStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    const { result } = renderHook(() => useAssessmentStore());
    act(() => {
      result.current.reset();
    });
  });

  it('initializes with null progress, result, and error', () => {
    const { result } = renderHook(() => useAssessmentStore());
    expect(result.current.currentProgress).toBeNull();
    expect(result.current.result).toBeNull();
    expect(result.current.error).toBeNull();
    expect(result.current.progressHistory).toEqual([]);
  });

  it('setProgress updates currentProgress and accumulates in history', () => {
    const { result } = renderHook(() => useAssessmentStore());

    const progress1 = {
      assessmentId: 'test-123',
      status: AssessmentStatus.PROCESSING,
      progressPercent: 25,
      currentStep: 'analyzing',
      queuePosition: 0,
      confidence: 0.8,
      timestamp: '2026-06-19T10:00:00Z',
    };

    act(() => {
      result.current.setProgress(progress1);
    });

    expect(result.current.currentProgress).toEqual(progress1);
    expect(result.current.progressHistory).toHaveLength(1);
    expect(result.current.progressHistory[0]).toEqual(progress1);

    // Add another progress event
    const progress2 = {
      ...progress1,
      progressPercent: 50,
      currentStep: 'extracting_skills',
    };

    act(() => {
      result.current.setProgress(progress2);
    });

    expect(result.current.currentProgress).toEqual(progress2);
    expect(result.current.progressHistory).toHaveLength(2);
  });

  it('setProgress clears error', () => {
    const { result } = renderHook(() => useAssessmentStore());

    act(() => {
      result.current.setError({ error: 'test error', code: 'TEST_ERROR' });
    });

    expect(result.current.error).not.toBeNull();

    const progress = {
      assessmentId: 'test-123',
      status: AssessmentStatus.PROCESSING,
      progressPercent: 25,
      currentStep: 'analyzing',
      queuePosition: 0,
      confidence: 0.8,
      timestamp: '2026-06-19T10:00:00Z',
    };

    act(() => {
      result.current.setProgress(progress);
    });

    expect(result.current.error).toBeNull();
  });

  it('setResult updates result and clears error', () => {
    const { result } = renderHook(() => useAssessmentStore());

    act(() => {
      result.current.setError({ error: 'test error', code: 'TEST_ERROR' });
    });

    const skillScores: SkillScore[] = [
      { skillName: 'React', score: 85, category: 'Frontend' },
      { skillName: 'TypeScript', score: 90, category: 'Backend' },
    ];

    const assessmentResult = {
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/test/repo',
      languageDistribution: { TypeScript: 60, JavaScript: 40 },
      skillScores,
      overallScore: 87,
      completedAt: '2026-06-19T10:30:00Z',
    };

    act(() => {
      result.current.setResult(assessmentResult);
    });

    expect(result.current.result).toEqual(assessmentResult);
    expect(result.current.error).toBeNull();
  });

  it('setError updates error state', () => {
    const { result } = renderHook(() => useAssessmentStore());

    const errorObj = { error: 'Network error', code: 'NETWORK_ERROR' };

    act(() => {
      result.current.setError(errorObj);
    });

    expect(result.current.error).toEqual(errorObj);
  });

  it('reset clears all state', () => {
    const { result } = renderHook(() => useAssessmentStore());

    const progress = {
      assessmentId: 'test-123',
      status: AssessmentStatus.PROCESSING,
      progressPercent: 25,
      currentStep: 'analyzing',
      queuePosition: 0,
      confidence: 0.8,
      timestamp: '2026-06-19T10:00:00Z',
    };

    const skillScores: SkillScore[] = [
      { skillName: 'React', score: 85, category: 'Frontend' },
    ];

    const assessmentResult = {
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/test/repo',
      languageDistribution: { TypeScript: 100 },
      skillScores,
      overallScore: 85,
      completedAt: '2026-06-19T10:30:00Z',
    };

    act(() => {
      result.current.setProgress(progress);
      result.current.setResult(assessmentResult);
      result.current.setError({ error: 'test', code: 'TEST' });
    });

    expect(result.current.currentProgress).not.toBeNull();
    expect(result.current.result).not.toBeNull();
    expect(result.current.error).not.toBeNull();

    act(() => {
      result.current.reset();
    });

    expect(result.current.currentProgress).toBeNull();
    expect(result.current.result).toBeNull();
    expect(result.current.error).toBeNull();
    expect(result.current.progressHistory).toEqual([]);
  });
});
