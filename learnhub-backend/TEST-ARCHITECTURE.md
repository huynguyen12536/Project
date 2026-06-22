# Phase 1 Test Architecture - Assessment Engine

**Date:** 2026-06-19  
**Status:** Architecture Scaffolded & Ready for Implementation  
**Test Framework:** JUnit 5, Mockito, Testcontainers, REST Assured, Awaitility

---

## Test Structure Overview

### Test Pyramid

```
           │         E2E Tests (Playwright)
           │        /                  \
           │       / 20% (4 scenarios)  \
           │      ╱________________________╲
           │     │   Integration Tests    │
           │     │   40% (8+ tests)       │
           │     ╱________________________╲
           │    │     Unit Tests (30%)    │
           │────│   Job, Queue, Events   │────
           └────────────────────────────────
```

---

## Test Categories & Files

### 1. Unit Tests (30% effort) - Existing + Extended

**Location:** `/learnhub-backend/src/test/java/com/learnhub/assessment/`

**Scope:** Isolated component testing (no DB, no Redis)

| File | Coverage | Status |
|------|----------|--------|
| `queue/RedisAssessmentJobQueueTest.java` | Job serialization, queue ops | ✅ Complete |
| `dto/AssessmentProgressEventTest.java` | Event validation, invariants | ✅ Complete |
| `service/AssessmentServiceTest.java` | Business logic, mocked repos | ✅ Complete |

**Key Tests:**
- Job serialization/deserialization
- Progress event validation (0-100%, confidence 0-1)
- Rate limit validation rules
- Status transition logic
- Assessment result aggregation

**Run Command:**
```bash
mvn test -Dtest=**/assessment/**/*Test.java
```

---

### 2. Integration Tests (40% effort) - NEW SKELETON

**Location:** `/learnhub-backend/src/test/java/com/learnhub/assessment/test/`

**Framework:** Testcontainers (PostgreSQL) + Embedded Redis

**Base Class:** `AssessmentIntegrationTestBase`
- Manages PostgreSQL container lifecycle
- Manages embedded Redis startup
- Provides dynamic Spring property registration

**Test File:** `AssessmentPhase1IntegrationTest.java`

#### Critical Tests (8 Scenarios)

| # | Test | Effort | Risk Level | Status |
|---|------|--------|-----------|--------|
| 1 | SSE endpoint emits events correctly | 20% | CRITICAL | 🔨 Skeleton |
| 2 | SSE closes gracefully on completion | 15% | HIGH | 🔨 Skeleton |
| 3 | Polling endpoint returns progress | 20% | CRITICAL | 🔨 Skeleton |
| 4 | Polling returns consistent data with SSE | 20% | CRITICAL | 🔨 Skeleton |
| 5 | Rate limiter enforces 3 concurrent limit | 15% | CRITICAL | 🔨 Skeleton |
| 6 | Rate limiter detects duplicates (60s) | 15% | CRITICAL | 🔨 Skeleton |
| 7 | Ownership validation blocks unauthorized access | 15% | CRITICAL | 🔨 Skeleton |
| 8 | User can't submit for other's snapshot | 10% | HIGH | 🔨 Skeleton |
| 9 | Dequeued job has fresh state (no staleness) | 15% | CRITICAL | 🔨 Skeleton |
| 10 | Concurrent dequeue has no duplicates | 20% | CRITICAL | 🔨 Skeleton |
| 11 | Retry loop preserves context | 15% | HIGH | 🔨 Skeleton |
| 12 | Job moves to DLQ after max retries | 15% | HIGH | 🔨 Skeleton |

**Infrastructure Provided:**

✅ PostgreSQL container setup  
✅ Embedded Redis configuration  
✅ Dynamic property registration  
✅ Test data fixtures (@BeforeEach)  
✅ Transactional test isolation  

**Run Command:**
```bash
mvn test -Dtest=AssessmentPhase1IntegrationTest
```

**Expected Duration:** ~45 seconds (container startup + 12 tests)

