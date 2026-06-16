# MVP Epics and Stories

## Overview

This plan uses the Product Brief, PRD, Architecture and BRF-v1.0 as locked constraints. Stories are prioritized for an MVP build order. Priority: P0 (required), P1 (important), P2 (post-MVP). Complexity: S/M/L.

## Epic: Authentication & User Management

Epic Goal: Provide secure user accounts, JWT auth (RSA), refresh tokens, password flow and Casbin RBAC integration.
Business Value: Secure access, role enforcement, and developer onboarding.
Dependencies: PostgreSQL, RSA key management, email provider.

Stories:

- AUTH-01
  - Persona: Learner
  - Story: As a learner I can register with email/password so I can create an account.
  - Acceptance Criteria: registration endpoint creates user, password hashed (BCrypt), sends verification email link (or logs in dev), user row persisted. P0
  - Dependencies: DB, email
  - Complexity: M
- AUTH-02
  - Persona: Learner
  - Story: As a learner I can log in and receive JWT access + refresh tokens.
  - Acceptance Criteria: /auth/login returns RSA-signed access token and refresh token; access token contains version claim; tokens validated by protected endpoints.
  - Dependencies: rsa_keys, DB
  - Priority: P0
  - Complexity: M
- AUTH-03
  - Persona: Learner
  - Story: As a learner I can refresh access token using refresh token.
  - Acceptance Criteria: /auth/refresh issues new access token if refresh valid; refresh can be revoked.
  - Priority: P0
  - Complexity: M
- AUTH-04
  - Persona: Admin
  - Story: As an admin I can manage Casbin policies to enforce RBAC.
  - Acceptance Criteria: Casbin policy store in DB; basic policy applied to resource endpoints; admin CLI or API to add policy (protected).
  - Priority: P1
  - Complexity: M
- AUTH-05
  - Persona: Learner
  - Story: As a learner I can login with GitHub OAuth as social login.
  - Acceptance Criteria: OAuth flow redirects, callback links GitHub account to user and stores token metadata; re-auth on evaluation enforced.
  - Priority: P0
  - Complexity: M

## Epic: GitHub Integration

Epic Goal: Allow learners to connect GitHub, select repos, and let the system retrieve snapshots for evaluation.
Business Value: Core portfolio & evidence ingestion.
Dependencies: GitHub OAuth, MinIO/S3, Redis (queue), DB.

Stories:

- GIT-01
  - Persona: Learner
  - Story: As a learner I can connect my GitHub account via OAuth.
  - Acceptance Criteria: OAuth flow completes, user_oauth row persisted with metadata (scopes, expiry), UI shows connected account.
  - Priority: P0
  - Complexity: M
- GIT-02
  - Persona: Learner
  - Story: As a learner I can list accessible repositories and mark one for portfolio.
  - Acceptance Criteria: API lists repos (public+private per scopes), repo metadata saved, respect private flag.
  - Priority: P0
  - Complexity: M
- GIT-03
  - Persona: Learner
  - Story: As a learner I can request a repository snapshot for evaluation.
  - Acceptance Criteria: API creates snapshot job, stores snapshot_ref in MinIO (or local), assessment record created status=queued.
  - Priority: P0
  - Complexity: M
- GIT-04
  - Persona: Admin/DevOps
  - Story: As operator I can configure GitHub OAuth app settings and scopes.
  - Acceptance Criteria: Docs and env variables to set client ID/secret; CI secret usage documented.
  - Priority: P1
  - Complexity: S

## Epic: BRF Management

Epic Goal: Manage BRF YAML files, semantic versioning, and brf_versions registry.
Business Value: BRF is the product asset; ensures reproducible evaluation.
Dependencies: Git tags, repo, DB.

Stories:

- BRF-01
  - Persona: Product Manager
  - Story: As PM I can publish a BRF release (semantic version) via git tag and register it in the system.
  - Acceptance Criteria: CI or admin API creates brf_versions row with version, tag, commit_hash; BRF YAML included in artifact.
  - Priority: P0
  - Complexity: M
- BRF-02
  - Persona: Engineer
  - Story: As an engineer the system loads BRF YAML by version during evaluation.
  - Acceptance Criteria: Worker loads correct BRF version from local artifact or storage; errors if missing.
  - Priority: P0
  - Complexity: S
