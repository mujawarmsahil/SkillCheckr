# Backend Review Checklist

Use only the sections relevant to the scope. For every candidate finding, inspect
the implementation, caller, configuration, schema, and tests. Report a problem
only with evidence and a practical impact.

## 1. Authentication and secrets

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Token format | `TokenService` | A token can be forged, replayed past expiry, altered to change role/id, or accepted with a missing/malformed signature |
| Signature verification | `TokenService.sign`, `MessageDigest.isEqual`, prefix handling | Non-constant-time comparison, unverified prefix/payload, ambiguous parsing, or a fallback secret reachable outside dev |
| Token claims | `AuthPrincipal`, `TokenAuthenticationFilter` | `userId` and `roleId` are confused; a role-table id is trusted as an account id; a token outlives a deactivated account |
| Account state | `AuthServiceImpl.login`, `TokenAuthenticationFilter`, `AuthInterceptor`, `AuthRepository` | A disabled user retains access, a deleted profile still authenticates, or two layers disagree about active status |
| Login behavior | `AuthController.login`, `AuthServiceImpl` | User enumeration, inconsistent 401/403, password errors leaked, missing rate limit, or account lockout with no recovery |
| Password storage | `PasswordConfig`, registration/profile services, seed migration | Plaintext storage, a non-BCrypt path, unchecked rehash/upgrade, weak minimum length, or a password in logs |
| Public routes | `SecurityConfig`, `AuthInterceptor.PUBLIC_ROUTES`, actuator config | A write or sensitive endpoint is public in one layer, method/path matching differs, or a route is accidentally anonymous |
| Request identity | Controllers, `AuthGuard`, DTOs | A client-supplied user/student/teacher id is trusted instead of the verified principal |
| Rate limiting | `PublicEndpointRateLimiter`, `AuthInterceptor` | Spoofable `X-Forwarded-For`, unbounded memory, no limit on a public write, or a per-process claim for a multi-replica deployment |
| Secrets | `application*.properties`, Dockerfile, CI, `.env.example`, logs | A real secret is committed, a production fallback exists, a secret is logged, or startup accepts an unsafe secret |
| Session/CSRF | `SecurityConfig`, `WebConfig`, `api/client.js` | CSRF is judged without checking bearer/cookie transport; a cookie is introduced while CSRF stays disabled; CORS credentials are unnecessary |
| Security headers | `SecurityConfig` | CSP/HSTS/frame/referrer protection is removed or broadened without a stated requirement; report-only CSP is described as enforced |

Do not flag CSRF-disabled solely because the setting exists. The repository uses
an `Authorization` bearer header; a cookie-based change would alter the analysis.

## 2. Authorization and object ownership

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Endpoint coverage | Every controller handler, `@PreAuthorize`, `AuthGuard` | A mutation or sensitive read has no server-side authorization, or relies only on client UI |
| Role rules | `RoleConstants`, `@PreAuthorize`, `AuthGuard.requireRole` | Admin/teacher/student roles are conflated, a role name is case-sensitive in one path, or a staff role gets broader access than intended |
| Object ownership | `AuthGuard.requireSelfOrAdmin`, `requireSelfOrAdminForRoleData`, `requireExamAccess`, `ExamAuthorizationService`, `StudentAuthorizationService` | `user.user_id` is compared with a `student_id`/`teacher_id`, or a teacher can access another teacher's exam/result/attempt |
| IDOR/BOLA | Path/body ids for exams, students, attempts, results, requests, questions | A caller can change an id and read or mutate another user's data; ownership is checked on the wrong identifier |
| Server authority | Exam create/status/approval, registration, attempt start, answer save, submit, marks award | The client can set score, status, owner, student id, exam id, timestamps, or answer correctness |
| Role/profile consistency | `AuthController.login`, repository lookups | A user with no role profile gets a usable token, a role id points at another user's profile, or a stale role survives a profile change |
| Staff scope | `StudentAuthorizationService.canAccessStudentData`, `canAccessAttempt`, exam-scoped endpoints | A teacher can read or act on data outside exams they manage, especially where only role, not exam ownership, is checked |
| Public reads | Exam detail, questions, upcoming/completed, question-bank download, results | Registration/visibility rules are weaker than the flow claims; an answer key or private PDF reaches an unauthorized caller |
| Resource enumeration | Error responses and 403/404 choices | A caller can distinguish "not found" from "exists but forbidden" where that leaks private ids or data |
| Tests | `SecurityIntegrationTest`, controller tests, `TestAuth` | Tests bypass the real filter/interceptor or lack other-owner, wrong-role, inactive-user, and forged-token cases |

