# Backend Baseline

Observed on 2026-10-26 from the working tree at `main`. This is an orientation
snapshot, not a guarantee. Re-verify versions, paths, commands, and behavior
before relying on them, and say so in the review when something has changed.

## Layout

```text
backend/
├── pom.xml                       Spring Boot, quality plugins, integration profile
├── checkstyle.xml                Checkstyle configuration
├── Dockerfile                    multi-stage image, non-root runtime, prod profile
├── architecture.md               partially stale; verify against source
└── src/
    ├── main/java/com/skillcheckr/
    │   ├── config/               CORS, deployment validation, password encoder
    │   ├── constant/             role and exam status/type constants
    │   ├── controller/           REST endpoints and controller-side guards
    │   ├── exception/            domain exceptions and HTTP mapping
    │   ├── mapper/               JDBC row mappers
    │   ├── model/                domain objects, DTOs, requests, responses
    │   ├── persistence/
    │   │   ├── entity/           JPA entities
    │   │   └── repository/       Spring Data JPA interfaces
    │   ├── repository/           custom repository interfaces and *Impl classes
    │   ├── security/             token, filter, interceptor, guards, SpEL checks
    │   ├── service/              interfaces, implementations, policies
    │   └── validation/           request and exam/attempt validators
    ├── main/resources/
    │   ├── application*.properties
    │   └── db/migration/         Flyway V1 through V8
    └── test/
        ├── java/com/skillcheckr/ 62 test files at this snapshot
        └── resources/            H2 default and mysql-it profiles
```

`backend/target/` is build output and not source.

## Stack and build

- Spring Boot `3.4.4`, Java `21`, Maven Wrapper (`./mvnw`), artifact
  `skillcheckr-backend`.
- Dependencies: Spring MVC, Spring Data JPA/Hibernate, Spring Security, Spring
  Validation, Actuator, Flyway (`flyway-core` and `flyway-mysql`), MySQL driver,
  Lombok, PDFBox `3.0.5`, H2 (test), Spring Boot Test, and Testcontainers
  JUnit/MySQL (test).
- Checkstyle `3.4.0` runs at Maven `validate`. JaCoCo `0.8.12` enforces at least
  50% line coverage at `verify`.
- The `mysql-integration` profile adds Failsafe tests matching `**/*MySqlIT.java`.
- Docker builds with Maven, runs on Temurin 21 JRE as non-root user `spring`,
  exposes `8080`, and activates the `prod` profile.
- CI uses Temurin Java 21 and runs
  `./mvnw --batch-mode --no-transfer-progress verify` in `backend/`.
  The root `npm test` runs frontend tests then `./mvnw test` in `backend/`.

## Configuration and profiles

Base `application.properties`:

- MySQL URL defaults to
  `jdbc:mysql://localhost:3306/exam_application_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`.
  Credentials default to `root` with an empty password for local use.
- `spring.config.import=optional:file:.env[.properties]`; real environment
  variables take precedence.
- Flyway is enabled with `classpath:db/migration`,
  `baseline-on-migrate=true`, `baseline-version=1`; SQL init is disabled.
- JPA uses `ddl-auto=validate` and `open-in-view=false`.
- Multipart limits are 10 MB per file and 11 MB per request.
- Hikari pool defaults: maximum 20, minimum idle 5, 30-second connection timeout.
- CORS defaults to `https://skill-checkr.vercel.app`; credentials default true.
- Jackson rejects fractional JSON numbers in integer fields
  (`accept-float-as-int=false`).
- Token secret has no committed fallback; TTL defaults to 120 minutes.
- Login/registration rate limits default to 10 per 60 seconds and 5 per hour.
- Actuator exposes only `health`, with details and components hidden.

`dev` restores loopback CORS origins and a committed development fallback
secret, and enables verbose health detail. `prod` allows only the hosted frontend
by default, requires `TOKEN_SECRET` with no fallback, and hides health detail.
`DeploymentConfigurationValidator` treats a profile set as development only when
every active profile is `dev`, `local`, or `test`; mixed sets such as `prod,dev`
are treated as deployments and fail unsafe configuration. It requires a non-empty
production secret of at least 32 characters and rejects wildcard CORS origins when
credentials are enabled.

## Request pipeline and security

The backend has two cooperating authentication layers:

1. `SecurityConfig` builds a stateless Spring Security filter chain with
   `@EnableMethodSecurity`. CSRF is disabled, sessions are stateless, and
   security headers include content-type protection, frame deny, no-referrer,
   HSTS for one year with subdomains, and a **report-only** CSP. Authentication
   and access-denied failures are rendered by `SecurityExceptionHandler` as JSON
   401/403 responses.
2. `TokenAuthenticationFilter` reads `Authorization`, verifies the token, checks
   that the user is still active, sets a `UsernamePasswordAuthenticationToken`
   with `ROLE_<ROLE>` authority and the `AuthPrincipal` request attribute.
3. `AuthInterceptor` is mapped over `/api/**`. It handles public routes and rate
   limiting, then requires a token, re-verifies it, re-checks active status, and
   publishes `AuthPrincipal` for controllers.

Public routes currently agree in `SecurityConfig` and `AuthInterceptor`:
`OPTIONS /**`, `POST /api/auth/login`, `POST /api/requests`, and actuator health
and info. Actuator exposes only health in configuration. Verify parity whenever a
route is added or moved.

`TokenService` issues custom HMAC-SHA256 bearer tokens, not JWTs:

```text
sc1.<base64url(payload)>.<base64url(signature)>
payload: userId:roleId:role:issuedAtEpochSecond:expiresAtEpochSecond:username
```

Verification trims an optional case-insensitive `Bearer ` prefix, checks the
prefix, signature in constant time, payload field count, known role, expiry, and
positive identifiers. The default TTL is 120 minutes. There is no server-side
token revocation store in this implementation.

`AuthPrincipal` distinguishes `user.user_id` (`userId`) from the role-table
primary key (`student.student_id`, `teacher.teacher_id`, or `admin.admin_id`)
which is `roleId`. `AuthGuard` provides principal, role, self/ownership, and exam
access helpers. `ExamAuthorizationService` and `StudentAuthorizationService` are
SpEL beans used by `@PreAuthorize` for exam management and student/attempt
ownership. Some controller handlers also repeat a guard call; verify that the
two checks agree rather than assuming the annotation is sufficient.

`PublicEndpointRateLimiter` is a bounded, synchronized, per-process fixed-window
limiter (maximum 10,000 tracked keys). `AuthInterceptor` trusts
`X-Forwarded-For` only when the immediate peer is listed in
`app.security.rate-limit.trusted-proxies`; that property is empty by default.
Multiple replicas need ingress-level/shared limiting.

CORS is registered as a highest-precedence `CorsFilter` over `/**`. Exact origins
are additive from `cors.allowed-origins`; trailing slashes are stripped. Methods
include GET, POST, PUT, PATCH, DELETE, OPTIONS, and HEAD. Allowed headers are
Authorization, Content-Type, Accept, X-Requested-With, and Origin; only
`Content-Disposition` is exposed. The API authenticates with a bearer header,
not a cookie, so the disabled Spring Security CSRF protection must be evaluated
against the actual credential transport, not toggled reflexively.

Passwords use `BCryptPasswordEncoder`. Registration hashes submitted passwords
before storage; profile changes require and verify the current password before
encoding a replacement.

## Persistence and schema

Flyway is the schema source of truth. The working tree contains V1 through V8:

- V1 creates `user`, `student`, `teacher`, `admin`, `subject`, `request`, `exam`,
  `question`, `answer`, `exam_question`, `exam_registration`, `exam_attempt`,
  `attempt_answer`, and `result`, plus a disabled repository-known admin seed.
- V2 disables only the unchanged seeded administrator.
- V3 adds `exam_question_bank` with a LONGBLOB PDF.
- V4 adds `exam.paper_frozen` and freezes existing approved/upcoming/completed/
  cancelled papers.
- V5 adds the attempt status/ID and exam-question ordering indexes.
- V6 makes profile `user_id` columns NOT NULL and unique.
- V7 adds indexes for foreign keys and frequent lookups.
- V8 adds optimistic-locking `version` columns to `exam` and `result`.

V1 deliberately omits some foreign keys: `exam.teacher_id`, `result.exam_id`, and
`result.student_id` have no FK; comments say application-level checks and soft
deactivation are the reason. `result` has a unique `attempt_id`; `exam_attempt`
has a unique `(exam_id, student_id)`; `exam_question` and `attempt_answer` have
uniques used by upserts.

Persistence is hybrid:

- Custom repositories in `com.skillcheckr.repository` use `JdbcTemplate` and
  explicit SQL, often with `FOR UPDATE` for attempt/exam workflows.
