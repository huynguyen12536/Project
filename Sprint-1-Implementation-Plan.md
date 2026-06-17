# Sprint 1 Implementation Plan

## Goal

Deliver a runnable vertical slice for LearnHub that validates end-to-end: user registration/login (JWT + refresh), GitHub OAuth connect, repository listing, repository submission for assessment (creates assessment record and enqueues job). This slice enables frontend-driven demo, manual BRF reference (BRF v1.0 included), and basic operational readiness.

## Minimum Stories (from MVP backlog)

- AUTH-01 Register (P0)
- AUTH-02 Login + JWT issuance (P0)
- AUTH-03 Refresh token lifecycle (P0)
- AUTH-05 GitHub OAuth connect (P0)
- GIT-01 List repositories & save metadata (P0)
- SUB-01 Submit repo for evaluation (P0)
- BRF-01 BRF v1.0 artifact in repo and brf_versions registration (P0)
- ADMIN-03 RSA key rotation endpoint/process (P0)

## Sprint deliverables

- Running backend (Spring Boot) with Postgres and Redis in docker-compose
- Basic React UI to register/login, connect GitHub, list repos and submit for evaluation
- Assessment persistence and job enqueueing (Redis list/stream)
- BRF v1.0 present and referenced by assessments
- Documentation: README with local dev steps

## Technical assumptions

- Java 17+ with Spring Boot
- Use Redis LPUSH/RPOP or Streams for job queue (abstracted service)
- MinIO optional; snapshots deferred (placeholder snapshot ref used)
- RSA keys provided via env or generated at first boot and stored in DB encrypted

## Sprint 1 Stories & Work Breakdown

Each story maps to implementation tasks below.

Technical Tasks (high-level)

- T1 Project skeleton: Initialize Spring Boot project, modules, build scripts, Dockerfile (Parallel)
- T2 Add dependencies: Spring Security, Spring Web, Spring Data JPA, Flyway, Redis client, Casbin adapter, OAuth client libs (Parallel)
- T3 Implement JWT util library (RSA signing/verification, version claim) (Backend)
- T4 Implement queue adapter interface (Redis-based, simple) (Backend)
- T5 Add BRF v1.0 YAML into /brf and add brf_versions seeded migration (DB/Backend)
- T6 API contract (OpenAPI minimal) for auth, oauth, repos, assessments (Backend)

Database Tasks

- DB1 Define schema migrations (Flyway SQL): users, user_oauth, rsa_keys, brf_versions, repositories, assessments, refresh_tokens, casbin_policy (Sequential)
- DB2 Create initial seed: brf_versions row for 1.0.0 (Parallel after DB1)
- DB3 Add DB migrations to Docker entrypoint for local dev (Parallel)

Backend Tasks

- B1 Implement User entity, repository, and service (register + password hashing) (Depends DB1)
- B2 Implement AuthController: /auth/register, /auth/login (issue access & refresh tokens), /auth/refresh (Depends B1, T3)
- B3 Implement RSA key manager service: load from env or generate on first run, persist to rsa_keys table encrypted (Depends DB1, T3) (Parallel with B1)
- B4 Implement Casbin skeleton (policy table + middleware) — basic deny-by-default hook (Depends DB1) (Parallel)
- B5 Implement GitHub OAuth flow endpoints and callback (store token metadata in user_oauth) (Depends B1, T2) (Parallel)
- B6 Implement GitHub API client service to list user repos (uses stored token metadata) (Depends B5)
- B7 Implement Repositories service & API to save selected repo metadata (Depends B6)
- B8 Implement Assessments service & endpoint POST /repos/:id/submit-eval: create assessment row, set brf_version default 1.0.0, enqueue job payload to Redis (Depends DB1, B7, T4) (Parallel)
- B9 Implement simple job worker stub (CLI or container) that can dequeue and mark assessment status=processing then status=queued_result_stub (for demo) (Depends T4) (Parallel)
- B10 Implement endpoints to GET assessment status/results (Depends B8, B9)
- B11 Implement admin endpoint for RSA key rotation: accept new key (or trigger generation) and increment key version; ensure tokens with old version invalidated (Depends B3) (Parallel)

Frontend Tasks

