# Frontend Review Report Template

Use this structure for a frontend-wide review or a scoped frontend/cross-cutting
review. Keep it factual and concise. Remove empty subsections rather than filling
them with boilerplate.

```markdown
# Frontend Code Quality Review

## 1. Executive summary

- Scope: <whole frontend | paths/modules | diff against <base> | PR #>
- Baseline: <branch, commit, date, Node/package versions when relevant>
- Mode: <audit only | audit plus approved remediation>
- Overall assessment: <2-4 sentences grounded in the inspected code>
- Highest-priority findings: <F-01, F-02, ... or "None confirmed">

## 2. Frontend architecture and data-flow assessment

Describe the frontend as verified from source, not from stale docs:

- Entry points and route/provider ownership.
- Component → hook/context → API → backend flow for the reviewed feature.
- Session and local-state ownership, including localStorage keys.
- API boundary: modules, direct axios usage, normalization, and error handling.
- Exam-attempt path where relevant.
- Existing conventions, quality gates, and test layers.
- Concrete strengths and concrete concerns. Do not list generic principles.

## 3. Findings

### Summary

| ID | Severity | Category | Finding | Location |
| --- | --- | --- | --- | --- |
| F-01 | Critical/High/Medium/Low/Informational | <category> | <short description> | `path:line` |

Order rows by severity: Critical, High, Medium, Low, Informational. If a severity
has no findings, omit it or state "None confirmed".

Categories: Architecture-State, Authentication-Session,
Exam-Attempt-Correctness, API-Integration, React-Effects, Accessibility, Forms,
Performance, Security, Testing, Configuration-Dependencies, Documentation.

### Finding details

Repeat this block for every finding. Merge duplicates with the same root cause.

#### F-01: <short title>

- **Severity and category:** <Critical/High/Medium/Low/Informational> —
  <category>
- **Nature:** <Confirmed defect | Risk | Suggestion>
- **Location:** `path/to/file.ext:line` (`Component`, hook, or handler)
- **Evidence:** <actual code behavior, caller/config/test evidence. Include the
  smallest relevant excerpt or describe the reachable path. Do not paste large
  blocks.>
- **Why it matters:** <practical user/application impact: who is affected, what
  operation/data/session is at risk, and under what conditions>
- **Recommended fix:** <smallest reasonable correction that fits existing
  conventions. If several options exist, name the preferred one and why.>
- **Regression risk:** <what could break; session, API, routing, or test impact>
- **Required tests:** <specific test(s) to add or run; state the expected
  assertion, not just the test layer>
- **Confidence:** <high | medium | low> — <brief reason if not high>

## 4. Authentication, session, and client-security risks

Summarize without repeating full finding detail. Group by:

- Session creation, rehydration, 401 handling, and logout.
- Protected routes and role-driven navigation as UX versus server enforcement.
- Token/storage exposure, unsafe rendering, redirects, and sensitive data.
- Destructive actions and client-side trust boundaries.

If none are confirmed, state that explicitly and list residual risks that were
not verified. Do not create a finding from a generic checklist item.

## 5. Accessibility and performance assessment

Report only concrete evidence:

- Forms, labels, focus, keyboard, dialogs, status announcements, and responsive
  reachability.
- Bundle/request/render costs and any measured or clearly reachable user impact.
- Deliberate limitations that remain accepted.

Do not include a generic WCAG or performance best-practice list. Do not claim a
manual browser or audit check unless it was performed.

## 6. Test and validation results

Report only what was actually executed in this session. Include the working
directory and the key result.

| Command | Working directory | Result | Notes |
| --- | --- | --- | --- |
| `<exact command>` | `<path>` | Passed/Failed/Not run | <test counts, warnings, skips, environment limits> |

Also state:

- Checks that were not run and why (for example, no browser/backend available).
- Any test/build result taken from documentation or previous reports is **not**
  current evidence and must not be presented as such.
- Manual checks separately from automated checks; name the flow exercised.
- Coverage/static-analysis results only if the command actually produced them.

## 7. Recommended remediation plan

Order by priority and dependency. Keep steps independently reviewable.

1. <Immediate auth/session or answer-integrity fix and exact affected files>
2. <Correctness fix that depends on or follows the first step>
3. <API/contract or state-ownership fix and coordinated consumers>
4. <Accessibility fix with the specific interaction and verification>
5. <Performance fix only with evidence>
6. <Test additions that lock in the fixes>
7. <Maintainability/documentation cleanup, only if justified>

For each step, name the finding IDs it resolves and the verification command.
Call out approval gates for token/session storage changes, route-access changes,
API contract changes, new dependencies, or significant compatibility impact.

## 8. Files changed

- None — audit only.
or
- `path/to/file` — <change and finding ID>

## 9. Remaining risks and limitations

- Areas not inspected and why.
- Checks not run and the resulting uncertainty.
- Stale documentation or conflicting conventions that were not corrected.
- Deliberate limitations that remain accepted.
- Any follow-up that requires product or architecture clarification.
```

## Rules for the report

- Do not report a finding without a location and evidence. If a stable line is
  unavailable, cite the component, hook, or exact configuration key.
- Do not turn stylistic preferences into defects. Use Low or Informational only
  when the practical benefit is clear.
- Do not duplicate one root cause across multiple findings. Cross-reference IDs.
- Do not claim a command passed, a suite passed, an audit was clean, or a manual
  flow worked unless the output or observation was inspected.
- Do not include secrets, tokens, passwords, answer keys, or raw personal data
  in evidence.
- Do not include a generic React best-practice section. Recommendations must be
  specific to this repository.
- Do not describe client-side guards as security enforcement.
- If remediation was authorized, separate the pre-change findings from the
  post-change verification. Never present intended changes as completed.