---

### 3. E2E Tests (20% effort) - NEW SKELETON

**Location:** `/learnhub-frontend/src/__tests__/`

**Framework:** Playwright (headless browser automation)

**Test File:** `assessment.e2e.ts`

#### Scenarios (4 Main + Variants)

| # | Scenario | Tests | Status |
|---|----------|-------|--------|
| 1 | Happy Path | 2 | 🔨 Skeleton |
| 2 | SSE Lifecycle | 2 | 🔨 Skeleton |
| 3 | Error Handling | 3 | 🔨 Skeleton |
| 4 | Network Resilience | 2 | 🔨 Skeleton |
| + | Accessibility | 2 | 🔨 Skeleton |

**Mock Backend:** `mocks/assessment-api.mock.ts`
- Simulates assessment progression
- Supports all error scenarios
- Configurable latency/failure rates
- No backend dependency needed

**Test Flows:**

1. **Happy Path (Complete Workflow)**
   - User submits repo → watches progress → views results
   - Verifies: UI responsiveness, progress updates, result display

2. **SSE Subscription Lifecycle**
   - EventSource connection established
   - Events received at 1-2s intervals
   - Connection closes on completion

3. **Polling Fallback**
   - SSE failure → system switches to polling
   - Progress continues via HTTP
   - Data consistency maintained

4. **Error Scenarios**
   - 409 Conflict (duplicate submission)
   - 429 Too Many Requests (rate limit)
   - Network errors with auto-retry

5. **Network Resilience**
   - Connection loss → automatic reconnection
   - Slow network (1-2s latency) → no timeouts
   - No data loss during brief disconnections

6. **Accessibility**
   - ARIA labels and roles
   - Heading hierarchy
   - Color not sole indicator

7. **Responsiveness**
   - Mobile viewport (375px)
   - No horizontal overflow
   - Touch-friendly buttons (48px+)

**Run Command:**
```bash
npm run test:e2e

# Or with options
npx playwright test --headed  # Show browser
npx playwright test --debug   # Debug mode
```

**Expected Duration:** ~30-45 seconds per scenario

---

## Test Infrastructure Setup

### 1. Testcontainers (PostgreSQL + Redis)

**File:** `AssessmentIntegrationTestBase.java`

```
┌─────────────────────────────────────────┐
│  Test JUnit5                            │
├─────────────────────────────────────────┤
│  Testcontainers Annotation              │
│  ├─ PostgreSQL Container (15-alpine)    │
│  │  ├─ Database: learnhub_test          │
│  │  ├─ User: test_user                  │
│  │  └─ Password: test_password          │
│  │                                       │
│  └─ Embedded Redis                      │
│     ├─ Host: localhost                  │
│     └─ Port: 6380                       │
│                                          │
└─────────────────────────────────────────┘
```

**Benefits:**
- ✅ Full database testing (not mocked)
- ✅ Redis queue testing (real operations)
- ✅ Transaction isolation per test
- ✅ Automatic cleanup after test
- ✅ No manual container management

**Performance:**
- First test suite: ~15s (container startup)
- Subsequent tests: ~50ms each (reuse containers)
- Total 12 tests: ~45s

### 2. Mock Backend (Frontend E2E)

**File:** `assessment-api.mock.ts`

```
┌─────────────────────────────────────┐
│  Playwright Test                    │
├─────────────────────────────────────┤
│  setupAssessmentMocks(page)         │
│  ├─ POST /api/v1/assessments        │
│  │  └─ 201 Created + simulate       │
│  ├─ GET /api/v1/assessments/{id}    │
│  │  └─ Current progress state       │
│  ├─ GET /*/progress/stream (SSE)    │
│  │  └─ Event stream simulation      │
│  └─ Fault Injection                 │
│     ├─ Latency: configurable        │
│     ├─ Failure Rate: %              │
│     └─ Rate Limit: 3 concurrent     │
│                                      │
└─────────────────────────────────────┘
```

