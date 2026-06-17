# BRF v1.0 Specification

## Purpose

This document specifies Backend Readiness Framework (BRF) v1.0 for LearnHub MVP. It defines competencies, evidence requirements, detection strategies, evaluation rules, scoring model, feedback templates, and remediation guidance. The spec is implementation-ready for building the Competency Evaluation Engine (detectors + mapper + feedback generator).

## Scoring model (global)

- Each competency scored 0–100.
- Bands: Not Demonstrated: 0–24, Emerging: 25–54, Proficient: 55–79, Advanced: 80–100.
- Evidence weight: Automated signals = 70% of score; Learner-provided explanations/annotations = 30%.
- Mandatory signals: certain evidence items are gating for minimal band (explicit per competency).
- BRF version embedded in each assessment record.

## Evaluation artifacts

- Evidence snapshot: repository snapshot uploaded to storage (archive reference).
- Detectors: automated analyzers producing structured signals (presence, metrics, examples).
- Learner explanation fields: short text answers linked to assessment (max 500 words each) where required.

## Competency template

For each competency the following sections are provided: 1. Name; 2. Purpose; 3. Employer rationale; 4. Hiring signals mapping; 5. Evidence requirements; 6. Observable indicators; 7. Detection strategy (automatable vs manual); 8. Evaluation rules; 9. Scoring model (weights per signal); 10. Example strong submission; 11. Example weak submission; 12. Feedback template; 13. Remediation guidance.

1. API Design

---

1. Competency Name
   - API Design

2. Competency Purpose
   - Evaluate the candidate's ability to design RESTful HTTP APIs (resources, verbs, status codes), API contracts (request/response schema), pagination, error handling, and HTTP semantics appropriate for a Java backend.

3. Why Employers Care
   - Correct API design means clients can integrate easily, reduces bugs, and shows system-thinking needed for backend roles.

4. Hiring Signal Mapping
   - Clear resource model, consistent route naming, use of appropriate HTTP methods, documented API contract (OpenAPI/Swagger or README), meaningful status codes, input validation, pagination for list endpoints.

5. Evidence Requirements
   - Required for baseline (Emerging): at least one HTTP API endpoint exposed by the project.
   - Strong evidence: OpenAPI/Swagger spec OR clear API docs in README, controllers with annotated request/response DTOs, example curl commands or Postman collection.

6. Observable Indicators
   - Presence of controller classes (Spring @RestController or @Controller).
   - OpenAPI YAML/JSON or Springdoc annotations.
   - DTO classes and validation annotations (javax.validation).
   - Consistent URI patterns (e.g., /api/v1/resources).
   - Use of ResponseEntity or proper status codes.
   - Request/response examples in README or tests.

