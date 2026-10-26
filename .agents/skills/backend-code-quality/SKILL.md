---
name: backend-code-quality
description: Audit and improve SkillCheckr's Spring Boot backend code quality, including security and authorization, JPA and JdbcTemplate persistence, SQL and Flyway migrations, transactions and concurrency, REST contracts, tests, configuration, and deployment. Use for backend-wide reviews, Spring feature or PR reviews, and scoped backend remediation; produce evidence-based findings by severity and do not edit code unless remediation is explicitly authorized.
---

# Backend Code Quality

Use this skill for a backend-wide quality review, a cross-cutting Spring change, or
a PR review of the SkillCheckr API. Use `repo-code-quality` when the review also
covers the React client or deployment end to end. Use `frontend-code-quality` for
client-only work, `module-reviewer` for one module, and `project-explorer` for
explanation only.

This skill is audit-first. The default output is a report, not code changes. Do
not modify application code, tests, configuration, dependencies, migrations, or
documentation unless the user explicitly authorizes remediation.

## Read first

1. `references/backend-baseline.md` for the verified backend snapshot, request
   pipeline, persistence model, commands, and known documentation drift.
2. `references/review-checklist.md` before evaluating a review area.
3. `references/report-template.md` before writing the report.

Re-verify the baseline against the working tree. It is a map, not authority.
`architecture.md`, `backend/architecture.md`, `WebConfig` comments, and old skills
have been stale about the backend; cite source behavior when they disagree.

## Operating rules

- Establish the current backend state before judging it. Read the actual
  `pom.xml`, profiles, security classes, schema, and tests rather than relying on
  documentation or previous reports.
- Trace each behavior through the real request path: security filter and
  interceptor, controller, service, validator, repository, JPA or `JdbcTemplate`,
  database constraints, and tests. Do not infer behavior from names or
  annotations alone.
- Verify a suspected issue against implementation, callers, configuration,
  schema, and tests. A pattern that merely looks unusual is not a defect.
- Treat architecture documents, comments, and test names as claims to check.
- Separate confirmed defects from risks, suggestions, style preferences, and
  deliberate design decisions.
- Prioritize authentication and object-level authorization, exam-attempt and
  grading integrity, data integrity, transaction/concurrency correctness,
  migration safety, and production readiness over style.
- Preserve the existing architecture: Spring MVC, constructor injection where
  present, custom repository interfaces with JDBC and JPA implementations,
  Flyway-owned schema, custom HMAC bearer tokens, the `AuthInterceptor` and
  `AuthGuard` layers, profile-based configuration, H2 unit tests, and opt-in
  MySQL integration tests.
- Do not recommend a full JPA rewrite, a JWT-library migration, a generic
  service/repository abstraction, or a new dependency without a concrete failure
  and a smaller alternative.
- Do not treat CSRF-disabled as a defect by itself. Determine whether
  authentication is bearer-header-only or cookie-based before evaluating CSRF.
- Server-derived identity is authoritative. Client-supplied user, student,
  teacher, owner, role, score, status, and timestamp values require explicit
  validation and authorization.
- Never recommend disabling or broadening authentication, authorization,
  validation, constraints, or logging safeguards. Never weaken a test to make a
  check pass.
- Treat Flyway migrations as production history. Do not edit an applied
  migration or recommend `ddl-auto=update/create` for a deployed environment.
- Never claim a test, build, check, scan, or runtime flow passed unless it was
  executed in this session and the output was inspected. Label environment
  limitations and manual checks.
- Do not stage or commit changes unless the user explicitly asks.

## 1. Establish scope and baseline

Determine the review mode before reading code:

- **Backend-wide**: review the API, prioritizing authentication and
  authorization, the exam-attempt/grading flow, persistence and migrations,
  REST contracts, and configuration. Sample lower-risk areas and follow
  evidence; do not claim every file was read.
- **Feature or module**: fix the boundary in the request, then trace the complete
  HTTP path, service workflow, repository queries, schema dependencies, and
  tests.
