/**
 * Mock Backend for Assessment Testing
 *
 * Simulates backend API responses for assessment submission and progress tracking.
 * - Mock submission endpoint
 * - Mock SSE progress emitter
 * - Mock result endpoint
 *
 * Usage:
 *   const mockBackend = new AssessmentMockBackend();
 *   mockBackend.emitProgress({ ...progressEvent });
 */

import { AssessmentProgressEvent, AssessmentResult, AssessmentStatus } from '../types';

export interface MockAssessmentState {
  assessmentId: string;
  status: AssessmentStatus;
  progressEvents: AssessmentProgressEvent[];
}

/**
 * Mock SSE Emitter for testing progress updates
 */
export class MockSSEEmitter {
  private listeners: Record<string, Set<Function>> = {
    progress: new Set(),
    error: new Set(),
  };

  addEventListener(type: string, listener: Function) {
    if (!this.listeners[type]) {
      this.listeners[type] = new Set();
    }
    this.listeners[type].add(listener);
  }

  removeEventListener(type: string, listener: Function) {
    if (this.listeners[type]) {
      this.listeners[type].delete(listener);
    }
  }

  emitProgress(data: AssessmentProgressEvent) {
    this.listeners['progress'].forEach((listener) => {
      (listener as Function)({
        data: JSON.stringify(data),
      });
    });
  }

  emitError(error?: Error) {
    this.listeners['error'].forEach((listener) => {
      (listener as Function)(error || new Error('SSE connection error'));
    });
  }

  close() {
    this.listeners = { progress: new Set(), error: new Set() };
  }
}

/**
 * Mock Backend for Assessment API
 */
export class AssessmentMockBackend {
  private assessments: Map<string, MockAssessmentState> = new Map();
  private sseEmitters: Map<string, MockSSEEmitter> = new Map();
  private queue: string[] = [];
  private processingTimeout: NodeJS.Timeout | null = null;

  /**
   * Simulate assessment submission
   */
  submitAssessment(_repositoryUrl: string): { assessmentId: string; status: AssessmentStatus } {
    const assessmentId = `mock-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

    const assessmentState: MockAssessmentState = {
      assessmentId,
      status: AssessmentStatus.PENDING,
      progressEvents: [],
    };

    this.assessments.set(assessmentId, assessmentState);
    this.queue.push(assessmentId);

    // Start processing after a short delay
    this.processQueue();

    return {
      assessmentId,
      status: AssessmentStatus.PENDING,
    };
  }

  /**
   * Simulate processing queue
   */
  private processQueue() {
    if (this.processingTimeout) {
      clearTimeout(this.processingTimeout);
    }

    if (this.queue.length === 0) return;

    const assessmentId = this.queue[0];
    const state = this.assessments.get(assessmentId);

    if (!state) return;

    // Transition from PENDING to PROCESSING
    if (state.status === AssessmentStatus.PENDING) {
      state.status = AssessmentStatus.PROCESSING;

      this.processingTimeout = setTimeout(() => {
        this.simulateProgress(assessmentId);
      }, 1000);
    }
  }

  /**
   * Simulate progress events
   */
  private simulateProgress(assessmentId: string) {
    const state = this.assessments.get(assessmentId);
    if (!state || state.status !== AssessmentStatus.PROCESSING) return;

    const steps = [
      { step: 'analyzing', progress: 25 },
      { step: 'extracting_skills', progress: 50 },
      { step: 'calculating_scores', progress: 75 },
      { step: 'generating_report', progress: 100 },
    ];

    let currentStepIndex = 0;

    const emitNextStep = () => {
      if (currentStepIndex >= steps.length) {
        // Complete assessment
        state.status = AssessmentStatus.COMPLETED;
        this.queue.shift();
        this.processQueue();
        return;
      }

      const { step, progress } = steps[currentStepIndex];
      const progressEvent: AssessmentProgressEvent = {
        assessmentId,
        status: AssessmentStatus.PROCESSING,
        progressPercent: progress,
        currentStep: step,
        queuePosition: 0,
        confidence: 0.5 + (currentStepIndex * 0.15),
        timestamp: new Date().toISOString(),
      };

      state.progressEvents.push(progressEvent);

      // Emit via SSE if listener is attached
      const emitter = this.sseEmitters.get(assessmentId);
      if (emitter) {
        emitter.emitProgress(progressEvent);
      }

      currentStepIndex++;
      this.processingTimeout = setTimeout(emitNextStep, 1500);
    };

    emitNextStep();
  }

  /**
   * Get current progress for polling
   */
  getProgress(assessmentId: string): AssessmentProgressEvent | null {
    const state = this.assessments.get(assessmentId);
    if (!state) return null;

    const lastEvent = state.progressEvents[state.progressEvents.length - 1];

    if (state.status === AssessmentStatus.PENDING && !lastEvent) {
      return {
        assessmentId,
        status: AssessmentStatus.PENDING,
        progressPercent: 0,
        currentStep: 'queued',
        queuePosition: this.queue.indexOf(assessmentId),
        confidence: 0,
        timestamp: new Date().toISOString(),
      };
    }

    return lastEvent || null;
  }

  /**
   * Get assessment result
   */
  getResult(assessmentId: string): AssessmentResult | null {
    const state = this.assessments.get(assessmentId);

    if (!state || state.status !== AssessmentStatus.COMPLETED) {
      return null;
    }

    return {
      assessmentId,
      status: AssessmentStatus.COMPLETED,
      repositoryUrl: 'https://github.com/mock/repository',
      languageDistribution: {
        TypeScript: 45,
        JavaScript: 30,
        Python: 15,
        CSS: 10,
      },
      skillScores: [
        { skillName: 'React', score: 85, category: 'Frontend' },
        { skillName: 'TypeScript', score: 90, category: 'Backend' },
        { skillName: 'Node.js', score: 80, category: 'Backend' },
        { skillName: 'Docker', score: 75, category: 'DevOps' },
        { skillName: 'PostgreSQL', score: 80, category: 'Database' },
        { skillName: 'AWS', score: 70, category: 'DevOps' },
      ],
      overallScore: 80,
      completedAt: new Date().toISOString(),
    };
  }

  /**
   * Create SSE emitter for assessment
   */
  createSSEEmitter(assessmentId: string): MockSSEEmitter {
    const emitter = new MockSSEEmitter();
    this.sseEmitters.set(assessmentId, emitter);

    // Emit current progress if available
    const progress = this.getProgress(assessmentId);
    if (progress) {
      emitter.emitProgress(progress);
    }

    return emitter;
  }

  /**
   * Cleanup
   */
  reset() {
    this.assessments.clear();
    this.sseEmitters.forEach((emitter) => emitter.close());
    this.sseEmitters.clear();
    this.queue = [];
    if (this.processingTimeout) {
      clearTimeout(this.processingTimeout);
    }
  }

  /**
   * Get all assessments (for debugging)
   */
  getAssessments(): Map<string, MockAssessmentState> {
    return this.assessments;
  }
}