## 3. Exam, attempt, and grading integrity

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Exam lifecycle | `ExamServiceImpl`, `ExamStatusProjector`, `ExamStatusReconciler`, admin updates | Illegal transition, stale projected status, or Java/SQL disagreement about upcoming/completed/cancelled |
| Registration | `ExamServiceImpl.registerStudentForExam`, `ExamRepositoryImpl` | Duplicate registration, registration after start, unregistered student starts, or another student's id accepted |
| Attempt start/resume | `ExamServiceImpl.startAttempt`, `AttemptExpiryPolicy`, unique attempt key | Second concurrent attempt, expired attempt resumes, live attempt replaced, or wrong student/exam accepted |
| Answer save | `AttemptAnswerServiceImpl`, `ExamAttemptRepositoryImpl.findByIdForUpdate`, upsert repository | Answer saved after submit/expiry, question from another exam, selected answer from another question, lost concurrent update, or wrong word limit |
| Submission | `ExamSubmissionServiceImpl.submit`, `ResultRepositoryImpl`, finalizer | Double submit creates two results, partial commit, already-submitted attempt is re-scored, expired attempt is not finalized, or finalizer/user race produces duplicates |
| Grading | `gradeObjectiveAnswers`, `awardAnswerMarks`, `AnswerKeyReleasePolicy`, `QuestionBreakdownService` | Wrong marks, negative/over-max marks, pass/fail boundary error, pending evaluation overwritten, or MCQ manually graded |
| Paper freeze | `ExamServiceImpl.acceptExam`, `QuestionServiceImpl`, `paper_frozen`, row locks | Questions/answers can be changed after approval, or attempt start races with paper mutation |
| Answer-key release | Result/question DTOs, `AnswerKeyReleasePolicy` | The correct answer or per-question correctness is exposed before the release rule allows it |
| Result visibility | `ResultController`, result repository queries, teacher/admin paths | A student reads another student's result, a teacher reads another teacher's exam result, or a list endpoint leaks cross-exam data |
| Time semantics | `exam_date`, `start_time`, `expires_at`, `Clock`, `CURDATE()`/`CURTIME()` | JVM/MySQL zone mismatch, expiry checked only once, client time trusted, or DST/date-boundary disagreement |

## 4. Transactions and concurrency

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Transaction boundary | `@Transactional` on services/repositories, cross-service calls | A multi-step write can partially commit; a self-invoked method bypasses the proxy; a read-only method writes |
| Rollback | Exceptions and `noRollbackFor` | A checked/runtime exception leaves a result, attempt status, or marks half-written; a deliberate no-rollback path commits more than intended |
| Locking reads | `findByIdForUpdate`, `getExamByIdForUpdate`, `FOR UPDATE`, lock ordering | A check-then-write runs outside the lock, two transactions acquire rows in opposite order, or a read uses a stale snapshot |
| Unique-key fallback | attempt/result/question uniques | The code assumes a pre-check is atomic; a duplicate-key path is unhandled or hides a different constraint failure |
| Optimistic locking | `ExamEntity.version`, `ResultEntity.version`, JPA updates | A JDBC update ignores the version, a stale result overwrites a newer evaluation, or retry behavior is undefined |
| Lost updates | Answer upsert, marks update, exam status, result evaluation | Two writers overwrite each other without version/lock/conditional update |
| Deadlocks/timeouts | Lock order, Hikari, MySQL isolation, long transactions | Unbounded waits, lock held during I/O, or no handling for a deadlock/timeout on a reachable path |
| Idempotency | submit, finalizer, registration, answer save | Repeated requests create duplicate rows or return inconsistent state |
| Tests | MySQL concurrency tests, `ExamAttemptLockIntegrationTest`, `AttemptTransactionsMySqlIT` | Races are claimed but only mocked; H2 semantics are treated as MySQL proof |