7. Detection Strategy
   - Automated detectors:  
     a) File-level: detect files under src/main/java/\**/*Controller*.java or classes with @RestController / @Controller / @RequestMapping.  
     b) OpenAPI detection: find openapi.yaml/openapi.json / swagger.yml / springdoc annotations.  
     c) DTO detection: classes under dto/, model/ or *Dto.java; presence of javax.validation annotations.  
     d) Example client scripts: files named README API section, examples/, postman_collection.json.  
     e) Status code usage: static scan for ResponseEntity.status(...) or HttpStatus usage.
   - Manual required: if API contract missing, ask learner to provide a short API contract or point to the expected endpoint behavior (question: "Describe the main resource(s) your API exposes and an example request/response for one core endpoint").

8. Evaluation Rules
   - Not Demonstrated: no detectable API endpoints, no docs, and no learner explanation. Score 0–24.
   - Emerging: at least one controller detected AND minimal documentation (README mentions endpoints) OR learner explanation present mapping 1 endpoint. Score 25–54.
   - Proficient: controllers for main resources + DTOs with validation + OpenAPI or documented examples + consistent URI patterns + at least one integration test that exercises an endpoint OR examples in README. Score 55–79.
   - Advanced: full OpenAPI spec matching controllers, consistent versioning in URL (e.g., /api/v1), strong error handling patterns, sample client usage, and pagination/filters implemented for list endpoints. Score 80–100.
   - Gating: To reach Proficient, either OpenAPI or integration tests / documented examples must exist.

9. Scoring Model (signal weights)
   - Controller presence & mapping signatures: 30% (automated)
   - API contract (OpenAPI or README examples): 25% (20% auto detect, 5% manual if explanation provided)
   - DTOs + validations: 15% (automated)
   - Error handling & status codes: 10% (automated)
   - Tests or client examples: 10% (automated)
   - Learner explanation quality: 10% (manual)

10. Example Strong Submission

- Spring controllers with @RestController, DTOs using javax.validation, springdoc-openapi YAML present, README includes curl examples and a postman collection, list endpoint shows pagination, integration tests in src/test/java verifying key endpoints.

11. Example Weak Submission

- Single controller with hard-coded responses, no DTOs, no docs, no examples, no tests.

12. Feedback Template

- Positive: "Detected X controllers and Y DTOs. Good structure and use of validation."
- Improvements: "Add an OpenAPI spec (or documented examples) and include pagination on list endpoints. Ensure status codes on error paths are meaningful (4xx/5xx)."

13. Remediation Guidance

- Add springdoc-openapi dependency and annotate controllers/DTOs.
- Provide a README section with example requests/responses.
- Add integration tests using MockMvc or TestRestTemplate.
- Implement pagination (limit/offset or page/size) for list endpoints.

---

2. Database Design & SQL

---

1. Competency Name
   - Database Design & SQL

2. Competency Purpose
   - Assess ability to design normalized schemas, write efficient SQL queries, model relationships, use indexes, and design migrations for a Postgres-backed Java application.

3. Why Employers Care
   - Proper data modeling prevents correctness/performance problems, and is a core backend skill.

4. Hiring Signal Mapping
   - Presence of SQL scripts/migrations, clear schema with FK constraints, normalized tables, appropriate indexes, sample queries, use of JPA entities with sensible mappings, use of transactions where appropriate.

5. Evidence Requirements
   - Required for Emerging: at least one migration or SQL file (Flyway/V Flyway or Liquibase, or SQL in repo).
   - Proficient: migrations + domain model + sample complex query (join/aggregation) or test proving correctness.

6. Observable Indicators
   - presence of db/migrations (V1\_\_\*.sql), src/main/resources/db/migration for Flyway, or Liquibase changelogs.
   - Entity classes with @Entity, @Table, @ManyToOne, @OneToMany, @JoinColumn.
   - SQL files under sql/ or scripts/.
   - Index creation statements in migrations.
   - Sample query usage in code or tests using JOIN/aggregation.

7. Detection Strategy
   - Automated detectors:  
     a) Detect migration frameworks and files (Flyway, Liquibase).  
     b) Parse entity classes for relationships and constraints.  
     c) Detect SQL files referencing JOIN/AGGREGATE.  
     d) Detect presence of primary/foreign key creation in migrations.
   - Manual required: rationale for design choices (question: "Explain why you designed schema X with these relationships and indexes").

8. Evaluation Rules
   - Not Demonstrated: no migrations, no entity models, no SQL evidence.
   - Emerging: migration files or JPA entities exist but lack FK constraints or indexes.
   - Proficient: migrations+entities with FK constraints, indexes for commonly filtered columns, sample query or test.
   - Advanced: normalized schema, evidence of query optimization (indexes + explain plan in repo or notes), transactions used appropriately, multi-table migrations, seed data for tests.

9. Scoring Model
   - Migrations presence/quality: 30% (automated/manual)
   - Entity-model relationships & FK: 25% (automated)
   - Index usage & query patterns: 20% (automated + manual)
   - Tests/sample queries: 15% (automated)
   - Learner explanation: 10% (manual)

10. Example Strong Submission

- Flyway migrations creating normalized tables with FK constraints and indexes; JPA entities mapping relationships; sample SQL query and a test that asserts aggregated results; a short README explaining index choices.

11. Example Weak Submission

- Single table with many nullable columns, no FK constraints, no migrations, data access via string-concatenated SQL without parameterization.

12. Feedback Template

- Positive: "Flyway migrations detected and JPA entities map relationships."
- Improvements: "Add FK constraints and indexes for frequently filtered columns; avoid schema with wide nullable columns and document your design rationale."

13. Remediation Guidance

- Introduce Flyway or Liquibase migrations.
- Model relationships explicitly and add FK constraints.
- Create indexes on filter/join columns; add tests covering query correctness.

---

3. Authentication & Authorization

---

1. Competency Name
   - Authentication & Authorization

2. Competency Purpose
   - Evaluate ability to implement secure user authentication (JWT + refresh tokens) and authorization (RBAC via Casbin) patterns, secure storage of credentials/keys, and safe integration with OAuth providers (GitHub).

3. Why Employers Care
   - Security is critical; mis-implemented auth leads to breaches and business risk.

4. Hiring Signal Mapping
   - Evidence of JWT usage, RSA key management, refresh token handling, secure password storage (bcrypt), Casbin policy usage, OAuth integration with GitHub.

5. Evidence Requirements
   - Required: password hashing for local accounts or GitHub OAuth implementation.
   - Proficient: JWT generation & verification with RSA keys, refresh token persistence + revocation, Casbin policy file/usage in code.

6. Observable Indicators
   - Auth endpoints (login, refresh), code that references JWT libraries (jjwt, nimbus), presence of RSA key files or keyloader with KMS/env handling, Casbin policy files, use of BCryptPasswordEncoder, OAuth adapter code, scopes and token metadata handling.

7. Detection Strategy
   - Automated detectors:  
     a) Search for dependencies (pom.xml) including jjwt, nimbus-jose-jwt, spring-security, casbin-spring-boot-adapter.  
     b) Detect endpoints named /auth/login, /auth/refresh.  
     c) Detect use of BCryptPasswordEncoder in code.  
     d) Detect Casbin policy files (policy.csv) or usage of Casbin APIs.
   - Manual required: explanation of token rotation and RSA key protection approach (question: "Describe how you store and rotate your RSA private key and how refresh tokens are revoked").

8. Evaluation Rules
   - Not Demonstrated: no auth code, passwords stored as plain text, no OAuth or JWT usage.
   - Emerging: simple login with password hashed, but no refresh token handling or RBAC.
   - Proficient: JWT signed with RSA keys, refresh tokens stored and revocable, Casbin integrated for RBAC enforcement.
   - Advanced: key rotation workflows, public-key endpoint for token verification, audit logging for token issuance/revocation, robust OAuth handling with minimal token storage.

9. Scoring Model
   - Secure password storage/OAuth integration: 20% (automated)
   - JWT with RSA implementation: 25% (automated/manual)
   - Refresh token lifecycle + revocation: 20% (automated/manual)
   - Casbin policy enforcement: 20% (automated detection of policy + enforcement points)
   - Learner explanation of key management: 15% (manual)

10. Example Strong Submission

- Login endpoints, BCrypt for passwords, RSA keyloader fetching encrypted private key from env/KMS, JWT creation with version claim, refresh tokens persisted with revoke flag, casbin policies in DB and enforcement in controllers, GitHub OAuth flow implemented.

11. Example Weak Submission

- Plain-text passwords, no token revocation, ad-hoc role checks scattered in code, no OAuth.

12. Feedback Template

- Positive: "Detected JWT RSA signing and Casbin policies."
- Improvements: "Provide a documented key rotation policy, ensure refresh tokens support revocation, centralize RBAC logic via Casbin policies rather than ad-hoc checks."

13. Remediation Guidance

- Use BCrypt for passwords, implement RSA-signed JWT and publish public key endpoint, persist refresh tokens and support revocation, integrate Casbin with a central policy store.

---

4. Code Organization & Layering

---

1. Competency Name
   - Code Organization & Layering

2. Competency Purpose
   - Judge whether the repository demonstrates clean separation of concerns, package/module layering (controller/service/repository), and readable, maintainable structure.

3. Why Employers Care
   - Well-organized code reduces onboarding time and maintenance cost and indicates engineering discipline.

4. Hiring Signal Mapping
   - Presence of layered packages (controller/service/repository), small classes with single responsibilities, consistent naming conventions, absence of large God classes.

5. Evidence Requirements
   - Required: at least basic layering (controllers, services, repositories) and package structure consistent with Spring Boot conventions.
   - Proficient: clear layering with DI, interfaces for services, minimal cyclic dependencies.

6. Observable Indicators
   - File paths and packages: controllers under ..controller.., services under ..service.., repositories under ..repository or ..dao..; classes with many responsibilities (>500 LOC) flagged.
   - Use of @Service, @Repository, @Component, interfaces for service contracts.

7. Detection Strategy
   - Automated detectors:  
     a) Path and annotation scanning for controller/service/repository patterns.  
     b) Static analysis for class size and method counts.  
     c) Detect package cycles via dependency graph (simple Jar analysis).
   - Manual required: rationale for any divergence from convention (question: "Explain your module/package organization and why it fits the project").

8. Evaluation Rules
   - Not Demonstrated: flat file layout, controllers containing DB code, no separation into services/repositories.
   - Emerging: basic separation exists but with some layering violations (e.g., business logic in controllers).
   - Proficient: clear packages, DI usage, small classes, repository/DAO separation.
   - Advanced: interface-driven design, modular packages with clear boundaries, unit tests per module, and evidence of dependency inversion.

9. Scoring Model
   - Layer presence and annotations: 40% (automated)
   - Class size & complexity metrics: 20% (automated)
   - Absence of layering violations (business logic in controllers): 20% (automated/manual)
   - Learner explanation: 20% (manual)

10. Example Strong Submission

- Controllers thin (delegate to services), services contain business logic and interfaces, repositories only interact with DB, small classes, package-level JavaDocs, unit tests per layer.

11. Example Weak Submission

- Controllers contain SQL and business branching, single service with hundreds of methods, no package structure.

12. Feedback Template

- Positive: "Layered structure detected; services and repositories separated."
- Improvements: "Move business logic from controllers into services and add interfaces for key services to aid testability."

13. Remediation Guidance

- Refactor large controllers into services; introduce repository/DAO classes; extract interfaces; add unit tests for services.

---

5. Business Logic Separation

---

1. Competency Name
   - Business Logic Separation

2. Competency Purpose
   - Verify that domain rules and business policies are implemented in separate modules/services and are not tightly coupled to transport or persistence layers.

3. Why Employers Care
   - Ensures maintainability and adaptability when changing UI or storage layers; signals design maturity.

4. Hiring Signal Mapping
   - Explicit domain services, domain models, absence of business logic in controllers/repositories, configuration-driven rules.

5. Evidence Requirements
   - Required: presence of service classes encapsulating core business rules and tests for business logic.
   - Proficient: domain model objects with encapsulated behavior, services isolated from repositories via interfaces.

6. Observable Indicators
   - Methods in service classes performing business computations, unit tests under src/test for services, little to no SQL or HTTP code in services, usage of DTOs to boundary-cross data.

7. Detection Strategy
   - Automated detectors:  
     a) Identify suspicious methods in controllers or repositories that contain business-rule-like logic (long conditional blocks, complex calculations).  
     b) Detect presence of dedicated service classes and tests for them.
   - Manual required: ask learner to explain domain model and boundaries if ambiguous (question: "Explain core business rules and where they are implemented").

8. Evaluation Rules
   - Not Demonstrated: business logic embedded in controllers/repositories, no tests.
   - Emerging: some business logic extracted into services but scattered.
   - Proficient: consistent separation with tests for business rules.
   - Advanced: domain-driven design patterns, domain events, and clear anti-corruption boundaries.

9. Scoring Model
   - Presence of service-level tests: 30% (automated/manual)
   - Separation evidence (no business logic in controllers): 30% (automated)
   - Encapsulation of domain model: 20% (automated/manual)
   - Learner explanation: 20% (manual)

10. Example Strong Submission

- Core business calculations in service classes with thorough unit tests; controllers orchestrate only input validation and delegation; repositories only persist domain objects.

11. Example Weak Submission

- Business rules implemented directly inside controller methods; no tests.

12. Feedback Template

- Positive: "Detected service classes and unit tests for core logic."
- Improvements: "Extract remaining business logic from controllers and add service-level unit tests. Consider defining clear domain entities."

13. Remediation Guidance

- Move logic into service classes, add unit tests that assert business outcomes, define domain model classes with behavior rather than anemic DTOs.

---

6. Documentation Quality

---

1. Competency Name
   - Documentation Quality

2. Competency Purpose
   - Assess clarity and completeness of README, setup instructions, API usage examples, architecture rationale, and decision notes.

3. Why Employers Care
   - Good documentation reduces onboarding friction for new teammates and recruiters reviewing a candidate's work.

4. Hiring Signal Mapping
   - Presence of README with setup steps, run instructions, architecture overview, API examples, and a CONTRIBUTING or developer notes section.

5. Evidence Requirements
   - Required: README.md that explains how to run the project locally (build & run).
   - Proficient: README includes architecture overview, API examples, testing instructions, and BRF references.
   - Advanced: additional design decisions, trade-offs, and developer guide.

6. Observable Indicators
   - README.md at repository root, docs/ folder, API examples, CI badges, CONTRIBUTING.md, CHANGELOG.md.

7. Detection Strategy
   - Automated detectors: locate README.md and check for presence of keywords ("build", "run", "test", "docker"); detect presence of docs/ folder; parse for code blocks showing commands.
   - Manual required: assess quality of prose and clarity (question: "Describe how a developer would get the project running locally in 5 steps").

8. Evaluation Rules
   - Not Demonstrated: no README or README has only project title.
   - Emerging: README with basic build/run steps.
   - Proficient: README plus API examples, architecture brief, and testing instructions.
   - Advanced: thorough docs, design trade-offs, contribution guidance, and migration notes.

9. Scoring Model
   - Basic run instructions: 30% (automated)
   - API examples and architecture: 25% (mix auto/manual)
   - Tests & CI instructions: 20% (automated)
   - Additional docs and clarity: 25% (manual)

10. Example Strong Submission

- Clear README with Docker-compose up instructions, how to run tests, API examples, architecture section, and CONTRIBUTING.md.

11. Example Weak Submission

- README only says "Java project" with no instructions.

12. Feedback Template

- Positive: "README contains clear run instructions."
- Improvements: "Add API examples and a short architecture overview. Add steps for running tests in CI."

13. Remediation Guidance

- Add step-by-step local dev instructions, Docker compose example, API example requests, and brief architecture notes.

---

7. Testing Practices

---

1. Competency Name
   - Testing Practices

2. Competency Purpose
   - Determine whether the project contains unit, integration, or end-to-end tests that verify core behavior.

3. Why Employers Care
   - Tests indicate engineering discipline and reduce regressions.

4. Hiring Signal Mapping
   - Presence of automated tests, CI configuration to run tests, usage of test frameworks (JUnit, Mockito, Testcontainers), and meaningful assertions.

5. Evidence Requirements
   - Required (Emerging): at least one unit test file.
   - Proficient: unit tests for services and at least one integration test for an endpoint (using TestRestTemplate, MockMvc, or Testcontainers).
   - Advanced: test coverage metrics, tests that cover edge cases, CI that runs tests, and mocking/stubbing used appropriately.

6. Observable Indicators
   - src/test/java presence, pom.xml dependencies for junit, mockito, testcontainers, CI pipeline yaml with test steps.

7. Detection Strategy
   - Automated detectors:  
     a) Detect files under src/test/java.  
     b) Parse build files for test dependencies.  
     c) Detect CI workflows (.github/workflows) invoking mvn test or gradle test.  
     d) Detect use of Testcontainers in tests.
   - Manual required: assess test quality when automated metrics ambiguous (question: "Describe which behaviors your tests assert and why they give confidence").

8. Evaluation Rules
   - Not Demonstrated: no tests.
   - Emerging: one or two unit tests present.
   - Proficient: unit tests across services + at least one integration test; CI runs tests.
   - Advanced: broad coverage, integration tests with DB/testcontainers, tests for edge cases, and stable CI.

9. Scoring Model
   - Test presence & density: 40% (automated)
   - Integration tests & CI: 30% (automated)
   - Use of Testcontainers or similar: 10% (automated)
   - Learner explanation & test rationale: 20% (manual)

10. Example Strong Submission

- Comprehensive unit tests for services, integration tests using Testcontainers and a CI workflow running tests on push, assertions for edge cases.

11. Example Weak Submission

- No tests or trivial tests that assert true.

12. Feedback Template

- Positive: "Detected unit and integration tests; CI configured to run tests."
- Improvements: "Increase coverage for critical services and add tests for failure modes. Consider Testcontainers for reliable integration testing."

13. Remediation Guidance

- Add unit tests for service logic, create integration tests that run against a transient database (Testcontainers), ensure CI runs tests on PRs.

---

## Cross-cutting rules & implementation notes

1. Mandatory gating: An assessment must include evidence snapshot and metadata; if repository is empty, mark Not Demonstrated across competencies.
2. Evidence provenance: Each detected signal must include file path, detector name, detection confidence, and sample snippet.
3. Learner explanation: For signals requiring manual context, collect structured responses (max 500 words) and include them in scoring per competency.
4. Extensibility: Detector outputs are typed JSON objects; mapping rules reference detector output keys to compute per-competency signals.
5. Thresholds & tunability: Thresholds for automated scoring should be configurable in BRF YAML (weights, gating rules, min-signal counts).
6. Human override: Admins can mark an assessment manual_review and adjust competency scores; manual adjustments must be audited and stored.

## BRF YAML representation (high-level)

- brf_version: "1.0.0"
- competencies:
  - id: api_design
    name: "API Design"
    weights:
    controllers: 0.30
    api_contract: 0.25
    dtos: 0.15
    error_handling: 0.10
    tests_examples: 0.10
    learner_explanation: 0.10
    gating:
    proficient: [api_contract OR integration_tests]
    detectors:
    - controller_detector
    - openapi_detector
    - dto_detector
    - test_detector
      thresholds:
      not_demonstrated: 24
      emerging: 25
      proficient: 55
      advanced: 80
      manual_questions:
    - id: api_summary
      prompt: "Describe the main resource your API exposes and provide an example request and response."
      max_length: 500

(Other competencies follow same pattern)

## Deliverable expectations for engineer implementing engine

- Implement detectors listed above producing typed JSON output (detector name, matched files, confidence score, snippet).
- Implement BRF YAML loader and mapping engine: read weights, gating rules, thresholds, detector-to-signal mapping.
- Implement scoring engine: compute automated score (70%) and combine with manual answers (30%), apply gating rules to assign band.
- Persist assessment with evidence_snapshot, raw detector outputs, computed competency_results, and learner explanations.
- Provide debugging endpoint to view detector outputs and intermediate scoring for each assessment.

## Appendix: Manual question list (starter)

- api_summary (API Design) — "Describe your main API resource and show example request/response."
- db_rationale (Database) — "Explain key relationships and indexing choices."
- auth_rationale (Auth) — "Explain your authentication/authorization design and key storage practice."
- doc_steps (Docs) — "List steps to run the app locally in 5 commands."
- tests_rationale (Testing) — "What do your tests assert and how do they provide confidence?"

## Change log

- v1.0.0 — Initial BRF v1.0 specification.

End of BRF v1.0 Specification
