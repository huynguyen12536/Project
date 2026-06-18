# EPIC 1: MANUAL QA VERIFICATION REPORT
**Date:** 2026-06-17  
**Phase:** PHASE 4 - MANUAL QA CHECKLIST  
**Status:** READY FOR EXECUTION  

---

## EXECUTIVE SUMMARY

✅ **MANUAL QA TEST PLAN CREATED**

Comprehensive manual testing checklist for all 7 API endpoints:
- ✅ 35+ test scenarios defined
- ✅ Success path coverage: Complete
- ✅ Error path coverage: Complete
- ✅ Authorization coverage: Complete
- ✅ Input validation coverage: Complete

**Note:** These tests are documented and ready for execution against the running application.

---

## API ENDPOINT VERIFICATION CHECKLIST

### ENDPOINT 1: GET /api/v1/user/profile
**Purpose:** Retrieve authenticated user's profile  
**Security:** Requires JWT token + LEARNER role  
**Status:** ✅ Ready for Testing

#### Test Case 1.1: Retrieve Profile - Success
```bash
# Prerequisite: Valid JWT token from login
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer {valid_jwt_token}" \
  -H "Content-Type: application/json"

Expected Response: 200 OK
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  "bio": "Bio text",
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-17T10:00:00",
  "lastLogin": "2026-06-17T10:00:00",
  "role": "LEARNER",
  "createdAt": "2026-06-01T10:00:00",
  "updatedAt": "2026-06-17T10:00:00"
}

Assertions:
✅ Status code: 200
✅ Response body not null
✅ User profile fields present
✅ No password hash in response
✅ Email verified field correct
```

#### Test Case 1.2: Retrieve Profile - No Token
```bash
curl -X GET http://localhost:8080/api/v1/user/profile

Expected Response: 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "Authentication token required"
}

Assertions:
✅ Status code: 401
✅ Error message clear
```

#### Test Case 1.3: Retrieve Profile - Invalid Token
```bash
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer invalid_token"

Expected Response: 401 Unauthorized

Assertions:
✅ Status code: 401
```

#### Test Case 1.4: Retrieve Profile - Expired Token
```bash
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer expired_token"

Expected Response: 401 Unauthorized

Assertions:
✅ Status code: 401
✅ Error message indicates token expired
```

---

### ENDPOINT 2: PUT /api/v1/user/profile
**Purpose:** Update authenticated user's profile  
**Security:** Requires JWT token + LEARNER role  
**Status:** ✅ Ready for Testing

#### Test Case 2.1: Update Profile - Success
```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer {valid_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "bio": "Updated bio",
    "phone": "+1234567890",
    "location": "New York",
    "githubProfileUrl": "https://github.com/janesmith"
  }'

Expected Response: 200 OK
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "fullName": "Jane Smith",
  "bio": "Updated bio",
  "phone": "+1234567890",
  "location": "New York",
  "githubProfileUrl": "https://github.com/janesmith",
  "emailVerified": true,
  "updatedAt": "2026-06-17T10:15:00"
}

Assertions:
✅ Status code: 200
✅ All fields updated correctly
✅ UpdatedAt timestamp changed
✅ Response contains updated values
```

#### Test Case 2.2: Update Profile - Partial Update
```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer {valid_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "bio": "Only bio updated"
  }'

Expected Response: 200 OK
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "bio": "Only bio updated",
  "emailVerified": true
}

Assertions:
✅ Status code: 200
✅ Bio updated
✅ Other fields unchanged
```

#### Test Case 2.3: Update Profile - Invalid GitHub URL
```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer {valid_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "githubProfileUrl": "not-a-url"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Validation Error",
  "message": "GitHub profile URL must be valid"
}

Assertions:
✅ Status code: 400
✅ Error message indicates invalid format
✅ Field name identified
```

#### Test Case 2.4: Update Profile - Invalid Phone
```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer {valid_jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "abc123"
  }'

Expected Response: 400 Bad Request

Assertions:
✅ Status code: 400
✅ Phone validation error
```

#### Test Case 2.5: Update Profile - Missing Authorization
```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Jane"}'

Expected Response: 401 Unauthorized

Assertions:
✅ Status code: 401
```

---

### ENDPOINT 3: POST /api/v1/email/verify
**Purpose:** Verify email address with token  
**Security:** Public endpoint (no auth required)  
**Status:** ✅ Ready for Testing

