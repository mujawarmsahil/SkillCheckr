# Review Report Template

Use this structure for a repository-wide review or a scoped/cross-cutting review.
Keep it factual and concise. Remove empty subsections rather than filling them
with boilerplate.

```markdown
# Repository Code Quality Review

## 1. Executive summary

- Scope: <whole repository | paths/modules | diff against <base> | PR #>
- Baseline: <branch, commit, date>
- Mode: <audit only | audit plus approved remediation>
- Overall assessment: <2-4 sentences grounded in the inspected code>
- Highest-priority findings: <F-01, F-02, ... or "None confirmed">

## 2. Architecture and quality assessment

Describe the current architecture as verified from source, not from stale docs:

- Relevant modules and dependency direction.
- Persistence approach in use (JPA, JdbcTemplate, or both) and where the
  transaction/schema boundaries are.
- Security boundary and how authorization is enforced.
- API/frontend integration points and serialization conventions.
- Existing conventions, quality gates, and test layers.
- Concrete strengths and concrete concerns. Do not list generic principles.

## 3. Findings

### Summary

| ID | Severity | Category | Finding | Location |
| --- | --- | --- | --- | --- |
| F-01 | Critical/High/Medium/Low/Informational | <category> | <short description> | `path:line` |

Order rows by severity: Critical, High, Medium, Low, Informational. If a severity
has no findings, omit it or state "None confirmed".

### Finding details

Repeat this block for every finding. Merge duplicates with the same root cause.

#### F-01: <short title>

- **Severity and category:** <Critical/High/Medium/Low/Informational> —
  <Architecture/Correctness/Security/Database-Persistence/API-Integration/
  Testing-Dependencies/Configuration-Deployment>
- **Nature:** <Confirmed defect | Risk | Suggestion>
- **Location:** `path/to/file.ext:line` (`Class.method` or handler)
- **Evidence:** <actual code behavior, caller/configuration/test evidence.
  Include the smallest relevant excerpt or describe the reachable path. Do not
  paste large blocks.>
- **Why it matters:** <practical impact in this application: who is affected,
  what data/operation/contract is at risk, and under what conditions>
- **Recommended fix:** <smallest reasonable correction that fits existing
  conventions. If several options exist, name the preferred one and why.>
- **Regression risk:** <what could break; compatibility or migration impact>
- **Required tests:** <specific test(s) to add or run; state the expected
  assertion, not just the test layer>
- **Confidence:** <high | medium | low> — <brief reason if not high>

## 4. Security and data-integrity risks

Summarize the security and data-integrity findings without repeating their full
detail. Group by:

- Authentication and authorization boundaries.
- Secrets and configuration defaults.
- Input/output handling and sensitive-data exposure.
- CORS, CSRF, session/token behavior.
- Persistence integrity, transactions, migrations, and destructive operations.

If none are confirmed, state that explicitly and list residual risks that were
not verified. Do not create a finding from a generic checklist item.

## 5. Test and validation results

Report only what was actually executed in this session. Include the working
directory and the key result.

| Command | Working directory | Result | Notes |
| --- | --- | --- | --- |
| `<exact command>` | `<path>` | Passed/Failed/Not run | <test counts, warnings, skips, environment limits> |

Also state:

- Checks that were not run and why (for example, Docker unavailable for
  Testcontainers MySQL).
- Any test/build result taken from documentation or previous reports is **not**
  current evidence and must not be presented as such.
- Coverage/static-analysis results only if the command actually produced them.

## 6. Recommended remediation plan

Order by priority and dependency. Keep steps independently reviewable.

1. <Immediate security/data-integrity fix and exact affected files>
2. <Correctness fix that depends on or follows the first step>
3. <Persistence/migration fix, with backup/rollback prerequisite>
4. <API/contract fix and coordinated frontend change>
5. <Test additions that lock in the fixes>
6. <Maintainability/documentation cleanup, only if justified>

For each step, name the finding IDs it resolves and the verification command.
Call out approval gates for destructive database changes, applied-migration
edits, Flyway baseline changes, or significant compatibility changes.

## 7. Files changed

- None — audit only.
or
- `path/to/file` — <change and finding ID>

## 8. Remaining risks and limitations

- Areas not inspected and why.
- Checks not run and the resulting uncertainty.
- Stale documentation or conflicting conventions that were not corrected.
- Deliberate limitations that remain accepted.
- Any follow-up that requires product or architecture clarification.
```

## Rules for the report

- Do not report a finding without a location and evidence. If a stable line is
  unavailable, cite the class/method or the exact configuration key.
- Do not turn stylistic preferences into defects. Use Low or Informational only
  when the practical benefit is clear.
- Do not duplicate one root cause across multiple findings. Cross-reference IDs.
- Do not claim a command passed, a suite passed, a scan was clean, or coverage
  increased unless the output was inspected.
- Do not include secrets, tokens, passwords, or raw personal data in evidence.
- Do not include a generic "best practices" section. Recommendations must be
  specific to this repository.
- If remediation was authorized, separate the pre-change findings from the
  post-change verification. Never present intended changes as completed.
