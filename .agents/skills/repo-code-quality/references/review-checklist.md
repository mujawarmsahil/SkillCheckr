# Review Checklist

Use only the sections relevant to the scope. For every candidate finding, inspect
the implementation, its callers, configuration, and tests. Report a problem only
with evidence and a practical impact.

## 1. Architecture and maintainability

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Layer direction | `controller/`, `service/`, `repository/`, `persistence/`, `security/` imports and calls | A controller reaching into a repository or persistence detail; a service depending on web/security plumbing; a repository owning business decisions |
| Responsibility boundaries | `ExamServiceImpl`, `ExamSubmissionServiceImpl`, `AdminServiceImpl`, `AuthRepositoryImpl`, `ExamRepositoryImpl`, `AdminRepositoryImpl`, security classes | One class changing for unrelated reasons, or duplicated rules in controllers/services/repositories |
| Hybrid persistence consistency | Custom `*RepositoryImpl` classes that mix `JdbcTemplate` with `JpaRepository` | The same aggregate read or written through both paths with different mappings, transaction semantics, or defaults |
| Duplication | Exam status checks, registration rules, grading arithmetic, role names, JSON field names, API paths, date parsing | Multiple copies that must change together, especially where one copy already differs |
| Complexity | Large services/repositories, deeply nested conditionals, long methods | A concrete defect risk or change hazard, not merely length |
| Dead code and stale artifacts | Unused imports, unreachable branches, obsolete comments, unused config, unused dependencies | Code or docs that contradict current behavior and could mislead a change |
| Frontend boundaries | `src/api`, `src/context`, `src/hooks`, `src/utils`, components | Components duplicating API/error/date logic; hooks leaking into unrelated surfaces; several owners for the same session state |
| Frontend state | `AuthContext`, `ToastContext`, local state, `localStorage` reads | Duplicated or contradictory auth state; components depending on storage keys they do not own |
| Conventions | Existing controller/service/repository, DTO, validation, test, naming, and formatting patterns | A new abstraction or pattern that conflicts with the repository without a concrete reason |
| Documentation | `README.md`, `architecture.md`, backend/frontend architecture files, comments | Stale claims that affect how a reviewer or maintainer would change the code |

Do not recommend a new layer, interface, base class, or design pattern merely
because it is familiar. Point to concrete duplication, coupling, or change risk.

## 2. Correctness

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Null and empty handling | Controllers, validators, mappers, DTOs, `Optional` use, entity getters | An unguarded null path reachable from a request or stored row |
| Parsing and validation | `RequestValueParser`, `ExamCreationValidator`, `QuestionValidator`, `ExamAttemptValidator`, controllers, Jackson settings | A malformed or boundary input reaching persistence as a valid value |
| Numeric boundaries | Marks, passing marks, duration, percentage, integer binding (`accept-float-as-int=false`) | Truncation, negative values, inconsistent totals, or a boundary that changes pass/fail |
| Date and time | `ExamRepositoryImpl`, `application.properties`, attempt expiry/finalizer, `dateUtils` | Timezone conversion, wall-clock versus instant confusion, DST/date-boundary behavior, or client/server clock disagreement |
| Exam lifecycle | `ExamServiceImpl`, `ExamStatusProjector`, `ExamStatusReconciler`, admin status updates | Illegal transition, stale status, or status derived differently in SQL and Java |
| Attempt lifecycle | Start/resume/submit paths, `ExamSubmissionServiceImpl`, `AttemptExpiryPolicy`, `ExpiredAttemptFinalizer` | Duplicate attempt, lost answer, repeated submission creating a second result, or expiry not finalizing once |
| Concurrency | `@Version` fields, `FOR UPDATE`, locking reads, Hikari/MySQL isolation, React timers | Lost update, stale validation, double submit, or a race that changes a grade |
| Transactions | `@Transactional` on services/repositories, `open-in-view=false`, lazy entity access | A multi-step write that can partially commit; lazy loads outside a transaction; read-only misused for writes |
| Resource handling | PDFBox streams, multipart uploads, JDBC resources, axios calls, intervals/timeouts | Leaked streams, unbounded memory/file size, or timers never cleared |
| Frontend async behavior | `useExamTimer`, `useAttemptAnswers`, `useExamAttempt`, axios interceptor | Auto-submit race, stale closure, duplicate save, answer loss, or error swallowed |
| Backward compatibility | DTO fields, routes, status codes, DB columns, migrations | A change that breaks an existing client, stored row, or deployed schema |