- F1 Setup React app scaffold (Vite/CRA) and auth client (Parallel)
- F2 Implement registration & login pages, store access/refresh tokens (secure storage) (Depends B2)
- F3 Implement GitHub Connect UI (button to start OAuth) and callback handling (Depends B5) (Parallel)
- F4 Implement Repo list UI and "Mark as portfolio / select" (Depends B6, B7)
- F5 Implement Submit for Evaluation UI and assessment status page (Depends B8, B10)
- F6 Add basic error handling and notification components (Parallel)

DevOps Tasks

- D1 Create docker-compose.yml for local dev: app, worker, postgres, redis, (minio optional), nginx reverse-proxy (Sequential early)
- D2 Configure environment variables template (.env.example) including RSA private key placeholder and GitHub client id/secret (Parallel)
- D3 CI skeleton: build + tests job (optional minimal) (Parallel)
- D4 Provide README dev instructions (run compose, migrate DB, seed BRF) (Parallel)

## Task Ordering & Dependencies

Phase A: Foundation (must start first)

- T1 Project skeleton
- T2 Add dependencies
- DB1 Define schema migrations
- D1 docker-compose (references app image built from T1)
- T3 Implement JWT util & T4 queue adapter (start after T2)
- B3 RSA key manager (depends DB1, T3)
  These can be started in parallel by a solo dev but follow order above for correctness.

Phase B: Core Backend (after DB migrations & JWT)

- B1 User entity & service (needs DB1)
- B2 AuthController (needs B1, T3)
- DB2 Seed BRF v1.0 (depends DB1)
- B5 GitHub OAuth flow (depends B1, T2)
- B6 GitHub client (depends B5)
  Parallelizable: B1, B3, B5 work in parallel once T2/T3 and DB1 are ready.

Phase C: Ingestion & Submission

- B7 Repositories service (depends B6)
- B8 Submit assessment endpoint (depends B7, T4, DB1)
- B10 GET assessment status (depends B8)
- B9 Worker stub (depends T4)

Phase D: Frontend & DevOps polish

- F1 React scaffold (can start early)
- F2 Auth pages (depends B2)
- F3 GitHub connect UI (depends B5)
- F4 Repo list UI (depends B6)
- F5 Submit & status UI (depends B8, B10)
- D2 .env and D4 README (ongoing)

Parallelizable tasks (can be developed concurrently)

- Project skeleton + dependencies (T1, T2)
- DB migrations and docker-compose (DB1, D1)
- JWT util, queue adapter, RSA key manager (T3, T4, B3)
- Backend user service & auth controller while frontend scaffolding (B1, B2, F1)
- GitHub OAuth backend & frontend (B5, F3) after B1
- Repos service & frontend listing (B7, F4)
- Worker stub & queue integration (B9, T4)

Minimum Acceptance Criteria (vertical slice)

- Dev can run docker-compose and access frontend at http://localhost:3000
- User can register and login; access token authenticates API requests
- User can connect GitHub via OAuth; app lists accessible repositories
- User can submit a repo for evaluation; assessment record created and queued
- API returns assessment status (queued/processing/done-stub)
- BRF v1.0 present and brf_versions seeded
- README describes local setup steps

Timeboxing & Estimates (solo dev rough)

- Foundation (T1,T2,DB1,D1): 2–3 days
- Auth & RSA (B1,B2,B3,T3): 3–4 days
- GitHub OAuth & repo listing (B5,B6,B7): 2–3 days
- Submission endpoint & worker stub (B8,B9,B10,T4): 2–3 days
- Frontend (F1–F5): 4–6 days (overlapped)
- DevOps docs & polish (D2–D4): 1–2 days
  Total: ~2–3 weeks (elastic depending on interruptions)

## Hand-off artifacts to produce during Sprint

- Flyway SQL migrations files
- API OpenAPI minimal spec for implemented endpoints
- .env.example and docker-compose.yml
- README with run/debug steps
- Postman collection or curl examples for core flows

## Developer notes

- Keep queue access via an adapter interface to allow later swap to Redis Streams/Kafka with minimal changes.
- For GitHub OAuth, run locally using ngrok or configure OAuth app with local redirect URL on GitHub dev settings.
- Snapshot retrieval is out of scope for Sprint 1; use a placeholder snapshot_ref and mark assessment.status accordingly.
- Use simple logging and health endpoints to aid debugging.

Sprint 1 Output file created.
