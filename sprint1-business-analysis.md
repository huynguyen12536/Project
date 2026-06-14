# LearnHub LMS — Sprint 1: Registration & Email Verification

**Business Analysis Document** | **Date:** 2026-06-13 | **Version:** 1.0

---

## 1. ACTORS

| # | Actor | Type | Description |
|---|-------|------|-------------|
| A1 | **Guest** | Human (Primary) | Unregistered visitor browsing LearnHub. Can view public course listings but cannot enroll, purchase, or save progress. Has no account or session identity. |
| A2 | **Registered Unverified User** | Human (Primary) | User who completed the registration form and received a verification email but has not yet confirmed their email. Account status: `PENDING_VERIFICATION`. Cannot log in or access protected resources. |
| A3 | **Email System** | System (Supporting) | External/internal service that composes and delivers transactional emails (verification link). Receives rendered email payload, handles SMTP delivery, bounce processing, and delivery status tracking. |
| A4 | **Verification Service** | System (Primary) | Internal subsystem that generates, hashes, stores, validates, and consumes verification tokens. Enforces token lifecycle rules (expiry, single-use, tamper detection) and transitions user status `PENDING_VERIFICATION` → `ACTIVE`. |

---

## 2. BUSINESS RULES

### Username Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-01 | Username must be unique, case-insensitive (`alice` = `ALICE` = `Alice`). | Prevents impersonation and lookup ambiguity. |
| BR-02 | Username must be 3—50 characters in length. | Minimum 3 prevents single-char spam; maximum 50 avoids URL bloat in `/u/<username>` profile links. |
| BR-03 | Allowed characters: lowercase letters `a-z`, digits `0-9`, dot `.`, underscore `_`. | Constrained charset simplifies routing, prevents XSS vectors, and avoids Unicode homoglyph attacks. |
| BR-04 | Username must not start or end with a dot or underscore. | Prevents ambiguous profile URLs (e.g., `.admin` or `admin.`). |
| BR-05 | Username must not contain consecutive dots (`..`) or consecutive underscores (`__`). | Prevents visual spoofing where `a..b` mimics `a.b` in certain fonts. |

### Email Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-06 | Email must be unique, case-insensitive (`User@Domain.com` = `user@domain.com`). | Prevents duplicate accounts; follows RFC 5321 §2.3.11 case-sensitivity guidance. |
| BR-07 | Email must conform to RFC 5322 format. | Ensures deliverability and prevents garbage input. |
| BR-08 | Email must not exceed 255 characters total length. | Enforces DB column constraint and RFC 5321 SMTP path limit. |
| BR-09 | Email domain must have a valid MX record (verified via DNS lookup at registration time). | Reduces bounce rate and blocks disposable/non-existent domains. |

### Password Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-10 | Password must be at least 12 characters. | Meets NIST SP 800-63B guidance for memorized secrets (recommends 12+). |
| BR-11 | Password must contain at least: one uppercase (`A-Z`), one lowercase (`a-z`), one digit (`0-9`), and one special character from printable ASCII special chars. | Enforces entropy; charset aligns with Argon2/bcrypt safe ranges. |
| BR-12 | Password must not contain the username, email local-part, or the string "password" (case-insensitive). | Prevents trivially guessable credentials. |
| BR-13 | Password is hashed with Argon2id (memory: 64MB, iterations: 3, parallelism: 4) before storage. Never stored in plaintext or logged. | Argon2id is the OWASP-recommended KDF resistant to GPU and side-channel attacks. |

### Full Name Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-14 | Full name is required and must be 1—200 characters. | Required for personalization (email salutation, certificate issuance); 200 chars accommodates long multi-part names. |
| BR-15 | Full name is trimmed of leading/trailing whitespace before validation and storage. | Prevents accidental whitespace-only names that pass client-side validation. |

### Verification Token Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-16 | Token is generated as a cryptographically random 32-byte value (CSPRNG), encoded as Base64URL. | 256 bits of entropy makes brute-force infeasible. |
| BR-17 | Only the SHA-256 hash of the token is stored in the database. The plaintext token is sent via email. | If the database is breached, attackers cannot forge verification links from hash data. |
| BR-18 | Token expires 24 hours after issuance (`expiresAt` = `createdAt` + 24h). | Balances user convenience with security; prevents indefinitely dangling tokens. |
| BR-19 | Token is single-use: consumed (deleted or marked `consumed`) upon successful verification. | Prevents replay attacks where the same link is clicked multiple times. |
| BR-20 | All existing unexpired tokens for a user are invalidated when a new token is generated. | Ensures only the most recent token is active; prevents token-stacking attacks. |

