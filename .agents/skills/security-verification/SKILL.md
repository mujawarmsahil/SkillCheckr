---
name: security-verification
description: Verify SkillCheckr's application security across the React client, Spring Boot API, authentication and authorization layers, persistence, file handling, configuration, and deployment; use for a repository-wide security audit, a focused security review of a feature or module, or verification of security controls, producing evidence-based findings by severity without modifying code unless remediation is explicitly authorized.
---

# Application Security Verification

Use this skill for a security-specific review of the SkillCheckr application or a
focused security-sensitive feature. It verifies what the code actually enforces,
not what the architecture documents or UI imply.

Use `repo-code-quality` for broad code-quality reviews, `backend-code-quality` or
`frontend-code-quality` for one stack, `git-diff-review` for a change set,
`module-reviewer` for one module, and `project-explorer` for explanation only.
Overlap is acceptable, but this skill owns the threat model, authorization matrix,
security evidence, and security-specific severity decisions.

The default mode is verification and reporting only. Do not modify application
code, tests, configuration, dependencies, migrations, documentation, or Git state
unless the developer explicitly authorizes remediation.

## Read first

1. `references/security-baseline.md` for the verified architecture, controls,
   known limitations, and review hotspots at the snapshot date.
2. `references/review-checklist.md` before evaluating each security area.
3. `references/report-template.md` before writing the report.

Re-verify the baseline against the working tree. It is a map, not authority.

## Non-negotiable rules

- Stay within the authorized scope. Do not attack external systems, production
  deployments, shared databases, third-party services, or accounts you do not
  own.
- Prefer source inspection and existing tests. Any dynamic testing must be
  isolated, authorized, and non-destructive. Do not brute-force, flood, fuzz
  without limits, upload malware, or attempt denial of service.
- Never run migrations, `flyway:clean`, destructive SQL, mass updates, or account
  deletion against a shared database.
- Do not disable, bypass, or weaken authentication, authorization, CORS, CSRF,
  validation, security headers, logging, tests, or database constraints in order
  to observe behavior.
- Do not print secrets, credentials, tokens, passwords, password hashes, answer
  keys, exam content, personal data, or private file contents. Report the
  location and type of exposure without reproducing the value.
- Do not invent endpoints, line numbers, runtime behavior, exploitability, or
  test results. If a path is unreachable or the environment is unavailable, say
  so and mark verification incomplete.
- Never claim a test, build, audit, scan, migration, or runtime check passed
  unless it was executed in this session and the output was inspected.
- Treat comments, architecture documents, README files, and test names as claims
  to verify. This repository has documented drift between prose and code.
- Do not report a generic best practice as a defect. Tie every finding to a
  reachable path, an asset, and a concrete impact in this application.
- Do not infer that a control is effective because it exists in one layer. Trace
  the effective request path through every applicable layer.
- Do not treat frontend route guards, hidden buttons, or `localStorage` role
  values as authorization controls.
- Do not treat the absence of a finding as proof that the application is secure.
  State the reviewed scope and unverified areas explicitly.

## 1. Establish scope and evidence baseline

Start from the repository root and record the review state:

```bash
git status --short
git branch --show-current
git log --oneline -10
```

Choose and state the mode:

- **Whole repository**: security review across the entire application.
- **Focused area**: one feature, module, endpoint family, or trust boundary.
- **Control verification**: authentication, authorization, file upload, secrets,
  dependency posture, and so on.
- **Post-remediation verification**: re-check specific findings and regression
  tests after an authorized fix.

Read the current manifests, runtime configuration, CI workflow, Dockerfile, and
security classes rather than relying only on the baseline. If the review covers
a change set, use `git-diff-review` as well and separate pre-existing issues from
issues introduced or exposed by the change.

Record:
- commit or working-tree state;
- active profiles and environment assumptions;
- database availability and whether it is disposable;
- commands that can safely run;
- paths and layers actually inspected;
- paths and layers excluded.

Do not start a review by running the application against a real database.

## 2. Build the threat model and attack surface

Use `references/security-baseline.md` as the starting map, then verify it.

Identify the assets:
- account credentials and password hashes;
- bearer tokens and the token-signing secret;
- registration requests, names, emails, contacts, and profile images;
- exam papers, question banks, answer options, answer keys, marks, and results;
- teacher and admin actions that change state;
- uploaded files and generated PDFs;
- database schema, backups, and deployment secrets.

Identify the actors:
- anonymous visitor;
- authenticated student;
- authenticated teacher;
- administrator;
- deactivated or role-changed account holder;
- attacker with a stolen token or a malicious browser origin;
- malicious or careless client sending crafted paths, bodies, headers, or files.

Identify the trust boundaries:
- browser ↔ API over HTTPS/TLS termination;
- client routes and local storage ↔ backend authorization;
- CORS/filter chain ↔ MVC interceptor ↔ method security ↔ controller guard;
- request path/query/body/header/file ↔ validation and persistence;
- application code ↔ MySQL, JdbcTemplate, JPA, and Flyway;
- uploaded PDF ↔ PDFBox and storage;
- source/build configuration ↔ CI, container, and runtime environment;
- application logs ↔ operators and log stores.

