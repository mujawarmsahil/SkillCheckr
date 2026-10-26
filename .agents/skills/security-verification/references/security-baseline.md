# Security Baseline

Observed on 2026-10-26 from the working tree at `main`. This is an orientation
snapshot, not a guarantee or a compliance statement. Re-verify paths, versions,
configuration, and behavior before relying on any item here.

## Stack and layout

- React 19 with Vite 6, axios, and React Router 7.
- Spring Boot 3.4.4 on Java 21 with Spring MVC, Spring Security, Spring Data JPA,
  `JdbcTemplate`, Validation, Actuator, Flyway, MySQL, and PDFBox.
- MySQL 8.x at runtime. Unit/context tests use H2 in MySQL mode; MySQL
  integration tests use Flyway plus a disposable database or Testcontainers.
- Schema source of truth: `backend/src/main/resources/db/migration/V1` through
  `V8`. Runtime uses `spring.jpa.hibernate.ddl-auto=validate` and
  `spring.jpa.open-in-view=false`.
- Backend layers: `controller/` → `service/` → `repository/` with JPA entities
  and Spring Data repositories under `persistence/`. Security code is in
  `security/`; cross-cutting configuration is in `config/`, `validation/`, and
  `exception/`.
- Frontend HTTP boundary: `frontend/src/api/`; session state:
  `frontend/src/context/AuthContext.jsx`; shared client:
  `frontend/src/api/client.js`.

## Effective request pipeline

On an API request, the relevant checks are:

1. `WebConfig` registers a highest-precedence `CorsFilter` for `/**`.
2. `SecurityConfig` applies a stateless Spring Security filter chain.
3. `TokenAuthenticationFilter` attempts bearer-token authentication and publishes
   `AuthPrincipal` when the token is valid and the account is active.
4. Spring Security authorizes the URL; protected routes require authentication.
5. `AuthInterceptor` runs on `/api/**`, rejects missing/invalid tokens, applies
   rate limiting to public API routes, and republishes the principal.
6. `@PreAuthorize` method security runs on annotated controller methods.
7. Controller `AuthGuard` calls enforce role, self, role-data, and exam ownership
   checks.
8. Service and repository code enforce lifecycle, transaction, and query scope.

Do not evaluate one layer in isolation. Some routes have both `@PreAuthorize`
and a controller guard; the effective decision is the combination. Verify that
method-security SpEL resolves the intended bean and parameter names.

## Spring Security configuration

`backend/src/main/java/com/skillcheckr/security/SecurityConfig.java`:

- CSRF disabled, sessions `STATELESS`, method security enabled.
- `OPTIONS /**` permitted; `POST /api/auth/login` and `POST /api/requests`
  permitted; `/actuator/health`, `/actuator/health/**`, and `/actuator/info`
  permitted; all other requests authenticated.
- Response headers: `X-Content-Type-Options: nosniff`, frame denial,
  `Referrer-Policy: no-referrer`, HSTS for one year with subdomains, and a
  **report-only** CSP (`default-src 'self'`; scripts self; styles self plus
  `unsafe-inline`; images self/data; connect self). Report-only CSP does not
  block violations.
- JSON 401/403 handlers via `SecurityExceptionHandler`.

`TokenAuthenticationFilter` accepts a valid bearer token, verifies that the
account is active, creates a `ROLE_<ROLE>` authority, and sets the principal on
the request and security context. It does not reject the request itself when
the token is absent; the filter chain and interceptor do that.

## Public routes and interceptor behavior

`backend/src/main/java/com/skillcheckr/security/AuthInterceptor.java` has a
second public-route allowlist. At this snapshot:

- `POST /api/auth/login`
- `POST /api/requests`
- `/actuator/health`
- `/actuator/health/**`
- `/actuator/info`

The interceptor matches path and method, normalizes the context path, removes a
trailing slash, and compares non-wildcard paths case-insensitively. It permits all
`OPTIONS` requests. Public `/api/` routes pass through a bounded per-process
rate limiter. The limiter trusts `X-Forwarded-For` only when the immediate peer
is listed in `app.security.rate-limit.trusted-proxies`; the default list is
empty.

