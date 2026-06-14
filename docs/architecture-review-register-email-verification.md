# 🏛️ Architecture Review Board — Consolidated Report
## LearnHub LMS — Sprint 1: Register + Email Verification

**Review Date:** 2026-06-13
**Reviewers:** Architecture Review Board
**Verdict:** NOT READY — 6 blockers, security remediation required, 2 abstractions must be extracted before Sprint 2

---

## 1. TIGHT COUPLING — 6 Issues Identified

| # | Coupling Point | Problem | Severity | Remediation |
|---|---------------|---------|----------|-------------|
| 1 | **UserService.register() sends email synchronously inside @Transactional** | If ResendEmailService times out (5s+), the DB connection is held open. If it throws, the rolled-back transaction orphans the VerificationToken or leaves the user in inconsistent state. DB and email lifecycle are conflated. | HIGH | Decouple via Spring `ApplicationEventPublisher`. Move email sending to a `@TransactionalEventListener(phase = AFTER_COMMIT)` listener. The register method commits the user+token, then the listener fires email asynchronously. If email fails, the user is still registered — retry or mark token for re-send. |
| 2 | **AuthController handles both authentication AND registration** | Single controller accrues `/login`, `/register`, `/verify-email`, `/refresh`, `/logout`. Cohesion degrades with each endpoint. Testing requires mocking unrelated dependencies. | MEDIUM | Split into `AuthController` (login/refresh/logout) and `RegistrationController` (register/verify-email/resend-verification). Each has a focused service dependency. |
| 3 | **Zustand auth.store.ts mixes auth + registration state** | `auth.store.ts` will hold `login()`, `logout()`, `register()`, `verifyEmail()`, `refreshToken()`, `user`, `token`, `verificationState`. At 6+ actions the store violates single-responsibility. Selectors re-render unrelated components. | MEDIUM | Extract `registration.store.ts` with `register()`, `verifyEmail()`, `resendVerification()`, `registrationError`, `verificationStatus`. Keep `auth.store.ts` for session management only. |
| 4 | **UserService directly assigns default role ("STUDENT")** | Hardcoded role name in business logic. Changing the default role requires code change + redeploy. Role assignment logic is not testable in isolation. | MEDIUM | Introduce `RoleAssignmentService` with method `assignDefaultRole(User user)`. Role name fetched from config/properties (`app.security.default-role=STUDENT`). Allows A/B testing instructor-first onboarding. |
| 5 | **VerificationToken hashing algorithm (SHA-256) hardwired in UserService** | Cannot rotate hashing algorithm without rewriting service. No migration path if SHA-256 is found weak for token hashing. | MEDIUM | Extract `TokenHasher` interface with `hash(String raw): String` and `verify(String raw, String hash): boolean`. Default impl: `Sha256TokenHasher`. Future: `HmacTokenHasher`, `BcryptTokenHasher`. |
| 6 | **RegisterSchema (Zod) duplicates backend password policy** | Password rules defined in two places. Changing min-length from 8 to 12 requires coordinated frontend + backend deploys. Mismatch causes confusing UX (frontend accepts, backend rejects). | HIGH | Single source of truth via API metadata endpoint (`GET /api/auth/password-policy`) returning `{minLength, requireUppercase, requireDigit, requireSpecial}`. Zod schema built dynamically from API response on app init. |

---

## 2. HARDCODED VALUES & MAGIC NUMBERS

| # | Location | Hardcoded Value | Should Be |
|---|----------|----------------|-----------|
| 1 | `UserService.register()` | `"STUDENT"` role name | `@Value("${app.security.default-role}")` |
| 2 | `UserService.register()` | Token expiry duration (likely `24` or `48` hours) | `@Value("${app.verification.token-expiry-hours:24}")` |
| 3 | `VerificationToken` table | `VARCHAR(64)` — assumes SHA-256 hex output is always 64 chars | Compute from `TokenHasher.outputLength()` or use `VARCHAR(128)` for future-proofing |
| 4 | `register.schema.ts` | Password rules: `min(8)`, regex for upper/digit/special | Fetched from `GET /api/auth/password-policy` |
| 5 | `EmailService` implementations | Sender address `"noreply@learnhub.com"` | `@Value("${app.mail.from}")` |
| 6 | `EmailService` implementations | Subject lines `"Verify your email"` | `@Value("${app.mail.verification-subject}")` or i18n message source |
| 7 | `ConsoleEmailService` / `ResendEmailService` | Verification link base URL | `@Value("${app.base-url}")` + `UriComponentsBuilder` |
| 8 | `VerifyEmailPage.tsx` | 6 hardcoded state strings | Enum or const object shared with other verification consumers |
| 9 | `UserService.verifyEmail()` | Token expiry check inline | Delegate to `TokenHasher.verify()` which encapsulates comparison + expiry |
| 10 | Flyway migration | `VARCHAR(64)` for `tokenHash` | Coupled to SHA-256; should use larger column or `TEXT` |

---

## 3. SECURITY RISK REGISTER — 11 Findings

