/**
 * Component: AssessmentProgressPage
 *
 * Main page for assessment progress tracking and results display.
 * Orchestrates:
 * - Assessment submission form
 * - Progress overlay (SSE/polling)
 * - Results radar chart display
 * - Error handling
 *
 * Usage:
 *   <AssessmentProgressPage />
 */

import React, { useCallback } from 'react';
import { useAssessmentSubmission } from '../../hooks/useAssessmentSubmission';
import { useAssessmentStore } from '../../stores/assessment-store';
import { AssessmentStatus } from '../../types';
import { AssessmentSubmissionForm } from './assessment-submission-form';
import { AssessmentProgressOverlay } from './assessment-progress-overlay';

export const AssessmentProgressPage: React.FC = () => {
  const {
    assessmentId,
    status,
    progress,
    result,
    error,
    isLoading,
    isPolling,
    submit,
  } = useAssessmentSubmission();

  const { reset } = useAssessmentStore();

  /**
   * Handle form submission
   */
  const handleSubmit = useCallback(async (snapshotId: string) => {
    try {
      await submit({ snapshotId });
    } catch (err) {
      console.error('Failed to submit assessment:', err);
    }
  }, [submit]);

  /**
   * Handle starting a new assessment
   */
  const handleStartNew = useCallback(() => {
    reset();
  }, [reset]);

  /**
   * Derive display state
   */
  const isProcessing = status === AssessmentStatus.PROCESSING;
  const isCompleted = status === AssessmentStatus.COMPLETED;
  const isFailed = status === AssessmentStatus.FAILED;

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 py-12 px-4">
      <div className="max-w-4xl mx-auto">
        {/* Page Header */}
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">Code Skills Assessment</h1>
          <p className="text-lg text-gray-600">
            Analyze your GitHub repository and get detailed skill insights
          </p>
        </div>

        {/* Main Content */}
        <div className="space-y-8">
          {/* Form or Result Display */}
          {!assessmentId ? (
            // Show form if no assessment started
            <div className="bg-white rounded-lg shadow-lg p-8">
              <AssessmentSubmissionForm
                onSubmit={handleSubmit}
                isLoading={isLoading}
                error={error}
                disabled={isProcessing}
              />

              {/* Info Section */}
              <div className="mt-8 pt-8 border-t border-gray-200 grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="text-center">
                  <div className="text-3xl mb-2">📊</div>
                  <h3 className="font-semibold text-gray-900 mb-1">Comprehensive Analysis</h3>
                  <p className="text-sm text-gray-600">
                    We analyze your entire repository including code structure, languages, and patterns
                  </p>
                </div>

                <div className="text-center">
                  <div className="text-3xl mb-2">⚡</div>
                  <h3 className="font-semibold text-gray-900 mb-1">Quick Results</h3>
                  <p className="text-sm text-gray-600">
                    Get instant feedback on your skills within minutes
                  </p>
                </div>

                <div className="text-center">
                  <div className="text-3xl mb-2">🎯</div>
                  <h3 className="font-semibold text-gray-900 mb-1">Skill Insights</h3>
                  <p className="text-sm text-gray-600">
                    Discover your strengths across multiple programming categories
                  </p>
                </div>
              </div>
            </div>
          ) : isCompleted && result ? (
            // Show results when completed
            <div className="space-y-6">
              {/* Success Banner */}
              <div className="bg-green-50 border border-green-200 rounded-lg p-6">
                <div className="flex items-start gap-3">
                  <span className="text-2xl">✅</span>
                  <div>
                    <h2 className="text-lg font-semibold text-green-900">Assessment Complete!</h2>
                    <p className="text-sm text-green-700 mt-1">
                      Your code analysis is ready. See your detailed results below.
                    </p>
                  </div>
                </div>
              </div>

              {/* Assessment Info */}
              <div className="bg-white rounded-lg shadow p-6">
                <h3 className="text-sm font-semibold text-gray-700 mb-4">Assessment Details</h3>
                <div className="space-y-3">
                  <div>
                    <span className="text-xs font-medium text-gray-600">Assessment ID</span>
                    <p className="text-sm font-mono text-gray-900 break-all">{result.id}</p>
                  </div>
                  <div>
                    <span className="text-xs font-medium text-gray-600">Snapshot ID</span>
                    <p className="text-sm font-mono text-gray-900 break-all">{result.snapshotId}</p>
                  </div>
                  <div>
                    <span className="text-xs font-medium text-gray-600">Status</span>
                    <p className="text-sm font-semibold text-green-700">{result.status}</p>
                  </div>
                  {result.completedAt && (
                    <div>
                      <span className="text-xs font-medium text-gray-600">Completed At</span>
                      <p className="text-sm text-gray-900">
                        {new Date(result.completedAt).toLocaleString()}
                      </p>
                    </div>
                  )}
                </div>
              </div>

              {/* Analysis Results */}
              {result.result ? (
                <div className="bg-white rounded-lg shadow p-6">
                  <h3 className="text-lg font-bold text-gray-900 mb-4">Analysis Results</h3>

                  {/* Overall Level */}
                  <div className="mb-6 pb-6 border-b border-gray-200">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-semibold text-gray-700">Overall Level</span>
                      <span className={`text-lg font-bold ${
                        result.result.overallLevel === 'ADVANCED' ? 'text-green-600' :
                        result.result.overallLevel === 'PROFICIENT' ? 'text-blue-600' :
                        result.result.overallLevel === 'EMERGING' ? 'text-yellow-600' :
                        'text-red-600'
                      }`}>
                        {result.result.overallLevel}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-gray-600">
                      <span>Confidence</span>
                      <span className="font-semibold">
                        {Math.round(result.result.overallConfidence * 100)}%
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                      <div
                        className="bg-blue-600 h-2 rounded-full"
                        style={{ width: `${result.result.overallConfidence * 100}%` }}
                      />
                    </div>
                  </div>

                  {/* Identified Gaps */}
                  {result.result.allGaps && result.result.allGaps.length > 0 && (
                    <div className="mb-6 pb-6 border-b border-gray-200">
                      <h4 className="text-sm font-semibold text-gray-900 mb-3">Identified Gaps</h4>
                      <ul className="space-y-2">
                        {result.result.allGaps.map((gap, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-sm text-gray-700">
                            <span className="text-yellow-600 font-bold">•</span>
                            <span>{gap}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Next Steps */}
                  {result.result.nextSteps && result.result.nextSteps.length > 0 && (
                    <div>
                      <h4 className="text-sm font-semibold text-gray-900 mb-3">Next Steps</h4>
                      <ul className="space-y-2">
                        {result.result.nextSteps.map((step, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-sm text-gray-700">
                            <span className="text-green-600 font-bold">✓</span>
                            <span>{step}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              ) : (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                  <p className="text-sm text-blue-800">
                    Assessment completed but results are still being processed. Please refresh the page in a moment.
                  </p>
                </div>
              )}

              {/* Start New Button */}
              <div className="text-center pt-8">
                <button
                  onClick={handleStartNew}
                  className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition"
                >
                  Analyze Another Snapshot
                </button>
              </div>
            </div>
          ) : isFailed ? (
            // Show error state
            <div className="bg-red-50 border border-red-200 rounded-lg p-8">
              <div className="text-center">
                <span className="text-5xl mb-4 block">❌</span>
                <h2 className="text-2xl font-bold text-red-900 mb-2">Assessment Failed</h2>
                <p className="text-red-700 mb-6">
                  {error?.message || 'An unexpected error occurred during assessment.'}
                </p>
                <button
                  onClick={handleStartNew}
                  className="px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition"
                >
                  Try Again
                </button>
              </div>
            </div>
          ) : (
            // Show submission form during processing
            <AssessmentSubmissionForm
              onSubmit={handleSubmit}
              isLoading={true}
              disabled={true}
            />
          )}
        </div>

        {/* Progress Overlay */}
        {isProcessing && progress && (
          <AssessmentProgressOverlay
            progress={progress}
            isConnected={isPolling}
            error={error}
          />
        )}
      </div>
    </div>
  );
};
