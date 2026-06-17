# EPIC 1: API DOCUMENTATION
## User Profile Management, Email Verification, Password Reset

---

## 1. USER PROFILE ENDPOINTS

### GET /api/v1/user/profile
Retrieve the current authenticated user's profile information.

**Authentication:** Required (JWT token, LEARNER role)

**Request:**
```bash
GET /api/v1/user/profile HTTP/1.1
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "id": "8fdf13b6-28f8-4e31-a6f5-0f4a47be05ad",
  "email": "testuser@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  "bio": "Learning backend development with LearnHub",
  "profilePictureUrl": null,
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "githubProfileUrl": "https://github.com/johndoe",
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-17T09:30:00",
  "lastLogin": "2026-06-17T09:45:00",
  "createdAt": "2026-06-15T10:00:00",
  "updatedAt": "2026-06-17T09:30:00",
  "role": "LEARNER"
}
```

**Error Responses:**

401 Unauthorized - Invalid or missing token
```json
{
  "timestamp": "2026-06-17T09:45:00",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired token"
}
```

404 Not Found - User not found
```json
{
  "timestamp": "2026-06-17T09:45:00",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "User not found"
}
```

---

### PUT /api/v1/user/profile
Update the current authenticated user's profile information.

**Authentication:** Required (JWT token, LEARNER role)

**Request:**
```bash
PUT /api/v1/user/profile HTTP/1.1
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "bio": "Backend developer | Java | Spring Boot",
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "githubProfileUrl": "https://github.com/johndoe"
}
```

**Request Body Validation:**

| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| firstName | String | 1-100 characters | ✅ |
| lastName | String | 1-100 characters | ✅ |
| bio | String | Max 1000 characters | ❌ |
| phone | String | 10+ digits, optional +prefix | ❌ |
| location | String | Max 255 characters | ❌ |
| githubProfileUrl | String | Valid GitHub URL format | ❌ |

**Response (200 OK):**
```json
{
  "id": "8fdf13b6-28f8-4e31-a6f5-0f4a47be05ad",
  "email": "testuser@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  "bio": "Backend developer | Java | Spring Boot",
  "profilePictureUrl": null,
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "githubProfileUrl": "https://github.com/johndoe",
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-17T09:30:00",
  "lastLogin": "2026-06-17T09:45:00",
  "createdAt": "2026-06-15T10:00:00",
  "updatedAt": "2026-06-17T10:00:00",
  "role": "LEARNER"
}
```

**Error Responses:**

400 Bad Request - Validation error
```json
{
  "timestamp": "2026-06-17T09:45:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "errors": {
      "firstName": "First name is required",
      "githubProfileUrl": "Must be a valid GitHub profile URL or empty"
    }
  }
}
```

---

## 2. EMAIL VERIFICATION ENDPOINTS

### POST /api/v1/email/verify
Verify user's email address using the verification token from email link.

**Authentication:** Not required (public endpoint)

**Request:**
```bash
POST /api/v1/email/verify HTTP/1.1
Content-Type: application/json

{
  "token": "eyJraWQiOiIxIiwiYWxnIjoiUlMyNTYifQ.ey..."
}
```

**Response (200 OK):**
```json
{
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-17T10:00:00",
  "message": "Email verified successfully"
}
```

**Error Responses:**

400 Bad Request - Invalid or expired token
```json
{
  "timestamp": "2026-06-17T10:00:00",
  "status": 400,
  "error": "INVALID_TOKEN",
  "message": "Invalid or expired verification token"
}
```

400 Bad Request - Token already used
```json
{
  "timestamp": "2026-06-17T10:00:00",
  "status": 400,
  "error": "INVALID_TOKEN",
  "message": "Verification token has expired or been used"
}
```

---

### POST /api/v1/email/resend-verification
Resend verification email to the current user.

**Authentication:** Required (JWT token, LEARNER role)

**Request:**
```bash
POST /api/v1/email/resend-verification HTTP/1.1
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "message": "Verification email has been resent to your email address"
}
```

**Error Responses:**

400 Bad Request - Email already verified
```json
{
  "timestamp": "2026-06-17T10:00:00",
  "status": 400,
  "error": "EMAIL_ALREADY_VERIFIED",
  "message": "Email is already verified"
}
```