- JPA entities and Spring Data repositories exist under `com.skillcheckr.persistence`.
  Most entities use scalar foreign-key columns; the student, teacher, and admin
  profile entities use lazy `@OneToOne` mappings to the account entity.
  `ExamEntity` and `ResultEntity` declare `@Version`.
- Some custom repository implementations mix both paths. Verify mapping, default,
  flush, and transaction consistency when both touch one aggregate.

Runtime Hibernate validates the Flyway schema. `open-in-view=false` means lazy
relationships must be loaded inside a transaction or mapped explicitly; a lazy
load during JSON serialization is a real failure mode.

Scheduling values are wall-clock `DATETIME`/`TIME` values bound as
`LocalDateTime`/`LocalTime`. Exam windows are also evaluated in SQL with
`CURDATE()`/`CURTIME()` and in Java with `LocalDateTime.now()` or an injected
`Clock`. The JVM zone and MySQL server zone must agree; do not introduce an
instant/UTC conversion without tracing every consumer.

## API surface

Controllers and route prefixes:

- `/api/auth`: login, profile read/update.
- `/api/admin`: teacher/student lists, create from requests, delete, status
  toggles, stats.
- `/api/exams`: create/list/detail, question-bank upload/download, attempt
  start/answer save/read/submit, teacher lists, questions, registration,
  approval/rejection/cancellation/status, registered students/count,
  unregistration, submissions, evaluation, marks award.
- `/api/questions`: question CRUD and lists by subject/exam.
- `/api/requests`: public registration request plus admin list/approve/reject/
  delete.
- `/api/results`: result status check, results by student, all results.
- `/api/subjects`: subject CRUD and stats.

Role and ownership checks are distributed across `@PreAuthorize`, `AuthGuard`,
and service validators. Some handlers are available to any authenticated user
(for example exam detail, upcoming/completed lists, question lists, and
question-bank downloads) and then narrow access inside the controller.

Some controllers return domain objects directly (`Exam`, `Subject`, `Student`,
`Teacher`, `RegistrationRequest`, `ExamRegistration`) while others return DTOs.
Inspect serialization and field exposure rather than assuming every response is a
dedicated DTO. Several controllers use field injection; services and security
components generally use constructor injection.

The frontend contract lives in `frontend/src/api/client.js` and
`frontend/src/api/*Api.js`. The shared client normalizes errors and some response
shapes, and expects bearer-token behavior and specific status semantics.

## Exam, attempt, and grading behavior

- Starting an attempt locks the exam row, checks registration and the effective
  window, reuses a live attempt, finalizes an expired one, and relies on the
  `(exam_id, student_id)` unique key as the final concurrent-start guard.
- Saving an answer locks the attempt row, validates exam/attempt/student,
  expiry, question assignment, type, word limit, and selected-answer ownership,
  then upserts by `(attempt_id, question_id)`.
- Submitting locks the attempt row, is idempotent for an already-submitted
  attempt, grades objective answers, leaves descriptive answers pending when
  needed, inserts a unique result row, and finalizes the attempt in one
  transaction. A controlled-failure test verifies rollback and retry.
- `ExpiredAttemptFinalizer` scans in-progress attempts in keyset batches and uses
  the same submission path, so user submission and background finalization are
  expected to serialize on the attempt row and produce one result.
- Paper approval and question assignment edit share an exam-row lock and
  `paper_frozen`, so post-approval paper mutation is rejected.
- Student result responses pass through `AnswerKeyReleasePolicy`; the answer key
  is not supposed to be exposed before release. Verify every result/question
  response path, including teacher/admin and list endpoints.

These are claims verified in source and tests at this snapshot, not proof that
every race, path, or role combination is covered.

## Tests and commands

At this snapshot there are 62 test files and 656 `@Test` annotations under
`backend/src/test/java`. Recount before citing suite size; the annotation count
is not an execution result.

Test layers include Mockito unit tests, standalone MockMvc controller tests with
the real token/interceptor path, `@SpringBootTest` integration tests,
`@DataJpaTest` lock tests, and MySQL-specific tests. Testcontainers MySQL `8.4`
fixtures are disabled without Docker. The opt-in `mysql-integration` suite also
includes tests that require an env-provided MySQL database; those tests require
`SKILLCHECKR_MYSQL_IT_URL`, `SKILLCHECKR_MYSQL_IT_USERNAME`, and
`SKILLCHECKR_MYSQL_IT_PASSWORD`, and refuse URLs not matching a dedicated
`skillcheckr_it_<suffix>` database.

