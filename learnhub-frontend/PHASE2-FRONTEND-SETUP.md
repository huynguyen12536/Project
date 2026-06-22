# Phase 2 Frontend Setup: Real Backend Integration

## Overview

This document describes how to wire the LearnHub frontend to the real Spring Boot backend API, replacing mock providers with actual SSE (Server-Sent Events) and HTTP endpoints.

## Key Changes Made

### 1. Removed Mock Backend Dependencies
- Mock backend is no longer imported in active components
- `learnhub-frontend/src/mocks/assessment-mock-backend.ts` remains for testing but is unused in production code
- All assessment hooks now call real backend endpoints

### 2. Updated API Endpoints
All endpoints now use the full `/api/v1` path to ensure proper routing:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/assessments` | POST | Submit a new assessment (requires snapshotId) |
| `/api/v1/assessments/{id}` | GET | Get assessment by ID |
| `/api/v1/assessments` | GET | Get all user assessments |
| `/api/v1/assessments/{id}/details` | GET | Get detailed results (with skill scores) |
| `/api/v1/assessments/{id}/progress` | GET | SSE stream for real-time progress |
| `/api/v1/assessments/{id}/progress/poll` | GET | Polling endpoint for progress (fallback) |

### 3. Updated Types and Payloads

**AssessmentSubmissionPayload** (changed):
```typescript
// Before: { repositoryUrl: string }
// After: { snapshotId: string }
```

**Why**: The backend requires a snapshotId (UUID) from a previously created repository snapshot, not a repository URL directly.

### 4. Configuration Files

#### Vite Proxy (vite.config.ts)
```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  },
}
```

This ensures:
- Requests to `http://localhost:3000/api/v1/...` are forwarded to `http://localhost:8080/api/v1/...`
- CORS issues are avoided during development
- Same origin for cookies/auth tokens

#### Environment Variables (.env.local)
```
VITE_API_BASE_URL=http://localhost:8080
```

This is optional and used for building documentation only. The proxy handles all requests during development.

## Starting the Frontend Dev Server

### Prerequisites
- Node.js 18+ installed
- Backend running on `http://localhost:8080`
- User authenticated with valid JWT token stored in `localStorage.authToken`

### Start Development Server
```bash
cd learnhub-frontend
npm install
npm run dev
```

Frontend will be available at: `http://localhost:3000`

## Assessment Flow

### Complete Flow (End-to-End)

1. **User Authenticates** 
   - JWT token stored in `localStorage.authToken`
   - Frontend requests require this token

2. **User Selects Repository** (Not in this PR, but prerequisite)
   - User goes through GitHub OAuth flow (already implemented)
   - Or uses repository selection UI (to be implemented)

3. **User Creates Snapshot** (Not in this PR, but prerequisite)
   - POST `/api/v1/repositories/{repoId}/snapshot`
   - Backend returns `snapshotId` (UUID)

4. **User Submits Assessment**
   - Form shows snapshot ID input field
   - User pastes snapshot ID from previous step
   - Form validates UUID format
   - POST `/api/v1/assessments` with `{ snapshotId }`

5. **Backend Creates Assessment**
   - Returns immediately with `{ assessmentId, status: "PENDING" }`
   - Assessment is enqueued asynchronously

6. **Frontend Shows Progress Overlay**
   - useAssessmentProgress hook activates
   - Attempts SSE connection to `/api/v1/assessments/{id}/progress`
   - Fallback to polling at `/api/v1/assessments/{id}/progress/poll` if SSE fails

7. **Progress Events Stream In**
   - SSE sends AssessmentProgressEvent every 1-2 seconds
   - Frontend updates progress bar (0% → 100%)
   - Frontend shows current step (analyzing → extracting_skills → calculating_scores → generating_report)
   - Frontend shows queue position if PENDING

8. **Assessment Completes**
   - Status changes to "COMPLETED"
   - useAssessmentProgress hook stops
   - Frontend fetches results from `/api/v1/assessments/{id}/details`
   - Results include:
     - languageDistribution: { "TypeScript": 45, "JavaScript": 30, ... }
     - skillScores: [ { skillName: "React", score: 85, category: "Frontend" }, ... ]
     - overallScore: 80