Build an endpoint table before judging individual findings. For each route,
record method, path, authentication, role, object ownership, request data,
response data, side effect, rate limit, and tests. Include public, admin, teacher,
student, actuator, file, and error paths. Verify the table against
`SecurityConfig`, `AuthInterceptor`, `@PreAuthorize`, controller guards, and the
service/repository call chain.

State the security invariants you expect in this application, for example:
- only the explicitly public routes are reachable anonymously;
- the server derives caller identity from the verified token, not from a body or
  path identifier;
- a student can access only their own attempts, registrations, profile, and
  results;
- a teacher can manage only their own exams and their submissions;
- admin-only operations remain admin-only through every route;
- answer keys and question breakdowns are not exposed before the exam window
  closes;
- passwords and secrets never appear in responses or logs;
- uploaded files are untrusted input;
- configuration fails closed when secrets are missing.

## 3. Verify authentication and session lifecycle

Inspect `TokenService`, `TokenAuthenticationFilter`, `AuthInterceptor`,
`SecurityConfig`, `AuthService`, `AuthRepository`, and the login controller.

Check:
- token format, signature verification, and constant-time comparison;
- rejection of malformed segments, invalid Base64, unknown prefixes, unknown
  roles, non-positive IDs, missing signatures, and expired tokens;
- expiry calculation and configured TTL boundaries;
- whether tokens are accepted only from the intended header format;
- whether the account's current status is checked on each request;
- whether role changes, password changes, logout, or account deletion revoke or
  invalidate existing tokens;
- whether a token without a linked role profile can authenticate;
- whether login responses leak account existence or account state beyond the
  intended policy;
- password hashing, BCrypt cost, password length limits, and registration/change
  password policy;
- rate limiting, client-IP resolution, proxy trust, per-process limits, and
  effects of multiple replicas;
- public-route parity between the filter chain and the MVC interceptor;
- path normalization, trailing slashes, case differences, encoded segments,
  `OPTIONS`, context paths, and actuator routes;
- seeded or default accounts and whether they are disabled.

Do not test credential stuffing, password spraying, or lockout behavior against
real accounts. Use existing tests or disposable local accounts.

## 4. Verify authorization and object ownership

Build and check the full authorization matrix. For every protected operation,
confirm all applicable layers agree:

1. route-level filter-chain authentication;
2. MVC interceptor authentication;
3. `@PreAuthorize` method security, if present;
4. controller `AuthGuard` checks;
5. service-level ownership or tenant checks;
6. repository query scoping.

Check explicitly:
- admin, teacher, student, and staff role gates;
- self-or-admin checks using `user.user_id`;
- self-or-admin checks for role data using `student.student_id` or
  `teacher.teacher_id`;
- teacher ownership of an exam;
- student ownership of an attempt, registration, and result;
- body/path identifiers that could override the authenticated principal;
- cross-exam access with a valid but unrelated teacher or student ID;
- role-ID collisions between different role tables;
- admin access to student/teacher data;
- teacher access to a student's list of results versus one exam-scoped result;
- unauthenticated access to actuator, file, error, and static routes;
- method-security parameter names and SpEL bean resolution;
- whether a broad annotation is later narrowed by a controller guard, or vice
  versa (report the effective behavior, not the annotation alone);
- whether internal/self-invoked calls bypass method security;
- 401 versus 403 versus 404 behavior and whether it discloses resource existence.

Test every privileged route with at least an unauthenticated request and the
wrong role where feasible. Prefer existing MockMvc tests or add tests only when
remediation is authorized.

## 5. Verify input, output, and injection defenses

Check:
- all SQL, JPQL, and native-query construction for parameter binding;
- dynamic `IN` clauses and sort/column names: only placeholders or fixed
  identifiers, never raw request strings;
- repository methods that accept IDs, search terms, statuses, or file names;
- mass assignment through DTOs and maps, especially `userId`, `roleId`, `role`,
  `status`, `teacherId`, `studentId`, `marks`, and file metadata;
- server-side override of ownership and lifecycle fields;
- validation of nulls, blanks, lengths, ranges, numeric overflow, fractions,
  duplicate JSON keys, and Unicode;
- path traversal, encoded traversal, case variants, and malformed multipart
  requests;
- exception mapping and whether errors disclose SQL, driver, stack, class,
  table, column, or secret information;
- reflected/stored XSS through profiles, exam names, question text, answer text,
  filenames, error messages, and PDF metadata;
- open redirects, unsafe `href`/`src` values, and untrusted HTML injection in the
  React client;
- logging of credentials, tokens, authorization headers, answer keys, or PII.

For file upload, verify size, extension, magic bytes, parser behavior, encrypted
or unreadable PDFs, page limits, storage authorization, filename sanitization,
content disposition, cache headers, and download authorization. Do not upload
real malware.