### User Status & Role Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-21 | New users are created with status `PENDING_VERIFICATION`. | Gates all authenticated access behind verified email ownership. |
| BR-22 | Status transitions to `ACTIVE` only upon successful token verification. No other path exists. | Explicit state machine; no implicit activation. |
| BR-23 | Default role at registration is `STUDENT`. | Least-privilege principle. Instructor and Admin roles require separate, audited elevation workflows. |

### Rate Limiting & Security Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-24 | Maximum 5 registration attempts per IP address per 60-second sliding window. | Mitigates mass account creation (Sybil attacks) and DoS on the email delivery system. |
| BR-25 | Maximum 3 failed verification attempts per token before the token is revoked and the account is flagged for review. | Defense-in-depth against brute-force guessing, even though tokens are 256-bit. |
| BR-26 | Registration endpoint returns rate-limiting headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`. | Enables the SPA client to show real-time feedback without polling. |

### Event Publishing Rules

| # | Rule | Rationale |
|---|------|-----------|
| BR-27 | A `USER_REGISTERED` event is published after successful registration (status `PENDING_VERIFICATION`). Payload: `{ userId, username, email, timestamp }`. | Enables downstream services (analytics, marketing, abuse detection) to react without tight coupling. |
| BR-28 | An `EMAIL_VERIFIED` event is published after successful verification (status `ACTIVE`). Payload: `{ userId, username, email, verifiedAt }`. | Enables downstream services (onboarding flow, welcome email, analytics, course recommendations). |

---

## 3. USE CASES

### UC-01: Successful Registration

| Field | Detail |
|-------|--------|
| **Actor** | Guest (A1) |
| **Preconditions** | Guest on registration page. Email and username do not exist. IP not rate-limited. |
| **Main Flow** | 1. Guest fills in username, email, password, full name. 2. Guest submits form. 3. System validates all fields. 4. System checks username/email uniqueness (case-insensitive). 5. System creates user with role=STUDENT, status=PENDING_VERIFICATION. 6. System generates verification token, stores SHA-256 hash with 24h expiry. 7. System publishes `USER_REGISTERED` event. 8. System queues verification email with plaintext token link. 9. System returns 201 Created with `{ userId, message }`. |
| **Postconditions** | User exists with PENDING_VERIFICATION status. Token hash stored with 24h expiry. Verification email queued. `USER_REGISTERED` event emitted. |

### UC-02: Duplicate Username

| Field | Detail |
|-------|--------|
| **Actor** | Guest (A1) |
| **Preconditions** | User with username `john_doe` already exists. |
| **Main Flow** | 1. Guest submits registration with username `John_Doe` (case-different). 2. System normalizes to lowercase: `john_doe`. 3. System finds existing record. 4. System returns 409 Conflict with error code `USERNAME_TAKEN`. |
| **Postconditions** | No user created. No event emitted. No email sent. |

### UC-03: Duplicate Email

| Field | Detail |
|-------|--------|
| **Actor** | Guest (A1) |
| **Preconditions** | User with email `alice@example.com` already exists. |
| **Main Flow** | 1. Guest submits registration with email `Alice@Example.com` (case-different). 2. System normalizes to lowercase: `alice@example.com`. 3. System finds existing record. 4. System returns 409 Conflict with error code `EMAIL_TAKEN`. |
| **Postconditions** | No user created. No event emitted. No email sent. |

### UC-04: Invalid Data (Multiple Validation Failures)

| Field | Detail |
|-------|--------|
| **Actor** | Guest (A1) |
| **Preconditions** | Guest submits malformed data. |
| **Main Flow** | 1. Guest submits form with username=`ab` (too short), email=`not-an-email`, password=`short`, fullName=`` (empty). 2. System validates all fields. 3. System collects all validation errors into a structured array. 4. System returns 422 Unprocessable Entity with `{ errors: [{ field, code, message }] }`. |
| **Postconditions** | No user created. No events emitted. No email sent. |

### UC-05: Expired Token

| Field | Detail |
|-------|--------|
| **Actor** | Registered Unverified User (A2) |
| **Preconditions** | User registered 25 hours ago. Token hash exists but `expiresAt` < now. |
| **Main Flow** | 1. User clicks verification link from old email. 2. System hashes token, queries DB for matching hash. 3. System finds token but `expiresAt` is in the past. 4. System returns 410 Gone with error code `TOKEN_EXPIRED`. 5. UI renders "Link expired — request a new one" with a resend button. |
| **Postconditions** | User remains PENDING_VERIFICATION. Expired token flagged for cleanup by background job. |

### UC-06: Already Used Token (Replay)

| Field | Detail |
|-------|--------|
| **Actor** | Registered Unverified User (A2) → now ACTIVE |
| **Preconditions** | User successfully verified their email 5 minutes ago. Token was consumed (deleted). |
| **Main Flow** | 1. User clicks the same verification link again (e.g., from browser history). 2. System hashes token, queries DB. 3. No matching hash found (token consumed). 4. System queries user record: status=ACTIVE. 5. System returns 200 OK with message "Email already verified" and redirects to login page. |
| **Postconditions** | No state change. User redirected to login. |

### UC-07: Already Verified — Resend Request

| Field | Detail |
|-------|--------|
| **Actor** | ACTIVE user (was A2) |
| **Preconditions** | User is already ACTIVE. They click "Resend verification email" (perhaps from a stale browser tab). |
| **Main Flow** | 1. User requests resend of verification email. 2. System checks user status: ACTIVE. 3. System returns 409 Conflict with error code `ALREADY_VERIFIED` and message "Your email is already verified. Please log in." |
| **Postconditions** | No token generated. No email sent. |

### UC-08: Invalid Token — Garbage or Forged

| Field | Detail |
|-------|--------|
| **Actor** | Guest or Malicious Actor |
| **Preconditions** | Token value does not correspond to any stored hash in the database. |
| **Main Flow** | 1. Actor navigates to `/verify?token=garbageValue123`. 2. System hashes the token and queries DB. 3. No matching hash found. 4. System increments failed verification counter for source IP. 5. System returns 404 Not Found with error code `TOKEN_INVALID`. |
| **Postconditions** | No user state change. Security event logged for anomaly detection. |

### UC-09: Rate Limited — Too Many Registrations from One IP

| Field | Detail |
|-------|--------|
| **Actor** | Guest (A1) — potentially malicious script |
| **Preconditions** | Same IP has made 5 registration POST requests in the last 60 seconds. |
| **Main Flow** | 1. Guest/script submits 6th registration within the sliding window. 2. System checks rate limit counter for the IP. 3. Counter exceeds allowed limit. 4. System returns 429 Too Many Requests with `Retry-After` header and error code `RATE_LIMITED`. |
| **Postconditions** | No user created. Request logged for abuse monitoring. |

### UC-10: Token Tampering

| Field | Detail |
|-------|--------|
| **Actor** | Malicious Guest |
| **Preconditions** | Attacker receives or intercepts a valid verification link and modifies one character of the token. |
| **Main Flow** | 1. Attacker changes token character and submits. 2. System hashes the tampered token. 3. SHA-256 avalanche effect produces a completely different hash — no DB match. 4. System returns 404 with error code `TOKEN_INVALID` and logs a `VERIFICATION_TAMPER_DETECTED` security event. |
| **Postconditions** | No user activated. Tamper event logged with IP and timestamp for security review. |

### UC-11: Concurrent Registration — Race Condition

| Field | Detail |
|-------|--------|
| **Actor** | Guest (A1) — two nearly-simultaneous requests from same client |
| **Preconditions** | No user exists with username `neo` or email `neo@matrix.com`. |
| **Main Flow** | 1. Request A and Request B both pass uniqueness checks (neither sees the other's record yet). 2. Both transactions attempt INSERT. 3. Database unique constraint catches the second insert. 4. Second transaction rolls back. 5. System returns 409 Conflict for the losing request. 6. First request succeeds normally (201 Created). |
| **Postconditions** | Exactly one user created. One success, one failure with `USERNAME_TAKEN` or `EMAIL_TAKEN`. |

### UC-12: Concurrent Verification — Double Click

| Field | Detail |
|-------|--------|
| **Actor** | Registered Unverified User (A2) |
| **Preconditions** | User has a valid, unexpired token. They double-click the verification link (two near-simultaneous GET requests). |
| **Main Flow** | 1. Request A and Request B both arrive, both with the same valid token hash. 2. Request A: token hash found, not expired, not consumed. Status updated to ACTIVE. Token consumed (deleted). `EMAIL_VERIFIED` event published. 3. Request B: token hash NOT found (consumed by A). System checks user record: status=ACTIVE. 4. Request B returns 200 "Email already verified". |
| **Postconditions** | Exactly one state transition (PENDING_VERIFICATION → ACTIVE). One `EMAIL_VERIFIED` event emitted. No duplicate events. |

---

## 4. USER STORIES

| # | Story |
|---|-------|
| US-01 | As a **Guest**, I want to create an account using my email, username, and password so that I can access LearnHub courses. |
| US-02 | As a **Guest**, I want real-time feedback on username availability so that I can choose a unique username without trial and error. |
| US-03 | As a **Guest**, I want clear password strength feedback so that I can create a password that meets the security requirements on the first attempt. |
| US-04 | As a **Guest**, I want all my validation errors shown at once after submission so that I can fix them in one pass rather than whack-a-mole. |
| US-05 | As a **Guest**, I want to be told if my email is already registered so that I can log in instead of creating a duplicate account. |
| US-06 | As a **Guest**, I want a registration success confirmation page so that I know my information was received and what to do next. |
| US-07 | As a **Registered Unverified User**, I want to receive a verification email with a secure link so that I can prove I own the email address. |
| US-08 | As a **Registered Unverified User**, I want to click the verification link and be automatically verified so that I can log in without manual steps. |
| US-09 | As a **Registered Unverified User**, I want to know if my verification link has expired so that I can request a new one without confusion. |
| US-10 | As a **Registered Unverified User**, I want to request a new verification email if the first one did not arrive so that I am not locked out of my account. |
| US-11 | As a **Registered Unverified User**, I want to be told if I click an already-used verification link so that I know my email is verified and I should log in. |
| US-12 | As the **System Administrator**, I want rate limiting on registration endpoints so that bots cannot flood the platform with fake accounts. |
| US-13 | As the **System Administrator**, I want security events logged for suspicious verification attempts (tampered tokens, brute-force) so that I can detect and respond to attacks. |
| US-14 | As a **Downstream Service** (Analytics/Marketing), I want to consume `USER_REGISTERED` and `EMAIL_VERIFIED` events so that I can track platform growth and trigger onboarding workflows. |

---

## 5. ACCEPTANCE CRITERIA (Gherkin)

### Scenario 1: Successful registration
```
GIVEN a Guest is on the registration page
AND the username "jane_doe" and email "jane@example.com" are not in use
WHEN the Guest submits the form with valid username, email, password, and full name
THEN the system creates a user with status PENDING_VERIFICATION and role STUDENT
AND returns HTTP 201 with { userId, message: "Verification email sent" }
AND publishes a USER_REGISTERED event
AND queues a verification email containing a valid token link
```

### Scenario 2: Duplicate username (case-insensitive)
```
GIVEN a user with username "john_doe" already exists
WHEN a Guest submits registration with username "John_Doe"
THEN the system returns HTTP 409 with error code "USERNAME_TAKEN"
AND no user is created
AND no email is sent
```

### Scenario 3: Duplicate email (case-insensitive)
```
GIVEN a user with email "alice@example.com" already exists
WHEN a Guest submits registration with email "Alice@Example.com"
THEN the system returns HTTP 409 with error code "EMAIL_TAKEN"
AND no user is created
AND no email is sent
```

### Scenario 4: Multiple validation failures
```
GIVEN a Guest is on the registration page
WHEN the Guest submits the form with username="ab", email="bad", password="123", and fullName=""
THEN the system returns HTTP 422 with errors for username (too short), email (invalid format), password (too short, no uppercase, no special), and fullName (required)
AND all four field-specific errors are returned in a single response array
```

### Scenario 5: Successful email verification
```
GIVEN a user "jane_doe" with status PENDING_VERIFICATION
AND a valid, unexpired verification token exists for this user
WHEN the user navigates to the verification link with the correct token
THEN the user status transitions to ACTIVE
AND the token is consumed (deleted)
AND the system publishes an EMAIL_VERIFIED event
AND the user is redirected to the login page with a success message
```

### Scenario 6: Verification with expired token
```
GIVEN a user "jane_doe" with status PENDING_VERIFICATION
AND a verification token that was created 25 hours ago (expired)
WHEN the user clicks the verification link
THEN the system returns HTTP 410 with error code "TOKEN_EXPIRED"
AND the user status remains PENDING_VERIFICATION
AND the UI displays "Link expired" with a "Request new link" button
```

### Scenario 7: Verification with already-used token
```
GIVEN a user "jane_doe" with status ACTIVE (already verified)
AND the verification token was consumed during the initial verification
WHEN the user clicks the same verification link again
THEN the system detects the user is already ACTIVE
AND returns HTTP 200 with message "Email already verified — please log in"
AND redirects to the login page
```

### Scenario 8: Verification with invalid/tampered token
```
GIVEN no stored token hash matches the provided token
WHEN any actor navigates to the verification link with an invalid token
THEN the system returns HTTP 404 with error code "TOKEN_INVALID"
AND no user status is changed
AND a security event is logged with the IP address and timestamp
```

### Scenario 9: Rate limiting — too many registrations
```
GIVEN the same IP address has submitted 5 registration requests in the last 60 seconds
WHEN a 6th registration is submitted from the same IP within the window
THEN the system returns HTTP 429 with error code "RATE_LIMITED"
AND includes a Retry-After header
AND no user is created
```

### Scenario 10: Concurrent registration race condition
```
GIVEN no user exists with username "neo" or email "neo@matrix.com"
WHEN two registration requests with identical username and email arrive nearly simultaneously
THEN exactly one request returns HTTP 201 (user created)
AND the other request returns HTTP 409 with "USERNAME_TAKEN" or "EMAIL_TAKEN"
AND at most one USER_REGISTERED event is published
```

### Scenario 11: Concurrent verification double-click
```
GIVEN a user with status PENDING_VERIFICATION and a valid token
WHEN two verification requests with the same token arrive nearly simultaneously
THEN exactly one request transitions the user to ACTIVE
AND exactly one EMAIL_VERIFIED event is published
AND the token is consumed exactly once
AND the second request returns HTTP 200 with "Email already verified"
```

### Scenario 12: Password contains username
```
GIVEN a Guest submits registration with username "johndoe"
WHEN the password field contains "MyJohndoe123!"
THEN the system returns HTTP 422 with error code "PASSWORD_CONTAINS_USERNAME"
AND the user is not created
```

### Scenario 13: Resend verification email — new token invalidates old
```
GIVEN a user with status PENDING_VERIFICATION and an existing unexpired token
WHEN the user requests a new verification email
THEN the old token is invalidated (deleted)
AND a new token is generated and stored
AND a new verification email is queued with the new token link
AND the old verification link will return TOKEN_INVALID if used
```

### Scenario 14: Email domain has no MX record
```
GIVEN a Guest submits registration with email "user@nonexistent-domain-xyz123.com"
AND the domain has no MX record in DNS
WHEN the system validates the email
THEN the system returns HTTP 422 with error code "EMAIL_DOMAIN_INVALID"
AND the user is not created
```

---

## 6. REGISTRATION JOURNEY (Narrative)

> **Persona:** Elena, a 27-year-old data analyst looking to learn machine learning.

### Step 1 — Landing
Elena arrives at **learnhub.com** via a Google search. The homepage features a prominent **"Start Learning Free"** and **"Sign Up"** button in the hero section and the top navigation bar. She clicks **"Sign Up"** and is taken to `/register`.

### Step 2 — The Registration Form
The registration page displays a clean, single-column form with four fields:

| Field | Input | Client-Side Behavior |
|-------|-------|---------------------|
| **Full Name** | Text input | No validation beyond required indicator. |
| **Username** | Text input | As she types, a 400ms debounced API call checks availability (`GET /api/check-username?q=jane_doe`). A green checkmark appears when available; a red warning when taken. The input auto-lowercases characters. Disallowed characters are silently rejected. |
| **Email** | Text input | A 400ms debounced API call checks availability (`GET /api/check-email?q=jane@example.com`). Same green/red indicator. |
| **Password** | Password input with show/hide toggle | A password strength meter fills in real-time: red (weak: < 3 criteria met), yellow (medium: 3 criteria), green (strong: all 4 criteria). Below the meter, a checklist of unmet criteria shows dynamically ("Add a special character", "At least 12 characters"). |

Elena enters:
- Full Name: `Elena Rodriguez`
- Username: `elena_rodriguez` ✅ Available
- Email: `elena.rodriguez@gmail.com` ✅ Available
- Password: `DataScience!2026` — the meter turns green, all checklist items resolve.

### Step 3 — Submit
Elena clicks the **"Create Account"** button. The button shows a spinner and becomes disabled to prevent double-submission.

The frontend sends a single `POST /api/auth/register` request:
```json
{
  "fullName": "Elena Rodriguez",
  "username": "elena_rodriguez",
  "email": "elena.rodriguez@gmail.com",
  "password": "DataScience!2026"
}
```

### Step 4 — Server-Side Processing (transparent to Elena)

1. **Rate limit check:** IP `203.0.113.45` has used 2/5 requests in this window → pass.
2. **Validation:** All fields pass server-side regex and constraint checks.
3. **DNS MX lookup:** `gmail.com` has valid MX records → pass.
4. **Uniqueness with DB unique constraint:** No existing user → pass.
5. **User creation:** INSERT into `users` table — `id=uuid`, `username=elena_rodriguez`, `email=elena.rodriguez@gmail.com`, `password_hash=$argon2id$...`, `full_name=Elena Rodriguez`, `role=STUDENT`, `status=PENDING_VERIFICATION`, `created_at=2026-06-13T15:28:00Z`.
6. **Token generation:** 32 random bytes via CSPRNG → Base64URL → `dGhpcyBpcyBhIHRva2Vu...`. SHA-256 hash stored in `verification_tokens` table with `userId`, `expiresAt=2026-06-14T15:28:00Z`.
7. **Event publishing:** `USER_REGISTERED` event published to message broker.
8. **Email queuing:** Verification email job added to email queue.

### Step 5 — Success Response
API returns `201 Created`:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Account created! Check your email to verify your address."
}
```

