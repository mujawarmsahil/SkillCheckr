# Changed-Code Review Checklist

Apply only the sections triggered by the changed files. For every candidate
finding, inspect the complete diff, the surrounding implementation, affected
callers, configuration, schema, and tests. Report a problem only when the change
introduces it or makes it reachable/worse, and only with a concrete impact.

## 1. Change-set integrity

- Review staged, unstaged, and relevant untracked files separately; they may be
  different logical changes.
- Confirm the base branch and merge base. A branch diff excludes uncommitted
  work; do not merge the two scopes silently.
- Identify new, deleted, renamed, and moved files. Check imports, route tables,
  static resources, build inputs, CI paths, and documentation references.
- Check whether a deleted symbol, endpoint, configuration key, column, or file is
  still referenced.
- Check whether a new file is actually reachable: imported, registered, included
  in the build, covered by a test, or loaded by configuration.
- Review changed tests and test fixtures as part of the change, not as optional
  evidence.
- Review related non-diff files when the change depends on them: callers,
  interfaces, DTOs, error handlers, migrations, repositories, consumers, scripts,
  and environment examples.
- Inspect dependency and lockfile changes for the direct dependency change,
  lifecycle-script risk, unexpected packages, and consistency between manifests.
- Do not review generated or ignored output as source: `node_modules/`, `dist/`,
  `target/`, coverage reports, caches, and build artifacts.
- Separate unrelated changes. If they create risk or make the change hard to
  validate, report that; do not fold them into an unrelated root cause.
- Check for accidental secrets, credentials, tokens, private keys, database
  dumps, personal data, debug output, and temporary files. Report the location and
  type without reproducing the value.
- Check for incomplete work: TODOs, placeholders, commented-out logic, dead
  branches, debug endpoints, and code added "for later" without a caller.

## 2. General correctness and maintainability

- Logic errors: inverted conditions, wrong operators, off-by-one boundaries,
  incorrect boolean composition, and accidentally changed default behavior.
- Null, `undefined`, empty, missing, duplicate, and malformed inputs; nullable
  database columns; optional API fields; and type coercion.
- Boundary values, maximum sizes, pagination limits, numeric overflow, date/time
  boundaries, and empty collections.
- Unhandled or swallowed exceptions, partial success, misleading error messages,
  incorrect status mapping, and failures that look like successful responses.
- Async ordering, unawaited promises, fire-and-forget writes, races between
  requests or effects, retry behavior, and duplicate operations.
- Resource cleanup: subscriptions, timers, listeners, streams, temporary files,
  object URLs, locks, and transactions.
- Duplicated logic, dead code, unnecessary abstractions, copy-paste divergence,
  and inconsistent conventions where the difference affects behavior.
- Accidental API, route, configuration, permission, schema, or dependency changes.
- Unnecessary dependencies, generated files, broad imports, and dependency
  changes with no corresponding implementation need.
- Missing tests for new or modified behavior, especially negative, error, and
  boundary paths.
- Unrelated changes mixed into the same diff that make review or rollback risky.

## 3. React and Vite

- Component behavior and state consistency: stale state, derived state that can
  disagree with source data, conditional rendering gaps, and incorrect keys.
- Hook correctness: dependency arrays, stale closures, missing cleanup, effects
  that run on every render, `StrictMode` double invocation, and state updates
  after unmount.
- Async and timer behavior: overlapping requests, out-of-order responses,
  debounce/flush behavior, interval cleanup, and auto-submit or expiry races.
- Query keys, cache invalidation, and server-state synchronization where a cache
  layer is present. This repository currently uses axios plus Context/manual
  fetching; if a query library is introduced, inspect its keys and invalidation.
- REST request and response compatibility: endpoint, method, request fields,
  headers, response shape, status handling, snake_case/camelCase mapping, and
  null/optional fields. Check `frontend/src/api/client.js` and the relevant API
  module or direct client call.
- Loading, error, empty, disabled, and retry states. Confirm the UI cannot submit
  or render stale data while a request is in flight.
- Form validation, field-to-payload mapping, server-error display, disabled
  submit controls, duplicate submission prevention, and reset behavior.
- Authentication flows and storage: login/signup, token attachment, 401 cleanup,
  logout, protected routes, localStorage/sessionStorage contents, and accidental
  exposure of credentials or answer data. Client checks are UX, not authorization.
- Accessibility regressions in changed interactions: labels, focus, keyboard
  access, dialogs, `aria` state, error association, and status announcements.
