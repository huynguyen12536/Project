/**
 * Component: AssessmentProgressOverlay
 *
 * Displays real-time progress overlay during assessment processing.
 * - Animated progress bar
 * - Current step/phase display
 * - Queue position or processing indicator
 * - Confidence score visualization
 * - Network connection status
 *
 * Usage:
 *   <AssessmentProgressOverlay
 *     progress={progressEvent}
 *     isConnected={isConnected}
 *     error={error}
 *   />
 */

import React from 'react';
import { AssessmentProgressEvent } from '../../types';

export interface AssessmentProgressOverlayProps {
  progress: AssessmentProgressEvent | null;
  isConnected: boolean;
  error: Error | null;
}

const getStepLabel = (step: string): string => {
  const stepLabels: Record<string, string> = {
    analyzing: 'Analyzing Repository',
    extracting_skills: 'Extracting Skills',
    generating_report: 'Generating Report',
    calculating_scores: 'Calculating Scores',
  };
  return stepLabels[step] || step;
};

const getStepIcon = (step: string): string => {
  const stepIcons: Record<string, string> = {
    analyzing: '🔍',
    extracting_skills: '🛠️',
    generating_report: '📊',
    calculating_scores: '⭐',
  };
  return stepIcons[step] || '⚙️';
};

export const AssessmentProgressOverlay: React.FC<AssessmentProgressOverlayProps> = ({
  progress,
  isConnected,
  error,
}) => {
  if (!progress) return null;

  const isInQueue = progress.queuePosition > 0;
  const connectionStatus = isConnected
    ? '🟢 Connected'
    : error
      ? '🔴 Disconnected'
      : '🟡 Connecting...';

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-8 space-y-6">
        {/* Header */}
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900">Assessment in Progress</h2>
          <p className="text-sm text-gray-600 mt-1">
            Assessment ID: <span className="font-mono text-xs">{progress.assessmentId}</span>
          </p>
        </div>

        {/* Queue or Processing Status */}
        {isInQueue ? (
          <div className="text-center bg-blue-50 rounded-lg p-4">
            <p className="text-lg font-semibold text-blue-900">📋 In Queue</p>
            <p className="text-sm text-blue-700 mt-1">Position: #{progress.queuePosition}</p>
            <p className="text-xs text-blue-600 mt-2">
              Your assessment will start shortly. Please wait...
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {/* Current Step */}
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-2xl">{getStepIcon(progress.currentStep)}</span>
                <span className="text-sm font-semibold text-gray-700">
                  {getStepLabel(progress.currentStep)}
                </span>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-blue-500 to-blue-600 h-full transition-all duration-300 ease-out"
                  style={{ width: `${progress.progressPercent}%` }}
                />
              </div>

              {/* Progress Percentage */}
              <p className="text-xs text-gray-600 mt-2 text-right">
                {progress.progressPercent}% Complete
              </p>
            </div>

            {/* Confidence Score */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-medium text-gray-600">Confidence</span>
                <span className="text-xs font-semibold text-gray-900">
                  {Math.round(progress.confidence * 100)}%
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-green-500 h-full transition-all duration-300"
                  style={{ width: `${progress.confidence * 100}%` }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Error Display */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <p className="text-sm text-red-800 font-medium">⚠️ Connection Issue</p>
            <p className="text-xs text-red-700 mt-1">{error.message}</p>
            <p className="text-xs text-red-600 mt-2">
              We're attempting to reconnect. Your progress is being tracked.
            </p>
          </div>
        )}

        {/* Connection Status Footer */}
        <div className="flex items-center justify-between pt-4 border-t border-gray-200">
          <div className="flex items-center gap-1">
            <span className="text-xs text-gray-600">Connection:</span>
            <span className="text-sm font-medium">{connectionStatus}</span>
          </div>
          <span className="text-xs text-gray-500">
            {new Date(progress.timestamp).toLocaleTimeString()}
          </span>
        </div>

        {/* Helpful Message */}
        <div className="text-center text-xs text-gray-600">
          <p>This window will automatically close when the assessment completes.</p>
        </div>
      </div>
    </div>
  );
};