## 3. Security

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Token verification | `TokenService`, `TokenAuthenticationFilter`, `AuthInterceptor` | Missing signature/expiry/role checks, non-constant-time comparison, or acceptance of malformed input |
| Account state | `AuthService`, `AuthRepository`, token filter/interceptor | Deactivated users retaining access or authentication checks disagreeing |
| Public routes | `SecurityConfig`, `AuthInterceptor.PUBLIC_ROUTES`, actuator exposure | A route public in one layer but protected in another, or an accidentally public method/path |
| Authorization coverage | Every controller handler, `AuthGuard`, `ExamAuthorizationService`, `StudentAuthorizationService` | A protected operation without a guard, role-only checks where ownership is required, or client-supplied identity |
| Ownership identifiers | `AuthPrincipal`, `requireSelfOrStaff`, `requireSelfOrAdmin`, `requireSelfOrAdminForRoleData`, `requireExamAccess` | Comparing `userId` with a role-table id (or the reverse), or granting staff blanket access where the rule is ownership |
| Server authority | Exam create/update/status, registration, attempt start, answer save, submit, marks award | Client can set a score, status, time, student id, exam ownership, or another user's id |
| Rate limiting | `PublicEndpointRateLimiter`, `AuthInterceptor`, trusted-proxy setting | Spoofable `X-Forwarded-For`, unbounded memory growth, or a public write endpoint with no limit |
| CORS and CSRF | `WebConfig`, `CorsProperties`, `DeploymentConfigurationValidator`, profiles | Credentialed wildcard origin, unintended origin list, or CSRF assumptions that do not match cookie/token use |
| Security headers | `SecurityConfig` CSP/HSTS/frame/referrer settings | Headers disabled or broadened without a stated requirement |
| Secrets | `application*.properties`, `.env.example`, Dockerfile, CI, Vite variables | A real secret in the repository, a fallback used in production, or a server secret exposed through `VITE_*` |
| Input and output | SQL construction, PDF upload/preview, JSON errors, logs, actuator details | SQL injection, unsafe file handling, exception/message leakage, PII in logs, or verbose health details |
| Sensitive data | DTOs, `GlobalExceptionHandler`, logs, frontend storage, result endpoints | Another student's data or answer key exposed; token or password logged or rendered |
| Dependencies | `pom.xml`, `package.json`, `npm audit`, Maven dependency tree | Known vulnerable or unnecessary dependency, or a claimed audit that was not actually run |
| Browser session | `api/client.js`, `AuthContext`, `ProtectedRoute` | Token handling that conflicts with XSS assumptions, stale auth state after 401, or client-side role checks treated as enforcement |

## 4. Database and persistence

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Migration order | `V1` through `V8` and any new file | Duplicate/out-of-order versions, a modified applied migration, or a migration that assumes the wrong starting schema |
| Migration strategy | `spring.flyway.*`, `application-mysql-it.properties`, README database section | `baseline-on-migrate` used without the existing-schema precondition; destructive change without backup/recovery plan; no plan for rollback/forward fix |
| Entity/schema agreement | `persistence/entity`, `V1` and later migrations, `ddl-auto=validate` | Column type, nullability, length, table name, FK, unique constraint, or `@Version` mismatch that fails startup or hides drift |
| Schema source of truth | Flyway versus Hibernate versus H2 `create-drop` | H2 tests pass while MySQL would reject the schema; entity generation used as the real migration path |
| Constraints and indexes | Migrations, foreign keys, unique constraints, V5/V7 index migrations | Missing integrity constraint, duplicate-row path, or a query that cannot use an existing index |
| Transactions | Service and repository annotations, lock ordering, repeated reads | Partial writes, read-modify-write races, deadlocks, or checks performed outside the write transaction |
| SQL correctness | `JdbcTemplate` queries, native JPA queries, dynamic placeholders | Wrong join/filter, unbounded list, `COUNT`/`SUM` null handling, or parameter binding mistakes |
| Query efficiency | Repository list/detail/stat queries, `N+1`, `DATE()` predicates, subqueries | Unbounded scans, repeated queries per row, or filters that defeat indexes |
| Destructive operations | Exam deletion, user deletion, status changes, question deletion, migration data changes | Cascading loss of attempts/results, irreversible deletion, or missing approval/guard |
| Data integrity | Grading totals, result rows, registration uniqueness, attempt uniqueness | Duplicate results, totals that do not equal the stored answers, or orphaned rows |
| Time semantics | `DATETIME`/`TIME` columns, `LocalDateTime`, `CURDATE()`/`CURTIME()` | JVM and database time zones disagree or a value is converted through an instant when it should remain wall-clock |

