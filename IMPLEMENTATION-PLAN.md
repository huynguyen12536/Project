IMPLEMENTATION PLAN — LearnHub MVP

Objective
Convert MVP epics into actionable technical tasks, organized into 7 implementation phases with dependencies, acceptance criteria, and complexity estimates. This plan guides coding order and ensures production-quality delivery.

---

# PHASE 1: Project Foundation (Weeks 1–2)

Task 1.1 | Spring Boot Project Setup

- Feature: Project scaffolding
- Backend package: com.learnhub.config
- Database: N/A
- API: N/A
- Dependencies: pom.xml (Spring Boot 3, Java 21)
- Acceptance: Successful Maven build, Docker image builds, all dependencies resolve
- Complexity: S

Task 1.2 | PostgreSQL Schema & Flyway Migrations v1–v2

- Feature: Database foundation
- Backend package: N/A (DB-only)
- Database: V1**initial_schema.sql (users, oauth, rsa_keys, brf_versions, repos, assessments, tokens, casbin)
  V2**github_oauth_extensions.sql (token_meta, html_url, selected_for_portfolio)
- API: N/A
- Dependencies: PostgreSQL 15+, Flyway
- Acceptance: All migrations pass on fresh and existing DBs; test data seeded; indexes created
- Complexity: M

Task 1.3 | Docker Compose Local Dev Stack

- Feature: Containerized local development
- Backend package: N/A (DevOps)
- Database: Uses V1, V2 migrations auto-run
- API: N/A
- Dependencies: Docker, docker-compose
- Acceptance: `docker compose up` succeeds; postgres/redis/app/worker containers healthy; logs visible
- Complexity: M

Task 1.4 | SecurityConfig & Filter Chain Foundation

