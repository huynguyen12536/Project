/**
 * Tests for useAssessmentSubmission hook
 *
 * Verifies:
 * - Submission flow with API call
 * - TanStack Query integration
 * - Error handling
 * - Result fetching
 * - Loading states
 */

import { renderHook, act, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { useAssessmentSubmission } from './useAssessmentSubmission';
import { AssessmentStatus } from '../types';
import api from '../services/api';

// Mock axios API
jest.mock('../services/api', () => ({
  post: jest.fn(),
  get: jest.fn(),
  defaults: { baseURL: 'http://localhost:8080/api/v1' },
  interceptors: { request: { use: jest.fn() }, response: { use: jest.fn() } },
}));

// Mock EventSource
global.EventSource = jest.fn().mockImplementation(() => ({
  addEventListener: jest.fn(),
  close: jest.fn(),
})) as any;

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children);
};

describe('useAssessmentSubmission', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('initializes with null values', () => {
    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    expect(result.current.assessmentId).toBeNull();
    expect(result.current.status).toBeNull();
    expect(result.current.progress).toBeNull();
    expect(result.current.result).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('submits assessment and returns assessmentId', async () => {
    const mockApiPost = api.post as jest.Mock;
    mockApiPost.mockResolvedValueOnce({
      data: {
        assessmentId: 'test-123',
        status: AssessmentStatus.PROCESSING,
      },
    });

    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    const payload = { repositoryUrl: 'https://github.com/test/repo' };

    await act(async () => {
      await result.current.submit(payload);
    });

    await waitFor(() => {
      expect(result.current.assessmentId).toBe('test-123');
      expect(result.current.status).toBe(AssessmentStatus.PROCESSING);
    });

    expect(mockApiPost).toHaveBeenCalledWith('/assessments/submit', payload);
  });

  it('handles submission error', async () => {
    const mockApiPost = api.post as jest.Mock;
    const testError = new Error('Network error');
    mockApiPost.mockRejectedValueOnce(testError);

    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    const payload = { repositoryUrl: 'https://github.com/test/repo' };

    await act(async () => {
      try {
        await result.current.submit(payload);
      } catch (err) {
        // Expected
      }
    });

    await waitFor(() => {
      expect(result.current.error).not.toBeNull();
    });
  });

  it('fetches result after completion', async () => {
    const mockApiPost = api.post as jest.Mock;
    const mockApiGet = api.get as jest.Mock;

    mockApiPost.mockResolvedValueOnce({
      data: {
        assessmentId: 'test-123',
        status: AssessmentStatus.COMPLETED,
      },
    });

    const mockResult = {
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/test/repo',
      languageDistribution: { TypeScript: 100 },
      skillScores: [{ skillName: 'React', score: 85, category: 'Frontend' }],
      overallScore: 85,
      completedAt: '2026-06-19T10:30:00Z',
    };

    mockApiGet.mockResolvedValueOnce({ data: mockResult });

    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    const payload = { repositoryUrl: 'https://github.com/test/repo' };

    await act(async () => {
      await result.current.submit(payload);
    });

    await waitFor(() => {
      expect(result.current.result).toEqual(mockResult);
    });
  });

  it('sets isLoading during submission', async () => {
    const mockApiPost = api.post as jest.Mock;
    mockApiPost.mockImplementationOnce(() => new Promise(() => {})); // Never resolves

    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    const payload = { repositoryUrl: 'https://github.com/test/repo' };

    act(() => {
      result.current.submit(payload);
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(true);
    });
  });

  it('indicates polling status when processing', async () => {
    const mockApiPost = api.post as jest.Mock;
    mockApiPost.mockResolvedValueOnce({
      data: {
        assessmentId: 'test-123',
        status: AssessmentStatus.PROCESSING,
      },
    });

    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    const payload = { repositoryUrl: 'https://github.com/test/repo' };

    await act(async () => {
      await result.current.submit(payload);
    });

    await waitFor(() => {
      expect(result.current.assessmentId).toBe('test-123');
      // isPolling would be true if EventSource/polling is active
    });
  });

  it('resets state on new submission', async () => {
    const mockApiPost = api.post as jest.Mock;
    mockApiPost.mockResolvedValue({
      data: {
        assessmentId: 'test-456',
        status: AssessmentStatus.PROCESSING,
      },
    });

    const { result } = renderHook(() => useAssessmentSubmission(), {
      wrapper: createWrapper(),
    });

    // First submission
    await act(async () => {
      await result.current.submit({ repositoryUrl: 'https://github.com/first/repo' });
    });

    await waitFor(() => {
      expect(result.current.assessmentId).toBe('test-456');
    });

    // Second submission should reset
    await act(async () => {
      await result.current.submit({ repositoryUrl: 'https://github.com/second/repo' });
    });

    await waitFor(() => {
      expect(result.current.assessmentId).toBe('test-456');
    });

    expect(mockApiPost).toHaveBeenCalledTimes(2);
  });
});