9. **Radar Chart Renders**
   - AssessmentProgressPage detects `status === COMPLETED` and `result !== null`
   - Hides progress overlay
   - Shows AssessmentSubmissionForm → form disappears
   - Shows RadarChartCard with skill scores
   - Shows language distribution bars
   - User can click "Analyze Another Repository" to start over

## Expected UI Rendering Flow

### Step 1: Initial Page Load
```
┌─────────────────────────────────────┐
│   Code Skills Assessment            │
│   Analyze your GitHub repository    │
├─────────────────────────────────────┤
│  [Input: Snapshot ID]               │
│  [Submit for Assessment]            │
└─────────────────────────────────────┘
```

### Step 2: After Submission (Processing)
```
┌─────────────────────────────────────┐
│   Code Skills Assessment            │
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │ Assessment in Progress          ││
│  │ 🔍 Analyzing Repository         ││
│  │ [████░░░░░░░░░░░░░░] 40%        ││
│  │ Confidence: [███░░░░░░░░░░] 60% ││
│  │ 🟢 Connected                    ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Step 3: Completion (Results)
```
┌─────────────────────────────────────┐
│   Code Skills Assessment            │
├─────────────────────────────────────┤
│  ✅ Assessment Complete!            │
│                                     │
│  Repository: github.com/...         │
│                                     │
│  Language Distribution              │
│  TypeScript:  [████████░░] 80%      │
│  JavaScript:  [█████░░░░░] 50%      │
│  Python:      [███░░░░░░░] 30%      │
│                                     │
│  Skill Scores (Radar Chart)         │
│     React: 85/100                   │
│     TypeScript: 90/100              │
│     Node.js: 80/100                 │
│                                     │
│  [Analyze Another Repository]       │
└─────────────────────────────────────┘
```

## Error Handling

### Connection Errors
- **404 Assessment Not Found**: Display error message, allow user to try again
- **401 Unauthorized**: Redirect to login
- **409 Rate Limited**: Display "Too many requests, try again in X seconds"
- **SSE Timeout**: Automatically fallback to polling
- **Network Error**: Display connection status in overlay, retry polling

### User Experience
1. Form shows validation errors for invalid UUID format
2. Progress overlay shows connection status (green/red/yellow indicator)
3. If SSE fails, automatic fallback to polling (silent to user)
4. Error messages are user-friendly and actionable

## Testing

### Manual Testing Checklist
- [ ] Frontend runs on http://localhost:3000
- [ ] Backend runs on http://localhost:8080
- [ ] User is authenticated (JWT in localStorage)
- [ ] Snapshot ID input accepts valid UUIDs
- [ ] Invalid UUIDs show validation error
- [ ] After submission, progress overlay appears
- [ ] Progress bar animates from 0% to 100%
- [ ] Progress events update in real-time from SSE
- [ ] If SSE fails, polling fallback works
- [ ] Current step updates (analyzing → extracting → calculating → generating)
- [ ] When status === COMPLETED, progress overlay closes
- [ ] Radar chart renders with skill scores
- [ ] Language distribution shows percentages
- [ ] "Analyze Another Repository" button works
- [ ] Network errors show user-friendly messages

### Testing with Real Backend

1. Start backend:
   ```bash
   cd learnhub-backend
   mvn spring-boot:run
   ```

2. Authenticate user:
   - Use OAuth login flow or manually set localStorage.authToken

3. Create a snapshot (or use existing):
   - POST `/api/v1/repositories/{repoId}/snapshot`
   - Copy the returned `snapshotId`

4. Start frontend:
   ```bash
   cd learnhub-frontend
   npm run dev
   ```

5. Test assessment submission:
   - Paste snapshot ID into form
   - Click "Submit for Assessment"
   - Watch progress bar animate
   - Wait for completion

## Troubleshooting

### "EventSource connection failed"
- **Cause**: Backend not running or CORS misconfigured
- **Solution**: 
  1. Ensure backend runs on `http://localhost:8080`
  2. Check backend logs for CORS headers
  3. Frontend will automatically fallback to polling