- Feature: Security framework
- Backend package: com.learnhub.config.SecurityConfig
- Database: N/A
- API: Endpoints marked /auth/**, /oauth/** as public; others authenticated
- Dependencies: Spring Security
- Acceptance: App starts; public endpoints accessible; authenticated endpoints return 401 without Bearer token
- Complexity: M

Task 1.5 | JwtAuthenticationFilter Implementation

- Feature: JWT token validation & SecurityContext setup
- Backend package: com.learnhub.config.JwtAuthenticationFilter
- Database: reads from rsa_keys table
- API: All /api/v1/\*\* endpoints check Bearer token validity
- Dependencies: JwtService, RsaKeyManager
- Acceptance: Valid JWT passes; expired/invalid JWT returns 401; SecurityContext contains user principal
- Complexity: M

Subtask 1.5.1: RsaKeyManager refactor

- Acceptance: Keys loaded in @PostConstruct; exceptions handled gracefully; fallback to generated keys on startup
- Complexity: S

---

# PHASE 2: Authentication System (Weeks 3–4)

Task 2.1 | Email/Password Registration & Login

- Feature: User accounts
- Backend package: com.learnhub.auth.service.AuthService, com.learnhub.auth.controller.AuthController
- Database: users table (email, password_hash, role)
- API: POST /api/v1/auth/register, POST /api/v1/auth/login
- Dependencies: PasswordEncoder (BCrypt), UserService
- Acceptance: Register creates user with hashed password; login returns valid access token; duplicate email rejected
- Complexity: M

Task 2.2 | JWT Access & Refresh Token Lifecycle

- Feature: Token generation, validation, rotation
- Backend package: com.learnhub.auth.service.JwtService
- Database: refresh_tokens table (token_hash, issued_at, expires_at, revoked)
- API: POST /api/v1/auth/refresh
- Dependencies: Nimbus JOSE, RsaKeyManager
- Acceptance: Access token TTL 15min; refresh token TTL 30 days; refresh rotates tokens; old refresh marked revoked
- Complexity: M

Task 2.3 | GitHub OAuth Integration (Backend)

- Feature: OAuth flow, token exchange, account linking
- Backend package: com.learnhub.oauth.service.GitHubOAuthService, com.learnhub.oauth.service.GitHubClient
- Database: user_oauth table (provider, github_login, token_meta JSONB)
- API: GET /api/v1/oauth/github/start, GET /api/v1/oauth/github/callback
- Dependencies: OAuth2Client, Redis (state store), TokenEncryptionService
- Acceptance: OAuth state flow succeeds; token exchanged and encrypted; user_oauth record created; re-auth works
- Complexity: L

Subtask 2.3.1: TokenEncryptionService

- Encrypts/decrypts GitHub tokens using APP_MASTER_KEY
- Acceptance: Tokens stored encrypted in token_meta; decryption succeeds for valid tokens
- Complexity: S

Subtask 2.3.2: RedisStateService

- Store/retrieve OAuth state (CSRF protection)
- Acceptance: State stored with 10-min TTL; single-use verification succeeds; expired state rejected
- Complexity: S

Task 2.4 | Casbin RBAC Integration

- Feature: Role-based access control
- Backend package: com.learnhub.persistence.casbin
- Database: casbin_policy table
- API: All endpoints that differ by role check Casbin
- Dependencies: jCasbin
- Acceptance: Admin endpoints protected; learner endpoints accessible to learners; policies persist in DB
- Complexity: M

---

# PHASE 3: GitHub Integration (Weeks 5–6)

Task 3.1 | Repository Metadata Persistence

- Feature: Save user's GitHub repos locally
- Backend package: com.learnhub.repo.service.RepoService, com.learnhub.repo.model.RepositoryEntity
- Database: repositories table (github_repo_id, full_name, private_flag, html_url, selected_for_portfolio)
- API: GET /api/v1/github/repos, POST /api/v1/repos/{id}/select
- Dependencies: GitHubClient, JpaRepository
- Acceptance: List repos from GitHub; persist selected repos; query saved repos by user
- Complexity: M

Task 3.2 | Repository Snapshot Orchestration

- Feature: Create snapshot for evaluation
- Backend package: com.learnhub.repo.service.SnapshotService
- Database: repositories.snapshot_ref (text reference to stored snapshot)
- API: POST /api/v1/repos/{id}/snapshot (or triggered via assessment submission)
- Dependencies: MinIO client (or local file storage)
- Acceptance: Repo snapshot created (git archive or shallow clone); stored with reference; retrieval succeeds
- Complexity: M

Subtask 3.2.1: MinIO/S3 adapter

- Store and retrieve snapshots
- Acceptance: Snapshot uploaded; retrieval by reference works; cleanup/expiry logic documented
- Complexity: S

---

# PHASE 4: BRF Engine (Weeks 7–8)

Task 4.1 | BRF YAML Loader & Versioning

- Feature: Load competency framework by version
- Backend package: com.learnhub.brf.service.BrfLoader, com.learnhub.brf.model.BrfVersion
- Database: brf_versions table (version, git_tag, commit_hash, released_at, description)
- API: GET /api/v1/brf/versions
- Dependencies: SnakeYAML, Spring Resource loading
- Acceptance: Load BRF v1.0.0 YAML; parse competencies, evidence rules, scoring weights; admin can list versions
- Complexity: M

Task 4.2 | Competency Model (Domain Objects)

- Feature: In-memory competency definitions
- Backend package: com.learnhub.brf.model (Competency, ProficiencyLevel, EvidenceRule, ScoringRule)
- Database: N/A (loaded from YAML)
- API: N/A (internal)
- Dependencies: BrfLoader
- Acceptance: Competency objects typed and immutable; scoring rules accessible; gating logic testable
- Complexity: M

Task 4.3 | Evidence Model (Assessment Results)

- Feature: Store detected signals and competency scores
- Backend package: com.learnhub.assessment.model (CompetencyResult, EvidenceSnapshot)
- Database: competency_results table (assessment_id, competency_key, score, band, evidence JSONB)
  evidence_snapshots table (assessment_id, storage_ref)
- API: N/A (populated by worker, retrieved via /assessments/:id)
- Dependencies: Assessment model
- Acceptance: Competency results persisted; evidence JSON stores detector outputs; banding logic correct
- Complexity: M

---

# PHASE 5: Evaluation Worker & Queue (Weeks 9–10)

Task 5.1 | Redis Queue Adapter & Job Model

- Feature: Async job processing
- Backend package: com.learnhub.queue.service.QueueService, com.learnhub.queue.model.EvaluationJob
- Database: N/A (Redis native; optional audit table for future)
- API: N/A (worker-internal)
- Dependencies: Spring Data Redis, Jackson serialization
- Acceptance: Enqueue job from API; dequeue in worker; job payload typed; retries configurable
- Complexity: M

Task 5.2 | Evaluation Worker Process

- Feature: Background job processor (separate entrypoint)
- Backend package: com.learnhub.worker (EvaluationWorkerApplication, EvaluationJobProcessor)
- Database: reads assessments, competency_results; writes status updates
- API: N/A (no API; internal)
- Dependencies: Spring Boot CLI, QueueService, BrfLoader
- Acceptance: Worker starts independently; consumes jobs; updates DB; handles retries; logs progress
- Complexity: L

Task 5.3 | Assessment Submission Workflow

- Feature: User submits repo for evaluation
- Backend package: com.learnhub.assessment.service.AssessmentService, com.learnhub.assessment.controller.AssessmentController
- Database: assessments table (repo_id, brf_version, submitted_by, status, result_summary)
- API: POST /api/v1/repos/{id}/submit-eval (creates assessment, enqueues job)
  GET /api/v1/assessments/:id (retrieve status & results)
- Dependencies: RepoService, QueueService, BrfLoader
- Acceptance: Submit endpoint creates assessment with status=QUEUED; enqueues job with brf_version; GET returns status
- Complexity: M

---

# PHASE 6: Detectors & Evidence Mapping (Weeks 11–12)

Task 6.1 | API Design Detector

- Feature: Detect API endpoints, controllers, DTOs, documentation
- Backend package: com.learnhub.detector.api
- Database: N/A (detector output serialized in CompetencyResult.evidence)
- API: N/A (worker-internal)
- Dependencies: Snapshot parser, regex/AST for Java code
- Acceptance: Detect controllers, OpenAPI specs, DTOs, example scripts; output typed JSON with confidence
- Complexity: L

Task 6.2 | Database Design Detector

- Feature: Detect SQL schemas, migrations, JPA entities, indexes
- Backend package: com.learnhub.detector.database
- Database: N/A
- API: N/A
- Dependencies: SQL parser (optional), file pattern matching
- Acceptance: Detect Flyway migrations, JPA entities, foreign keys, indexes; output evidence JSON
- Complexity: L

Task 6.3 | Authentication & Authorization Detector

- Feature: Detect auth patterns, JWT, BCrypt, OAuth, Casbin
- Backend package: com.learnhub.detector.auth
- Database: N/A
- API: N/A
- Dependencies: Code pattern matching
- Acceptance: Detect auth endpoints, JWT libraries, password hashing, OAuth code, Casbin policies
- Complexity: M

Task 6.4 | Code Organization Detector

- Feature: Detect package structure, layering, class size
- Backend package: com.learnhub.detector.organization
- Database: N/A
- API: N/A
- Dependencies: JAR/class file parsing
- Acceptance: Detect controller/service/repository packages; flag large classes; check dependency cycles
- Complexity: M

Task 6.5 | Business Logic Separation Detector

- Feature: Detect business logic in services vs controllers/repos
- Backend package: com.learnhub.detector.business_logic
- Database: N/A
- API: N/A
- Dependencies: Static analysis
- Acceptance: Identify business logic in correct layer; flag violations
- Complexity: M

Task 6.6 | Documentation Quality Detector

- Feature: Detect README, API docs, build/run instructions
- Backend package: com.learnhub.detector.documentation
- Database: N/A
- API: N/A
- Dependencies: File I/O, text analysis
- Acceptance: Detect README presence, run steps, API examples; score completeness
- Complexity: S

Task 6.7 | Testing Practices Detector

- Feature: Detect test files, frameworks, coverage
- Backend package: com.learnhub.detector.testing
- Database: N/A
- API: N/A
- Dependencies: JAR parsing, regex
- Acceptance: Detect test classes, JUnit/Mockito/Testcontainers usage; flag CI presence
- Complexity: S

Task 6.8 | Scoring Engine & Gating Rules

- Feature: Apply BRF weights, gating, compute competency bands
- Backend package: com.learnhub.evaluation.engine.ScoringEngine
- Database: N/A (logic only)
- API: N/A (worker-internal)
- Dependencies: BrfLoader, detector outputs, CompetencyResult model
- Acceptance: Combine detector scores with weights; apply gating rules; assign band (Not Demonstrated / Emerging / Proficient / Advanced)
- Complexity: L

---

# PHASE 7: Feedback, Progress & Portfolio (Weeks 13–14)

Task 7.1 | Feedback Rendering Engine

- Feature: Generate human-readable feedback per competency
- Backend package: com.learnhub.feedback.service.FeedbackService
- Database: N/A (queries assessment results)
- API: GET /api/v1/assessments/:id/feedback
- Dependencies: Assessment, CompetencyResult, BrfLoader (feedback templates)
- Acceptance: Feedback includes detected signals, missing evidence, remediation tips; human-readable
- Complexity: M

Task 7.2 | Assessment History & Progression Tracking

- Feature: List past assessments, show trends
- Backend package: com.learnhub.progression.service.ProgressionService
- Database: Queries assessments table with timestamps, brf_versions
- API: GET /api/v1/users/me/assessments
- Dependencies: AssessmentRepository
- Acceptance: Return assessment history sorted by date; include brf_version, scores, status
- Complexity: S

Task 7.3 | BRF Migration Analysis (Future, P1)

- Feature: Guide learners through re-evaluation on BRF version change
- Backend package: com.learnhub.brf.service.MigrationAnalysisService
- Database: Compare assessments across brf_versions
- API: GET /api/v1/brf/migration-analysis (admin endpoint)
- Dependencies: BrfLoader, CompetencyResult
- Acceptance: Show which competencies already satisfied, which require new evidence
- Complexity: L

Task 7.4 | Portfolio Page & Public Showcase

- Feature: Publish selected repos and competency badges
- Backend package: com.learnhub.portfolio.service.PortfolioService, com.learnhub.portfolio.controller.PortfolioController
- Database: Queries repositories (selected_for_portfolio), assessments, competency_results
- API: GET /api/v1/portfolio/:userId (public endpoint, no auth)
  POST /api/v1/portfolio/repos/:repoId/showcase
- Dependencies: Assessment, Repository models
- Acceptance: Public URL shows learner's portfolio; repos marked as featured; competency badges displayed; non-sensitive results only
- Complexity: M

---

# Implementation Dependencies & Coding Order

Sequential order (must complete before later phases):

1. Phase 1 (Foundation) — all tasks before any other coding
2. Phase 2 (Auth) — Phase 1 complete
3. Phase 3 (GitHub Integration) — Phase 2 complete
4. Phase 4 (BRF Engine) — Phase 1 + Phase 2 complete
5. Phase 5 (Worker & Queue) — Phase 3 + Phase 4 complete
6. Phase 6 (Detectors) — Phase 5 complete
7. Phase 7 (Feedback & Portfolio) — Phase 6 complete

Critical path:
Phase 1 (foundation) → Phase 2 (auth) → Phase 3 (GitHub) → Phase 5 (worker/queue + assessment submission) → Phase 6 (detectors) forms the MVP core.
Phase 4 (BRF loader) and Phase 7 (feedback) can be parallelized with Phase 6 detector implementation.

Parallelizable within phases:

- Phase 2: Tasks 2.1, 2.2 can overlap; 2.3 once 2.1 complete; 2.4 anytime within phase
- Phase 6: All detector tasks (6.1–6.7) can be implemented in parallel once worker process running; 6.8 (scoring engine) depends on all detectors
- Phase 7: Tasks 7.1–7.2 independent; 7.3–7.4 once assessments exist

---

# First Sprint (2 weeks) Coding Checklist

Completed (Sprint 1):
✓ 1.1 Spring Boot Project Setup
✓ 1.2 PostgreSQL Schemas V1–V2 (stubs)
✓ 1.3 Docker Compose Stack
✓ 1.4 SecurityConfig Foundation
✓ 1.5 JwtAuthenticationFilter

Sprint 1 Fixes Required (from Review Report):
□ Add Dockerfile (multi-stage) for backend
□ Refactor RsaKeyManager to use @PostConstruct
□ Implement UserService (minimal) or change SecurityConfig dep
□ Make Flyway insert idempotent (INSERT ... ON CONFLICT DO NOTHING)
□ Replace Map<String,String> auth DTOs with @Valid request classes
□ Add input validation and error handling (ProblemDetails)

Sprint 1 Recommended Additional Work:
□ 2.1 Email/Password Registration & Login (AUTH-01 story)
□ 2.2 JWT Refresh Token Lifecycle (AUTH-03 story)
□ 2.3 GitHub OAuth Start/Callback (AUTH-05 story)
□ Add OAuthController + GitHubOAuthService
□ Add TokenEncryptionService
□ Add RedisStateService
□ Add input/output DTOs for all endpoints

Post-Sprint-1 (start of Sprint 2):
□ 2.4 Casbin RBAC Integration
□ 3.1 Repository Metadata & Listing (GIT-02 story)
□ 3.2 Repository Snapshot Orchestration

Architecture compliance checklist

- Modular monolith: packages follow com.learnhub.{feature}.{layer} pattern ✓
- Spring Boot: all services/repos are beans; injection used ✓
- PostgreSQL: all models have @Entity and JpaRepository ✓
- Flyway: migrations versioned and tested ✓
- JWT RSA: JwtService uses Nimbus JOSE with RSA-256 ✓
- GitHub OAuth: OAuth2Client and state in Redis ✓
- BRF YAML: BrfLoader parses classpath resource ✓
- Redis queue: QueueService abstracts queue implementation ✓
- Docker-first: dockerfile + compose provided ✓
- Async evaluation: worker process separate from API ✓

---

# Folder & Module Structure Recommendation

learnhub-backend/
├── pom.xml
├── Dockerfile (multi-stage)
├── Dockerfile.worker (or multi-target single Dockerfile)
├── docker-compose.yml
├── scripts/
│ ├── docker-entrypoint.sh
│ └── generate-rsa-key.sh
├── src/
│ ├── main/
│ │ ├── java/com/learnhub/
│ │ │ ├── LearnHubApplication.java (main app)
│ │ │ ├── LearnHubWorkerApplication.java (worker app)
│ │ │ ├── config/ (Security, Redis, DB, CORS, Error handling)
│ │ │ ├── auth/ (Auth controller, service, JWT, password)
│ │ │ ├── user/ (User entity, repo, service)
│ │ │ ├── oauth/ (OAuth controller, GitHub client, token encryption, state service)
│ │ │ ├── repo/ (Repo entity, controller, service, snapshot)
│ │ │ ├── assessment/ (Assessment entity, controller, service)
│ │ │ ├── brf/ (BRF loader, version model, seed runner)
│ │ │ ├── detector/ (API, DB, Auth, Organization, BusinessLogic, Docs, Testing detectors)
│ │ │ ├── evaluation/ (Scoring engine, evaluation logic)
│ │ │ ├── queue/ (Queue service, job model, adapter)
│ │ │ ├── worker/ (Worker processor, job loop)
│ │ │ ├── feedback/ (Feedback service)
│ │ │ ├── progression/ (Progression service)
│ │ │ ├── portfolio/ (Portfolio service & controller)
│ │ │ ├── persistence/ (Casbin adapter, entity repos)
│ │ │ └── util/ (Crypto, mappers, logging)
│ │ └── resources/
│ │ ├── application.yml
│ │ ├── brf/ (BRF v1.0.yaml + future versions)
│ │ └── db/migration/ (V1**\*, V2**\*, etc.)
│ └── test/
│ └── java/com/learnhub/
│ ├── auth/ (AuthServiceTest, JwtServiceTest)
│ ├── detector/ (DetectorTests)
│ ├── evaluation/ (ScoringEngineTest)
│ └── integration/ (E2E flows)

learnhub-frontend/
├── package.json
├── vite.config.ts
├── src/
│ ├── pages/
│ │ ├── LoginPage.tsx
│ │ ├── RegisterPage.tsx
│ │ ├── GithubConnectPage.tsx
│ │ ├── RepoListPage.tsx
│ │ ├── AssessmentPage.tsx
│ │ ├── ResultsPage.tsx
│ │ ├── ProgressionPage.tsx
│ │ └── PortfolioPage.tsx
│ ├── components/
│ │ ├── ProtectedRoute.tsx
│ │ ├── Notification.tsx
│ │ ├── TokenManager.ts
│ │ └── ApiClient.ts
│ └── App.tsx

---

# Time & Resource Estimates

Phase 1 (Foundation): 2 weeks, 1 developer
Phase 2 (Auth): 2 weeks, 1–2 developers (can parallelize OAuth + email/pass)
Phase 3 (GitHub Integration): 1.5 weeks, 1 developer
Phase 4 (BRF Engine): 1.5 weeks, 1–2 developers (Yaml loader + models in parallel)
Phase 5 (Worker & Queue): 2 weeks, 1–2 developers (queue adapter + processor)
Phase 6 (Detectors & Scoring): 3–4 weeks, 1–2 developers (detectors parallelizable)
Phase 7 (Feedback & Portfolio): 1.5 weeks, 1 developer

Total MVP: ~14 weeks (solo developer with minimal parallelization)
With 2–3 developers and parallelization: ~8–10 weeks

---

# Success Criteria

MVP Definition of Done:

1. All 7 phases complete
2. Docker compose runs locally and in CI/CD
3. End-to-end flow: register → connect GitHub → submit repo → receive feedback (async)
4. All acceptance criteria per task met
5. Test coverage >70% for critical paths (auth, scoring, queue)
6. Documentation: README, API OpenAPI spec, BRF guide, deployment runbook
7. Security audit: no plaintext secrets, encrypted tokens, RBAC enforced, CORS configured
8. Performance: assessment submission <500ms, worker processes jobs within 60s for small repos

---

End of IMPLEMENTATION-PLAN.md