### Step 6 — Success Page
The frontend navigates to `/register/success`. The page displays:
- A large envelope icon with a checkmark.
- "We sent a verification email to **elena.rodriguez@gmail.com**"
- "Check your inbox and click the link to activate your account. The link expires in 24 hours."
- A "Resend email" link (disabled with a 60-second countdown to prevent spam).
- A "Go to login" link.

### Step 7 — Email Received
Within seconds, Elena's Gmail inbox receives an email from `no-reply@learnhub.com`:

> **Subject:** Verify your email for LearnHub
>
> Hi Elena,
>
> Welcome to LearnHub! Click the button below to verify your email address and start learning.
>
> **[ Verify My Email ]**
>
> Or copy this link: `https://learnhub.com/verify?token=dGhpcyBpcyBhIHRva2Vu...`
>
> This link expires in 24 hours. If you didn't create this account, you can safely ignore this email.
>
> — The LearnHub Team

---

## 7. VERIFICATION JOURNEY (Narrative)

> **Persona:** Elena clicks the verification link from her Gmail inbox on her phone.

### Path A — Successful Verification

1. **Email Click:** Elena taps the **"Verify My Email"** button in the Gmail app. Her mobile browser opens `https://learnhub.com/verify?token=dGhpcyBpcyBhIHRva2Vu...`.