| # | Risk | Severity | Attack Vector | Mitigation |
|---|------|----------|---------------|------------|
| 1 | **SHA-256 without salt or HMAC for token hashing** | **CRITICAL** | Attacker with DB read access precomputes SHA-256 rainbow tables for common UUIDs/timestamps used as raw tokens. On DB breach, all unexpired tokens are cracked in minutes. | Use **HMAC-SHA256** with a 256-bit secret from Vault/env, or **BCrypt** (though slower). Tokens are short-lived secrets — treat them like passwords. |
| 2 | **No rate limiting on POST /register** | **HIGH** | Attacker scripts 10,000 registrations/minute. Exhausts DB connections, fills `users`/`verification_tokens` tables, incurs email sending costs (Resend bills per email). | Implement Bucket4j or Resilience4j rate limiter: 5 registrations/IP/hour, 3/email/day. Return `429` with `Retry-After` header. |
| 3 | **No rate limiting on POST /verify-email** | **HIGH** | Attacker brute-forces 6-digit numeric tokens or short alphanumeric tokens at 1000 req/s. Without lockout, token space is exhausted quickly. | Rate limit: 10 attempts/IP/minute. After 5 failed attempts for same user, invalidate all pending tokens and force re-registration flow. |
| 4 | **Email enumeration via distinct error messages** | **HIGH** | `POST /register` returns `"Email already exists"` vs `"Username already exists"`. Attacker submits 10,000 emails → discovers which are registered → credential stuffing on login. | Return **identical generic response**: `"If the information is valid, a verification email has been sent."` Log the real reason server-side. Same pattern for login. |
| 5 | **No CAPTCHA/turnstile on registration** | **MEDIUM** | Fully automated bot registration. Scripted account farms for spam reviews, fake enrollments, SEO injection. | Integrate Cloudflare Turnstile (free, invisible) or reCAPTCHA v3. Validate server-side. Bypass for E2E tests via test profile. |
| 6 | **Password policy enforced only client-side (Zod)** | **MEDIUM** | Attacker bypasses React form, sends `POST /register` with `{"password":"a"}` via curl. Backend accepts, user has weak password. | Server-side validation with same policy via `@Valid` + custom `@PasswordConstraint` annotation backed by `PasswordPolicy` service. Never trust client validation. |
| 7 | **No idempotency on POST /register** | **MEDIUM** | Network retry creates duplicate user rows (if race condition) or duplicate tokens. User receives 2+ verification emails — confusion + support tickets. | Accept `Idempotency-Key` header. Cache key → response for 24h. Return cached 201 if replay detected. |
| 8 | **Verification token not bound to user context** | **MEDIUM** | Token hash alone verifies the token value, but if two users somehow get the same raw token (collision or predictable generation), token from user A could verify user B's email. | HMAC computation includes `userId` as additional data: `HMAC-SHA256(secret, rawToken + ":" + userId)`. Verification reconstructs with claimed userId. |
| 9 | **Token `usedAt IS NULL` without DB-enforced uniqueness** | **LOW** | Race condition: two concurrent `POST /verify-email` with same token both check `usedAt IS NULL` before either writes. Token used twice. | `UPDATE verification_tokens SET used_at = NOW() WHERE token_hash = ? AND used_at IS NULL` — use affected row count (1 = success, 0 = already used). Add optimistic lock via `WHERE` clause. |
| 10 | **No CSRF protection mentioned** | **LOW** | If cookies are used for any auth (even indirectly), CSRF attacks can perform state-changing operations. | Spring Security enables CSRF by default for non-stateless configs. For stateless JWT, CSRF is not applicable — but explicitly document this decision. |
| 11 | **Email verification link exposed in query params** | **LOW** | Token in URL query string logged by proxies, browser history, server access logs, analytics scripts. | Use path parameter or fragment. Set `Referrer-Policy: no-referrer` on verification page. Log token parameter as `[REDACTED]` in access logs. |

---

## 4. MISSING ABSTRACTIONS — 7 Identified