The Spring Security allowlist and the interceptor allowlist are separate. A
route missing from one but present in the other is a high-priority review
target. Do not assume they stay synchronized.

## Authentication and tokens

`backend/src/main/java/com/skillcheckr/security/TokenService.java`:

- Custom HMAC-SHA256 bearer token, not JWT.
- Format: `sc1.<base64url(payload)>.<base64url(signature)>`.
- Payload: `userId:roleId:role:issuedAt:expiresAt:username`.
- Verification checks prefix, segment count, Base64 decoding, constant-time
  signature equality, field count, positive user/issued/expiry values, known
  role, non-positive role ID, and expiry.
- Default TTL is 120 minutes. TTL is clamped to at least one minute.
- No token ID, refresh token, server-side revocation list, or logout endpoint was
  identified. The frontend logout clears local storage; it does not invalidate a
  copied token.
- The role and role ID are embedded in the token. Account status is re-read on
  each protected request through `AuthService.isUserActive`. A role change or
  password change therefore does not necessarily invalidate an existing token;
  verify the intended policy.
- If no secret is configured, `TokenService` generates an ephemeral random
  secret. `DeploymentConfigurationValidator` rejects that state outside
  development profiles.

`AuthServiceImpl.login` verifies BCrypt through the configured
`PasswordEncoder`, rejects an inactive account, and returns no token for invalid
credentials. A missing status is treated as active. Login is rate limited by
remote address. The login response includes profile data and a token; it does not
return the `User` object directly. If no role profile is linked, `roleId` may be
`0`, and the issued token is rejected by `TokenService`; this is fail-closed, not
a privilege grant.

At this snapshot the code only verifies BCrypt; tests explicitly reject legacy
plaintext passwords. This contradicts older architecture prose that claims
plaintext passwords are accepted and upgraded. Trust the code and tests.

`RegistrationRequestServiceImpl` hashes submitted registration passwords unless
the value already starts with `$2a$`, `$2b$`, or `$2y$`, in which case it stores
the value as-is. The public registration endpoint enforces 8–72 characters on the
submitted string. Verify whether accepting a pre-computed BCrypt value is
intentional and whether it can bypass the intended password policy.

## Authorization map at this snapshot

These are route groups, not a complete matrix. Rebuild the matrix from the
controllers for every review.

- **Anonymous**: `POST /api/auth/login`; `POST /api/requests`; actuator health
  and info; CORS preflight.
- **Any authenticated principal**: profile read/update only for self or admin;
  subject reads; exam visibility reads; upcoming/completed exam lists.
- **Admin**: `/api/admin/**`; registration-request list/status/reject/delete;
  subject creation/update/delete; exam approval/rejection/cancellation/status;
  `/api/results/all`; admin statistics.
- **Staff (admin or teacher)**: exam creation and staff exam list; question
  management; subject statistics.
- **Teacher ownership**: teacher-owned operations include exam management and
  deletion where guarded by `ExamAuthorizationService.canManage`, own-exam
  question-bank upload, own-exam submissions, evaluation, and manual marks.
  Exam approval, rejection, cancellation, and status changes are admin-only.
- **Student**: start/resume/submit own attempt; answer own attempt; register
  themselves for an exam; read their own registrations, results, and question
  bank when the exam is visible.
- **Self/admin account data**: `/api/auth/profile/{userId}` uses
  `AuthGuard.requireSelfOrAdmin` with `user.user_id`.
- **Self/admin role data**: result lists and registration lists use
  `AuthGuard.requireSelfOrAdminForRoleData` with `student_id`; teachers are not
  granted blanket access by that guard.

`ExamAuthorizationService.canManage` grants admin, or a teacher whose `roleId`
equals the exam's `teacherId`. `StudentAuthorizationService` grants admin and
teacher blanket access to student data and attempts, while some endpoints also
call `AuthGuard` methods that deny teachers. This discrepancy is a review hotspot:
trace the effective route behavior and the intended policy rather than reading
either layer alone.

