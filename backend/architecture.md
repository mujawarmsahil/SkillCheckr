# SkillCheckr Backend Architecture

This document describes the backend architecture implemented in this directory. It is based on the current source and configuration, not on intended future behavior.

## System overview

SkillCheckr backend is a modular monolithic Spring Boot REST API.

| Concern | Implementation |
| --- | --- |
| Language | Java 21 |
| Web framework | Spring MVC with Spring Boot 3.4.4 |
| Persistence | Spring JDBC and explicit SQL through `JdbcTemplate` |
| Production database | MySQL 8.x |
| Build | Maven Wrapper |
| Password hashing | BCrypt through Spring Security Crypto |
| Schema migrations | Flyway versioned SQL migrations |
| Operational endpoint | Spring Boot Actuator health endpoint |
| Model generation | Lombok |
| Deployment | Executable JAR, with an optional multi-stage Docker image |

There is no JPA, Hibernate, message broker, or separate frontend build in this module.

## Runtime flow

```text
HTTP client
    |
    v
CorsFilter
    |
    v
Spring DispatcherServlet
    |
    v
Controller
    |
    v
Service interface and implementation
    |
    v
Repository interface and implementation
    |
    v
JdbcTemplate and row mappers
    |
    v
MySQL
```

Errors from the lower layers are translated by `GlobalExceptionHandler` before a response is returned to the client.

## Package structure

| Package | Responsibility |
| --- | --- |
| `com.skillcheckr` | Application entry point |
| `config` | Password encoder and CORS configuration |
| `constant` | Shared exam, question, attempt, and result constants |
| `controller` | HTTP routes, request binding, basic input checks, and response selection |
| `service` | Business operations and transaction orchestration |
| `repository` | JDBC queries, generated-key handling, and persistence operations |
| `mapper` | Conversion from JDBC result sets to domain models |
| `model` | Request, response, and domain data types serialized by Jackson |
| `validation` | Shared attempt ownership and lifecycle checks |
| `exception` | Application exceptions and HTTP error translation |

The normal dependency direction is:

```text
controller -> service -> repository -> JdbcTemplate
                  |             |
                  +-> validation |
                                +-> mapper
```

Controllers do not call repositories directly. Repositories do not depend on controllers or services.

## HTTP layer

The API is split across seven controller groups:

| Controller | Primary responsibility |
| --- | --- |
| `AuthController` | Login and profile retrieval/update |
| `AdminController` | Teacher/student administration and aggregate statistics |
| `ExamController` | Exams, registrations, attempts, autosaved answers, and attempt submission |
| `QuestionController` | Question creation, retrieval, update, and deletion |
| `RegistrationRequestController` | Teacher and student registration requests |
| `ResultController` | Result retrieval and submission status checks |
| `SubjectController` | Subject management and subject statistics |

Canonical REST routes are authoritative across the API surface. Controllers use `ResponseEntity` directly; responses are not wrapped in one universal response type.

Request validation is currently split between:

- Jackson binding and model annotations in `model`.
- Explicit null, identifier, and field checks in controllers.
- Domain and attempt checks in services and `ExamAttemptValidator`.
- SQL existence checks in repositories.

The codebase does not currently use Bean Validation annotations or `@Valid` request validation.

## Request lifecycle

A normal request follows this sequence:

1. `CorsFilter` applies CORS handling before MVC dispatch.
2. `DispatcherServlet` selects a controller route.
3. Jackson deserializes the request body where applicable.
4. The controller performs boundary checks and calls a service.
5. The service applies workflow rules and delegates persistence to a repository.
6. The repository executes parameterized SQL with `JdbcTemplate`.
7. A row mapper, where needed, converts the result set into a model.
8. The service returns a domain or DTO result.
9. The controller selects the HTTP status and response representation.

No startup runner, bootstrap task, scheduled job, or event consumer performs domain work during application startup.

## Domain modules

### Authentication and profiles

`AuthService` and `AuthRepository` implement username lookup, password verification, role-specific user ID lookup, and profile persistence. Profiles can be synchronized across user and registration-request records.