**Features:**
- ✅ Assessment progress simulation (1-2s updates)
- ✅ Rate limiting (3 concurrent)
- ✅ Duplicate detection (60s window)
- ✅ Error scenarios (409, 429, 500)
- ✅ Network conditions (latency, packet loss)

**No Backend Needed:** Tests run against mock API only

### 3. Toxiproxy (Network Fault Injection)

**File:** `src/test/resources/toxiproxy/SETUP.md`

```
┌──────────────────────────────────────────┐
│  Test Application                        │
├──────────────────────────────────────────┤
│           │                              │
│           ▼                              │
│    Toxiproxy Proxy (port 20000)          │
│    ├─ Inject Latency (0-5s)              │
│    ├─ Drop Packets (0-50%)               │
│    ├─ Simulate Timeouts                  │
│    └─ Connection Loss                    │
│           │                              │
│           ▼                              │
│    Actual Redis/PostgreSQL               │
│                                          │
└──────────────────────────────────────────┘
```

**Scenarios:**
- SSE with 1-2s latency (slow network)
- Job queue with 10% packet loss (unreliable network)
- Complete connection loss for 5s (network partition)
- Intermittent failures (chaos engineering)

**Usage in Tests:**
```java
// Inject Redis latency
ToxiproxyFixture.injectRedisLatency(1500, 200);

try {
  // Test behavior under latency
  testAssessmentQueueUnderLatency();
} finally {
  ToxiproxyFixture.disableRedisProxy();
}
```

---

## Test Execution Matrix

### Unit Tests (Fast)

```bash
mvn test -Dtest=**/assessment/**/*Test.java
```

| Test | Duration | Status |
|------|----------|--------|
| RedisAssessmentJobQueueTest | 3s | ✅ 10 tests |
| AssessmentProgressEventTest | 1s | ✅ 5 tests |
| AssessmentServiceTest | 2s | ✅ 8 tests |
| **Total** | **~6s** | ✅ Complete |

### Integration Tests (Medium)

```bash
mvn test -Dtest=AssessmentPhase1IntegrationTest
```

| Category | Tests | Duration | Status |
|----------|-------|----------|--------|
| SSE Endpoint | 2 | 8s | 🔨 Skeleton |
| Polling Endpoint | 2 | 8s | 🔨 Skeleton |
| Rate Limiting | 2 | 6s | 🔨 Skeleton |
| Ownership Validation | 2 | 6s | 🔨 Skeleton |
| Race Conditions | 2 | 8s | 🔨 Skeleton |
| Retry Loop | 2 | 6s | 🔨 Skeleton |
| Integration Flow | 1 | 3s | 🔨 Skeleton |
| **Total** | **13** | **~45s** | 🔨 Skeleton |

### E2E Tests (Slow)

```bash
npm run test:e2e
```

| Scenario | Tests | Duration | Status |
|----------|-------|----------|--------|
| Happy Path | 2 | 15s | 🔨 Skeleton |
| SSE Lifecycle | 2 | 12s | 🔨 Skeleton |
| Error Handling | 3 | 20s | 🔨 Skeleton |
| Network Resilience | 2 | 18s | 🔨 Skeleton |
| Accessibility | 2 | 8s | 🔨 Skeleton |
| **Total** | **11** | **~73s** | 🔨 Skeleton |

### Complete Test Suite

```bash
# Run all tests
mvn clean test                    # Backend: 6s + 45s = 51s
npm run test:e2e                  # Frontend: 73s

# Total: ~2 minutes (including container startup)
```

---

## Critical Requirements Mapped to Tests

