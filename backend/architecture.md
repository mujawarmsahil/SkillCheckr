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
| Operational endpoint | Spring Boot Actuator health endpoint |
| Model generation | Lombok |
| Deployment | Executable JAR, with an optional multi-stage Docker image |

There is no JPA, Hibernate, database migration library, message broker, or separate frontend build in this module.

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
| `ResultController` | Legacy result submission and result retrieval |
| `SubjectController` | Subject management and subject statistics |

Existing route aliases are part of the public HTTP contract and should not be removed as dead code. Controllers use `ResponseEntity` directly; responses are not wrapped in one universal response type.

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

`POST /api/results/submit` remains a separate legacy result path. It can score submission DTO data directly and can create an attempt when one is not supplied. It is distinct from the persisted attempt submission flow.

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

`src/main/resources/schema.sql` is a checked-in reference schema. The application does not automatically run it against the production MySQL datasource, and the project has no Flyway or Liquibase migration history.

Several row mappers tolerate missing optional projection columns. This is required because repositories use more than one projection shape. Those fallback paths are part of current persistence behavior and should not be removed without replacing them with equivalent null/default handling.

## Transaction boundaries

Transactions are explicit where multi-step consistency is implemented:

- `AttemptAnswerServiceImpl.saveAnswer` validates and upserts one answer transactionally.
- `ExamSubmissionServiceImpl.submit` locks the attempt, scores persisted MCQ answers, updates marks, inserts a result, and finalizes the attempt transactionally.

Other multi-write catalog, approval, and legacy result operations do not consistently declare a transaction boundary. This is a current architectural characteristic, not an invariant for new code.

## Authentication behavior

The current implementation does not use Spring Security's HTTP filter chain for authentication or authorization.

- Passwords prefixed with BCrypt are verified with `BCryptPasswordEncoder`.
- A matching legacy plaintext password is accepted and upgraded to a BCrypt hash after successful login.
- Login returns an unsigned token shaped as `jwt-mock-<userId>-<timestamp>`.
- Exam attempt routes parse the optional `Authorization` header and derive the student ID from that token.
- Attempt ownership checks compare the derived student ID with the persisted attempt.
- Other controller groups do not pass through a global authentication or role-authorization filter.

The role values stored for users describe business roles but are not enforced globally by the current HTTP layer. CORS configuration controls browser origin access; it is not authentication or authorization.

## Error handling

`GlobalExceptionHandler` provides the central exception-to-HTTP mapping:

| Exception | HTTP status |
| --- | --- |
| `ResourceNotFoundException` | 404 |
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `EmptyResultDataAccessException` | 401 |
| Malformed request body | 400 |
| Request type mismatch | 400 |
| Unhandled exception | 500 |

Unhandled exceptions currently expose their message in the 500 response.

## CORS and runtime configuration

`WebConfig` registers one high-precedence `CorsFilter` for all routes. It derives origin patterns from:

- `cors.allowed-origins`
- `frontend.url`
- The configured production frontend
- Localhost and loopback development patterns

`src/main/resources/application.properties` contains the runtime defaults. Important environment-backed settings include:

| Setting | Environment variables |
| --- | --- |
| HTTP port | `PORT`, `SERVER_PORT` |
| Bind address | `SERVER_ADDRESS` |
| JDBC URL | `DB_URL`, or `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SSL` |
| JDBC driver | `DB_DRIVER` |
| Database credentials | `DB_USERNAME`, `DB_PASSWORD` |
| Hikari pool settings | `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_POOL_IDLE_TIMEOUT`, `DB_POOL_MAX_LIFETIME`, `DB_POOL_CONN_TIMEOUT` |
| CORS and frontend origins | `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL` |

Actuator exposes `health` over HTTP. The health response currently includes component details because production properties enable detailed health output.

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

- Authentication is mock-token based and is only consulted by attempt-related routes.
- There is no global role-based authorization.
- Exam status transitions depend on request-time database checks rather than a scheduler.
- Transaction coverage is limited to answer saving and persisted attempt submission.
- The reference schema is not a migration mechanism and is not the sole source of runtime database structure.
- The legacy result submission path and the persisted attempt workflow coexist.
- Public route aliases, DTO fields, and Jackson compatibility accessors are compatibility surfaces, not dead code.

New code should follow the existing controller-service-repository direction, preserve the existing HTTP contracts unless a versioned change is explicitly planned, and add equivalent fallback behavior before simplifying tolerant row mappings.