## 5. JPA and Hibernate

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Mapping agreement | `persistence/entity`, V1-V8, `ddl-auto=validate` | Wrong table/column name, length, nullability, type, generated key, or enum/status representation |
| Relationships | Entity annotations, profile `@OneToOne`, scalar foreign-key columns | Wrong fetch type, missing optional flag, lazy relationship serialized outside a transaction, or relationship/cascade semantics that do not match the schema |
| N+1 and query shape | JPA repository methods, service loops, repository helpers | A per-row query in a list path, unbounded eager loading, or a query count that grows with result size |
| DTO projections | Service/repository return types, controllers | Entity graphs leak into JSON, internal/password fields are exposed, or a projection reads a lazy relation after the transaction |
| Cascade/orphan rules | `@OneToOne`, FKs without `ON DELETE`, repository delete order | A delete cascades farther than intended, leaves orphans, or violates a deliberate "deactivate instead of delete" rule |
| Identifiers | Generated keys, entity IDs, JDBC-only writes | A JPA save assumes an id that only the JDBC path populates, or a detached entity is treated as managed |
| Flush/consistency | Mixed JPA and `JdbcTemplate` repositories | A later JDBC query does not see a pending JPA write, or JPA overwrites a JDBC update |
| Transactions | `open-in-view=false`, service annotations | Lazy loading happens after the transaction; a repository call opens its own transaction unexpectedly |
| Pagination | List endpoints and repository queries | An unbounded result set is returned to the client; pagination is requested only if the data volume and contract make it real |
| Tests | `PersistenceConfigurationTest`, persistence integration tests | Only H2-generated schema is tested; MySQL entity validation is skipped |

## 6. SQL and database design

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Parameterization | All `JdbcTemplate` SQL, dynamic sort/filter clauses | User input concatenated into SQL, unsafe identifiers, or a wrong placeholder count |
| Correctness | Joins, filters, null comparisons, `COUNT`/`SUM`, `DATE()`, `LIMIT` | Wrong rows, null treated as zero, duplicate counts, or a query that differs from the Java rule |
| Index use | V5/V7 indexes, predicates, ordering, `FOR UPDATE` | A hot query filters/sorts on an unindexed column, or an index exists but the query shape defeats it |
| Constraints | Unique keys, foreign keys, NOT NULL, status/type columns | Application checks substitute for a missing integrity constraint, or an existing constraint is broader/narrower than the rule |
| Cascades | FK actions, repository delete order, soft deactivation | Deleting a subject/exam/user destroys attempts/results unexpectedly or fails on a referenced row |
| Bulk operations | Multi-row updates/deletes, `IN` lists, generated keys | Unbounded `IN`, partial bulk update, or no check that every intended row changed |
| Time zones | `DATETIME`/`TIME`, `serverTimezone`, `LocalDateTime`, `CURDATE()`/`CURTIME()` | Instant and wall-clock values are mixed, the driver converts a value unexpectedly, or JVM/MySQL zones disagree |
| Encoding/collation | V1 charset/collation, username/email uniqueness | Case/space variants bypass a uniqueness rule or comparisons differ between Java and SQL |
| Data preservation | Deletes, status changes, migrations | A destructive path has no backup/approval/recovery plan or removes history that results depend on |
| Tests | MySQL IT, migration tests, repository tests | SQL is tested only against H2 or only with mocks |