2. **Loading State:** The `/verify` page renders immediately with a skeleton placeholder: a centered card with a pulsing spinner and the text "Verifying your email..." The frontend extracts the `token` query parameter and calls `POST /api/auth/verify` with `{ token: "dGhpcyBpcyBhIHRva2Vu..." }`.

3. **Server Processing (200–400ms):**
   - System hashes the received token: `SHA-256("dGhpcyBpcyBhIHRva2Vu...")` → `a1b2c3...`.
   - Database query: `SELECT * FROM verification_tokens WHERE token_hash = 'a1b2c3...'`.
   - Match found. Check: `expiresAt` (2026-06-14T15:28:00Z) > now → valid.
   - Check: `consumed` = false → valid.
   - Begin atomic transaction:
     - `UPDATE users SET status = 'ACTIVE', verified_at = NOW() WHERE id = '550e8400-...' AND status = 'PENDING_VERIFICATION'`.
     - `DELETE FROM verification_tokens WHERE token_hash = 'a1b2c3...'`.
     - Publish `EMAIL_VERIFIED` event.
   - Commit transaction.

4. **Success State:** The frontend receives `200 OK` with `{ message: "Email verified successfully!" }`. The skeleton screen transitions to a success card:
   - Green checkmark icon.
   - "Email verified — you're all set!"
   - "You can now log in and start learning."
   - A prominent **"Go to Login"** button.
   - After 3 seconds, an automatic redirect to `/login`.

