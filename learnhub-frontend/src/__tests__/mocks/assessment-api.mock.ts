/**
 * Mock Backend for Assessment API (E2E Testing)
 *
 * Provides mock implementations of assessment API endpoints
 * for frontend E2E tests without requiring a real backend.
 *
 * Supports:
 * - Assessment submission
 * - Progress polling
 * - SSE event streaming
 * - Error scenarios (409, 429, 500)
 * - Rate limiting simulation
 * - Network latency simulation
 *
 * Usage:
 * import { setupAssessmentMocks } from './mocks/assessment-api.mock';
 *
 * test.beforeEach(async ({ page }) => {
 *   await setupAssessmentMocks(page);
 * });
 */

import { Page, Route } from '@playwright/test';

export interface AssessmentState {
  id: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progressPercent: number;
  currentStep: string;
  estimatedSecondsRemaining: number;
  confidence: number;
  timestamp: string;
}

/**
 * Global mock state for all assessments
 */
const assessmentStates: Map<string, AssessmentState> = new Map();
const userAssessments: Map<string, string[]> = new Map();
let submissionCount = 0;
let rateLimitUserId = '';

/**
 * Simulate assessment progress updates
 */
function simulateProgress(assessmentId: string, state: AssessmentState): void {
  const interval = setInterval(() => {
    if (!assessmentStates.has(assessmentId)) {
      clearInterval(interval);
      return;
    }

    const current = assessmentStates.get(assessmentId)!;

    // Progress states: PENDING -> PROCESSING -> COMPLETED
    if (current.status === 'PENDING') {
      current.status = 'PROCESSING';
      current.progressPercent = 10;
      current.currentStep = 'language_detection';
      current.estimatedSecondsRemaining = 30;
    } else if (current.status === 'PROCESSING') {
      // Increment progress
      current.progressPercent = Math.min(100, current.progressPercent + Math.random() * 20);

      // Update step based on progress
      if (current.progressPercent < 30) {
        current.currentStep = 'language_detection';
      } else if (current.progressPercent < 60) {
        current.currentStep = 'skill_extraction';
      } else if (current.progressPercent < 90) {
        current.currentStep = 'competency_scoring';
      } else {
        current.currentStep = 'finalizing_results';
      }

      current.estimatedSecondsRemaining = Math.max(
        0,
        Math.ceil((100 - current.progressPercent) / 10)
      );
      current.confidence = Math.min(1.0, current.progressPercent / 100 * 0.9 + 0.1);

      // Complete when progress reaches 100%
      if (current.progressPercent >= 100) {
        current.status = 'COMPLETED';
        current.progressPercent = 100;
        current.currentStep = 'completed';
        current.estimatedSecondsRemaining = 0;
        current.confidence = 0.95;
        clearInterval(interval);
      }
    }

    current.timestamp = new Date().toISOString();
    assessmentStates.set(assessmentId, current);
  }, 1500); // Update every 1.5 seconds
}

/**
 * Setup mock routes for assessment API
 */
