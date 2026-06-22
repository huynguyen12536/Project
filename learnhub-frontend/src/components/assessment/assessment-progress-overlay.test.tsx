/**
 * Tests for AssessmentProgressOverlay component
 *
 * Verifies:
 * - Displays progress event data
 * - Shows queue position vs processing status
 * - Displays confidence score
 * - Shows connection status
 * - Displays error messages
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import { AssessmentProgressOverlay } from './assessment-progress-overlay';
import { AssessmentStatus } from '../../types';

describe('AssessmentProgressOverlay', () => {
  const mockProgress = {
    assessmentId: 'test-123',
    status: AssessmentStatus.PROCESSING,
    progressPercent: 50,
    currentStep: 'extracting_skills',
    queuePosition: 0,
    confidence: 0.8,
    timestamp: '2026-06-19T10:00:00Z',
  };

  it('does not render when progress is null', () => {
    const { container } = render(
      <AssessmentProgressOverlay progress={null} isConnected={true} error={null} />
    );

    expect(container.firstChild).toBeNull();
  });

  it('renders progress overlay with progress data', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/Assessment in Progress/i)).toBeInTheDocument();
    expect(screen.getByText(/test-123/)).toBeInTheDocument();
    expect(screen.getByText(/Extracting Skills/i)).toBeInTheDocument();
  });

  it('displays progress percentage', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/50% Complete/i)).toBeInTheDocument();
  });

  it('displays confidence score', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/Confidence/)).toBeInTheDocument();
    expect(screen.getByText(/80%/)).toBeInTheDocument();
  });

  it('shows queue status when in queue', () => {
    const queueProgress = {
      ...mockProgress,
      queuePosition: 3,
    };

    render(
      <AssessmentProgressOverlay progress={queueProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/In Queue/i)).toBeInTheDocument();
    expect(screen.getByText(/#3/)).toBeInTheDocument();
  });

  it('shows processing status when not in queue', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={true} error={null} />
    );

    expect(screen.queryByText(/In Queue/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Extracting Skills/i)).toBeInTheDocument();
  });

  it('displays connected status', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/🟢 Connected/)).toBeInTheDocument();
  });

  it('displays disconnected status', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={false} error={null} />
    );

    expect(screen.getByText(/🟡 Connecting/)).toBeInTheDocument();
  });

  it('displays error message', () => {
    const error = new Error('Connection lost');

    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={false} error={error} />
    );

    expect(screen.getByText(/Connection Issue/i)).toBeInTheDocument();
    expect(screen.getByText(/Connection lost/)).toBeInTheDocument();
    expect(screen.getByText(/🔴 Disconnected/)).toBeInTheDocument();
  });

  it('renders step icon for known steps', () => {
    render(
      <AssessmentProgressOverlay progress={mockProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/🛠️/)).toBeInTheDocument(); // extracting_skills icon
  });

  it('renders analyzing step correctly', () => {
    const analyzingProgress = {
      ...mockProgress,
      currentStep: 'analyzing',
      progressPercent: 25,
    };

    render(
      <AssessmentProgressOverlay progress={analyzingProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/Analyzing Repository/i)).toBeInTheDocument();
    expect(screen.getByText(/🔍/)).toBeInTheDocument();
  });

  it('renders report generation step correctly', () => {
    const reportProgress = {
      ...mockProgress,
      currentStep: 'generating_report',
      progressPercent: 75,
    };

    render(
      <AssessmentProgressOverlay progress={reportProgress} isConnected={true} error={null} />
    );

    expect(screen.getByText(/Generating Report/i)).toBeInTheDocument();
    expect(screen.getByText(/📊/)).toBeInTheDocument();
  });
});