`AdminService` supports approval of teacher and student requests, user-status operations, roster retrieval, and platform statistics.

### Subjects, exams, and questions

`SubjectService`, `ExamService`, and `QuestionService` manage the academic catalog. Repositories use explicit SQL for joins, counts, duplicate checks, generated IDs, and cross-table deletes.

Exam status is reconciled lazily with database time during selected exam reads. There is no background scheduler that performs transitions independently of requests.

### Attempts and answers

The attempt lifecycle is split across:

- `ExamService` for starting and querying attempts.
- `AttemptAnswerService` for answer validation and autosaving.
- `ExamSubmissionService` for final scoring and submission.
- `ExamAttemptValidator` for attempt state, exam ownership, and student ownership checks.
- Attempt, attempt-answer, and exam-question repositories for persistence.

## Persistence architecture

Repositories use `JdbcTemplate` directly. `spring-boot-starter-data-jdbc` supplies the required Spring JDBC and transaction auto-configuration. SQL is written in the repository implementations; there are no ORM entities or Spring Data repository interfaces.

Generated keys are obtained through JDBC `PreparedStatement` and `KeyHolder` where tables use auto-increment IDs.

The schema currently contains these tables:

- `user`
- `student`
- `teacher`
- `admin`
- `subject`
- `request`
- `exam`
- `question`
- `answer`
- `exam_question`
- `exam_registration`
- `exam_attempt`
- `attempt_answer`
- `result`

`src/main/resources/db/migration/V1__initial_schema.sql` defines the initial
database structure, tables, keys, constraints, and inactive administrator seed.
Flyway applies it automatically to a new, empty database. A non-empty existing
database is baselined at version 1 and skips V1; this assumes its schema already
matches V1. `V2__disable_known_admin.sql` is safe on both paths: it disables only
the unchanged repository-seeded password and leaves a replaced password alone.
New schema changes must use new versioned SQL migrations; applied migrations
must not be edited.

RowMappers use explicit query projections rather than runtime metadata fallback. Repositories explicitly project the required columns (such as `student.name AS student_name` via `LEFT JOIN`), eliminating `ResultSetMetaData` and `hasColumn` probing.

## Transaction boundaries

Transactions are explicit where multi-step consistency is implemented:

- `AttemptAnswerServiceImpl.saveAnswer` validates and upserts one answer transactionally.
- `ExamSubmissionServiceImpl.submit` locks the attempt, scores persisted MCQ answers, updates marks, inserts a result, and finalizes the attempt transactionally.
- `AdminServiceImpl` declares a transaction boundary for student/teacher creation from an approved request, student/teacher deletion, and student/teacher status toggles.
- `QuestionServiceImpl` declares a transaction boundary for `attachQuestionsToExam` and `saveQuestionsWithAnswers`.
- `SubjectServiceImpl.deleteSubjectById` declares a transaction boundary.
- `ExamSubmissionServiceImpl.awardAnswerMarks` declares a transaction boundary.

Transactional coverage is therefore broader than answer saving and attempt submission. Single-statement reads and simple updates remain unannotated, which is an implementation characteristic rather than an invariant for new code.

## Authentication behavior

The current implementation does not use Spring Security's HTTP filter chain for authentication or authorization. Tokens are custom and stateless; JWT is not used.

- Passwords prefixed with BCrypt are verified with `BCryptPasswordEncoder`.
- A matching legacy plaintext password is accepted and upgraded to a BCrypt hash after successful login.
- `TokenService` issues a custom stateless token. It is **not** a JWT and carries no header or standard claim set.
- A token is `sc1.<base64url payload>.<base64url signature>`, where the payload is a colon-joined `userId:roleId:role:issuedAt:expiresAt:username`.
- The signature is **HMAC-SHA256** over the encoded payload, base64url encoded without padding. Without the server secret a token cannot be forged, so a forged or tampered token fails `verifyToken` and the request is rejected.
- Signature verification is constant-time, and `verifyToken` also rejects an unknown role, a malformed value, or an expired token.
- `AuthInterceptor` is registered globally over `/api/**` in `WebConfig`. It requires a valid token on every request except an explicit public-route allowlist, which is matched on path **and** HTTP method.
- On success the interceptor stores the parsed `AuthPrincipal` as the `skillcheckr.principal` request attribute.
- Role authorization is enforced per handler by `AuthGuard` (`requireRole`, `requireAdmin`, `requireStaff`, `requireStudent`, `requireStudentId`, plus self/staff and exam-access checks), not by a global role filter.