## 6. Verify sensitive data and business-logic security

Check:
- serialization of domain objects that contain passwords, password hashes,
  internal IDs, answer flags, or other users' data;
- registration list/detail projections and approval responses;
- profile reads and updates, including admin access to another account;
- student results before and after answer-key release;
- question payloads before an exam, during an attempt, and after completion;
- exam paper freezing and mutation after approval or start;
- attempt start/resume/submit behavior, one-attempt rules, expiry, idempotency,
  and concurrent requests;
- grading and manual marks changes, including negative or excessive marks;
- registration, unregistration, approval, rejection, deletion, and status
  transitions;
- deactivation versus hard deletion and retention of academic history;
- concurrency, transactions, locks, optimistic version fields, and duplicate
  operations;
- whether a lower-privileged user can cause a privileged side effect or observe
  its result.

Do not change data to test these paths unless the database and accounts are
disposable and the test is explicitly authorized.

## 7. Verify persistence, configuration, and deployment

Check:
- Flyway migration order, immutability, constraints, indexes, seed accounts, and
  whether migrations contain secrets or usable default credentials;
- schema constraints versus application assumptions;
- transaction boundaries and consistency across JdbcTemplate/JPA paths;
- database credentials, connection URL flags (`useSSL`, certificate validation,
  timezone), and production profile behavior;
- token-secret length, fallback secret, mixed profile handling, and startup
  failure behavior;
- CORS exact origins, credentials, wildcards, and loopback origins;
- CSRF rationale for header-based bearer tokens;
- security headers, CSP enforcement versus report-only mode, HSTS, framing, and
  content sniffing;
- actuator exposure and health detail;
- TLS termination, reverse-proxy trust, forwarded-header handling, and rate-limit
  IP resolution;
- Docker user, runtime profile, base image, exposed ports, and secrets passed at
  runtime;
- CI permissions, dependency audits, build artifacts, and whether security gates
  can fail silently;
- dependency versions and known vulnerabilities using the repository's available
  tooling; state if no scanner is configured;
- frontend `VITE_*` values for secret leakage and API base URL trust.

Do not run dependency scanners against arbitrary external services. Do not run
the MySQL integration suite against a shared database.

## 8. Validate findings

A reportable finding needs:
- exact location: file and line/range, or class/method/configuration key;
- the asset and trust boundary affected;
- the caller, role, request, and preconditions;
- the exact code path and effective control decision;
- a safe demonstration or test reference, or a clearly stated reason it was not
  executed;
- concrete impact in this application;
- the smallest practical remediation that preserves existing conventions;
- a regression test that would fail before the fix and pass after it;
- whether it is confirmed, likely, or unverified.

Distinguish:
- **Confirmed**: the vulnerable path is reachable and the behavior is
  demonstrated or unambiguous from code.
- **Likely**: code and callers strongly support it, but environment-dependent
  behavior could not be exercised.
- **Unverified risk**: a plausible weakness with an unresolved precondition.
  Keep it out of confirmed findings.

Drop or downgrade:
- style-only observations;
- defense-in-depth suggestions with no demonstrated impact;
- duplicate findings with the same root cause;
- issues in code that is not reachable;
- deliberate documented limitations unless the review explicitly reassesses
  them;
- claims based only on a comment, README, architecture document, or test name.

Use this severity scale unless the developer supplies a different one:

| Severity | Use for |
| --- | --- |
| Critical | Unauthenticated or trivially exploitable compromise of admin capability, mass sensitive-data exposure, destructive data loss, or equivalent urgent impact |
| High | Authentication or authorization bypass, privilege escalation, exploitable injection, exposure of answer keys or substantial PII, or a serious data-integrity flaw |
| Medium | Security defect with meaningful preconditions or limited scope, or a missing control with a demonstrated practical impact |
| Low | Narrow impact, difficult preconditions, or defense-in-depth issue with a concrete but limited consequence |
| Informational | Observation or optional hardening with no demonstrated vulnerability |

Report confidence separately. A confirmed defect can still have incomplete
environmental evidence; do not overstate exploitability.

## 9. Report

Use `references/report-template.md`. Lead with the outcome and reviewed scope,
then findings ordered by severity. Include a controls-verified table and an
authorization/coverage matrix so the reader can see what was checked and what was
not.

Do not include a generic security checklist, praise, or a claim that the
application is secure. Do not include raw secrets, tokens, password hashes,
answer keys, or personal data.

## 10. Remediation only when authorized

If the developer authorizes fixes:
- present the verification findings first;
- fix only the approved scope with small, reviewable changes;
- preserve the existing Spring Security, interceptor, method-security,
  controller-guard, service, and repository conventions;
- do not weaken tests, validation, CORS, CSRF, headers, or constraints to make a
  check pass;
- do not edit an applied Flyway migration without explicit approval;
- add a regression test for every security fix;
- run the narrowest relevant tests, then the affected build gates;
- record exact commands and results;
- report files changed separately from the pre-fix findings;
- do not commit unless explicitly asked.