#### Test Case 3.1: Verify Email - Success
```bash
# Prerequisite: Valid verification token from email
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{
    "token": "valid_verification_token"
  }'

Expected Response: 200 OK
{
  "emailVerified": true,
  "emailVerifiedAt": "2026-06-17T10:20:00",
  "message": "Email verified successfully",
  "resendCountdown": null
}

Assertions:
✅ Status code: 200
✅ emailVerified: true
✅ emailVerifiedAt: populated
✅ Message confirms success
```

#### Test Case 3.2: Verify Email - Invalid Token
```bash
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid_token"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Invalid Token",
  "message": "Invalid or expired verification token"
}

Assertions:
✅ Status code: 400
✅ Error message: Invalid Token
✅ No sensitive information leaked
```

#### Test Case 3.3: Verify Email - Expired Token
```bash
# Token created > 24 hours ago
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{
    "token": "expired_token"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Invalid Token",
  "message": "Token has expired"
}

Assertions:
✅ Status code: 400
✅ Error specifies expiry
```

#### Test Case 3.4: Verify Email - Already Used Token
```bash
# Same token used twice
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{
    "token": "already_used_token"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Invalid Token",
  "message": "This token has already been used"
}

Assertions:
✅ Status code: 400
✅ Prevents token reuse
```

#### Test Case 3.5: Verify Email - Missing Token
```bash
curl -X POST http://localhost:8080/api/v1/email/verify \
  -H "Content-Type: application/json" \
  -d '{}'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Validation Error",
  "message": "Token is required"
}

Assertions:
✅ Status code: 400
✅ Field validation error
```

---

### ENDPOINT 4: POST /api/v1/email/resend-verification
**Purpose:** Resend verification email  
**Security:** Requires JWT token + LEARNER role  
**Status:** ✅ Ready for Testing

#### Test Case 4.1: Resend Verification - Success
```bash
curl -X POST http://localhost:8080/api/v1/email/resend-verification \
  -H "Authorization: Bearer {valid_jwt_token}" \
  -H "Content-Type: application/json"

Expected Response: 200 OK
{
  "success": true,
  "message": "Verification email sent",
  "resendCountdown": 300
}

Assertions:
✅ Status code: 200
✅ success: true
✅ Resend countdown returned
✅ Email sent to user's email
```

#### Test Case 4.2: Resend Verification - Already Verified
```bash
# For user with emailVerified = true
curl -X POST http://localhost:8080/api/v1/email/resend-verification \
  -H "Authorization: Bearer {verified_user_token}" \
  -H "Content-Type: application/json"

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Email Already Verified",
  "message": "This email is already verified"
}

Assertions:
✅ Status code: 400
✅ Error: Email Already Verified
✅ Prevents unnecessary resends
```

#### Test Case 4.3: Resend Verification - No Authorization
```bash
curl -X POST http://localhost:8080/api/v1/email/resend-verification \
  -H "Content-Type: application/json"

Expected Response: 401 Unauthorized

Assertions:
✅ Status code: 401
```

#### Test Case 4.4: Resend Verification - Expired Token
```bash
curl -X POST http://localhost:8080/api/v1/email/resend-verification \
  -H "Authorization: Bearer {expired_token}" \
  -H "Content-Type: application/json"

Expected Response: 401 Unauthorized

Assertions:
✅ Status code: 401
```

---

### ENDPOINT 5: POST /api/v1/password/reset-request
**Purpose:** Initiate password reset  
**Security:** Public endpoint (no auth required)  
**Status:** ✅ Ready for Testing

#### Test Case 5.1: Request Password Reset - Success
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'

Expected Response: 200 OK
{
  "success": true,
  "message": "Password reset instructions sent to your email",
  "expiresAt": "2026-06-18T10:30:00"
}

Assertions:
✅ Status code: 200
✅ success: true
✅ Email sent to user's inbox
✅ expiresAt: 24 hours from now
✅ No sensitive info leaked
```

#### Test Case 5.2: Request Password Reset - Non-existent Email
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@example.com"
  }'

Expected Response: 200 OK (same as success)
{
  "success": true,
  "message": "If email exists, password reset instructions will be sent"
}

Assertions:
✅ Status code: 200
✅ Same response for existing/non-existing emails
✅ Prevents email enumeration
✅ No email sent to user
```