## 5. API contracts and integration

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| HTTP semantics | Controllers, `ResponseEntity`, exception handlers | Wrong method or status, 200 for a failure, 500 for a client error, or inconsistent 404/409 behavior |
| Error contract | `GlobalExceptionHandler`, frontend `apiClient` | Response shape differs between handlers; frontend reads a field the server does not send; internal detail leaked |
| Request validation | Controller checks, validators, Jackson config | Missing/incorrect validation, unbounded values, or validation only on the client |
| Authorization | Each endpoint's guard, DTO scope | Data returned or mutated for the wrong role/owner, or authorization only in the UI |
| DTO exposure | `model/`, persistence entities returned from controllers | JPA entity serialization, password/token fields, lazy-loading failures, or internal fields exposed |
| Serialization | `@JsonProperty`, frontend normalization, date/number formats | snake_case/camelCase mismatch, `is_evaluated`-style field mismatch, float-to-int coercion, or null/omitted field incompatibility |
| Frontend compatibility | `src/api/*.js`, components, `VITE_API_URL`, CORS | Path/base URL mismatch, list normalization breaking, missing header, or error handling that assumes another shape |
| Pagination/filter/sort | List endpoints and frontend consumers | Unbounded list or a client sending unsupported parameters; do not invent a requirement if none exists |
| Files | Question-bank upload/preview/download | Authorization missing on one endpoint, size/type limits bypassed, or content-disposition/stream handling broken |
| Backward compatibility | Route aliases, field names, status codes, migrations | Existing client or stored data breaks without a migration/compatibility plan |

## 6. Testing and dependencies

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Existing coverage | `backend/src/test/java`, `frontend/src/**/*.test.*` | A high-risk behavior with no meaningful test, or a test that only asserts mocks were called |
| Test layers | Mockito units, MockMvc/`@SpringBootTest`, `@DataJpaTest`, Testcontainers MySQL, RTL | Security/DB behavior "tested" only with mocks that bypass the real filter, transaction, or schema |
| Auth matrix | `SecurityIntegrationTest`, controller tests, `AuthGuard` tests | Missing unauthenticated/forbidden/other-owner cases or a role/ownership path with no test |
| Data integrity tests | Attempt start/submit, grading, duplicate submit, optimistic locking, expiry | No test for the race/idempotency/rollback case that the code claims to handle |
| Migration tests | Flyway/H2/MySQL IT | No MySQL validation of migrations; H2-only confidence for MySQL-specific SQL |
| Frontend tests | hooks, utils, TakeExam, QuestionBankViewer | Timer, debounce, 401, render, or error-state behavior untested |
| Test quality | Arrange/act/assert, fixtures, fake timers, cleanup | Flaky time/async tests, over-broad mocks, or assertions that cannot fail for the right reason |
| Dependency health | `pom.xml`, `package-lock.json`, CI audit | Unused/duplicate dependency, deprecated API, unpinned build tool, or a known advisory not addressed |
| Reproducibility | Maven wrapper, lockfile, CI versions, Docker base images | Local/CI version drift or a build that needs undeclared services |
| Existing gates | `checkstyle.xml`, ESLint, JaCoCo, CI, pre-commit | A quality gate that does not cover the changed area or is weaker than the documentation claims |

## 7. Configuration and deployment

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Profile separation | `application.properties`, `application-dev.properties`, `application-prod.properties`, test properties | Production inheriting a development secret, origin, or verbose health setting |
| Required configuration | `DeploymentConfigurationValidator`, env examples, README | A missing value starts silently, or startup failure is undocumented |
| Secrets | Env templates, Dockerfile, CI, Vite env | Secret committed, logged, or bundled into the client |
| CORS | `cors.*`, `WebConfig`, validator, deployment origins | Over-broad origin, credential mismatch, or a local-only origin used in production |
| Observability | Actuator exposure, logging, exception handlers | Health detail exposed anonymously, missing startup diagnostics, or logs containing secrets/PII |
| Docker | `backend/Dockerfile` | Root runtime user, tests skipped unnoticed, production profile/port mismatch, missing required env behavior |
| CI/CD | `.github/workflows/ci.yml`, `scripts/pre-commit` | CI and local gates differ; a claimed check is not actually enforced; secrets or fork-PR exposure |
| Documentation commands | README, package scripts, Maven wrapper | Documented command does not run or does not match the current profile/secret requirement |

## Review anti-patterns

- Do not report "missing tests" without naming the behavior, risk, and a
  realistic test.
- Do not report "no pagination" unless the endpoint can return unbounded data
  and the impact is concrete.
- Do not flag a documented limitation as a security defect unless the review
  scope changes the requirement.
- Do not recommend a dependency upgrade solely because a newer version exists.
- Do not treat line count, comment density, or naming taste as a defect.
- Do not reproduce a command result from `report.md`, the README, or a previous
  conversation as current evidence.