`ResultController` allows a student to read only their own result, an admin to
read any, and a teacher only for the teacher's own exam. `/api/results/all` is
admin-only. Student result payloads are passed through `AnswerKeyReleasePolicy`,
which clears the question breakdown until the exam window closes.

## Sensitive data and exposure points

- `User` carries a `password` field and is converted from
  `UserAccountEntity`; avoid returning it directly or logging it.
- Registration list projections omit passwords; the approval path intentionally
  reads the stored hash to create the account. Preserve that distinction.
- Login and profile DTOs can contain name, email, contact, profile image, role,
  user ID, and role ID. Treat all as personal data.
- Exam, question, answer, attempt, and result objects can contain answer keys,
  correct-option text, marks, or other students' identities. Check each response
  projection and release policy.
- `GlobalExceptionHandler` maps unexpected exceptions to a fixed 500 message and
  logs the full cause server-side. Verify logs do not contain tokens, passwords,
  answer keys, or sensitive query values.
- The seed administrator in `V1__initial_schema.sql` is inactive and
  `V2__disable_known_admin.sql` re-disables the unchanged known-password account.
  Do not re-enable it or restore a repository-known password.

## Persistence and SQL

- Custom repositories use `JdbcTemplate` with `?` parameters and JPA repositories
  use named parameters. Dynamic `IN` clauses build placeholders from collection
  size, not from user-controlled SQL text.
- No confirmed SQL injection was found in the inspected repository paths at this
  snapshot, but the review is not exhaustive. Re-check all new queries, dynamic
  identifiers, sorting, `LIKE`, and raw native queries.
- Transactions cover attempt creation/submission, grading, deletion, approval, and
  profile updates. Verify locking and isolation for concurrent start/submit and
  duplicate approval.
- Flyway V1 intentionally omits foreign keys for `result.exam_id`,
  `result.student_id`, and `exam.teacher_id`; `result.attempt_id` is unique and
  `exam_attempt (exam_id, student_id)` is unique. Treat these as data-integrity
  assumptions to verify.
- Admin deletion deactivates a teacher with exams and a student with results or
  attempts; otherwise it hard-deletes the profile and account. There is no
  obvious audit trail for these actions at this snapshot; verify whether one is
  required.

## File handling

`QuestionBankService` enforces:
- explicit `questionOnlyConfirmed`;
- non-empty content and a 10 MB limit;
- a `.pdf` original filename;
- `%PDF-` magic bytes;
- readability through PDFBox, not encrypted, at least one page;
- exam mutation policy locking;
- a sanitized basename, falling back to `question-bank.pdf`.

`ExamController` requires authentication; students may download only for a
visible exam, staff must own the exam. Downloads use
`Content-Disposition: attachment`, `Cache-Control: private, no-store`,
`nosniff`, and `application/pdf`. The frontend creates a blob URL and embeds it
in an iframe. Verify size/decompression limits, parser resource use, filename
header handling, storage access, and whether generated PDFs contain answer keys.

## Frontend security posture

- The axios client reads `VITE_API_URL` or defaults to
  `http://localhost:8080`, attaches `Authorization: Bearer <token>` from
  `localStorage`, and on 401 removes token, role, and user ID.
- `AuthContext` stores token, role, user/role IDs, name, email, contact, and
  profile image in `localStorage`. Logout clears the main keys. Browser storage is
  readable by any successful XSS, and client-side route guards are not an
  authorization boundary.
- No `dangerouslySetInnerHTML`, `innerHTML`, or `eval` use was found in
  `frontend/src` at this snapshot. Re-check for untrusted URL construction,
  third-party embeds, and profile-image handling.
- The CSP is report-only and allows inline styles. Treat it as hardening, not an
  enforced XSS control.
- `VITE_*` values are compiled into the client bundle; they must never contain
  server secrets.

## Configuration and deployment

- Base configuration has no token fallback. `dev` supplies a committed local
  fallback; `prod` requires `TOKEN_SECRET`. The validator rejects missing/short
  secrets and the fallback outside `dev`/`local`/`test`, and treats mixed
  profiles such as `prod,dev` as production.
- CORS uses exact configured origins; credentials default to `true`. The
  validator rejects wildcard origins when credentials are enabled. `dev` adds
  loopback origins.