401 Unauthorized - Invalid token
```json
{
  "timestamp": "2026-06-17T10:00:00",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired token"
}
```

---

## 3. PASSWORD RESET ENDPOINTS

### POST /api/v1/password/reset-request
Initiate password reset flow by requesting a reset link via email.

**Authentication:** Not required (public endpoint)

**Rate Limiting:** 3 attempts per 15 minutes (IP-based)

**Request:**
```bash
POST /api/v1/password/reset-request HTTP/1.1
Content-Type: application/json

{
  "email": "testuser@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset link has been sent to your email"
}
```

**Note:** 
- Response is same regardless of whether email exists (security: user enumeration prevention)
- Email is sent only if user account exists
- Reset link is valid for 24 hours
- Only 1 active reset token per user (older tokens invalidated)

**Error Responses:**

400 Bad Request - Invalid email format
```json
{
  "timestamp": "2026-06-17T10:05:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "errors": {
      "email": "Email must be valid"
    }
  }
}
```

429 Too Many Requests - Rate limited
```json
{
  "timestamp": "2026-06-17T10:05:00",
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many password reset requests. Try again later."
}
```

---

### POST /api/v1/password/reset-confirm
Complete password reset with new password.

**Authentication:** Not required (public endpoint)

**Request:**
```bash
POST /api/v1/password/reset-confirm HTTP/1.1
Content-Type: application/json

{
  "token": "eyJraWQiOiIxIiwiYWxnIjoiUlMyNTYifQ.ey...",
  "newPassword": "SecurePass123!@#",
  "confirmPassword": "SecurePass123!@#"
}
```

**Password Requirements:**
- Length: 8-128 characters
- Must include: uppercase letter (A-Z)
- Must include: lowercase letter (a-z)
- Must include: number (0-9)
- Must include: special character (@$!%*?&)

**Valid Examples:**
- `MyPassword123!`
- `Secure@Pass456`
- `Test#Pass2026`

**Invalid Examples:**
- `password123` (no uppercase, no special char)
- `PASSWORD123!` (no lowercase)
- `PassWord!` (no number)
- `Pass1!` (too short)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password has been reset successfully",
  "expiresAt": "2026-06-17T10:10:00"
}
```

**Error Responses:**

400 Bad Request - Mismatched passwords
```json
{
  "timestamp": "2026-06-17T10:10:00",
  "status": 400,
  "error": "PASSWORD_RESET_ERROR",
  "message": "Passwords do not match"
}
```

400 Bad Request - Invalid password format
```json
{
  "timestamp": "2026-06-17T10:10:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "errors": {
      "newPassword": "Password must contain uppercase, lowercase, number, and special character"
    }
  }
}
```

400 Bad Request - Invalid or expired token
```json
{
  "timestamp": "2026-06-17T10:10:00",
  "status": 400,
  "error": "INVALID_TOKEN",
  "message": "Invalid or expired reset token"
}
```

---

### GET /api/v1/password/reset-token-valid/{token}
Validate if a password reset token is still valid (useful for frontend UX).

**Authentication:** Not required (public endpoint)

**Request:**
```bash
GET /api/v1/password/reset-token-valid/eyJraWQiOiIxIiwiYWxnIjoiUlMyNTYifQ.ey... HTTP/1.1
```

**Response (200 OK - Valid Token):**
```json
{
  "valid": true
}
```

**Response (200 OK - Invalid Token):**
```json
{
  "valid": false
}
```

---

## 4. EXPECTED EMAIL CONTENT

### Verification Email

**Subject:** Verify Your LearnHub Email Address

**Body:**
```
Hi John,

Welcome to LearnHub! Please verify your email by clicking the link below:

https://your-frontend.com/verify-email?token=eyJraWQiOiIxIiwiYWxnIjoiUlMyNTYifQ.ey...

This link expires in 24 hours.

If you didn't create this account, you can safely ignore this email.

Best regards,
LearnHub Team
```

---

### Password Reset Email

**Subject:** Reset Your LearnHub Password

**Body:**
```
Hi John,

We received a request to reset your password. Click the link below to set a new password:

https://your-frontend.com/reset-password?token=eyJraWQiOiIxIiwiYWxnIjoiUlMyNTYifQ.ey...

This link expires in 24 hours.

If you didn't request this, you can safely ignore this email.

Best regards,
LearnHub Team
```

---

## 5. COMMON WORKFLOWS

### Complete Registration & Email Verification Workflow

**Step 1: Register User**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"SecurePass123!"}'
```

**Response:**
```json
{
  "id": "8fdf13b6-28f8-4e31-a6f5-0f4a47be05ad",
  "email": "testuser@example.com"
}
```

**Step 2: Check Email** (Simulated in testing)
- Receive verification email with token
- Extract token from email

**Step 3: Verify Email**
```bash
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{"token":"<TOKEN_FROM_EMAIL>"}'
```

**Response:**
```json
{
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-17T10:00:00",
  "message": "Email verified successfully"
}
```

**Step 4: Update Profile** (Now can access protected endpoints)
```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "bio": "Learning backend development",
    "phone": "+1234567890",
    "location": "San Francisco",
    "githubProfileUrl": "https://github.com/johndoe"
  }'
```

---

### Complete Password Reset Workflow

**Step 1: Request Password Reset**
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com"}'
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset link has been sent to your email"
}
```

**Step 2: Check Email** (Simulated in testing)
- Receive password reset email with token
- Extract token from email

**Step 3: Validate Token** (Optional, for UX)
```bash
curl http://localhost:8080/api/v1/password/reset-token-valid/<TOKEN_FROM_EMAIL>
```

**Response:**
```json
{
  "valid": true
}
```

**Step 4: Reset Password**
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<TOKEN_FROM_EMAIL>",
    "newPassword": "NewSecurePass456!",
    "confirmPassword": "NewSecurePass456!"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Password has been reset successfully",
  "expiresAt": "2026-06-17T10:10:00"
}
```

**Step 5: Login with New Password**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"NewSecurePass456!"}'
```

---

## 6. TESTING ENVIRONMENT SETUP

### Docker Environment Variables
```bash
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your-email@gmail.com>
MAIL_PASSWORD=<app-password>
MAIL_FROM=noreply@learnhub.local

# Frontend URL (for email links)
FRONTEND_URL=http://localhost:3000

# Token Configuration
TOKEN_VERIFICATION_EXPIRY_HOURS=24
TOKEN_RESET_EXPIRY_HOURS=24

# Security Configuration
MAX_FAILED_LOGIN_ATTEMPTS=5
LOCKOUT_DURATION_MINUTES=30
```

### Running Tests Locally
```bash
# Start Docker stack
docker-compose up -d

# Wait for app to start
sleep 15

# Run tests
cd learnhub-backend
./mvnw test

# Stop Docker stack when done
docker-compose down
```

---

## 7. CURL EXAMPLES FOR MANUAL TESTING

### Register and Verify Email (Simplified)
```bash
# Step 1: Register
USER_ID=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePass123!"}' \
  | jq -r '.id')

echo "User registered: $USER_ID"

# Step 2: In real scenario, user gets email with token
# For testing, extract token from database or logs

# Step 3: Verify email (use token from email)
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{"token":"<TOKEN>"}'

# Step 4: Login to get JWT
JWT=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePass123!"}' \
  | jq -r '.accessToken')

echo "JWT Token: $JWT"

# Step 5: Update profile
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "bio": "Test bio",
    "phone": "+1234567890",
    "location": "Test Location",
    "githubProfileUrl": "https://github.com/testuser"
  }'
```

---

## REFERENCE

**API Base URL:** `http://localhost:8080`

**Default Ports:**
- Application: 8080
- PostgreSQL: 5432
- Redis: 6379

**Token Validity:**
- Access Token: 15 minutes
- Refresh Token: 30 days
- Verification Token: 24 hours
- Reset Token: 24 hours

**Security:**
- All tokens are hashed (SHA-256) before storage
- Passwords are bcrypt hashed
- All endpoints validate input
- CORS enabled
- CSRF disabled (stateless JWT auth)

---

**Document Generated:** 2026-06-17  
**Last Updated:** 2026-06-17  
**Version:** 1.0