### "Assessment not found" (404)
- **Cause**: Invalid or expired assessment ID
- **Solution**: Ensure snapshot ID is correct and assessment was just submitted

### "Unauthorized" (401)
- **Cause**: JWT token expired or missing
- **Solution**: Re-authenticate via login flow

### Progress bar not animating
- **Cause**: SSE not streaming or backend not processing
- **Solution**:
  1. Check browser DevTools → Network → type "EventStream"
  2. Verify backend is enqueuing assessment jobs
  3. Check backend logs for assessment processing

### API requests going to wrong URL
- **Cause**: Proxy misconfigured
- **Solution**: Ensure `/api` requests proxy to `http://localhost:8080` in vite.config.ts

## File Structure

```
learnhub-frontend/
├── .env.local                          # Environment variables
├── vite.config.ts                      # Vite config with API proxy
├── src/
│   ├── hooks/
│   │   ├── useAssessmentProgress.ts   # SSE + polling progress tracking
│   │   ├── useAssessmentSubmission.ts # Submit assessment + result fetching
│   │   └── index.ts
│   ├── components/assessment/
│   │   ├── assessment-progress-page.tsx    # Main page component
│   │   ├── assessment-submission-form.tsx  # Input form (snapshotId)
│   │   ├── assessment-progress-overlay.tsx # Real-time progress display
│   │   └── radar-chart-card.tsx            # Skill scores visualization
│   ├── stores/
│   │   └── assessment-store.ts        # Zustand store for assessment state
│   ├── services/
│   │   └── api.ts                     # Axios client with interceptors
│   ├── types/
│   │   └── index.ts                   # TypeScript interfaces
│   └── mocks/                         # Mock backend (unused in production)
│       └── assessment-mock-backend.ts # For testing only
└── PHASE2-FRONTEND-SETUP.md           # This file
```

## Architecture Decisions

### Why Relative URLs?
- Vite's proxy handles `/api/*` routing to backend
- Same-origin requests include auth cookies automatically
- No need to hardcode backend URL in frontend code
- Works with any backend URL in production

### Why SSE + Polling Fallback?
- SSE is efficient for real-time updates (one-way push)
- Some networks/proxies block SSE (e.g., old firewalls)
- Polling is a reliable fallback (2-second intervals)
- User never knows which is used (transparent fallback)

### Why Poll via HTTP GET?
- Stateless: no connection state to maintain
- Cache-friendly: responses can be cached
- Easier to debug: standard HTTP requests
- Works with any proxy/load balancer

### Why snapshotId Instead of URL?
- Immutable: snapshot captures repo state at a point in time
- Safe: backend creates snapshot atomically
- Versioned: multiple snapshots of same repo possible
- Avoids re-fetching: snapshot already cached in DB

## Next Steps

1. **Repository Selection UI** (if not implemented):
   - Add component to browse GitHub repositories
   - Call `POST /api/v1/repositories/{repoId}/select`

2. **Snapshot Management UI** (if not implemented):
   - Show user's previous snapshots
   - Add "Create Snapshot" button
   - Call `POST /api/v1/repositories/{repoId}/snapshot`
   - Display snapshot ID for assessment

3. **Assessment History**:
   - Fetch user's assessments: `GET /api/v1/assessments`
   - Allow users to re-run assessments on different snapshots
   - Cache previous results

4. **Export Results**:
   - Add button to export skill scores as PDF/CSV
   - Include radar chart visualization

5. **Sharing Results**:
   - Generate shareable links to results
   - Public profile with assessment history

## Support

For issues or questions:
1. Check browser DevTools Console for error messages
2. Check backend logs at `learnhub-backend/logs/`
3. Verify all prerequisites are met (Node.js, npm, backend running)
4. Re-run `npm install` if dependencies are missing