Default tests use H2 in MySQL mode with Flyway disabled and
`ddl-auto=create-drop`; they do not prove MySQL migration compatibility. The
`mysql-it` profile enables Flyway, sets `baseline-on-migrate=false`,
`clean-disabled=true`, and `ddl-auto=validate`.

| Purpose | Command | Notes |
| --- | --- | --- |
| Backend tests | `./mvnw test` in `backend/` | H2 default; Docker/Testcontainers behavior depends on environment |
| Full backend gate | `./mvnw verify` in `backend/` | Checkstyle, tests, JaCoCo 50% line check |
| Checkstyle only | `./mvnw checkstyle:check` in `backend/` | |
| MySQL integration | `./mvnw -Pmysql-integration verify` in `backend/` | `*MySqlIT`; some need dedicated MySQL env, others use Testcontainers |
| Package without tests | `./mvnw clean package -DskipTests` in `backend/` | Matches root build |
| Local run | `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` in `backend/` | Needs MySQL; optional `.env` |
| Root backend tests | `npm run test:backend` at repository root | Delegates to `./mvnw test` |
| Root backend lint | `npm run lint:backend` at repository root | Delegates to Checkstyle |

The local pre-commit hook runs frontend lint and backend `test-compile`; it does
not run the full test suite or MySQL integration tests.

## Known documentation drift

At this snapshot:

- Root `architecture.md` and `backend/architecture.md` describe a
  `JdbcTemplate`-only backend with no JPA/Hibernate and no Spring Security filter
  chain. The source uses Data JPA/Hibernate, JPA entities/repositories, and an
  active Spring Security filter chain.
- `WebConfig` still contains a comment saying there is no Spring Security filter
  chain, while `SecurityConfig` and `TokenAuthenticationFilter` are active.
- `docs/PROJECT_GUIDE.md` does not exist despite references in older skills.
- `report.md` contains historical verification results, not current evidence.

Report documentation drift only when it affects maintainability or a review
decision; cite both the stale claim and the source that supersedes it.

## Deliberate limitations and review boundaries

- Login/registration throttling is in-memory and per process; multi-replica
  deployments need an ingress/shared limiter.
- The default test schema is Hibernate-generated H2, not Flyway MySQL. MySQL
  confidence requires the dedicated integration profile.
- There is no proctoring or anti-cheat implementation.
- The backend owns authentication, registration, scheduling, answer persistence,
  grading, and result release; client-side checks are not security.
- A disabled seeded admin exists in V1 and is disabled again by V2; treat it as
  provisioning data, not a working account.

## Review hotspots to verify, not findings

The following are reachable patterns worth checking. They are not confirmed
defects and must not be reported without source, caller, schema, and test
evidence:

- `SecurityConfig` and `AuthInterceptor` maintain separate public-route
  allowlists; check method/path normalization and parity on every changed route.
- `@PreAuthorize` SpEL uses parameter names such as `#examId` and `#attemptId`;
  verify compiler `-parameters` behavior and that the bean check matches the
  controller/service ownership rule.
- `StudentAuthorizationService` grants teachers access to student data and
  attempts; check which endpoints use each method and whether the teacher's exam
  ownership is actually constrained for that operation.
- Login can initialize `roleId` to zero when no profile is found, while token
  verification rejects non-positive role IDs; verify the reachable behavior for
  incomplete accounts.
- Several controllers return persistence/domain objects rather than dedicated
  DTOs; check lazy relationships, internal fields, and cross-role data exposure.
- Custom repositories mix `JdbcTemplate` and JPA writes; check flush timing,
  generated IDs, transaction propagation, and consistency after a partial update.
- `@Transactional(noRollbackFor = AttemptStartException.class)` in attempt start
  is deliberate; verify every throwing path and whether prior writes should
  commit.
- `PersistenceConfigurationTest` and the H2 default can hide MySQL-only schema
  behavior; inspect the opt-in MySQL tests and migration validation.
- `baseline-on-migrate=true` in the base profile assumes an existing schema
  matches V1; do not apply it blindly to a fresh or unknown database.
- CORS uses `allowCredentials=true` with bearer tokens; verify the deployment
  origin list and whether credentials are actually needed.
- CSP is report-only, and actuator health is public; both are deliberate
  configuration choices that need evidence before being reported as defects.