export async function setupAssessmentMocks(page: Page, options?: {
  latency?: number;
  failureRate?: number;
  enableSSE?: boolean;
}): Promise<void> {
  const latency = options?.latency || 0;
  const failureRate = options?.failureRate || 0;
  const enableSSE = options?.enableSSE !== false;

  // Apply network latency if specified
  if (latency > 0) {
    await page.route('**/*', async (route) => {
      await new Promise(resolve => setTimeout(resolve, latency));
      await route.continue();
    });
  }

  // Mock POST /api/v1/assessments (submit assessment)
  await page.route('**/api/v1/assessments', async (route: Route) => {
    if (route.request().method() === 'POST') {
      // Simulate random failures for chaos testing
      if (Math.random() < failureRate) {
        route.abort('failed');
        return;
      }

      try {
        const body = route.request().postDataJSON();
        const snapshotId = body.snapshotId;

        // Check for duplicate submission within 60s
        const existingAssessments = userAssessments.get('test-user') || [];
        const duplicateCheck = assessmentStates.values();
        for (const assessment of duplicateCheck) {
          const createdTime = new Date(assessment.timestamp).getTime();
          const now = Date.now();
          if (now - createdTime < 60000) {
            // Duplicate within 60s
            route.respond({
              status: 409,
              contentType: 'application/json',
              body: JSON.stringify({
                error: 'Duplicate submission within 60 seconds',
                existingAssessmentId: assessment.id,
              }),
            });
            return;
          }
        }

        // Check rate limit (3 concurrent)
        const processingAssessments = Array.from(assessmentStates.values())
          .filter(a => a.status === 'PROCESSING' || a.status === 'PENDING');
        if (processingAssessments.length >= 3) {
          route.respond({
            status: 429,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'Rate limit exceeded: maximum 3 concurrent assessments',
              retryAfter: 60,
            }),
          });
          return;
        }

        // Create assessment
        const assessmentId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        const assessment: AssessmentState = {
          id: assessmentId,
          status: 'PENDING',
          progressPercent: 0,
          currentStep: 'queued',
          estimatedSecondsRemaining: 45,
          confidence: 0,
          timestamp: new Date().toISOString(),
        };

        assessmentStates.set(assessmentId, assessment);
        const userId = 'test-user';
        if (!userAssessments.has(userId)) {
          userAssessments.set(userId, []);
        }
        userAssessments.get(userId)!.push(assessmentId);

        // Start progress simulation
        simulateProgress(assessmentId, assessment);

        // Return 201 Created
        route.respond({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: assessmentId,
            snapshotId,
            status: 'PENDING',
            createdAt: assessment.timestamp,
          }),
        });
      } catch (error) {
        route.respond({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Invalid request' }),
        });
      }
    } else {
      route.continue();
    }
  });

  // Mock GET /api/v1/assessments/{id}/progress (polling endpoint)
  await page.route('**/api/v1/assessments/*/progress', async (route: Route) => {
    if (route.request().method() === 'GET') {
      // Extract assessment ID from URL
      const url = new URL(route.request().url());
      const pathParts = url.pathname.split('/');
      const assessmentId = pathParts[pathParts.length - 2];

      if (assessmentStates.has(assessmentId)) {
        const assessment = assessmentStates.get(assessmentId)!;
        route.respond({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(assessment),
        });
      } else {
        route.respond({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Assessment not found' }),
        });
      }
    } else {
      route.continue();
    }
  });

  // Mock GET /api/v1/assessments/{id}/progress/stream (SSE endpoint)
  if (enableSSE) {
    await page.route('**/api/v1/assessments/*/progress/stream', async (route: Route) => {
      if (route.request().method() === 'GET') {
        // Extract assessment ID from URL
        const url = new URL(route.request().url());
        const pathParts = url.pathname.split('/');
        const assessmentId = pathParts[pathParts.length - 2];

        if (assessmentStates.has(assessmentId)) {
          // For SSE, we'd need to handle streaming differently
          // This is a simplified mock that responds with initial state
          const assessment = assessmentStates.get(assessmentId)!;
          route.respond({
            status: 200,
            contentType: 'text/event-stream',
            headers: {
              'Cache-Control': 'no-cache',
              'Connection': 'keep-alive',
            },
            body: `data: ${JSON.stringify(assessment)}\n\n`,
          });
        } else {
          route.respond({
            status: 404,
            contentType: 'application/json',
            body: JSON.stringify({ error: 'Assessment not found' }),
          });
        }
      } else {
        route.continue();
      }
    });
  }

  // Mock GET /api/v1/assessments/{id} (get assessment details)
  await page.route('**/api/v1/assessments/*', async (route: Route) => {
    if (route.request().method() === 'GET') {
      const url = new URL(route.request().url());

      // Ignore progress and progress/stream endpoints
      if (url.pathname.includes('/progress')) {
        route.continue();
        return;
      }

      const pathParts = url.pathname.split('/');
      const assessmentId = pathParts[pathParts.length - 1];

      if (assessmentStates.has(assessmentId)) {
        const assessment = assessmentStates.get(assessmentId)!;
        route.respond({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(assessment),
        });
      } else {
        route.respond({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Assessment not found' }),
        });
      }
    } else {
      route.continue();
    }
  });

  // Mock GET /api/v1/assessments (list assessments)
  await page.route('**/api/v1/assessments', async (route: Route) => {
    if (route.request().method() === 'GET') {
      const userId = 'test-user';
      const assessmentIds = userAssessments.get(userId) || [];
      const assessments = assessmentIds
        .map(id => assessmentStates.get(id))
        .filter(Boolean);

      route.respond({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          assessments,
          total: assessments.length,
        }),
      });
    } else {
      route.continue();
    }
  });
}

/**
 * Clear all mock state (call between tests)
 */
export function clearAssessmentMocks(): void {
  assessmentStates.clear();
  userAssessments.clear();
  submissionCount = 0;
  rateLimitUserId = '';
}

/**
 * Get assessment state (for test assertions)
 */
export function getAssessmentState(assessmentId: string): AssessmentState | undefined {
  return assessmentStates.get(assessmentId);
}

/**
 * Get all assessments
 */
export function getAllAssessments(): Map<string, AssessmentState> {
  return new Map(assessmentStates);
}

/**
 * Manually trigger assessment completion (for testing)
 */
export function completeAssessment(assessmentId: string): void {
  if (assessmentStates.has(assessmentId)) {
    const assessment = assessmentStates.get(assessmentId)!;
    assessment.status = 'COMPLETED';
    assessment.progressPercent = 100;
    assessment.currentStep = 'completed';
    assessment.estimatedSecondsRemaining = 0;
    assessment.confidence = 0.95;
  }
}

/**
 * Manually fail assessment (for testing)
 */
export function failAssessment(assessmentId: string, reason: string = 'Test failure'): void {
  if (assessmentStates.has(assessmentId)) {
    const assessment = assessmentStates.get(assessmentId)!;
    assessment.status = 'FAILED';
    assessment.currentStep = `failed: ${reason}`;
  }
}