## 7. Flyway and schema safety

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Naming/order | `db/migration/V*.sql`, Flyway history | Duplicate/out-of-order versions, non-deterministic ordering, or a new migration that assumes the wrong starting schema |
| Flyway/MySQL compatibility | `pom.xml`, deployed MySQL version, migration SQL | A migration relies on syntax or behavior unsupported by the deployed MySQL/Flyway version, or a dependency upgrade changes migration behavior |
| Immutability | Git history and applied migrations | An applied migration is edited, checksum-breaking changes are planned, or a bad migration has no forward fix |
| Baseline | `spring.flyway.baseline-on-migrate`, `baseline-version`, mysql-it config | A fresh/unknown schema baselines at V1 without the required V1 tables; an existing installation skips a needed migration |
| Production DDL | New columns/tables/indexes/constraints | `ddl-auto=update/create` is proposed, destructive DDL is unguarded, or a schema change requires downtime without acknowledgment |
| NOT NULL/backfill | V6 and new migrations | Adding NOT NULL without a deterministic backfill for existing rows, or a backfill that can overwrite legitimate data |
| Uniqueness/FK validation | V6, V7, V8, data assumptions | A new unique/FK fails on existing duplicates/orphans and no preflight query is provided |
| Data preservation | Updates/deletes in migrations | A migration mutates production data irreversibly without a backup/rollback plan or a narrowly scoped predicate |
| Transactional DDL | MySQL behavior, multi-statement migrations | The migration assumes rollback across statements when MySQL would leave partial DDL |
| Idempotency/repeatable | Migration SQL, repeatable migrations | A migration is rerun or partially applied and fails on duplicate objects; repeatable migrations are used where a versioned change is required |
| Rolling deploy | New code + old schema and old code + new schema | Deploy order makes one version read/write a column or constraint it does not understand |
| Tests | `application-mysql-it.properties`, `*MySqlIT` | Only H2 `create-drop` is tested; no real Flyway MySQL validation for a migration change |

## 8. REST and API contracts

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| HTTP semantics | Controllers, `GlobalExceptionHandler` | Wrong method/status, 200 for a failure, 500 for a client error, inconsistent 401/403/404/409, or a missing `Location`/created status |
| Error contract | Exception handlers, `api/client.js` | Different JSON shapes for the same class of error, an internal exception message leaked, or the frontend reads a field the server omits |
| Request validation | `@Valid`, validators, `RequestValueParser`, Jackson config | Missing/incorrect validation, unbounded values, float-to-int coercion, or a client-only rule treated as authoritative |
| Response DTOs | Controllers returning `Exam`, `Student`, `Teacher`, `Subject`, `RegistrationRequest`, `ExamRegistration` | Entity serialization exposes internal fields, lazy loading fails, or a response shape is unstable |
| Serialization | `@JsonProperty`, camelCase/snake_case, dates, nulls | Frontend/backend mismatch, wall-clock timestamps without an explicit contract, or optional fields assumed present |
| Files | question-bank upload/download, multipart limits, PDFBox | Authorization differs between upload and download, type/size limits are bypassed, filename/content-disposition is unsafe, or a stream leaks |
| List bounds | result/admin/exam/question lists | Unbounded response or a client sends unsupported pagination/filter parameters; do not invent pagination without a real volume/contract need |
| Idempotency | submit/register/update endpoints | A retry creates duplicate state or returns a misleading success |
| Compatibility | Routes, statuses, field names, enums, migrations | A change breaks `frontend/src/api/*`, stored rows, or a deployed client without a compatibility plan |
| Tests | MockMvc/controller tests, frontend API consumers | Tests bypass the real security chain or assert a response shape the frontend does not use |

