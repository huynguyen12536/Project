# LearnHub Frontend - Quick Start Guide

## 5-Minute Setup

### 1. Install Dependencies
```bash
cd learnhub-frontend
npm install
```

### 2. Start Development Server
```bash
npm run dev
```

Frontend runs at: **http://localhost:3000**

### 3. Make Sure Backend is Running
```bash
# In another terminal, from project root:
cd learnhub-backend
mvn spring-boot:run
```

Backend runs at: **http://localhost:8080**

### 4. Test the Flow

1. Open http://localhost:3000 in browser
2. Login (requires JWT token in localStorage)
3. Get a snapshot ID:
   - Call: `POST http://localhost:8080/api/v1/repositories/{repoId}/snapshot`
   - Copy the returned `snapshotId`
4. Paste snapshot ID into the form
5. Click "Submit for Assessment"
6. Watch progress bar animate
7. Wait for completion, see results

## Key Files

| File | Purpose |
|------|---------|
| `src/hooks/useAssessmentProgress.ts` | SSE + polling for progress |
| `src/hooks/useAssessmentSubmission.ts` | Submit assessment, fetch results |
| `src/components/assessment/assessment-progress-page.tsx` | Main page component |
| `src/types/index.ts` | TypeScript types matching backend |
| `src/services/api.ts` | Axios HTTP client with auth |
| `.env.local` | Environment variables |
| `vite.config.ts` | Vite config with API proxy |

## Common Commands

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm run test

# Run type check
npm run type-check
```

## API Endpoints

All endpoints are under `/api/v1`:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/assessments` | Submit assessment (requires snapshotId) |
| GET | `/assessments/{id}` | Get assessment details |
| GET | `/assessments` | List user's assessments |
| GET | `/assessments/{id}/details` | Get full results with analysis |
| GET | `/assessments/{id}/progress` | SSE stream for real-time progress |
| GET | `/assessments/{id}/progress/poll` | Polling endpoint for progress |

## Debugging Tips

### Check SSE Connection
1. Open DevTools (F12)
2. Go to Network tab
3. Filter for "EventStream"
4. You should see a GET request to `/api/v1/assessments/{id}/progress`
5. Look at "Messages" tab to see events

### Check API Calls
1. Open DevTools (F12)
2. Go to Network tab
3. Filter for "XHR" (XMLHttpRequest)
4. Submit assessment
5. Watch for POST to `/api/v1/assessments` and GET to `/api/v1/assessments/{id}/progress/poll`

### Check Store State
1. Open DevTools (F12)
2. Go to Console
3. Type: `localStorage.getItem('authToken')`
4. Should see your JWT token

### Check Proxy
1. Open DevTools (F12)
2. Go to Network tab
3. Submit assessment
4. Look for requests to `/api/v1/...`
5. Request URL should show relative path, not http://localhost:8080

## Environment Variables

### Development (.env.local)
```
VITE_API_BASE_URL=http://localhost:8080
```

### Production (via deployment)
Set via environment variable or build-time configuration:
```bash
REACT_APP_API_URL=https://api.example.com npm run build
```

## Troubleshooting

### "Cannot POST /api/v1/assessments"
- Backend not running on http://localhost:8080
- Vite proxy not working
- Fix: Start backend with `mvn spring-boot:run`

### "EventSource connection failed"
- Backend not reachable
- CORS misconfigured
- Fix: Check backend is running, check logs for CORS errors

### "Assessment not found" (404)
- Invalid or expired assessment ID
- Backend doesn't have assessment record
- Fix: Create new assessment, check returned assessmentId

### "Unauthorized" (401)
- JWT token expired or missing
- Fix: Login again to get new token

### Progress bar not updating
- SSE connection closed
- Backend not sending events
- Fix: Check DevTools Network tab for EventStream, check backend logs

## Architecture Overview

```
React App (Port 3000)
    ↓
Vite Dev Proxy
    ↓ (routes /api/* to backend)
    ↓
Spring Boot Backend (Port 8080)
    ↓
Assessment APIs
    ├── POST /api/v1/assessments (submit)
    ├── GET /api/v1/assessments/{id}/progress (SSE)
    ├── GET /api/v1/assessments/{id}/progress/poll (fallback)
    └── GET /api/v1/assessments/{id}/details (results)
```

## Next Steps

1. **Test end-to-end flow** between frontend and backend
2. **Implement Repository Selection UI** (browse GitHub repos)
3. **Implement Snapshot Management** (create snapshots)
4. **Add Story 2.4 support** (language detection, skill scores)
5. **Deploy to production** (configure VITE_API_BASE_URL)

## Getting Help

- Read `PHASE2-FRONTEND-SETUP.md` for comprehensive setup guide
- Check `/home/ubuntu/Project-Moi/PHASE2_FRONTEND_IMPLEMENTATION_SUMMARY.md` for detailed changes
- Review backend API docs at `learnhub-backend/docs/assessment-api.md`
- Check browser console for error messages
- Check backend logs for API errors

---

**Status**: Frontend ready to connect to real backend ✅