| Requirement | Test File | Test Method | Risk |
|-------------|-----------|------------|------|
| SSE emits events at 1-2s | IntegrationTest | testSSEEndpoint_ReceivesEvents | CRITICAL |
| Polling returns same data | IntegrationTest | testPollingEndpoint_ConsistentWithSSE | CRITICAL |
| Rate limit: 3 concurrent | IntegrationTest | testRateLimiter_Enforces3ConcurrentLimit | CRITICAL |
| Rate limit: 60s duplicate | IntegrationTest | testRateLimiter_DetectsDuplicateWithin60s | CRITICAL |
| Ownership validation | IntegrationTest | testOwnershipValidation_BlocksUnauthorizedAccess | CRITICAL |
| Snapshot ownership | IntegrationTest | testOwnershipValidation_SnapshotOwnershipVerified | CRITICAL |
| No stale state on dequeue | IntegrationTest | testRaceCondition_StaleStatePrevention | CRITICAL |
| No duplicate dequeue | IntegrationTest | testRaceCondition_ConcurrentDequeueNoDuplicates | CRITICAL |
| Retry preserves context | IntegrationTest | testRetryLoop_ContextPreservation | HIGH |
| DLQ on max retries | IntegrationTest | testRetryLoop_DeadLetterQueueAfterMaxRetries | HIGH |
| E2E happy path | E2ETest | testCompleteAssessmentWorkflow | CRITICAL |
| E2E network resilience | E2ETest | testAssessmentContinuesAfterReconnection | HIGH |

---

## Implementation Roadmap

### Phase 1.1: Infrastructure Setup (Days 1-2)

- [x] Add Testcontainers dependencies to pom.xml
- [x] Create AssessmentIntegrationTestBase
- [x] Create EmbeddedRedisConfig
- [x] Setup test properties (application-test.yml)
- [x] Create test database init script (db/init-test.sql)

**Deliverable:** Base infrastructure ready for test implementation

### Phase 1.2: Integration Test Implementation (Days 2-3)

- [ ] Implement SSE endpoint tests (with async context)
- [ ] Implement Polling endpoint tests
- [ ] Implement Rate limiting tests
- [ ] Implement Ownership validation tests
- [ ] Implement Race condition tests
- [ ] Implement Retry loop tests
- [ ] Verify all tests pass with real containers

**Deliverable:** 12+ integration tests passing against real PostgreSQL + Redis

### Phase 1.3: E2E Test Implementation (Days 3-4)

- [ ] Install Playwright (`npm install -D @playwright/test`)
- [ ] Implement mock API backend (assessment-api.mock.ts)
- [ ] Implement Happy Path scenario
- [ ] Implement SSE Lifecycle scenario
- [ ] Implement Error Handling scenario
- [ ] Implement Network Resilience scenario
- [ ] Implement Accessibility checks
- [ ] Verify all tests pass with mock backend

**Deliverable:** 11 E2E tests passing with mock backend

### Phase 1.4: Toxiproxy Setup (Day 4)

- [ ] Install Toxiproxy (Docker or binary)
- [ ] Create Redis proxy configuration
- [ ] Create HTTP proxy configuration
- [ ] Implement ToxiproxyFixture
- [ ] Create network resilience tests
- [ ] Document common fault patterns

**Deliverable:** Chaos testing infrastructure ready

### Phase 1.5: Verification & Documentation (Day 5)

- [ ] Run full test suite (unit + integration + E2E)
- [ ] Verify test coverage > 80%
- [ ] Generate test report
- [ ] Document test execution procedures
- [ ] Create troubleshooting guide

**Deliverable:** Complete test suite with documentation

---

## Test Coverage Goals

### Target Metrics

| Metric | Target | Status |
|--------|--------|--------|
| Line Coverage | > 80% | 📊 TBD |
| Branch Coverage | > 75% | 📊 TBD |
| Method Coverage | > 85% | 📊 TBD |
| Critical Paths | 100% | 📊 TBD |

### Coverage Excluded

- Boilerplate (getters/setters)
- Lombok-generated code
- Configuration classes (if tested via integration tests)

**Generate Coverage Report:**
```bash
mvn clean test jacoco:report

# Open report
open target/site/jacoco/index.html
```

---

## Common Issues & Solutions

### Issue 1: PostgreSQL Container Won't Start