- Performance regressions: render/fetch loops, repeated requests, unbounded list
  rendering, expensive work in render, timer leaks, and unnecessary bundle
  additions. Report only when the changed code creates a practical cost.
- Tests: behavior-focused React Testing Library coverage, fake timers, async
  flushing, role/auth transitions, and error/empty states.

## 4. Spring Boot, security, and REST

- Controller/service/repository responsibility: HTTP concerns stay at the
  boundary; business rules, validation, and transactions belong in the intended
  layer. Do not impose a layer rule the codebase does not use.
- Request validation and exception handling: `@Valid`, validators, malformed
  input, missing fields, unsupported values, `GlobalExceptionHandler`, status
  codes, and error response compatibility.
- Authentication changes: login, token issuance/verification, expiry, active
  account checks, password encoding, public-route allowlists, and filter versus
  interceptor behavior. In this repository, check `SecurityConfig`,
  `TokenAuthenticationFilter`, `AuthInterceptor`, and `TokenService` together.
- Authorization changes: method security, `AuthGuard`, controller checks, and
  object ownership. Verify server-derived identity, `userId` versus role-table
  `roleId`, cross-user resource access, teacher scope, admin scope, and IDOR/BOLA.
- Never accept a client-supplied owner, student, teacher, role, score, status,
  timestamp, or correctness value without validation and authorization.
- CSRF: do not flag CSRF-disabled by itself. Determine whether authentication is
  bearer-header-only or cookie/session-based; a new cookie flow changes the
  analysis.
- Transaction boundaries and concurrency: `@Transactional`, self-invocation,
  rollback rules, `noRollbackFor`, locking reads, lock ordering, unique-key
  fallbacks, optimistic versions, idempotency, and partial writes.
- Password, token, and session handling: BCrypt usage, HMAC signing and constant-
  time verification, token parsing, expiry, role validation, logout, rate
  limiting, and accidental logging of credentials.
- REST status codes and response compatibility: methods, 400/401/403/404/409/
  500 choices, created/updated semantics, response DTOs, entity exposure, field
  names, null handling, and frontend consumers.
- Sensitive data exposure: passwords, tokens, answer keys, personal data, stack
  traces, internal IDs, and debug output in responses or logs.
- Configuration changes: profiles, secrets, CORS, actuator exposure, startup
  validation, Docker, CI, and environment examples. Confirm the change does not
  make production depend on a development fallback.
- Relevant tests: unit, MockMvc/controller, security integration, repository,
  transaction, and MySQL integration tests. Check that tests use the real
  filter/interceptor and ownership path where those are the changed behavior.

## 5. JPA, SQL, and Flyway

- Entity mapping and relationship consistency: table/column names, types,
  lengths, nullability, generated keys, enums/status values, `@OneToOne` and other
  relationships, fetch behavior, cascades, orphan removal, and version fields.
- Entity-to-schema compatibility with the actual Flyway schema, not only with an
  H2 `create-drop` schema. With `open-in-view=false`, check lazy relationships
  before serialization and outside transaction boundaries.
- Query correctness: joins, filters, null comparisons, sorting, grouping, counts,
  time/date predicates, and agreement with the Java business rule.
- Parameter binding: use bound parameters for values; never concatenate
  user-controlled values or unsafe identifiers into SQL. Check dynamic sort and
  filter clauses specifically.
- Transaction, locking, and data integrity: `FOR UPDATE`, lock ordering,
  conditional updates, optimistic locking, unique constraints, idempotency, and
  retry/deadlock behavior. Verify that check-then-write paths are actually
  protected at the database boundary.
- Indexes and constraints: check new predicates, joins, sort columns, uniqueness,
  foreign keys, and `NOT NULL` requirements against V5/V7 and the changed query.
- Migration ordering and compatibility: new file version, dependency on earlier
  migrations, existing-data assumptions, baseline behavior, Flyway/MySQL
  compatibility, and rolling-deploy order.
- Unsafe operations: destructive deletes, drops, truncation, broad updates,
  missing `WHERE` predicates, and irreversible data changes. Flag missing backup,
  approval, or recovery plans where the change needs them.
- Backfills: deterministic handling of existing rows, `NOT NULL` additions,
  uniqueness/FK validation, chunking, idempotency, and data preservation.
- Unsafe migration edits: do not edit an applied migration or change a checksum;
  prefer a forward migration. Never recommend `ddl-auto=update/create` for a
  deployment where Flyway owns the schema.
- Migration tests and deployment implications: check whether a real MySQL/
  Testcontainers/Flyway path covers the change and whether old and new application
  versions can coexist during the deploy.