- **Diff or PR**: use `git diff <base>...HEAD`, then inspect affected callers,
  configuration, migrations, and tests. Do not attribute pre-existing issues to
  the change unless the change makes them reachable or worse.
- **Authorized remediation**: record the audit baseline first, then make small,
  approved changes with verification.

Start with:

```bash
git status --short
git branch --show-current
git log --oneline -10
git diff --stat <base>...HEAD   # diff/PR mode only
```

Read `backend/pom.xml`, `backend/checkstyle.xml`, `backend/Dockerfile`,
`backend/src/main/resources/application.properties`,
`application-dev.properties`, `application-prod.properties`, both test resource
property files, the Flyway migrations, `backend/architecture.md`, root
`architecture.md`, the root `package.json`, and `.github/workflows/ci.yml`.
Record exact declared versions, profiles, schema strategy, scripts, quality
gates, and environment requirements. CI uses Java 21 (Temurin) and runs
`./mvnw --batch-mode --no-transfer-progress verify` from `backend/`.

Do not treat `target/` as source.

## 2. Map the request and data path

For each review target, build a short map:

- **Security pipeline**: `SecurityConfig` filter chain, `TokenAuthenticationFilter`,
  `AuthInterceptor`, public-route allowlists, `AuthPrincipal`, `TokenService`,
  `AuthGuard`, and SpEL authorization beans.
- **HTTP boundary**: controller route, method, request DTO, validation, status
  codes, response DTO/entity, exception handling, CORS, and content type.
- **Business workflow**: service interface and implementation, validators,
  state transitions, idempotency, ownership checks, clock/time rules, and
  cross-service calls.
- **Persistence**: custom repository implementation, `JdbcTemplate` SQL or JPA
  repository call, entity mapping, transaction annotation, lock mode, generated
  keys, and schema constraint.
- **Async/background work**: schedulers, expiry sweeper, retry behavior, lock
  ordering, and shutdown/restart semantics.
- **Tests**: unit, MockMvc/`@SpringBootTest`, `@DataJpaTest`, Testcontainers or
  opt-in MySQL integration tests, and the exact behavior they assert.

Cross-layer flows such as exam creation and approval, registration, attempt
start/resume/answer-save/submit, grading, result release, and question-bank
upload/download need the complete map.

## 3. Review by risk order

Apply `references/review-checklist.md` selectively. Inspect the implementation,
its callers, configuration, schema, and tests for each area.

1. **Authentication, authorization, and secrets.** Token issuance and
   verification, active-account checks, public route parity, method security,
   ownership checks, `userId` versus role-table `roleId`, server-derived
   identity, CORS/CSRF assumptions, rate limiting, startup validation, and
   sensitive logging.
2. **Exam, attempt, and grading integrity.** State transitions, registration,
   uniqueness, frozen papers, answer ownership, answer-key release, marking
   bounds, totals, pass/fail status, idempotent submission, expiry
   finalization, and cross-role data exposure.
3. **Transactions and concurrency.** Transaction boundaries, read-only misuse,
   rollback behavior, locking reads, lock ordering, unique-key fallbacks,
   optimistic versions, lost updates, double submission, and deadlock/retry
   behavior.
4. **JPA and persistence correctness.** Entity/schema agreement, relationship
   fetch behavior, lazy loading with open-in-view disabled, N+1 queries,
   DTO/entity exposure, cascade/orphan behavior, generated identifiers, and
   consistency between JDBC and JPA writes to the same aggregate.
5. **SQL design and database constraints.** Parameterization, join/filter
   correctness, indexes, null semantics, bounded queries, pagination,
   uniqueness, foreign keys, cascades, bulk operations, and wall-clock versus
   instant time handling.
6. **Flyway and schema safety.** Migration ordering and naming, immutability,
   baseline assumptions, production DDL, `NOT NULL`/backfills, uniqueness and
   foreign-key validation, data preservation, transactional DDL, idempotency,
   repeatable migrations only where appropriate, rolling deployment, and MySQL
   verification.