Stored role values are therefore enforced at the HTTP layer, both globally for authentication and per handler for authorization.

CORS configuration controls browser origin access; it is not authentication or authorization.

## Error handling

`GlobalExceptionHandler` provides the central exception-to-HTTP mapping:

| Exception | HTTP status |
| --- | --- |
| `ResourceNotFoundException` | 404 |
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `AccountDisabledException` | 403 |
| `EmptyResultDataAccessException` | 401 |
| Malformed request body | 400 |
| Request type mismatch | 400 |
| Non-numeric request value | 400 |
| Unknown URL | 404 |
| Unsupported HTTP method | 405 |
| Unhandled exception | 500 |

Unhandled exceptions are logged in full server side and returned to the client as a fixed generic message. The exception message is never echoed, because an unexpected exception is frequently a SQL or driver failure whose message can disclose internal detail.

## CORS and runtime configuration

`WebConfig` registers one high-precedence `CorsFilter` for all routes. It derives origin patterns from configuration only:

- `cors.allowed-origins`

Loopback origins are **not** appended unconditionally. Earlier revisions added `http://localhost:*` and `http://127.0.0.1:*` to every environment while credentials were enabled, so any deployed instance accepted credentialed cross-origin requests from any loopback port. Those origins now come only from the `dev` profile, as exact `host:port` pairs rather than a wildcard.

Allowed and exposed headers are enumerated rather than terminated with `*`, because a `*` entry is widened by the browser to every header. `cors.allow-credentials` stays configurable; the API is stateless and authenticates with an `Authorization` bearer header rather than a cookie.

### Profiles

| Profile | Token secret | CORS defaults |
| --- | --- | --- |
| default (`application.properties`) | required, no fallback | hosted frontend only |
| `dev` / `local` | development fallback allowed, warned | hosted frontend plus `localhost:5173`, `localhost:3000`, `localhost:8080`, `127.0.0.1:5173`, `127.0.0.1:3000` |
| `prod` | required, no fallback | hosted frontend only |

The production image sets `SPRING_PROFILES_ACTIVE=prod`. `npm run dev:backend` starts with the `dev` profile.

### Startup validation

`DeploymentConfigurationValidator` runs during context startup and refuses to start when:

- `app.security.token-secret` is empty outside a development profile;
- it is still the committed development fallback value;
- it is shorter than 32 characters;
- `cors.allowed-origins` contains a wildcard while credentials are enabled, which would make the browser reflect any origin.

The base properties file deliberately carries **no** default token secret. An unset secret makes `TokenService` generate a random ephemeral secret for the run — safe, but every issued token stops working after a restart — and the validator then refuses to continue unless the `dev` profile is active. This replaced a committed fallback value that let a deployment which forgot `TOKEN_SECRET` start successfully while signing forgeable tokens.

`src/main/resources/application.properties` contains the runtime defaults. Important environment-backed settings include:

| Setting | Environment variables |
| --- | --- |
| HTTP port | `PORT`, `SERVER_PORT` |
| Bind address | `SERVER_ADDRESS` |
| JDBC URL | `DB_URL`, or `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SSL` |
| JDBC driver | `DB_DRIVER` |
| Database credentials | `DB_USERNAME`, `DB_PASSWORD` |
| Hikari pool settings | `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_POOL_IDLE_TIMEOUT`, `DB_POOL_MAX_LIFETIME`, `DB_POOL_CONN_TIMEOUT` |
| CORS allowed origins | `CORS_ALLOWED_ORIGINS` |
| CORS credentials toggle | `CORS_ALLOW_CREDENTIALS` |
| Token signing secret | `TOKEN_SECRET` |
| Token lifetime | `TOKEN_TTL_MINUTES` |
| Anonymous login limit | `AUTH_LOGIN_RATE_LIMIT_REQUESTS`, `AUTH_LOGIN_RATE_LIMIT_WINDOW_SECONDS` |
| Anonymous registration limit | `REGISTRATION_RATE_LIMIT_REQUESTS`, `REGISTRATION_RATE_LIMIT_WINDOW_SECONDS` |
| Active profile | `SPRING_PROFILES_ACTIVE` |