## 9. Configuration, dependencies, and deployment

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Profile separation | base/dev/prod/test properties | Production inherits a dev secret, loopback origin, verbose health, or H2 driver |
| Required values | `DeploymentConfigurationValidator`, README, env templates | Missing secret/origin starts silently in production or the failure is undocumented |
| Secrets | `application*.properties`, Dockerfile, CI, `.env.example`, logs | A secret is committed, logged, exposed in an error, or present in a client-visible variable |
| CORS | `cors.*`, `WebConfig`, validator, deployment origins | Wildcard with credentials, unintended origin, missing exposed header, or a change that breaks the hosted frontend |
| CSRF/session | `SecurityConfig`, token transport | Cookie/session behavior is introduced without revisiting CSRF; statelessness is claimed while a session is created |
| Actuator/observability | actuator config, health tests | Health detail or components are exposed anonymously in production; logs lack actionable startup/security context |
| Docker | `backend/Dockerfile`, profile, port, user | Root runtime, tests skipped without acknowledgment, missing prod profile, or wrong port/JVM settings |
| CI/pre-commit | `.github/workflows/ci.yml`, `scripts/pre-commit` | A changed area is outside the gate; local checks differ from CI; MySQL/JaCoCo/Checkstyle is claimed but not run |
| Dependencies | `pom.xml`, Maven dependency tree, version advisories | A known vulnerable or unnecessary dependency, an unpinned build tool, or an upgrade proposed without evidence |
| Reproducibility | Maven wrapper, Java version, MySQL/Docker requirements | Local/CI drift or a command that needs an undeclared service |

## 10. Testing and maintainability

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Security matrix | `SecurityIntegrationTest`, controller tests, `TestAuth` | No unauthenticated, wrong-role, other-owner, inactive-user, malformed-token, or public-route case |
| Persistence realism | H2 default, Testcontainers MySQL, `*MySqlIT` | Security/DB behavior is asserted only with mocks or H2, while MySQL constraints/migrations are untested |
| Concurrency tests | `ExamAttemptLockIntegrationTest`, `AttemptTransactionsMySqlIT` | A race is claimed but not exercised, or the test uses mocks that bypass the real transaction/lock |
| Migration tests | Flyway IT, `PersistenceConfigurationTest` | No real migration validation for a schema change; H2 `create-drop` is treated as equivalent |
| Test quality | Fixtures, cleanup, fake clocks, assertions | Flaky time/async tests, over-broad mocks, assertions that cannot fail for the right reason, or leaked state between tests |
| Coverage/gates | JaCoCo, Checkstyle, CI | A changed area is excluded or the threshold is misrepresented; coverage is claimed without running `verify` |
| Duplication | Status rules, ownership checks, date parsing, SQL joins, role names | Copies must change together, especially where one copy already differs |
| Complexity | Large services/repositories, long methods, nested branches | A concrete defect/change risk, not merely length |
| Dead/stale code | Unused imports, obsolete comments, unused config | Code or comments contradict the active security/persistence pipeline |
| Documentation | README, architecture files, comments | A stale claim affects how a maintainer changes auth, schema, or tests |

## Review anti-patterns

- Do not call an endpoint "authorized" because it has an annotation; verify the
  principal, ownership identifier, and reachable call path.
- Do not report IDOR/BOLA from an id in a path alone; show that the caller can
  supply another owner's id and that no effective ownership check rejects it.
- Do not flag `csrf().disable()` without establishing the credential transport.
- Do not recommend `ddl-auto=update/create` as a fix for schema drift.
- Do not recommend editing an applied migration; prefer a new forward migration.
- Do not treat a JPA annotation, a unique key, or a `FOR UPDATE` query as proof
  of correctness without tracing the transaction and lock order.
- Do not report "N+1" without a query path and a practical list size; do not
  demand pagination without a real unbounded endpoint and contract need.
- Do not report "missing tests" without naming the behavior, risk, and a
  realistic assertion.
- Do not treat line count, comment density, field injection, or naming taste as a
  defect without a concrete consequence.
- Do not present historical `report.md`, README, or architecture claims as
  current evidence.
