/**
 * Tests for AssessmentMockBackend
 *
 * Verifies:
 * - Assessment submission and ID generation
 * - Progress event emission
 * - Result generation
 * - Queue management
 * - SSE emitter functionality
 */

import { AssessmentMockBackend, MockSSEEmitter } from './assessment-mock-backend';
import { AssessmentStatus } from '../types';

describe('AssessmentMockBackend', () => {
  let backend: AssessmentMockBackend;

  beforeEach(() => {
    backend = new AssessmentMockBackend();
  });

  afterEach(() => {
    backend.reset();
  });

  it('submits assessment and returns ID with PENDING status', () => {
    const result = backend.submitAssessment('https://github.com/test/repo');

    expect(result.assessmentId).toBeDefined();
    expect(result.assessmentId).toMatch(/^mock-/);
    expect(result.status).toBe(AssessmentStatus.PENDING);
  });

  it('generates unique assessment IDs', () => {
    const result1 = backend.submitAssessment('https://github.com/test/repo1');
    const result2 = backend.submitAssessment('https://github.com/test/repo2');

    expect(result1.assessmentId).not.toBe(result2.assessmentId);
  });

  it('returns pending progress initially', () => {
    const submission = backend.submitAssessment('https://github.com/test/repo');
    const progress = backend.getProgress(submission.assessmentId);

    expect(progress).not.toBeNull();
    expect(progress?.status).toBe(AssessmentStatus.PENDING);
    expect(progress?.progressPercent).toBe(0);
  });

  it('queues multiple assessments', () => {
    const submission1 = backend.submitAssessment('https://github.com/test/repo1');
    const submission2 = backend.submitAssessment('https://github.com/test/repo2');

    const progress1 = backend.getProgress(submission1.assessmentId);
    const progress2 = backend.getProgress(submission2.assessmentId);

    // Should have queue positions
    expect(progress1?.queuePosition).toBeDefined();
    expect(progress2?.queuePosition).toBeDefined();
  });

  it('returns null result for non-existent assessment', () => {
    const result = backend.getResult('non-existent-id');
    expect(result).toBeNull();
  });

  it('returns null result for incomplete assessment', () => {
    const submission = backend.submitAssessment('https://github.com/test/repo');
    const result = backend.getResult(submission.assessmentId);

    expect(result).toBeNull();
  });

  it('creates SSE emitter for assessment', () => {
    const submission = backend.submitAssessment('https://github.com/test/repo');
    const emitter = backend.createSSEEmitter(submission.assessmentId);

    expect(emitter).toBeInstanceOf(MockSSEEmitter);
  });

  it('SSE emitter emits progress events', (done) => {
    const submission = backend.submitAssessment('https://github.com/test/repo');
    const emitter = backend.createSSEEmitter(submission.assessmentId);

    const progressListener = jest.fn((event) => {
      expect(event.data).toBeDefined();
      const data = JSON.parse(event.data);
      expect(data.assessmentId).toBe(submission.assessmentId);
      done();
    });

    emitter.addEventListener('progress', progressListener);

    // Manually emit progress
    const progress = backend.getProgress(submission.assessmentId);
    if (progress) {
      emitter.emitProgress(progress);
    }
  });

  it('SSE emitter emits error events', (done) => {
    const submission = backend.submitAssessment('https://github.com/test/repo');
    const emitter = backend.createSSEEmitter(submission.assessmentId);

    const errorListener = jest.fn((error) => {
      expect(error).toBeInstanceOf(Error);
      done();
    });

    emitter.addEventListener('error', errorListener);
    emitter.emitError(new Error('Test error'));
  });

  it('SSE emitter can remove event listeners', () => {
    const emitter = new MockSSEEmitter();
    const listener = jest.fn();

    emitter.addEventListener('progress', listener);
    emitter.removeEventListener('progress', listener);

    emitter.emitProgress({
      assessmentId: 'test',
      status: AssessmentStatus.PROCESSING,
      progressPercent: 50,
      currentStep: 'analyzing',
      queuePosition: 0,
      confidence: 0.8,
      timestamp: new Date().toISOString(),
    });

    expect(listener).not.toHaveBeenCalled();
  });

  it('progresses assessment through phases', async () => {
    const submission = backend.submitAssessment('https://github.com/test/repo');

    // Wait for processing to start
    await new Promise((resolve) => setTimeout(resolve, 1500));

    const progress1 = backend.getProgress(submission.assessmentId);
    expect(progress1?.status).toBe(AssessmentStatus.PROCESSING);

    // Wait for multiple steps
    await new Promise((resolve) => setTimeout(resolve, 6000));

    const final = backend.getProgress(submission.assessmentId);
    expect(final).toBeDefined();
  });

  it('returns complete assessment result', async () => {
    const submission = backend.submitAssessment('https://github.com/test/repo');

    // Wait for completion (processing simulates ~6 seconds)
    await new Promise((resolve) => setTimeout(resolve, 7000));

    const result = backend.getResult(submission.assessmentId);

    expect(result).not.toBeNull();
    expect(result?.status).toBe(AssessmentStatus.COMPLETED);
    expect(result?.assessmentId).toBe(submission.assessmentId);
    expect(result?.skillScores.length).toBeGreaterThan(0);
    expect(result?.overallScore).toBeGreaterThan(0);
    expect(Object.keys(result?.languageDistribution || {}).length).toBeGreaterThan(0);
  });

  it('resets backend state', () => {
    const submission1 = backend.submitAssessment('https://github.com/test/repo1');
    const submission2 = backend.submitAssessment('https://github.com/test/repo2');

    backend.reset();

    const assessments = backend.getAssessments();
    expect(assessments.size).toBe(0);

    // Can submit new assessments after reset
    const submission3 = backend.submitAssessment('https://github.com/test/repo3');
    expect(submission3.assessmentId).toBeDefined();
  });

  it('returns all assessments for debugging', () => {
    const submission1 = backend.submitAssessment('https://github.com/test/repo1');
    const submission2 = backend.submitAssessment('https://github.com/test/repo2');

    const assessments = backend.getAssessments();

    expect(assessments.size).toBe(2);
    expect(assessments.has(submission1.assessmentId)).toBe(true);
    expect(assessments.has(submission2.assessmentId)).toBe(true);
  });
});