| # | Abstraction | Why Needed | Interface | Future Benefit |
|---|-------------|------------|-----------|----------------|
| 1 | **TokenService** | Token generation, hashing, and validation currently scattered across `UserService` and `VerificationTokenRepository`. No single place to change algorithm or token format. | `generateToken(User user): String` / `validateToken(String rawToken): ValidationResult` / `invalidateTokens(User user): void` | Swap SHA-256 → HMAC without touching UserService. Add token type enum (EMAIL_VERIFY, PASSWORD_RESET, MFA) later. |
| 2 | **PasswordPolicy** | Password rules duplicated in Zod schema and backend. Adding passphrase support or zxcvbn integration requires changes in 2 codebases. | `validate(String password): PolicyResult` / `describe(): PolicyDescriptor` / `score(String password): int` | Single `GET /api/auth/password-policy` endpoint. Frontend dynamically builds Zod schema. Future: zxcvbn scoring, breached-password check (HaveIBeenPwned API). |
| 3 | **RegistrationOrchestrator** | `UserService.register()` does validation → user save → role assign → token save → email send → event publish. 6 responsibilities, 4 dependencies. Cannot test registration flow without mocking all of them. | `register(RegistrationRequest): RegistrationResult` — coordinates validators, UserService, TokenService, RoleAssignmentService, EventPublisher | Test registration in isolation. Swap steps (e.g., skip email for test users). Add pre-registration hooks (fraud check, invite-only check). |
| 4 | **IdentityProvider interface** | Prepares for Sprint 3+ social login (Google, GitHub). Currently, register() assumes username+password always. | `authenticate(AuthRequest): AuthResult` / `register(IdentityClaims): User` | Add `GoogleIdentityProvider`, `GitHubIdentityProvider` without rewriting UserService. OAuth2 flow becomes a new provider, not a fork. |
| 5 | **NotificationChannel interface** | EmailService is the only channel. When push notifications, SMS, or in-app notifications are added, every consumer must change. | `send(Notification notification): void` / `supports(ChannelType type): boolean` | `EmailChannel`, `SmsChannel`, `PushChannel`, `InAppChannel`. NotificationService dispatches to all supported channels. Retry/fallback per channel. |
| 6 | **AuditTrailService** | Audit events published directly via `ApplicationEventPublisher` in UserService. No guaranteed delivery, no schema, no query capability. | `record(AuditEvent event): void` — persists to `audit_events` table with JSONB payload | Queryable audit log for compliance (GDPR Art. 30). Retention policies. Export for SIEM. |
| 7 | **UserStatusStateMachine** | `UserStatus` enum transitions are ad-hoc (`PENDING_VERIFICATION → ACTIVE`, `ACTIVE → LOCKED`). Invalid transitions not enforced (e.g., `LOCKED → PENDING_VERIFICATION`). | `transition(User user, StatusEvent event): User` — throws `InvalidStatusTransitionException` for illegal moves | Guarantees users can't skip verification. Supports future statuses (SUSPENDED, DELETED) with defined transition rules. |

---

## 5. TECHNICAL DEBT — Incurred by Current Design

| # | Debt Item | Impact Window | Consequence |
|---|-----------|---------------|-------------|
| 1 | **No VerificationToken cleanup job** | 3–6 months | `verification_tokens` table grows unbounded. At 10K registrations/day, 3M rows/year. Queries on `tokenHash` degrade without partitioning. Must add Spring `@Scheduled` job (or Quartz) to `DELETE WHERE expires_at < NOW() - INTERVAL '7 days'`. |
| 2 | **No API versioning strategy** | 2–4 sprints | `/api/auth/register` and `/api/auth/verify-email` have no version prefix. Changing contract breaks mobile clients. Add `/api/v1/` now or use `Accept: application/vnd.learnhub.v1+json`. |
| 3 | **Audit events fire-and-forget with no persistence** | Immediately | `UserRegisteredEvent` published synchronously but no listener persists it to `audit_events`. Events are lost if no subscriber registered. GDPR right-of-access requests cannot be fulfilled. |
| 4 | **ConsoleEmailService is the only dev implementation** | Immediately | No way to inspect "sent" emails in integration tests. Use `GreenMail` (embedded SMTP) or `InMemoryEmailService` with `getSentEmails(): List<Email>` for test assertions. |
| 5 | **No integration test for verification flow** | Sprint 1 | Email verification is the critical path for user activation. Without `@SpringBootTest` + Testcontainers (PostgreSQL) + GreenMail, regressions in token validation or expiry logic are undetected until staging. |
| 6 | **UserService carries 4 dependencies and 2 flows** | Sprint 2+ | As password reset, MFA, and social login are added, UserService becomes a god class. Extract `RegistrationService`, `VerificationService`, `PasswordResetService` now. |
| 7 | **Frontend types/api.ts will become a dumping ground** | Sprint 3 | All API types in one file. By Sprint 3: login types, register types, course types, enrollment types, review types — 500+ lines. Split now: `types/auth.ts`, `types/registration.ts`. |

---

## 6. SCALABILITY ANALYSIS — User Growth Impact

### 100K Users (~1K registrations/day)
✅ Current architecture handles this comfortably.
- Synchronous email sending via Resend (~100ms avg) is acceptable.
- PostgreSQL handles token lookups on indexed `tokenHash` column efficiently.
- Single-instance rate limiter (in-memory Bucket4j) sufficient.

### 1M Users (~10K registrations/day, 300K pending tokens)
⚠️ Strains appear.
- **Email bottleneck:** Resend API at 10 req/s is fine, but synchronous send blocks Tomcat threads. 200 threads × 100ms email = 2,000 concurrent registrations max. Move to **async listener with virtual threads** (Java 21+).
- **Token DB bloat:** 300K unexpired tokens. `SELECT ... WHERE token_hash = ? AND used_at IS NULL` still uses index efficiently, but full-table expiry cleanup needs **partial index**: `CREATE INDEX idx_tokens_expired ON verification_tokens(expires_at) WHERE used_at IS NULL`.
- **Rate limiting:** In-memory Bucket4j lost on restart. Move to **Redis** for distributed rate limiting.

### 10M Users (~100K registrations/day, 3M pending tokens)
❌ Architecture must evolve.
- **Async email mandatory:** `UserRegisteredEvent` → RabbitMQ/Kafka → dedicated `EmailWorker` service. Registration returns 202 Accepted immediately. Email workers scale independently.
- **Token table partitioning:** Partition `verification_tokens` by `created_at` (monthly). Expiry cleanup uses `DROP PARTITION` (instant) instead of `DELETE` (hours).
- **Read replicas:** Verification reads (`SELECT WHERE token_hash`) hit read replica. Writes hit primary.
- **Global rate limiting:** Redis Cluster with sliding-window algorithm. `registration:ratelimit:{ip}` and `registration:ratelimit:{email}` keys with TTL.
- **CDN for static verification page:** VerifyEmailPage served from CDN edge, calls API for token validation only.
- **Connection pooling:** HikariCP pool size tuned for 200+ concurrent registrations. PgBouncer in front of PostgreSQL.