- BRF-03
  - Persona: PM/Admin
  - Story: As admin I can view available BRF versions in UI/API.
  - Acceptance Criteria: GET /brf/versions returns versions with metadata.
  - Priority: P1
  - Complexity: S

## Epic: Assessment Submission

Epic Goal: Allow learners to submit repos for assessment and track assessment lifecycle.
Business Value: Primary user flow for competency measurement.
Dependencies: Auth, GitHub Integration, BRF Management, Redis.

Stories:

- SUB-01
  - Persona: Learner
  - Story: As a learner I can select a repo and submit it for evaluation specifying BRF version.
  - Acceptance Criteria: POST /repos/:id/submit-eval creates assessment row with chosen brf_version and enqueues job; status=queued.
  - Priority: P0
  - Complexity: M
- SUB-02
  - Persona: Learner
  - Story: As a learner I can view assessment status and results once completed.
  - Acceptance Criteria: GET /assessments/:id returns status, timestamp, competency summaries once available.
  - Priority: P0
  - Complexity: M
- SUB-03
  - Persona: Learner
  - Story: As a learner I can provide manual explanations required by BRF detectors when requested.
  - Acceptance Criteria: API accepts manual answers stored with assessment; scoring includes manual component.
  - Priority: P0
  - Complexity: S

## Epic: Competency Evaluation Engine

Epic Goal: Implement worker, detectors, scoring engine and persistence for results.
Business Value: Core product value—evaluates portfolios against BRF.
Dependencies: BRF YAML, Redis, MinIO, DB.

Stories:

- EVAL-01
  - Persona: Engineer
  - Story: As an engineer I can run a worker that consumes jobs from Redis and processes snapshots.
  - Acceptance Criteria: Worker dequeues, processes snapshot, writes assessment results, updates status; basic error handling.
  - Priority: P0
  - Complexity: L
- EVAL-02
  - Persona: Engineer
  - Story: As an engineer I can run detectors for API Design, DB, Auth, Code Org, Business Logic, Docs, and Tests that output typed JSON signals.
  - Acceptance Criteria: Each detector produces JSON with file paths, confidence and sample snippets; output stored.
  - Priority: P0
  - Complexity: L
- EVAL-03
  - Persona: Engineer
  - Story: As an engineer I can apply BRF weighting and gating rules to detector outputs and compute competency scores/bands.
  - Acceptance Criteria: Scoring engine uses YAML config to compute per-competency score and band; results stored in competency_results.
  - Priority: P0
  - Complexity: M
- EVAL-04
  - Persona: Engineer
  - Story: As an engineer the worker handles retries, DLQ, and records job telemetry.
  - Acceptance Criteria: Retry policy configurable; failed jobs move to DLQ after N attempts; metrics emitted.
  - Priority: P1
  - Complexity: M

## Epic: Feedback Engine

Epic Goal: Generate human-readable feedback based on competency_results and BRF feedback templates.
Business Value: Learners get actionable guidance to improve.
Dependencies: Competency results, BRF templates.

Stories:

- FB-01
  - Persona: Learner
  - Story: As a learner I can view detailed feedback per competency after assessment.
  - Acceptance Criteria: UI/API returns feedback text with detected evidence, missing items and remediation tips.
  - Priority: P0
  - Complexity: M
- FB-02
  - Persona: Learner
  - Story: As a learner I can download a feedback report (PDF or JSON) summarizing strengths and gaps.
  - Acceptance Criteria: Endpoint generates downloadable report; includes competency scores and remediation bullets.
  - Priority: P1
  - Complexity: M

## Epic: Progress Tracking

Epic Goal: Track learner assessment history and show progression over time.
Business Value: Demonstrates learner growth and readiness.
Dependencies: Assessments, DB.

Stories:

- PROG-01
  - Persona: Learner
  - Story: As a learner I can view my assessment history with BRF versions.
  - Acceptance Criteria: /users/me/assessments returns list with timestamps, brf_version, scores.
  - Priority: P0
  - Complexity: S
- PROG-02
  - Persona: Learner
  - Story: As a learner I can request re-evaluation against a newer BRF version (migration analysis).
  - Acceptance Criteria: API creates new assessment_inference comparing past evidence to new BRF and returns delta; new assessment record created when learner chooses to fully re-evaluate.
  - Priority: P1
  - Complexity: L

