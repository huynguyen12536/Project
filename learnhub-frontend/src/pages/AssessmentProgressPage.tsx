import React, { useState } from 'react';
import { GapAnalysisPanel } from '@/components/GapAnalysisPanel';
import { RadarChartCard } from '@/components/RadarChartCard';
import { AssessmentProgressOverlay } from '@/components/AssessmentProgressOverlay';
import { AssessmentSubmissionForm } from '@/components/AssessmentSubmissionForm';
import { useAssessmentStore } from '@/store/assessmentStore';
import './AssessmentProgressPage.css';

export const AssessmentProgressPage: React.FC = () => {
  const {
    status,
    resultsData,
    error,
    isSubmitting
  } = useAssessmentStore();
  const [selectedAxis, setSelectedAxis] = useState<string | undefined>();

  // PENDING: Show submission form
  if (status === 'PENDING' && !isSubmitting) {
    return (
      <div className="assessment-page">
        <AssessmentSubmissionForm />
      </div>
    );
  }

  // PROCESSING: Show progress overlay with animation
  if (status === 'PROCESSING' || isSubmitting) {
    return (
      <div className="assessment-page">
        <AssessmentProgressOverlay />
      </div>
    );
  }

  // COMPLETED: Show rich results dashboard
  if (status === 'COMPLETED' && resultsData) {
    return (
      <div className="assessment-page">
        <div className="results-container">
          <header className="results-header">
            <div className="header-content">
              <h1>✅ Assessment Complete</h1>
              <p className="subtitle">
                Your repository has been analyzed across 4 competency dimensions
              </p>
            </div>
          </header>

          <section className="results-content">
            <div className="chart-column">
              <RadarChartCard
                data={resultsData}
                onAxisSelect={setSelectedAxis}
              />
            </div>

            <div className="analysis-column">
              <GapAnalysisPanel
                radarData={resultsData}
                selectedAxis={selectedAxis}
              />
            </div>
          </section>

          <footer className="results-footer">
            <div className="footer-actions">
              <button className="btn btn-primary" onClick={() => {/* download report */}}>
                📥 Download Full Report
              </button>
              <button className="btn btn-secondary" onClick={() => window.location.reload()}>
                🔄 Run Another Assessment
              </button>
            </div>
          </footer>
        </div>
      </div>
    );
  }

  // FAILED: Show error message
  if (status === 'FAILED' || error) {
    return (
      <div className="assessment-page">
        <div className="error-container">
          <div className="error-card">
            <h2>❌ Assessment Failed</h2>
            <p className="error-message">
              {error || 'An unexpected error occurred during assessment processing.'}
            </p>
            <button
              className="btn btn-primary"
              onClick={() => window.location.reload()}
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Default: Show empty state
  return (
    <div className="assessment-page">
      <div className="empty-state">
        <p>Loading...</p>
      </div>
    </div>
  );
};