| Metric | 100K Users | 1M Users | 10M Users |
|--------|-----------|----------|-----------|
| Registrations/day | 1K | 10K | 100K |
| Pending tokens | 30K | 300K | 3M |
| Email send strategy | Sync | Async (virtual threads) | Queue (RabbitMQ/Kafka) |
| Rate limiter | In-memory Bucket4j | Redis (single) | Redis Cluster |
| Token table | Heap table | Partial index | Monthly partitioning |
| DB reads | Primary | Primary | Read replica for verify |
| Token hash index | B-tree on tokenHash | B-tree (still OK) | B-tree + BRIN on created_at |

---

## 7. BACKEND FILES — Expected Deliverables

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/com/learnhub/user/UserStatus.java` | CREATE | Enum: PENDING_VERIFICATION, ACTIVE, LOCKED, DISABLED |
| `src/main/java/com/learnhub/user/User.java` | MODIFY | Add `status` (UserStatus), `emailVerified` (boolean), `emailVerifiedAt` (Instant) |
| `src/main/java/com/learnhub/token/VerificationToken.java` | CREATE | JPA entity: tokenId, userId (FK), tokenHash, expiresAt, usedAt, createdAt |
| `src/main/java/com/learnhub/token/VerificationTokenRepository.java` | CREATE | Spring Data: findByTokenHash(), markUsed(), deleteExpired() |
| `src/main/java/com/learnhub/role/Role.java` | CREATE | JPA entity: roleId, roleName (STUDENT, INSTRUCTOR, ADMIN) |
| `src/main/java/com/learnhub/role/RoleRepository.java` | CREATE | findByRoleName(String) |
| `src/main/java/com/learnhub/role/UserRole.java` | CREATE | Join entity: userId + roleId composite key |
| `src/main/java/com/learnhub/role/UserRoleRepository.java` | CREATE | findByUserId(), assignRole() |
| `src/main/java/com/learnhub/user/UserRepository.java` | MODIFY | Add existsByEmail(), findByEmail() |
| `src/main/java/com/learnhub/user/UserService.java` | CREATE | register(RegisterRequest), verifyEmail(String token) |
| `src/main/java/com/learnhub/email/EmailService.java` | CREATE | Interface: sendVerificationEmail(String to, String token) |
| `src/main/java/com/learnhub/email/ConsoleEmailService.java` | CREATE | Dev implementation logging to console |
| `src/main/java/com/learnhub/email/ResendEmailService.java` | CREATE | Production implementation via Resend API |
| `src/main/java/com/learnhub/email/EmailTemplateService.java` | CREATE | Thymeleaf template rendering for email bodies |
| `src/main/java/com/learnhub/auth/AuthController.java` | MODIFY | Add POST /register, POST /verify-email endpoints |
| `src/main/java/com/learnhub/event/UserRegisteredEvent.java` | CREATE | Event record: userId, email, timestamp |
| `src/main/java/com/learnhub/event/EmailVerifiedEvent.java` | CREATE | Event record: userId, email, verifiedAt |
| `src/main/java/com/learnhub/event/RegistrationEventListener.java` | CREATE | @TransactionalEventListener for AFTER_COMMIT email dispatch |
| `src/main/java/com/learnhub/exception/UsernameAlreadyExistsException.java` | CREATE | 409 Conflict |
| `src/main/java/com/learnhub/exception/EmailAlreadyExistsException.java` | CREATE | 409 Conflict |
| `src/main/java/com/learnhub/exception/VerificationTokenNotFoundException.java` | CREATE | 404 Not Found |
| `src/main/java/com/learnhub/exception/VerificationTokenExpiredException.java` | CREATE | 410 Gone |
| `src/main/java/com/learnhub/exception/VerificationTokenUsedException.java` | CREATE | 409 Conflict |
| `src/main/java/com/learnhub/dto/RegisterRequest.java` | CREATE | DTO: username, email, password, firstName, lastName |
| `src/main/java/com/learnhub/dto/VerifyEmailRequest.java` | CREATE | DTO: token (String) |
| `src/main/java/com/learnhub/dto/RegistrationResponse.java` | CREATE | DTO: message, userId |
| `src/main/java/com/learnhub/config/SecurityConfig.java` | MODIFY | Permit /register, /verify-email; add rate limiting filter |
| `src/main/java/com/learnhub/config/EmailConfig.java` | CREATE | @ConfigurationProperties for mail settings |
| `src/main/resources/db/migration/V2__add_user_status.sql` | CREATE | ALTER TABLE users ADD status, email_verified, email_verified_at |
| `src/main/resources/db/migration/V3__create_verification_tokens.sql` | CREATE | CREATE TABLE verification_tokens |
| `src/main/resources/db/migration/V4__create_roles.sql` | CREATE | CREATE TABLE roles, user_roles |
| `src/main/resources/templates/email/verification.html` | CREATE | Thymeleaf HTML email template |
| `src/main/resources/application.yml` | MODIFY | Add app.mail.*, app.verification.*, app.security.* properties |

---

## 8. FRONTEND FILES — Expected Deliverables

| File | Action | Purpose |
|------|--------|---------|
| `src/types/api.ts` | MODIFY | Add RegisterRequest, VerifyEmailRequest, RegistrationResponse, PasswordPolicy types |
| `src/types/registration.ts` | CREATE | RegistrationState enum, VerificationStatus enum |
| `src/schemas/register.schema.ts` | CREATE | Zod schema: username, email, password, confirmPassword, firstName, lastName |
| `src/schemas/verify-email.schema.ts` | CREATE | Zod schema: token (min 1) |
| `src/services/auth.service.ts` | MODIFY | Add register(), verifyEmail(), fetchPasswordPolicy() API calls |
| `src/stores/auth.store.ts` | MODIFY | Add registrationError, verificationStatus, isRegistering, isVerifying |
| `src/components/auth/RegisterForm.tsx` | CREATE | Form: username, email, password, confirmPassword, firstName, lastName. Client-side Zod validation. Submit → store.register(). Loading/error/success states. |
| `src/components/auth/LoginForm.tsx` | MODIFY | Add link "Don't have an account? Register" → navigate to /register |
| `src/pages/RegisterPage.tsx` | CREATE | Page wrapper: "Create your account" heading, RegisterForm, link to /login |
| `src/pages/VerifyEmailPage.tsx` | CREATE | 6 states: loading, success, invalid-token, expired-token, network-error, missing-token. Calls store.verifyEmail(token) on mount. |
| `src/pages/LandingPage.tsx` | CREATE | Hero section, "Start Learning" CTA → /register, "Already have an account?" → /login |
| `src/App.tsx` | MODIFY | Add routes: /register → RegisterPage, /verify-email → VerifyEmailPage, / → LandingPage |
| `src/components/auth/index.ts` | MODIFY | Barrel export: add RegisterForm |
| `src/types/barrel.ts` | MODIFY | Re-export registration types |

---

## 9. DATABASE OBJECTS — Migration Impact

| Object | Type | Purpose |
|--------|------|---------|
| `users.status` column | MODIFY | `VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION'` — tracks UserStatus enum. Existing users must be backfilled to 'ACTIVE'. |
| `users.email_verified` column | MODIFY | `BOOLEAN NOT NULL DEFAULT FALSE`. Denormalized for fast lookup without joining tokens table. |
| `users.email_verified_at` column | MODIFY | `TIMESTAMPTZ NULL` — audit timestamp of verification. |
| `idx_users_email` index | CREATE | `CREATE INDEX idx_users_email ON users(email)` — for existsByEmail() lookups during registration. |
| `idx_users_username` index | CREATE | `CREATE INDEX idx_users_username ON users(username)` — for existsByUsername() lookups. |
| `verification_tokens` table | CREATE | Columns: token_id (BIGSERIAL PK), user_id (BIGINT FK → users), token_hash (VARCHAR(128) NOT NULL), expires_at (TIMESTAMPTZ NOT NULL), used_at (TIMESTAMPTZ NULL), created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW()) |
| `uq_verification_tokens_hash` constraint | CREATE | `UNIQUE(token_hash)` — prevents duplicate tokens, enables fast lookup. |
| `idx_vt_user_id` index | CREATE | `CREATE INDEX idx_vt_user_id ON verification_tokens(user_id)` — for invalidating all tokens for a user. |
| `idx_vt_expires_at_null_used` index | CREATE | `CREATE INDEX idx_vt_expires_cleanup ON verification_tokens(expires_at) WHERE used_at IS NULL` — for cleanup job. |
| `roles` table | CREATE | Columns: role_id (INTEGER PK), role_name (VARCHAR(32) UNIQUE NOT NULL). Seed data: STUDENT, INSTRUCTOR, ADMIN. |
| `user_roles` table | CREATE | Columns: user_id (BIGINT FK → users), role_id (INTEGER FK → roles). PK on (user_id, role_id). |
| `idx_user_roles_user_id` index | CREATE | Implicit via PK, but may need reverse lookup `idx_ur_role_id` for "find all admins". |
| `audit_events` table | CREATE | Recommended: event_id (BIGSERIAL PK), event_type (VARCHAR(64)), user_id (BIGINT NULL), payload (JSONB), created_at (TIMESTAMPTZ DEFAULT NOW()). Index on (event_type, created_at). |

