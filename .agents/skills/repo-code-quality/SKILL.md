---
name: repo-code-quality
description: Audit and improve this repository's Spring Boot/React code quality, security, persistence, API contracts, tests, and deployment configuration. Use for repository-wide reviews, cross-cutting changes, or PR quality reviews; produce evidence-based findings by severity and do not edit code unless remediation is explicitly authorized.
---

# Repository Code Quality

Use this skill for a repository-wide quality review, a cross-cutting change, or a
PR review of SkillCheckr. Use `module-reviewer` for one module and
`project-explorer` for explanation only.

This skill is audit-first. The default output is a report, not code changes. Do
not modify application code, tests, configuration, dependencies, migrations, or
documentation unless the user explicitly authorizes remediation.

## Read first

1. `references/repository-baseline.md` for the verified repository snapshot,
   commands, environment constraints, and known documentation drift.
2. `references/review-checklist.md` before evaluating a review area.
3. `references/report-template.md` before writing the report.

Re-verify the baseline against the working tree. It is a map, not authority.

## Operating rules

- Establish the repository's current state before judging it.
- Verify every suspected issue against implementation, callers, configuration,
  and tests. Do not report a defect because a pattern "looks wrong".
- Treat architecture documents and comments as claims to check. They can be
  stale. Cite source behavior when they disagree.
- Separate confirmed defects from risks, suggestions, style preferences, and
  documented limitations.
- Prioritize correctness, security, authorization, data integrity, and
  maintainability over cosmetic changes.
- Reuse existing abstractions and conventions. Do not recommend a rewrite,
  new layer, or new dependency without a concrete problem and a smaller
  alternative.
- Never claim a test, build, audit, scan, or command passed unless it was
  executed in this session and the output was inspected.
- Do not stage or commit changes unless the user explicitly asks.

## 1. Establish scope and baseline

Determine the review mode before reading code:

- **Whole repository**: review the full tree, prioritizing authorization,
  grading/attempt state, persistence and migrations, API contracts, and
  configuration. Sample lower-risk areas and follow evidence; do not pretend
  every file was read.
- **Diff or PR**: use `git diff <base>...HEAD`, then inspect affected callers,
  configuration, tests, and adjacent code. Do not attribute pre-existing issues
  to the change unless the change makes them reachable or worse.
- **Authorized remediation**: record the audit baseline first, then propose
  small, logically grouped changes and obtain approval for risky ones.

Start with:

```bash
git status --short
git branch --show-current
git log --oneline -10
git diff --stat <base>...HEAD   # diff/PR mode only
```

Read `README.md`, `architecture.md`, `backend/architecture.md`,
`frontend/architecture.md`, `backend/pom.xml`, `frontend/package.json`,
`backend/checkstyle.xml`, `frontend/eslint.config.js`, the CI workflow, and the
pre-commit script. Record exact versions, scripts, gates, and environment
requirements. Note which checks are feasible locally (Java 21, Node, MySQL,
Docker/Testcontainers).

## 2. Map the implementation

Build a short map of the paths relevant to the review:

- **Backend layers**: `controller/` → `service/` (interface plus `*Impl`) →
  `repository/` (custom interfaces and JDBC/JPA implementations). JPA entities
  and Spring Data repositories live in `persistence/`; they are used from some
  custom repository implementations. Cross-cutting code is in `security/`,
  `config/`, `validation/`, `exception/`, `mapper/`, and `constant/`.
- **Persistence**: Flyway SQL in `backend/src/main/resources/db/migration/` is
  the schema source of truth. Runtime uses `ddl-auto=validate`; H2 tests use
  `create-drop` with Flyway disabled; MySQL integration tests use Flyway plus
  schema validation.
- **Security**: a custom HMAC bearer-token implementation, a Spring Security
  filter chain, a servlet `AuthInterceptor` over `/api/**`, and controller-side
  `AuthGuard` checks all participate. Public routes are declared in more than
  one place; verify they agree.
- **API and DTOs**: controllers own HTTP status/response selection; domain rules
  belong in services/validators. Some DTOs serialize snake_case
  `@JsonProperty` names and are normalized at the frontend API boundary.