**Symptom:** Test fails with "container failed to start"

**Solution:**
```bash
# Check Docker is running
docker ps

# Check ports are available
lsof -i :5432
```

### Issue 2: Redis Port Conflict

**Symptom:** "Port 6380 already in use"

**Solution:**
```bash
# Find process using 6380
lsof -i :6380

# Kill it
kill -9 <PID>

# Or change port in EmbeddedRedisConfig.java
private static final int REDIS_PORT = 6381;  // Try different port
```

### Issue 3: Tests Timeout

**Symptom:** "Test timed out after 30 seconds"

**Reason:** Container startup is slow on first run

**Solution:**
```bash
# Increase timeout
@Test(timeout = 60000)  // 60 seconds
```

### Issue 4: Flaky Tests (Intermittent Failures)

**Symptom:** Same test passes sometimes, fails other times

**Root Cause:** Race conditions in async operations

**Solution:** Use Awaitility with proper conditions
```java
Awaitility.await()
  .atMost(5, TimeUnit.SECONDS)
  .pollInterval(100, TimeUnit.MILLISECONDS)
  .untilAsserted(() -> {
    // Assertions that might take time to be true
  });
```

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/tests.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_PASSWORD: postgres
      redis:
        image: redis:7-alpine
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Unit & Integration Tests
        run: mvn clean test
      
      - name: Upload Coverage
        run: mvn jacoco:report
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: E2E Tests
        run: npm run test:e2e
      
      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: |
            target/site/jacoco/
            playwright-report/
```

---

## Files Created/Modified

### Created

✅ `/learnhub-backend/pom.xml` - Added test dependencies  
✅ `/learnhub-backend/src/test/java/com/learnhub/assessment/test/AssessmentIntegrationTestBase.java`  
✅ `/learnhub-backend/src/test/java/com/learnhub/assessment/test/EmbeddedRedisConfig.java`  
✅ `/learnhub-backend/src/test/java/com/learnhub/assessment/test/AssessmentPhase1IntegrationTest.java`  
✅ `/learnhub-frontend/src/__tests__/assessment.e2e.ts`  
✅ `/learnhub-frontend/src/__tests__/mocks/assessment-api.mock.ts`  
✅ `/learnhub-backend/src/test/resources/toxiproxy/SETUP.md`  
✅ `/learnhub-backend/TEST-ARCHITECTURE.md` (this file)

### To Create

- `db/init-test.sql` - PostgreSQL initialization script
- `application-test.yml` - Test Spring configuration
- `ToxiproxyFixture.java` - Network fault injection helper

---

## Next Steps for Implementation Teams

### Agent 1 (Backend - Core Service)

1. Implement `submitAssessment()` endpoint to make integration tests pass
2. Implement `getAssessmentProgress()` endpoint (polling)
3. Implement rate limiting middleware
4. Ensure all critical tests pass

### Agent 2 (Backend - Job Queue)

1. Implement Redis job queue integration tests
2. Ensure job serialization tests pass
3. Implement retry mechanism
4. Implement dead-letter queue handling

### Agent 3 (Frontend)

1. Create AssessmentPage component
2. Implement SSE subscription logic
3. Implement polling fallback
4. Ensure E2E tests pass

### Integration (QA)

1. Run full test suite
2. Generate coverage reports
3. Identify gaps and flaky tests
4. Update CI/CD pipeline
5. Document procedures

---

## References

- [Testcontainers Documentation](https://testcontainers.com/)
- [JUnit 5 Guide](https://junit.org/junit5/)
- [Playwright Testing](https://playwright.dev/docs/intro)
- [REST Assured](https://rest-assured.io/)
- [Awaitility](https://github.com/awaitility/awaitility)
- [Toxiproxy](https://github.com/Shopify/toxiproxy)

---

**Status:** ✅ Architecture Complete  
**Last Updated:** 2026-06-19  
**Owner:** QA Architect (Agent 4)  
**Next Review:** After implementation completion