7. **REST and API contracts.** HTTP methods and statuses, error shape, request
   validation, response DTOs, serialization, upload/download handling, list
   bounds, compatibility with `frontend/src/api/*`, and accidental exposure of
   entities or secrets.
8. **Configuration, tests, and maintainability.** Profile separation, required
   secrets, CORS, actuator, Docker/CI, test realism, migration coverage,
   dependency health, duplication, oversized units, and stale documentation.

## 4. Verify and classify findings

Keep a candidate list, then verify each item before reporting it. A finding is
reportable only when you can show:

- the exact file and line, or the class/method and configuration key when no
  stable line exists;
- the actual behavior and the caller/config/schema/test that demonstrates it;
- why it matters in this application, not just in Spring generally;
- the smallest practical fix that preserves existing conventions;
- the regression risk and the test that would prove the fix.

Exclude or downgrade:

- style-only preferences without a practical consequence;
- hypothetical defects with no reachable path;
- duplicated restatements of the same root cause;
- documented deliberate limitations unless the review scope reassesses them or
  a change makes them materially worse;
- recommendations that introduce a new architecture, dependency, or broad
  rewrite without a concrete failure;
- "missing tests" without naming the behavior, risk, and a realistic assertion.

Use these severities:

| Severity | Use for |
| --- | --- |
| Critical | Exploitable authentication/authorization bypass, severe data loss, forgeable credentials, or equivalent urgent risk |
| High | Significant security, ownership, grading, transaction, or data-integrity defect |
| Medium | Meaningful correctness, migration, API-contract, performance, configuration, or test gap |
| Low | Minor issue with a clear practical benefit |
| Informational | Optional improvement with no demonstrated defect |

State confidence separately. A confirmed defect can still be low confidence if
the evidence is incomplete; do not mark it confirmed in that case.

Backend categories: Architecture, Security-Authentication, Security-Authorization,
Exam-Attempt-Integrity, Transactions-Concurrency, JPA-Hibernate, SQL-Database,
Flyway-Migrations, API-Contract, Configuration-Deployment, Testing,
Documentation.

## 5. Report

Use `references/report-template.md` exactly. Keep the report factual and
concise:

1. Executive summary.
2. Backend architecture, request-pipeline, and persistence assessment.
3. Findings ordered by severity.
4. Security, authorization, and data-integrity risks.
5. Persistence and migration assessment.
6. Test and validation results, including commands not run.
7. Recommended remediation plan by priority and dependency.
8. Files changed, or "None — audit only".
9. Remaining risks and limitations.

Do not present a generic Spring or JPA best-practice checklist. Every nontrivial
recommendation must be tied to code evidence or a clearly stated risk in this
repository.

## 6. Remediation only when authorized

If the user authorizes fixes:

- Present the audit findings first, then change only the approved scope.
- Prefer small, reviewable changes that preserve existing controller, service,
  repository, security, DTO, validation, and test boundaries.
- Follow the current patterns for constructor injection, custom repositories,
  JDBC/JPA usage, `@Transactional`, `AuthGuard`/SpEL checks, Flyway, and tests.
- Do not perform broad rewrites, mass formatting, unrelated cleanup, or
  opportunistic dependency upgrades.
- Never weaken authentication, authorization, validation, constraints,
  transaction guarantees, or tests to make a check pass.
- Stop and ask for explicit approval before editing an applied migration,
  changing the Flyway baseline or production schema, performing destructive
  data changes, changing authentication/token or CORS semantics, or changing an
  API contract with significant compatibility impact.
- Run the narrowest relevant tests first, then the affected build and quality
  gates. Record the exact command, working directory, result, and skipped
  checks, including Docker/MySQL availability.
- Report files changed and verification results. Do not commit unless asked.

## Invocation examples

- `Use backend-code-quality to audit the whole backend; report findings only.`
- `Use backend-code-quality to review the exam-attempt and grading flow for authorization, transaction, and data-integrity defects.`
- `Use backend-code-quality to review the current branch against main; focus on JPA mappings, SQL, Flyway, and security.`
- `Use backend-code-quality to audit and then fix only the approved High findings; run the relevant tests and Maven checks.`