- Do not execute migrations, destructive SQL, or writes against a shared database
  during an ordinary review. Use only a disposable database when validation is
  appropriate.

## 6. API and cross-stack contracts

- Changed routes, methods, status codes, field names, enums, serialization, error
  shape, headers, and authentication requirements must match all consumers.
- Check `frontend/src/api/*`, direct `apiClient` calls, backend controllers, DTOs,
  and tests. Do not assume the frontend and backend were updated together.
- For schema changes, check entities, repositories, DTOs, migrations, seed data,
  and integration tests as one compatibility set.
- For file upload/download changes, check authorization, size/type limits,
  filename handling, content headers, streaming/cleanup, and frontend handling.
- For pagination or list changes, check bounds, default/max sizes, and whether the
  client contract actually supports the new parameters.
- For configuration changes, check base/dev/prod/test values, environment
  examples, Docker, CI, and documentation that describes the changed behavior.
- Preserve backward compatibility unless the developer has explicitly accepted a
  breaking change and coordinated all consumers.

## 7. Tests, dependencies, and configuration

- New or modified behavior has at least one assertion that fails without the
  change and passes with it.
- Security changes include unauthenticated, wrong-role, other-owner, inactive/
  expired-token, and public-route cases where applicable.
- Persistence changes include schema validation, constraint, transaction, and
  MySQL-specific coverage where H2 cannot prove the behavior.
- Frontend changes include loading, error, empty, duplicate-submit, and auth
  transition behavior where those states changed.
- Tests are not weakened, skipped, over-mocked, or made flaky to accommodate the
  change. A passing suite does not prove behavior it never asserts.
- Dependency changes are necessary, compatible with the declared runtime, and
  accompanied by the expected lockfile update. Avoid speculative upgrades.
- Check whether the change alters commands, environment variables, ports,
  generated files, or CI/pre-commit behavior that reviewers and deployments rely
  on.
- If documentation becomes materially wrong, name the exact section; do not make
  documentation cleanup the main subject of a code review.

## Repository map

Use this as a starting map, then verify the current tree.

- Frontend entry and shell: `frontend/src/App.jsx`, `frontend/src/main.jsx`,
  `frontend/src/pages/DashboardPage.jsx`.
- Frontend state and behavior: `frontend/src/context/AuthContext.jsx`,
  `frontend/src/context/ToastContext.jsx`, `frontend/src/hooks/*`.
- Frontend data boundary: `frontend/src/api/client.js`, `attemptApi.js`,
  `examApi.js`, `resultApi.js`, and direct `apiClient` calls.
- Frontend validation and tests: `frontend/src/utils/*`,
  `frontend/src/**/*.test.jsx`, `frontend/vitest.config.js`.
- Backend security: `backend/src/main/java/com/skillcheckr/security/`.
- Backend HTTP and business flow: `controller/`, `service/`, `validation/`,
  `exception/`, `mapper/`.
- Backend persistence: `repository/` (custom JDBC/JPA implementations),
  `persistence/entity/`, `persistence/repository/`.
- Schema: `backend/src/main/resources/db/migration/V*.sql`.
- Backend tests: `backend/src/test/java/com/skillcheckr/` including
  `security/`, `repository/`, `service/`, `support/TestAuth.java`, and
  `*MySqlIT` tests.
- Quality gates: `backend/pom.xml`, `frontend/package.json`,
  `.github/workflows/ci.yml`, `scripts/pre-commit`.

## Review anti-patterns

- Do not call an endpoint authorized because it has an annotation; trace the
  principal, ownership identifier, and reachable path.
- Do not report IDOR/BOLA from an id in a path alone; show that another owner's id
  is accepted without an effective check.
- Do not flag `csrf().disable()` without establishing the credential transport.
- Do not treat a JPA annotation, unique key, or `FOR UPDATE` as proof of
  correctness without tracing the transaction and lock order.
- Do not report N+1 or demand pagination without a query path, data volume, and
  practical consequence.
- Do not recommend `ddl-auto=update/create` as a schema fix or edit an applied
  migration casually.
- Do not report "missing tests" without naming the behavior, risk, and realistic
  assertion.
- Do not turn line count, comment density, field injection, or naming taste into a
  defect without a concrete effect.
- Do not report unrelated legacy issues as regressions from this change.
- Do not trust stale architecture documents, comments, test names, or previous
  reports as current evidence.
- Do not expose secrets or personal data while explaining a finding.