### Path B — Expired Token

1. **Email Click:** Same flow, but Elena clicked a 30-hour-old email.

2. **Server Response:** `410 Gone` with `{ error: "TOKEN_EXPIRED", message: "This verification link has expired. Request a new one." }`.

3. **Failure State:** The skeleton screen transitions to an error card:
   - Orange clock icon.
   - "Link expired — this verification link is no longer valid."
   - "Don't worry, we can send you a new one."
   - A **"Resend Verification Email"** button that triggers `POST /api/auth/resend-verification`.
   - Clicking the button sends a new token, invalidating the old one, and displays a confirmation: "New verification email sent — check your inbox."

### Path C — Already Verified

1. **Email Click:** Elena clicks a verification link after she's already verified (e.g., from a forwarded or saved email).

2. **Server Response:** `200 OK` with `{ message: "Email already verified — please log in." }`.

3. **Success + Redirect State:** The skeleton transitions to:
   - Blue info icon.
   - "Your email is already verified."
   - A **"Go to Login"** button with an automatic 3-second redirect.

### Path D — Invalid/Tampered Token

1. **Visit:** Someone navigates to `/verify?token=invalidOrTamperedValue`.

2. **Server Response:** `404 Not Found` with `{ error: "TOKEN_INVALID", message: "Invalid verification link." }`.