- Actuator exposes only health; health details/components are `never` in base and
  prod, `always` in dev. Health/info are public.
- `server.address` defaults to `0.0.0.0`; the app does not itself configure TLS.
  Confirm the platform terminates TLS and forwards correctly.
- Database URL defaults to local MySQL via environment overrides; password
  defaults to empty. Production must supply DB credentials and appropriate TLS
  flags.
- `backend/Dockerfile` builds with Maven/Temurin 21, runs as non-root `spring`,
  sets `SPRING_PROFILES_ACTIVE=prod`, and exposes 8080.
- CI runs frontend `npm ci`, `npm audit --audit-level=low`, lint, tests, and build;
  backend runs `./mvnw --batch-mode --no-transfer-progress verify`. No backend
  dependency-vulnerability scanner or DAST step was identified.
- `.env` files are ignored; `.env.example` is intended to be credential-free.
  Verify any new configuration or example does not add a real secret.

## Test coverage signals

Security-relevant tests observed:
- `TokenServiceTest` covers valid, missing/withdrawn Bearer prefix, wrong secret,
  tampering, malformed tokens, expiry, unknown role, zero role ID, ephemeral
  secret, and TTL.
- `AuthInterceptorTest` covers public route/method separation, missing/tampered/
  expired/unknown-role tokens, disabled accounts, principal publication, and
  per-address rate limits.
- `SecurityIntegrationTest` covers unauthenticated 401, student-to-admin 403,
  admin allowed, and CORS preflight.
- `AuthServiceImplTest` covers BCrypt success, plaintext rejection, inactive
  accounts, and password updates.
- Controller tests include many negative authorization cases for admin,
  registration, results, exams, questions, and subjects.
- No direct unit test for `StudentAuthorizationService` was found at this
  snapshot. The integration test does not exercise the full role/ownership
  matrix. Presence of a security test does not prove complete coverage; recount
  and inspect assertions before citing coverage.

## Documented limitations and open policy questions

Verify these against the current code and the developer's intended policy:
- rate limiting is per process and in-memory; multi-replica deployment needs
  ingress-level limits;
- no server-side token revocation, refresh, logout, or password-change
  invalidation was identified;
- no MFA, password-reset, or account-recovery flow was identified;
- no dedicated audit/security-event log was identified for admin actions;
- registration is public but account creation requires admin approval;
- teachers are granted broad access in `StudentAuthorizationService`, but some
  routes narrow it with controller guards;
- hard deletion is used when no academic history exists;
- CSP is report-only;
- security headers are emitted by the app but TLS termination is external.

## Review cautions and known documentation drift

- `backend/architecture.md` says legacy plaintext passwords are accepted and
  upgraded; `AuthServiceImpl` and `AuthServiceImplTest` show that plaintext is
  rejected. Do not repeat the prose claim.
- `architecture.md` and `backend/architecture.md` previously described
  JdbcTemplate-only persistence and no Spring Security filter chain; both are
  false at this snapshot.
- `WebConfig` retains a comment saying there is no Spring Security filter chain
  while `SecurityConfig` exists. Verify the effective pipeline from source.
- `report.md` contains historical results; do not use it as current evidence.
- Test counts and dependency versions drift. Re-run or recount before citing
  them.

## Safe commands

Use read-only inspection first:

```bash
git status --short
git branch --show-current
git log --oneline -10
rg -n "AuthGuard|PreAuthorize|permitAll|requestMatchers|PUBLIC_ROUTES" backend/src/main
rg -n "JdbcTemplate|@Query|query\\(|update\\(" backend/src/main/java/com/skillcheckr
```

Validation commands, only when the environment is safe and the check is needed:

```bash
cd backend && ./mvnw --batch-mode --no-transfer-progress test
cd backend && ./mvnw --batch-mode --no-transfer-progress verify
cd frontend && npm ci && npm run lint && npm test && npm run build
cd frontend && npm audit --audit-level=low
```

The MySQL integration profile and any live-server testing require a disposable
database and explicit authorization. Do not run Flyway or destructive SQL against
a shared database.