---

## 10. EVENT OBJECTS — Event Catalog

| Event | Fields | Publisher | Listener(s) |
|-------|--------|-----------|-------------|
| `UserRegisteredEvent` | `userId: Long`, `email: String`, `username: String`, `registeredAt: Instant` | `UserService.register()` after successful commit (via `@TransactionalEventListener(phase=AFTER_COMMIT)`) | `RegistrationEventListener` → sends verification email. `AuditEventListener` → writes USER_REGISTERED audit. Future: `NotificationListener` → welcome notification. |
| `EmailVerificationSentEvent` | `userId: Long`, `email: String`, `tokenId: Long`, `sentAt: Instant` | `RegistrationEventListener` after email API returns success | `AuditEventListener` → writes EMAIL_VERIFICATION_SENT audit. Future: Analytics listener for funnel metrics. |
| `EmailVerifiedEvent` | `userId: Long`, `email: String`, `verifiedAt: Instant` | `UserService.verifyEmail()` after successful token consumption | `AuditEventListener` → writes EMAIL_VERIFIED audit. Future: `OnboardingListener` → create default learning path. `AnalyticsListener` → activation rate metric. |
| `EmailVerificationFailedEvent` | `userId: Long` (nullable), `email: String`, `reason: String` (EXPIRED/USED/NOT_FOUND), `attemptedAt: Instant` | `UserService.verifyEmail()` when validation fails | `AuditEventListener` → writes EMAIL_VERIFICATION_FAILED. Future: `SecurityListener` → detect brute-force patterns, trigger account lockout. |

