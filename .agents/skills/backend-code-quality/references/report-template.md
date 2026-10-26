# Backend Review Report Template

Use this structure for a backend-wide review or a scoped Spring/backend review.
Keep it factual and concise. Remove empty subsections rather than filling them
with boilerplate.

```markdown
# Backend Code Quality Review

## 1. Executive summary

- Scope: <whole backend | paths/modules | diff against <base> | PR #>
- Baseline: <branch, commit, date, Java/Spring/Maven versions when relevant>
- Mode: <audit only | audit plus approved remediation>
- Overall assessment: <2-4 sentences grounded in the inspected code>
- Highest-priority findings: <F-01, F-02, ... or "None confirmed">

## 2. Backend architecture, request-pipeline, and persistence assessment

Describe the backend as verified from source, not from stale docs:

- Relevant controllers, services, repositories, and dependency direction.
- Effective security pipeline: filter chain, token filter, interceptor,
  controller guards, method security, and public-route ownership.
- Identity model: `userId` versus role-table `roleId`, active-account checks,
  and how server-derived identity reaches services.
- Persistence model: JPA, `JdbcTemplate`, or both; where transactions, locking,
  and schema validation apply.
- Flyway strategy and the H2/Testcontainers/MySQL test split.
- API/frontend contract and serialization conventions.
- Concrete strengths and concrete concerns. Do not list generic principles.

## 3. Findings

### Summary

| ID | Severity | Category | Finding | Location |
| --- | --- | --- | --- | --- |
| F-01 | Critical/High/Medium/Low/Informational | <category> | <short description> | `path:line` |

Order rows by severity: Critical, High, Medium, Low, Informational. If a severity
has no findings, omit it or state "None confirmed".

Categories: Architecture, Security-Authentication, Security-Authorization,
Exam-Attempt-Integrity, Transactions-Concurrency, JPA-Hibernate, SQL-Database,
Flyway-Migrations, API-Contract, Configuration-Deployment, Testing,
Documentation.

### Finding details

Repeat this block for every finding. Merge duplicates with the same root cause.

#### F-01: <short title>

- **Severity and category:** <Critical/High/Medium/Low/Informational> —
  <category>
- **Nature:** <Confirmed defect | Risk | Suggestion>
- **Location:** `path/to/file.ext:line` (`Class.method` or handler)
- **Evidence:** <actual code behavior, caller/configuration/schema/test evidence.
  Include the smallest relevant excerpt or describe the reachable path. Do not
  paste large blocks.>
- **Why it matters:** <practical impact in this application: who is affected,
  what operation/data/contract is at risk, and under what conditions>
- **Recommended fix:** <smallest reasonable correction that fits existing
  conventions. If several options exist, name the preferred one and why.>
- **Regression risk:** <what could break; API, auth, schema, transaction, or test
  impact>
- **Required tests:** <specific test(s) to add or run; state the expected
  assertion, not just the test layer>
- **Confidence:** <high | medium | low> — <brief reason if not high>

## 4. Security, authorization, and data-integrity risks

Summarize without repeating full finding detail. Group by:

- Authentication, token/secret handling, active-account checks, and public
  routes.
- Role and object ownership: IDOR/BOLA, `userId` versus `roleId`, teacher/admin
  scope, and server-derived identity.
- Exam/attempt/grading integrity: registration, concurrency, idempotency,
  answer-key release, marks, and results.
- Persistence: transactions, locks, constraints, destructive operations, and
  data preservation.
- CORS/CSRF, rate limiting, actuator, logging, and deployment secrets.

If none are confirmed, state that explicitly and list residual risks that were
not verified. Do not create a finding from a generic checklist item.

## 5. Persistence and migration assessment

Report only concrete evidence:

- Entity/schema agreement and whether MySQL validation is actually exercised.
- JPA relationship/fetch behavior with `open-in-view=false`.
- SQL correctness, index use, constraints, and bounded query behavior.
- Locking/transaction boundaries and hybrid JDBC/JPA consistency.
- Flyway ordering, baseline assumptions, DDL/backfill/data-preservation risk,
  and rolling-deploy implications.
- Deliberate limitations that remain accepted.

Do not recommend `ddl-auto=update/create` for production or an edit to an applied
migration. If a migration change is proposed, name the approval, backup, and
forward-fix requirements.

## 6. Test and validation results

Report only what was actually executed in this session. Include the working
directory and the key result.

| Command | Working directory | Result | Notes |
| --- | --- | --- | --- |
| `<exact command>` | `<path>` | Passed/Failed/Not run | <test counts, warnings, skips, environment limits> |

Also state:

- Checks that were not run and why (for example, no Docker, MySQL, or required
  `TOKEN_SECRET`).
- Any MySQL/Testcontainers test that was skipped or disabled by environment.
- Test/build/coverage results taken from documentation or previous reports are
  **not** current evidence and must not be presented as such.
- Manual checks separately from automated checks; name the flow exercised.
- Coverage/static-analysis results only if the command actually produced them.

## 7. Recommended remediation plan

Order by priority and dependency. Keep steps independently reviewable.

1. <Immediate auth/authorization or grading/data-integrity fix and exact affected
   files>
2. <Transaction/concurrency fix and the lock/rollback/retry behavior it
   restores>
3. <JPA/SQL fix and the query or mapping it corrects>
4. <Flyway migration/backfill with backup, validation, and rolling-deploy
   prerequisites>
5. <API-contract fix and coordinated frontend consumers>
6. <Test additions that lock in the fixes>
7. <Configuration/deployment or maintainability cleanup, only if justified>

For each step, name the finding IDs it resolves and the verification command.
Call out approval gates for destructive database changes, applied-migration
edits, Flyway baseline changes, auth/token/CORS changes, or significant API
compatibility impact.

## 8. Files changed

- None — audit only.
or
- `path/to/file` — <change and finding ID>

## 9. Remaining risks and limitations

- Areas not inspected and why.
- Checks not run and the resulting uncertainty.
- Stale documentation or conflicting conventions that were not corrected.
- Deliberate limitations that remain accepted.
- Any follow-up that requires product, security, or architecture clarification.
```

## Rules for the report

- Do not report a finding without a location and evidence. If a stable line is
  unavailable, cite the class/method or exact configuration key.
- Do not turn stylistic preferences into defects. Use Low or Informational only
  when the practical benefit is clear.
- Do not duplicate one root cause across multiple findings. Cross-reference IDs.
- Do not claim a command passed, a suite passed, a migration applied, a scan was
  clean, or coverage increased unless the output was inspected.
- Do not include secrets, tokens, passwords, answer keys, or raw personal data
  in evidence.
- Do not include a generic Spring/JPA best-practice section. Recommendations must
  be specific to this repository.
- Do not describe a security annotation, a client check, or a database comment
  as proof of authorization without tracing the effective path.
- Do not recommend disabling CSRF, widening CORS, weakening a constraint, or
  switching `ddl-auto` to `update/create` as a convenience.
- If remediation was authorized, separate the pre-change findings from the
  post-change verification. Never present intended changes as completed.
