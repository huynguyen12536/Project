# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.0.0] - 2026-06-17

### Added

#### Authentication
- User registration with email and password
- User login with JWT token generation
- Separate access and refresh token generation
- Refresh token endpoint for session rotation
- Logout endpoint with token revocation
- Protected endpoints requiring Bearer token authentication
- GET /api/v1/users/me endpoint for authenticated user info

#### Security
- RS256 JWT signing algorithm with configurable key rotation
- JwtAuthenticationFilter for validating Bearer tokens on protected endpoints
- BCrypt password hashing with 10 rounds strength
- Refresh token persistence in database with revocation tracking
- User validation against database before authentication
- Stateless session configuration (no HTTP sessions)
- SecurityContextHolder properly configured for request context

#### Database & Persistence
- PostgreSQL integration for users and refresh_tokens tables
- Flyway database migrations (V1 - initial schema)
- refresh_tokens table with columns: id, user_id, token_hash, issued_at, expires_at, revoked
- User table with email uniqueness constraint
- Foreign key constraints for data integrity
- Timestamp tracking for audit trails

#### Infrastructure & Deployment
- Docker Compose orchestration with PostgreSQL, Redis, and application services
- Multi-stage Dockerfile for optimized image building
- Redis cache integration for session management
- Health check endpoints (/actuator/health)
- Environment variable configuration for all services
- Spring Boot 3 with Java 21 runtime

### Security

#### Implemented
- JWT RS256 signature validation before token acceptance
- BCrypt password hashing prevents plaintext password storage
- Token revocation mechanism prevents use of logged-out sessions
- CSRF protection disabled appropriately for stateless JWT
- User existence validated in database (prevents token escalation)
- No timing attack vulnerabilities in auth logic
- SecurityContextHolder set only after successful validation

#### Best Practices
- Proper error messages without timing leaks
- Constructor injection throughout application
- JPA for SQL injection prevention
- No user input rendering (XSS prevention)

### Infrastructure

#### Containerization
- Docker Compose with 3 services (PostgreSQL, Redis, LearnHub App)
- Port mapping: 8080 (app), 5432 (PostgreSQL), 6379 (Redis)
- Health checks for service readiness
- Environment variable injection for configuration

#### Database
- PostgreSQL 15 with uuid-ossp extension
- Automatic schema initialization via Flyway
- Prepared for migrations in future sprints
- Foreign key constraints enabled

#### Caching
- Redis 7 integration
- Ready for session storage and caching in future sprints

### Fixed

- Redis configuration: Changed from custom `redis.url` to standard `spring.data.redis.url`
- This aligns with Spring Boot convention and fixed health check failures

## How to use this Release

### Installation

```bash
docker-compose up -d
```

### User Registration

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePass123"}'
```

### User Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePass123"}'
```

Response includes `accessToken` and `refreshToken` in JSON.

### Access Protected Endpoint

```bash
curl -H "Authorization: Bearer <accessToken>" \
  http://localhost:8080/api/v1/users/me
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

### Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Known Limitations

- No rate limiting on authentication endpoints (recommended for Sprint 2)
- No audit logging for authentication events (recommended for Sprint 2)
- No two-factor authentication (planned for future)
- No OAuth2 integration (placeholder in security config for Sprint 3)
- No email verification (planned for future)
- No password reset mechanism (planned for future)

## Testing

All features have been verified with:
- Functional tests (HTTP status and response validation)
- Database tests (persistence and revocation)
- Docker tests (containerization and orchestration)
- Security tests (no critical vulnerabilities)

## Next Steps

See [Sprint 2 - User Authorization](docs/sprint2-plan.md) for upcoming features.

---

[v1.0.0]: https://github.com/yourusername/learnhub/releases/tag/v1.0.0