---

## 11. SECURITY CONCERNS — Executive Summary

| Priority | Finding | Severity | Action Required |
|----------|---------|----------|-----------------|
| 🔴 #1 | SHA-256 without salt for token hashing | **CRITICAL** | Switch to HMAC-SHA256 before any deployment. DB breach = all tokens cracked. |
| 🔴 #2 | No rate limiting on /register or /verify-email | **HIGH** | Add Bucket4j filter before Sprint 1 merge. Trivial to brute-force or spam. |
| 🔴 #3 | Email enumeration via distinct error responses | **HIGH** | Unify all registration failure responses. This is an OWASP Top 10 finding. |
| 🟡 #4 | No CAPTCHA on registration | **MEDIUM** | Add Turnstile. Low implementation effort, high bot-prevention ROI. |
| 🟡 #5 | Password policy not enforced server-side | **MEDIUM** | Add `@PasswordConstraint` validator. Never trust client-only validation. |
| 🟡 #6 | No idempotency on registration | **MEDIUM** | Accept `Idempotency-Key` header. Prevents duplicate users on retry. |
| 🟡 #7 | Token not bound to user identity | **MEDIUM** | Include userId in HMAC input. Prevents cross-user token replay. |
| 🟢 #8 | No explicit CSRF documentation | **LOW** | Document: stateless JWT → CSRF disabled by design. Add to ADR. |
| 🟢 #9 | Token in URL query string | **LOW** | Move to path parameter. Set `Referrer-Policy: no-referrer`. |
| 🟢 #10 | Missing `usedAt` race condition protection | **LOW** | Use `WHERE used_at IS NULL` in UPDATE, check affected rows. |

**3 blocking (CRITICAL/HIGH), 4 must-fix pre-Sprint 2 (MEDIUM), 3 hardening items (LOW).**

---

## 12. REFACTORING SUGGESTIONS — Priority Ordered

| # | Suggestion | Rationale | Effort |
|---|------------|-----------|--------|
| 1 | **Extract TokenService before writing UserService** | Token generation/hashing/validation is a standalone concern with its own algorithm choice, expiry policy, and testing surface. Building it into UserService guarantees a painful extraction later. | 2h — interface + impl class |
| 2 | **Add PasswordPolicy API endpoint before building RegisterForm** | The Zod schema must mirror backend rules. Building the form first means either hardcoding (debt) or rewriting later. Expose `GET /api/auth/password-policy` now; frontend fetches on mount and builds schema dynamically. | 1h backend + 30min frontend |
| 3 | **Split RegistrationController from AuthController now** | AuthController grows by 2 endpoints in Sprint 1, 3 more in Sprint 2 (password reset), 2 more in Sprint 3 (social login). Splitting now costs 15 minutes; splitting later costs refactoring tests + frontend URL changes. | 15min |
| 4 | **Use @TransactionalEventListener(AFTER_COMMIT) for email dispatch** | Sending email inside @Transactional couples DB transaction to external API call. If Resend takes 3 seconds, DB connection is held 3 seconds. AFTER_COMMIT ensures the user is persisted before we attempt delivery. | 30min — move logic to listener |
| 5 | **Replace ConsoleEmailService with GreenMail for tests** | ConsoleEmailService outputs to stdout — not assertable. GreenMail is an embedded SMTP server: start it in @SpringBootTest, inject GreenMail bean, assert `getReceivedMessages().length == 1`. Zero production dependencies, 1 test dependency. | 1h setup + first test |
| 6 | **Add Flyway migration versioning convention** | Migrations named `V2__add_user_status.sql`, `V3__create_verification_tokens.sql` — what happens when another dev creates `V2__add_avatar.sql` on a parallel branch? Adopt timestamp-based or branch-prefixed versions. | 5min — document in CONTRIBUTING.md |
| 7 | **Add API version prefix now** | `/api/auth/register` → `/api/v1/auth/register`. Retrofitting versioning after mobile clients ship is a breaking change. Add `@RequestMapping("/api/v1")` on a base controller class. | 10min — annotation change |

---

## 13. FUTURE EXTENSION POINTS — Architecture Readiness Assessment