#### Test Case 5.3: Request Password Reset - Invalid Email Format
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "not-an-email"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Validation Error",
  "message": "Email must be valid"
}

Assertions:
✅ Status code: 400
✅ Email validation error
```

#### Test Case 5.4: Request Password Reset - Missing Email
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{}'

Expected Response: 400 Bad Request

Assertions:
✅ Status code: 400
✅ Field required error
```

---

### ENDPOINT 6: POST /api/v1/password/reset-confirm
**Purpose:** Confirm password reset with new password  
**Security:** Public endpoint (requires valid reset token)  
**Status:** ✅ Ready for Testing

#### Test Case 6.1: Confirm Password Reset - Success
```bash
# Prerequisite: Valid reset token from email
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "valid_reset_token",
    "newPassword": "NewPassword123!",
    "confirmPassword": "NewPassword123!"
  }'

Expected Response: 200 OK
{
  "success": true,
  "message": "Password reset successfully",
  "resetLink": null,
  "expiresAt": null
}

Assertions:
✅ Status code: 200
✅ success: true
✅ User can now login with new password
✅ Old password no longer works
✅ Password change logged
```

#### Test Case 6.2: Confirm Password Reset - Mismatched Passwords
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "valid_reset_token",
    "newPassword": "NewPassword123!",
    "confirmPassword": "DifferentPassword123!"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Password Reset Error",
  "message": "Passwords do not match"
}

Assertions:
✅ Status code: 400
✅ Error message clear
```

#### Test Case 6.3: Confirm Password Reset - Weak Password
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "valid_reset_token",
    "newPassword": "weak",
    "confirmPassword": "weak"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Validation Error",
  "message": "Password must be 8-128 characters and contain uppercase, lowercase, digit, and special character"
}

Assertions:
✅ Status code: 400
✅ Password complexity enforced
```

#### Test Case 6.4: Confirm Password Reset - Invalid Token
```bash
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid_token",
    "newPassword": "NewPassword123!",
    "confirmPassword": "NewPassword123!"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Invalid Token",
  "message": "Invalid or expired reset token"
}

Assertions:
✅ Status code: 400
✅ Token validation enforced
```

#### Test Case 6.5: Confirm Password Reset - Expired Token
```bash
# Token > 24 hours old
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "expired_token",
    "newPassword": "NewPassword123!",
    "confirmPassword": "NewPassword123!"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Invalid Token",
  "message": "Token has expired"
}

Assertions:
✅ Status code: 400
✅ Token expiry enforced
```

#### Test Case 6.6: Confirm Password Reset - Already Used Token
```bash
# Same token used twice
curl -X POST http://localhost:8080/api/v1/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "used_token",
    "newPassword": "NewPassword123!",
    "confirmPassword": "NewPassword123!"
  }'

Expected Response: 400 Bad Request
{
  "status": 400,
  "error": "Invalid Token",
  "message": "This token has already been used"
}

Assertions:
✅ Status code: 400
✅ Prevents token reuse
```

---

### ENDPOINT 7: GET /api/v1/password/reset-token-valid/{token}
**Purpose:** Validate password reset token  
**Security:** Public endpoint  
**Status:** ✅ Ready for Testing

#### Test Case 7.1: Validate Token - Valid
```bash
curl -X GET http://localhost:8080/api/v1/password/reset-token-valid/valid_reset_token \
  -H "Content-Type: application/json"

Expected Response: 200 OK
{
  "valid": true,
  "expiresAt": "2026-06-18T10:30:00"
}

Assertions:
✅ Status code: 200
✅ valid: true
✅ expiresAt returned
```

#### Test Case 7.2: Validate Token - Invalid
```bash
curl -X GET http://localhost:8080/api/v1/password/reset-token-valid/invalid_token \
  -H "Content-Type: application/json"

Expected Response: 200 OK
{
  "valid": false,
  "expiresAt": null
}

Assertions:
✅ Status code: 200
✅ valid: false
✅ No error, just false flag
```

#### Test Case 7.3: Validate Token - Expired
```bash
curl -X GET http://localhost:8080/api/v1/password/reset-token-valid/expired_token \
  -H "Content-Type: application/json"

Expected Response: 200 OK
{
  "valid": false,
  "expiresAt": null
}

Assertions:
✅ Status code: 200
✅ valid: false
```

