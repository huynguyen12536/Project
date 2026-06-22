/**
 * Tests for AssessmentProgressPage component
 *
 * Verifies:
 * - Initial form display
 * - Results display after completion
 * - Error state display
 * - Processing state overlay
 * - New assessment flow
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AssessmentProgressPage } from './assessment-progress-page';

// Mock the hooks
jest.mock('../../hooks/useAssessmentSubmission', () => ({
  useAssessmentSubmission: jest.fn(),
}));

jest.mock('../../stores/assessment-store', () => ({
  useAssessmentStore: jest.fn(),
}));

jest.mock('./assessment-submission-form', () => ({
  AssessmentSubmissionForm: ({ onSubmit, isLoading, error }: any) => (
    <div data-testid="submission-form">
      <input
        placeholder="repo url"
        onBlur={(e) => {
          if (e.target.value) onSubmit(e.target.value);
        }}
      />
      {isLoading && <p>Loading...</p>}
      {error && <p>{error.message}</p>}
    </div>
  ),
}));

jest.mock('./assessment-progress-overlay', () => ({
  AssessmentProgressOverlay: ({ progress }: any) => (
    <div data-testid="progress-overlay">{progress?.currentStep}</div>
  ),
}));

jest.mock('./radar-chart-card', () => ({
  RadarChartCard: ({ overallScore }: any) => (
    <div data-testid="radar-chart">Score: {overallScore}</div>
  ),
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children);
};

import { useAssessmentSubmission } from '../../hooks/useAssessmentSubmission';
import { useAssessmentStore } from '../../stores/assessment-store';
import { AssessmentStatus } from '../../types';

const mockUseAssessmentSubmission = useAssessmentSubmission as jest.Mock;
const mockUseAssessmentStore = useAssessmentStore as jest.Mock;

describe('AssessmentProgressPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders form initially', () => {
    mockUseAssessmentSubmission.mockReturnValue({
      assessmentId: null,
      status: null,
      progress: null,
      result: null,
      error: null,
      isLoading: false,
      isPolling: false,
      submit: jest.fn(),
    });

    mockUseAssessmentStore.mockReturnValue({
      reset: jest.fn(),
    });

    render(<AssessmentProgressPage />, { wrapper: createWrapper() });

    expect(screen.getByTestId('submission-form')).toBeInTheDocument();
    expect(screen.getByText(/Code Skills Assessment/i)).toBeInTheDocument();
  });

  it('displays results after completion', () => {
    const mockResult = {
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/test/repo',
      languageDistribution: { TypeScript: 60, JavaScript: 40 },
      skillScores: [
        { skillName: 'React', score: 85, category: 'Frontend' },
      ],
      overallScore: 85,
      completedAt: '2026-06-19T10:30:00Z',
    };

    mockUseAssessmentSubmission.mockReturnValue({
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      progress: null,
      result: mockResult,
      error: null,
      isLoading: false,
      isPolling: false,
      submit: jest.fn(),
    });

    mockUseAssessmentStore.mockReturnValue({
      reset: jest.fn(),
    });

    render(<AssessmentProgressPage />, { wrapper: createWrapper() });

    expect(screen.getByText(/Assessment Complete/i)).toBeInTheDocument();
    expect(screen.getByText(/https:\/\/github.com\/test\/repo/)).toBeInTheDocument();
    expect(screen.getByText(/TypeScript/)).toBeInTheDocument();
    expect(screen.getByTestId('radar-chart')).toBeInTheDocument();
  });

  it('displays error state on failure', () => {
    mockUseAssessmentSubmission.mockReturnValue({
      assessmentId: 'test-123',
      status: AssessmentStatus.FAILED,
      progress: null,
      result: null,
      error: new Error('Repository not found'),
      isLoading: false,
      isPolling: false,
      submit: jest.fn(),
    });

    mockUseAssessmentStore.mockReturnValue({
      reset: jest.fn(),
    });

    render(<AssessmentProgressPage />, { wrapper: createWrapper() });

    expect(screen.getByText(/Assessment Failed/i)).toBeInTheDocument();
    expect(screen.getByText(/Repository not found/)).toBeInTheDocument();
  });

  it('shows progress overlay during processing', () => {
    const mockProgress = {
      assessmentId: 'test-123',
      status: AssessmentStatus.PROCESSING,
      progressPercent: 50,
      currentStep: 'extracting_skills',
      queuePosition: 0,
      confidence: 0.8,
      timestamp: '2026-06-19T10:00:00Z',
    };

    mockUseAssessmentSubmission.mockReturnValue({
      assessmentId: 'test-123',
      status: AssessmentStatus.PROCESSING,
      progress: mockProgress,
      result: null,
      error: null,
      isLoading: true,
      isPolling: true,
      submit: jest.fn(),
    });

    mockUseAssessmentStore.mockReturnValue({
      reset: jest.fn(),
    });

    render(<AssessmentProgressPage />, { wrapper: createWrapper() });

    expect(screen.getByTestId('progress-overlay')).toBeInTheDocument();
    expect(screen.getByText('extracting_skills')).toBeInTheDocument();
  });

  it('allows starting new assessment after completion', () => {
    const mockResult = {
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/test/repo',
      languageDistribution: { TypeScript: 100 },
      skillScores: [{ skillName: 'React', score: 85, category: 'Frontend' }],
      overallScore: 85,
      completedAt: '2026-06-19T10:30:00Z',
    };

    const mockReset = jest.fn();

    mockUseAssessmentSubmission.mockReturnValue({
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      progress: null,
      result: mockResult,
      error: null,
      isLoading: false,
      isPolling: false,
      submit: jest.fn(),
    });

    mockUseAssessmentStore.mockReturnValue({
      reset: mockReset,
    });

    render(<AssessmentProgressPage />, { wrapper: createWrapper() });

    const newButton = screen.getByText(/Analyze Another Repository/i);
    fireEvent.click(newButton);

    expect(mockReset).toHaveBeenCalled();
  });

  it('displays language distribution', () => {
    const mockResult = {
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/test/repo',
      languageDistribution: {
        TypeScript: 50,
        JavaScript: 30,
        Python: 20,
      },
      skillScores: [{ skillName: 'React', score: 85, category: 'Frontend' }],
      overallScore: 85,
      completedAt: '2026-06-19T10:30:00Z',
    };

    mockUseAssessmentSubmission.mockReturnValue({
      assessmentId: 'test-123',
      status: AssessmentStatus.COMPLETED,
      progress: null,
      result: mockResult,
      error: null,
      isLoading: false,
      isPolling: false,
      submit: jest.fn(),
    });

    mockUseAssessmentStore.mockReturnValue({
      reset: jest.fn(),
    });

    render(<AssessmentProgressPage />, { wrapper: createWrapper() });

    expect(screen.getByText(/Language Distribution/i)).toBeInTheDocument();
    expect(screen.getByText('TypeScript')).toBeInTheDocument();
    expect(screen.getByText('50%')).toBeInTheDocument();
  });
});