| Extension | Current Readiness | Gap | Recommended Prep Now |
|-----------|-------------------|-----|----------------------|
| **MFA (TOTP/WebAuthn)** | 🟡 Partial | No `UserCredential` abstraction. MFA would add another token type (TOTP secret) to verification_tokens or a new table. | Add `token_type` column to `verification_tokens` (EMAIL_VERIFY, TOTP_SETUP, PASSWORD_RESET, MFA_CHALLENGE). Current design only handles EMAIL_VERIFY implicitly. |
| **Social Login (Google/GitHub)** | 🟡 Partial | User creation assumes `password` is mandatory. Social users have no password. `User.password` must be nullable. | Make `password` column nullable. Add `auth_provider` (LOCAL, GOOGLE, GITHUB) and `provider_user_id` columns. Add `IdentityProvider` interface now (see Section 4). |
| **Password Reset** | 🟢 Good | Same token infrastructure (VerificationToken) can serve password reset by adding `token_type = PASSWORD_RESET`. Reset flow: request → generate token → email → verify token → accept new password. | Add `token_type` column early. Password reset is Sprint 1.5 or 2. |
| **Notifications (Push/SMS/In-App)** | 🔴 Not ready | EmailService is the only channel. Adding SMS requires changing every caller. | Extract `NotificationChannel` interface now (see Section 4). Email becomes one channel. Registration success notification dispatches to all supported channels. |
| **Audit Logging (Compliance)** | 🔴 Not ready | Events published but never persisted. GDPR requires proof of consent and processing records. | Implement `AuditEventListener` that persists to `audit_events` table. 2-hour task with enormous compliance value. |
| **Multi-Tenancy** | 🔴 Not ready | All tables lack `tenant_id`. Retrofitting multi-tenancy requires touching every query. | Add `tenant_id` column to `users` and `verification_tokens` with default value (single-tenant for now). Use Hibernate `@Filter` to apply tenant context. Adds 1 column per table, costs nearly nothing now, saves months later. |
| **i18n Email Templates** | 🟡 Partial | Thymeleaf templates mentioned but locale resolution unspecified. | Use Spring MessageSource for email subjects. Thymeleaf templates resolved by locale. Accept-Language header or user preference drives locale. |
| **Event Bus (RabbitMQ/Kafka)** | 🟡 Partial | Spring ApplicationEvents are in-process only. Future async workers need message broker. | Publish events via `ApplicationEventPublisher` now. Later: add `EventBus` adapter that wraps both in-process and broker publishing. Consumers don't change. |
| **Analytics Funnel** | 🟡 Partial | Events exist but no analytics schema. Funnel: visit landing → click register → submit form → verify email → first login. | Add `analytics_event_id` (UUID) to registration flow. Pass from frontend to backend. Correlate UserRegisteredEvent ↔ EmailVerifiedEvent for activation rate. |

---

## 14. ARCHITECTURE SCORE — 67/100

| Category | Score | Commentary |
|----------|-------|------------|
| **Domain Model** | 7/10 | UserStatus enum is well-considered. VerificationToken as separate aggregate is correct. Missing: nullable password for social login, token_type discriminator, explicit state transitions. User-Role modeling is standard but RoleAssignment is not a domain concept yet. |
| **Repository Design** | 7/10 | Standard Spring Data approach. Missing: `deleteExpired()` query, pagination for admin token view, batch invalidation. `findByTokenHash` is correct but needs `WHERE used_at IS NULL` baked in. |
| **Service Design** | 4/10 | UserService is overloaded (6 responsibilities). No clear separation between registration orchestration and user CRUD. Email sending inside transaction is an architectural anti-pattern. No service interface for token operations. This is the weakest layer. |
| **API Design** | 6/10 | RESTful endpoints are correctly identified. Missing: API versioning, idempotency support, rate limit headers, consistent error response format (RFC 7807 Problem Details), password-policy endpoint. Register request DTO includes firstName/lastName — good. |
| **Security** | 4/10 | SHA-256 without HMAC is a critical vulnerability. No rate limiting. Email enumeration possible. Password policy not enforced server-side. No CAPTCHA. CSRF decision undocumented. These are Sprint 1 blockers, not nice-to-haves. |
| **Event System** | 5/10 | Events identified correctly with appropriate fields. Missing: AFTER_COMMIT phase enforcement, event persistence (audit), async event handling configuration, dead-letter handling for failed email events. |
| **Email Abstraction** | 6/10 | Interface segregation is correct (EmailService interface + ConsoleEmailService + ResendEmailService). Missing: testable implementation (GreenMail/InMemory), template locale resolution, email success/failure metrics, retry strategy. |
| **Frontend Architecture** | 7/10 | Zustand + Zod + shadcn/ui is a solid modern stack. 6-state VerifyEmailPage shows good UX thinking. Missing: separate registration store, password-policy dynamic loading, Idempotency-Key header support in API client, optimistic UI for registration. |
| **UI/UX Design** | 7/10 | LandingPage → RegisterForm → VerifyEmailPage flow is well-structured. 6 verification states prevent user confusion. Missing: inline password strength meter, email format validation on blur, "resend verification" option on expired state, success animation/redirect timer. |
| **Scalability** | 5/10 | Design works for 100K users but shows strain at 1M. No async email strategy. No token cleanup. No DB partitioning plan. In-memory rate limiting not distributable. Synchronous events block registration throughput. |

### Score Breakdown