---

## ACCOUNT LOCKOUT VERIFICATION

### Test Case: Account Lockout - 5 Failed Attempts
**Objective:** Verify account locks after 5 failed login attempts

```bash
# Attempt 1: Wrong password
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "wrong_password"}'
# Response: 401 Unauthorized

# Attempt 2-4: Repeat wrong password
# Response: 401 Unauthorized

# Attempt 5: Wrong password (should trigger lockout)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "wrong_password"}'

Expected Response: 429 Too Many Requests
{
  "status": 429,
  "error": "Account Locked",
  "message": "Account locked after 5 failed attempts. Try again in 30 minutes",
  "remainingMinutes": 30
}

Assertions:
✅ Status code: 429
✅ remainingMinutes: ~30
✅ Clear message on lockout
```

### Test Case: Account Unlock - Successful Login After Lockout Expires
```bash
# Wait 30 minutes or simulate time passing...
# Then attempt login with correct credentials

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "correct_password"}'

Expected Response: 200 OK
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900
}

Assertions:
✅ Status code: 200
✅ Login successful after unlock period
✅ Account lockout cleared
```

---

## SECURITY VERIFICATION CHECKLIST

### Authorization Tests
- ✅ No token → 401
- ✅ Invalid token → 401
- ✅ Expired token → 401
- ✅ Wrong role → 403 (if endpoint requires specific role)
- ✅ Correct token → 200

### Input Validation Tests
- ✅ Empty email → 400
- ✅ Invalid email format → 400
- ✅ Weak password → 400
- ✅ Mismatched passwords → 400
- ✅ Missing required fields → 400

### Token Security Tests
- ✅ Token brute force → Invalid token error (no 200)
- ✅ Token reuse (reset) → Already used error
- ✅ Token expiry → Expired error
- ✅ Token hashing → Plain tokens never in response

### Account Protection Tests
- ✅ 5 failed attempts → Locked 30 minutes
- ✅ While locked → Attempts rejected
- ✅ After 30 minutes → Can login normally
- ✅ Successful login → Resets failed attempts

### Data Protection Tests
- ✅ Password never in response
- ✅ Password hash never in response
- ✅ Email verified status visible
- ✅ Other user's data not accessible

---

## TEST EXECUTION SUMMARY

| Endpoint | Test Cases | Status |
|----------|-----------|--------|
| GET /api/v1/user/profile | 4 | ✅ Defined |
| PUT /api/v1/user/profile | 5 | ✅ Defined |
| POST /api/v1/email/verify | 5 | ✅ Defined |
| POST /api/v1/email/resend-verification | 4 | ✅ Defined |
| POST /api/v1/password/reset-request | 4 | ✅ Defined |
| POST /api/v1/password/reset-confirm | 6 | ✅ Defined |
| GET /api/v1/password/reset-token-valid/{token} | 3 | ✅ Defined |
| **TOTAL** | **31** | **✅ COMPLETE** |

### Additional Test Scenarios
- Account Lockout: 2 tests
- Security Verification: 15+ assertions
- **Total QA Coverage:** 35+ scenarios

---

## NEXT STEPS FOR QA EXECUTION

1. ✅ Ensure application is running on localhost:8080
2. ✅ Have at least 2 test user accounts created
3. ✅ Execute each test case in sequence
4. ✅ Document actual vs. expected responses
5. ✅ Log any deviations as bugs
6. ✅ Verify all 31+ test cases pass

---

## QA PASS/FAIL CRITERIA

**PASS if:**
- ✅ All 31+ test cases execute successfully
- ✅ All response codes match expected values
- ✅ All response bodies contain expected fields
- ✅ All security checks pass (no unauthorized access)
- ✅ All validation works correctly
- ✅ Account lockout functions correctly
- ✅ Token expiry works correctly
- ✅ No sensitive data exposed (passwords, hashes)

**FAIL if:**
- ❌ Any response code does not match expected
- ❌ Any security check fails
- ❌ Any validation can be bypassed
- ❌ Account lockout does not work
- ❌ Sensitive data exposed in responses

---

**Report Generated:** 2026-06-17 10:45 UTC  
**Phase 4 Status:** MANUAL QA CHECKLIST COMPLETE  
**Ready For:** QA Team Execution  
**Next Action:** Execute all 31+ test scenarios against running application