3. **Failure State:**
   - Red warning icon.
   - "Invalid verification link — this link is not recognized."
   - "If you copied the link from an email, make sure you copied the entire URL."
   - A link back to the registration page: "Create a new account".

### Path E — Network Error / Token Verification Service Down

1. **Visit:** Elena clicks the link but the backend is unreachable.

2. **Frontend Behavior:** The fetch call times out after 10 seconds. The skeleton transitions to:
   - Gray connection error icon.
   - "Something went wrong — we couldn't verify your email right now."
   - A **"Try Again"** button that re-sends the verification request.
   - "If this problem persists, please try again later or contact support."

---

## Appendix A: Error Code Reference

| HTTP Status | Error Code | Context |
|-------------|-----------|---------|
| 201 | — | Registration successful |
| 200 | — | Verification successful or already verified |
| 409 | `USERNAME_TAKEN` | Duplicate username |
| 409 | `EMAIL_TAKEN` | Duplicate email |
| 409 | `ALREADY_VERIFIED` | Resend requested for already-active user |
| 410 | `TOKEN_EXPIRED` | Verification token past 24h |
| 422 | `VALIDATION_ERROR` | One or more fields failed validation |
| 422 | `PASSWORD_CONTAINS_USERNAME` | Password contains username substring |
| 422 | `EMAIL_DOMAIN_INVALID` | Email domain has no MX record |
| 429 | `RATE_LIMITED` | Too many registration attempts |

## Appendix B: User State Machine

```
    [Guest]
       |
       | POST /api/auth/register (success)
       v
 +-------------------+
 | PENDING_VERIFICATION |
 +-------------------+
       |                    |
       | (verify success)   | (24h expiry + no action)
       v                    v
   [ACTIVE]          [PENDING_VERIFICATION]
                      (token expired — user can request new token
                       via POST /api/auth/resend-verification)
```

## Appendix C: Event Schema

**`USER_REGISTERED`**
```json
{
  "eventId": "evt_abc123",
  "eventType": "USER_REGISTERED",
  "timestamp": "2026-06-13T15:28:00Z",
  "payload": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "elena_rodriguez",
    "email": "elena.rodriguez@gmail.com"
  }
}
```

**`EMAIL_VERIFIED`**
```json
{
  "eventId": "evt_def456",
  "eventType": "EMAIL_VERIFIED",
  "timestamp": "2026-06-13T15:29:30Z",
  "payload": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "elena_rodriguez",
    "email": "elena.rodriguez@gmail.com",
    "verifiedAt": "2026-06-13T15:29:30Z"
  }
}
```

---

*End of Sprint 1 Business Analysis — Registration & Email Verification*