- **Frontend**: `src/api/` is the HTTP boundary; `src/context/` owns session and
  toast state; `src/hooks/` owns exam-attempt behavior; components call API
  modules or, in some screens, the shared axios client directly.
- **Cross-layer behavior**: exam lifecycle, registration, attempt start/resume/
  submit, grading, result access, and file download/preview cross both layers
  and the database.

For every important path, follow the call chain rather than inferring behavior
from names.

## 3. Review by risk order

Apply `references/review-checklist.md` selectively. Inspect the implementation,
its callers, configuration, and tests for each area.

1. **Authentication and authorization.** Token verification, public-route
   allowlists, `AuthGuard` coverage, object ownership (`userId` versus `roleId`),
   server-derived identity, and role/ownership tests.
2. **Exam, attempt, and grading integrity.** State transitions, idempotency,
   concurrent start/submit, optimistic locking, marking arithmetic, result
   status, and access to another student's or teacher's data.
3. **Persistence and migrations.** Flyway ordering and immutability,
   entity/schema agreement, transaction boundaries, locking, constraints,
   indexes, destructive operations, and H2-versus-MySQL differences.
4. **API contracts and frontend integration.** Methods, status codes, error
   shape, DTOs, serialization, request validation, backward compatibility, and
   frontend consumers.
5. **Configuration and deployment.** Profiles, required secrets, CORS,
   actuator exposure, logging, startup validation, Docker, and CI/pre-commit
   parity.
6. **Maintainability and tests.** Duplication, oversized units, dead code,
   stale comments/docs, dependency health, and high-risk test gaps.

## 4. Verify and classify findings

Keep a candidate list, then verify each item before reporting it. A finding is
reportable only when you can show:

- the exact file and line (or class/method when no stable line exists);
- the actual behavior and the caller/config/test that demonstrates it;
- why it matters in this application, not just in general;
- the smallest practical fix;
- the regression risk and the test that would prove the fix.

Exclude or downgrade:

- style-only preferences without a practical consequence;
- hypothetical defects with no reachable path;
- duplicated restatements of the same root cause;
- documented deliberate limitations unless the review scope reassesses them;
- recommendations that conflict with the existing architecture without a
  concrete failure.

Use these severities:

| Severity | Use for |
| --- | --- |
| Critical | Exploitable vulnerability, severe data loss, or equivalent urgent risk |
| High | Significant security, authorization, data-integrity, or correctness defect |
| Medium | Meaningful maintainability, reliability, performance, or test gap |
| Low | Minor issue with a clear practical benefit |
| Informational | Optional improvement with no demonstrated defect |

State confidence separately. A confirmed defect can still be low confidence if
the evidence is incomplete; do not mark it confirmed in that case.

## 5. Report

Use `references/report-template.md` exactly. Keep the report factual and
concise:

1. Executive summary.
2. Architecture and quality assessment.
3. Findings ordered by severity.
4. Security and data-integrity risks.
5. Test and validation results, including commands not run.
6. Recommended remediation plan by priority and dependency.
7. Files changed, or "None — audit only".
8. Remaining risks and limitations.

Do not present a generic best-practice checklist. Every nontrivial recommendation
must be tied to code evidence or a clearly stated risk in this repository.

## 6. Remediation only when authorized

If the user authorizes fixes:

- Present the audit findings first, then change only the approved scope.
- Prefer small, reviewable, logically grouped changes that preserve behavior.
- Follow the existing controller/service/repository, DTO, validation, security,
  and test conventions. Do not introduce parallel abstractions.
- Do not perform broad rewrites, mass formatting, unrelated cleanup, or
  opportunistic dependency upgrades.
- Never weaken security, authorization, validation, tests, or database
  constraints to make a check pass.
- Stop and ask for explicit approval before destructive database changes,
  editing an already-applied migration, changing the Flyway baseline strategy,
  or making API/configuration changes with significant compatibility impact.
- Run the narrowest relevant tests first, then the affected build/lint gates.
  Record the exact command, working directory, result, and any skipped checks.
- Report files changed and verification results. Do not commit unless asked.

## Invocation examples

- `Use repo-code-quality to audit the whole repository; report findings only.`
- `Use repo-code-quality to review the current branch against main; focus on authorization and data integrity.`
- `Use repo-code-quality to audit and then fix only the approved High findings; run the relevant tests.`