No secret is stored in the repository. `backend/.env.example` is a git-ignored, credential-free template; copy it to `backend/.env` and fill in real values, or set the same variables in the deployment platform.

Actuator exposes `health` over HTTP. The health response publishes component details only under the `dev` profile; the base properties and the `prod` profile keep `show-details` and `show-components` at `never`, so a deployment does not disclose component names to anonymous callers.

## Anonymous endpoints and rate limiting

`AuthInterceptor` requires a valid token on `/api/**` except for an explicit allowlist matched on path **and** HTTP method. The anonymous surface is:

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/auth/login` | `POST` | sign in |
| `/api/requests` | `POST` | public registration request |
| `/actuator/health`, `/actuator/info` | any | health probe |

Login is limited to 10 requests per minute per remote address; registration is
limited to 5 requests per hour per remote address. A rejected request returns
HTTP 429 and a `Retry-After` header. These limits are in-memory and per process,
so production deployments with multiple replicas must enforce shared limits at
the ingress as well.

## Password storage and legacy migration

Passwords are stored as BCrypt hashes and verified with `BCryptPasswordEncoder`. `AuthServiceImpl.login` treats a stored value that lacks a BCrypt prefix (`$2a$`, `$2b$`, `$2y$`) as legacy plaintext, compares it in constant time, and rewrites the row as a BCrypt hash on the next successful sign in.

New registration and profile password changes require at least eight characters.

`V1__initial_schema.sql` seeds one inactive administrator row. It must be
provisioned with a unique BCrypt password before activation; the repository
contains no usable default administrator credential.

## Tests

The test suite has three broad layers:

- Mockito unit tests for controllers, services, and repositories.
- MockMvc tests for HTTP contracts and CORS.
- Full Spring context tests for application startup, Actuator health, and CORS integration.

Repository tests generally mock `JdbcTemplate`. The full context tests provide a mocked primary `DataSource`; the test resource file also defines an H2 MySQL-mode fallback with SQL initialization disabled.

Common commands from `backend/` are:

```bash
./mvnw clean compile
./mvnw test
./mvnw checkstyle:check
./mvnw verify
```

Checkstyle is bound to Maven's `validate` phase. JaCoCo reports during tests and its configured 50% line-coverage rule runs as part of `verify`.

## Build and deployment

`mvnw` builds an executable Spring Boot JAR. The `Dockerfile` uses a Maven/Temurin 21 build image and a non-root Temurin 21 JRE runtime image.

The Docker build packages with tests skipped, so image construction does not independently execute the test suite. The container listens on the runtime `PORT`, with 8080 as its fallback, and expects database, CORS, and frontend settings to be supplied through environment variables.

## Architectural constraints

The following are current constraints that callers and maintainers must account for:

- Every controller resolves the caller through `AuthGuard`, which centralizes role and ownership checks; authorization is therefore not attempt-specific.
- Role-based authorization is enforced at the controller boundary, but not by a Spring Security filter chain or an interceptor.
- Exam status transitions depend on request-time database checks rather than a scheduler.
- Transaction coverage spans answer saving, attempt submission, administrative approval/deletion/status changes, question authoring, and subject deletion.
- Versioned Flyway SQL migrations are the source of truth for runtime database structure.
- Result submission is exposed only through the attempt workflow in `ExamController`; `ResultController` is read-only.
- Canonical REST routes are authoritative across all controllers; legacy route aliases have been removed.

New code should follow the existing controller-service-repository direction, adhere to canonical REST contracts, and use explicit query projections in row mappings.