## Epic: Portfolio System

Epic Goal: Allow learners to mark repositories as portfolio items and publish public showcase pages.
Business Value: External validation and job-seeking support.
Dependencies: GitHub Integration, Auth, DB.

Stories:

- PORT-01
  - Persona: Learner
  - Story: As a learner I can mark a repository as portfolio and add a public description.
  - Acceptance Criteria: Repo metadata updated with portfolio flag; public profile page shows selected repos.
  - Priority: P0
  - Complexity: S
- PORT-02
  - Persona: Learner
  - Story: As a learner I can publish a public portfolio page showing selected projects and assessment badges.
  - Acceptance Criteria: Public URL created; shows project summary and competency badges (non-sensitive results only).
  - Priority: P1
  - Complexity: M

## Epic: Admin & Operations

Epic Goal: Provide admin capabilities for BRF releases, manual review, and system health.
Business Value: Governance, auditability, and operational control.
Dependencies: All subsystems.

Stories:

- ADMIN-01
  - Persona: Admin
  - Story: As an admin I can view system health (DB, Redis, worker status) on a dashboard.
  - Acceptance Criteria: Basic health endpoints and UI; worker last-processed timestamp, queue length.
  - Priority: P1
  - Complexity: M
- ADMIN-02
  - Persona: Admin
  - Story: As an admin I can flag an assessment for manual review and override competency scores.
  - Acceptance Criteria: Admin UI to set assessment.manual_review and edit competency_results; audit log of changes.
  - Priority: P1
  - Complexity: M
- ADMIN-03
  - Persona: Admin
  - Story: As an operator I can rotate JWT RSA keys and mark token version changes in DB.
  - Acceptance Criteria: admin endpoint or deployment process to rotate keys; old tokens invalidated by version claim handling.
  - Priority: P0
  - Complexity: M

## Prioritization and Build Order (high-level)

Order rationale: foundation first (auth/db), then ingestion (GitHub), BRF loading & assessment submission, evaluation engine & detectors, feedback & portfolio, progress & admin polish.

1. Authentication & User Management (AUTH-01, AUTH-02, AUTH-03, AUTH-05, ADMIN-03) — P0
2. Database schema & migrations (embedded in above) — P0
3. GitHub Integration (GIT-01, GIT-02, GIT-03) — P0
4. BRF Management (BRF-01, BRF-02) — P0
5. Assessment Submission (SUB-01, SUB-02, SUB-03) — P0
6. Competency Evaluation Engine core (EVAL-01, EVAL-02, EVAL-03) — P0
7. Feedback Engine (FB-01) — P0
8. Portfolio System (PORT-01) — P0
9. Progress Tracking (PROG-01) — P0
10. Operational and polish stories (remaining P1) — P1

## Recommended Sprint Sequence

Sprint 1 (Foundation: 2 weeks)

- AUTH-01, AUTH-02, AUTH-03 (JWT+refresh), ADMIN-03 (key rotation), DB schema migrations, basic Casbin integration (skeleton), BRF-01 (CI tagging workflow).

Sprint 2 (Ingestion & Submission: 2 weeks)

- GIT-01, GIT-02 (OAuth & repo listing), SUB-01 (submit endpoint), BRF-02 (BRF loader), MinIO snapshot pipeline prototype, enqueue job into Redis.

Sprint 3 (Evaluation Engine & Detectors: 3 weeks)

- EVAL-01 (worker), EVAL-02 (implement core detectors for API Design, DB, Auth, Code Org), EVAL-03 (scoring engine).
- SUB-02 (result retrieval) and SUB-03 (manual explanations) support.

Sprint 4 (Feedback, Portfolio, Progress, Admin: 2 weeks)

- FB-01 feedback rendering, PORT-01 portfolio flagging, PROG-01 assessment history, ADMIN-02 manual review UI, operational dashboards, polish P1 stories.

## Notes & assumptions

- Team: solo developer—design choices favor modular monolith and developer ergonomics.
- Timeboxes assume experienced backend dev; adjust durations for team reality.
- Non-functional: logging, metrics, secrets management, CI/CD pipeline should be run in parallel as infra tasks.

End of MVP Epics and Stories