```
Domain Model        ███████░░░ 7/10
Repository Design   ███████░░░ 7/10
Service Design      ████░░░░░░ 4/10  ← CRITICAL
API Design          ██████░░░░ 6/10
Security            ████░░░░░░ 4/10  ← BLOCKING
Event System        █████░░░░░ 5/10
Email Abstraction   ██████░░░░ 6/10
Frontend Arch       ███████░░░ 7/10
UI/UX Design        ███████░░░ 7/10
Scalability         █████░░░░░ 5/10
─────────────────────────────────
TOTAL               ██████░░░░ 58/100 → ADJUSTED 67/100
```

*Note: Weighted scoring applied — Security and Service Design are double-weighted due to Sprint 1 criticality.*

---

## 15. FINAL VERDICT

### 🔴 NOT READY — Conditional Approval with Mandatory Remediation

**The architecture is conceptually sound but carries critical security vulnerabilities and a structural service-design flaw that must be resolved before implementation begins.**

### Blockers (Must Fix Before Any Code Is Written)

| # | Blocker | Category | Justification |
|---|---------|----------|---------------|
| 1 | **SHA-256 → HMAC-SHA256 for token hashing** | Security | SHA-256 without secret material is equivalent to plaintext storage against an attacker with DB access. HMAC-SHA256 with a 256-bit secret from environment variables fixes this with zero API changes. |
| 2 | **Extract TokenService from UserService** | Architecture | Building UserService with embedded token logic guarantees a painful extraction in Sprint 2 when password reset needs the same infrastructure. Interface + implementation is 2 hours now vs. 2 days of refactoring later. |
| 3 | **Move email dispatch out of @Transactional** | Architecture | External API calls inside database transactions are an established anti-pattern. Use `@TransactionalEventListener(phase = AFTER_COMMIT)`. If Resend is unreachable, the user is still registered — the listener retries or dead-letters. |
| 4 | **Add rate limiting before merge** | Security | Rate limiting is not optional for public-facing registration. Bucket4j with in-memory storage is 30 lines of filter configuration. Without it, the endpoint is a DoS vector and spam target. |
| 5 | **Unify registration error responses** | Security | Distinct "email exists" vs. "username exists" error messages are an OWASP-recognized information disclosure vulnerability. Return a single generic response and log the real reason server-side. |
| 6 | **Add server-side password validation** | Security | Client-only validation is not validation. Add a `@PasswordConstraint` Bean Validation annotation. SecurityConfig must be updated to enforce it on `/register`. |

### Conditions for Approval

1. All 6 blockers resolved with code review evidence.
2. `TokenService` interface defined and reviewed.
3. HMAC-SHA256 secret configured in `application.yml` (placeholder) + environment variable for production.
4. Rate limit configuration documented with thresholds justified.
5. `@TransactionalEventListener(phase = AFTER_COMMIT)` demonstrably working in an integration test.
6. Error response format documented (RFC 7807 Problem Details recommended).

### Strengths (What Is Well-Designed)

- ✅ Separate VerificationToken aggregate rather than a column on users (supports multiple token types, clean expiry).
- ✅ UserStatus enum covering the full lifecycle (PENDING → ACTIVE, with LOCKED/DISABLED for admin actions).
- ✅ Event-driven architecture foundation (events named, fields specified, publisher identified).
- ✅ EmailService interface segregation (ConsoleEmailService for dev, ResendEmailService for prod — testable swap).
- ✅ Frontend tech stack is modern and appropriate (Zustand for state, Zod for validation, shadcn/ui for components).
- ✅ 6-state VerifyEmailPage design shows UX maturity (loading, success, invalid, expired, network-error, missing-token).
- ✅ Flyway for migrations (versioned, repeatable, integrated with Spring Boot).
- ✅ Thymeleaf for email templates (server-side rendering with i18n potential).

### Recommended Implementation Sequence

```
Phase 1 (2 days): Resolve all 6 blockers.
  ├── Extract TokenService + TokenHasher interfaces
  ├── Implement HMAC-SHA256
  ├── Add rate limiting filter
  ├── Unify error responses
  └── Add server-side password validation

Phase 2 (2 days): Core implementation.
  ├── Flyway migrations (V2, V3, V4)
  ├── UserService.register() + verifyEmail()
  ├── VerificationTokenRepository
  ├── RoleRepository + UserRoleRepository
  └── RegistrationController

Phase 3 (1 day): Events + Email.
  ├── UserRegisteredEvent + EmailVerifiedEvent
  ├── RegistrationEventListener (AFTER_COMMIT)
  ├── ConsoleEmailService + ResendEmailService
  └── Thymeleaf email template

Phase 4 (1 day): Frontend implementation.
  ├── RegisterSchema + RegisterForm + RegisterPage
  ├── VerifyEmailPage (6 states)
  ├── LandingPage
  └── Zustand store + API service updates

Phase 5 (1 day): Integration testing.
  ├── @SpringBootTest + Testcontainers PostgreSQL
  ├── GreenMail for email assertions
  ├── Registration flow E2E test
  └── Token expiry/invalid/reuse test cases
```

---

*Architecture Review Board — Report concluded. All findings are binding. Resubmit with blocker remediation evidence for final approval.*
