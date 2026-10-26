---
name: frontend-code-quality
description: Audit and improve SkillCheckr's React/Vite frontend code quality, including hooks, state and session handling, API integration, accessibility, performance, and tests. Use for frontend-wide reviews, React feature or PR reviews, and scoped frontend remediation; produce evidence-based findings by severity and do not edit code unless remediation is explicitly authorized.
---

# Frontend Code Quality

Use this skill for a frontend-wide quality review, a cross-cutting React change, or
a PR review of the SkillCheckr client. Use `repo-code-quality` when the review
must also cover Spring Boot, persistence, deployment, or end-to-end contracts.
Use `module-reviewer` for one module and `project-explorer` for explanation only.

This skill is audit-first. The default output is a report, not code changes. Do
not modify application code, tests, configuration, dependencies, or documentation
unless the user explicitly authorizes remediation.

## Read first

1. `references/frontend-baseline.md` for the verified frontend snapshot, feature
   map, commands, and known documentation drift.
2. `references/review-checklist.md` before evaluating a review area.
3. `references/report-template.md` before writing the report.

Re-verify the baseline against the working tree. It is a map, not authority.
Two documents are known to lag the source: `frontend/architecture.md` and older
skills that reference a missing `docs/PROJECT_GUIDE.md`.

## Operating rules

- Establish the current frontend state before judging it.
- Trace each behavior through its route/entry, component, hook/context, API
  module, shared axios client, and test. Do not infer behavior from file names.
- Verify a suspected issue against implementation, callers, configuration, and
  tests. A pattern that merely looks unusual is not a defect.
- Treat architecture documents, comments, and test names as claims to check.
- Separate confirmed defects from risks, suggestions, style preferences, and
  documented limitations.
- Prioritize user-visible correctness, session/security behavior, answer and
  submission integrity, accessibility, and maintainability over cosmetics.
- Preserve the repository's existing conventions: React function components,
  React Context plus local state, plain JavaScript, CSS-via-Tailwind, axios,
  React Router, and Vitest plus React Testing Library. Do not recommend a new
  state library, form library, TypeScript migration, CSS framework, or test
  runner without a concrete problem and a smaller alternative.
- Client-side route checks and validation are user experience, not security
  enforcement. The backend remains authoritative for identity, registration,
  schedules, answer persistence, grading, and result visibility.
- Never claim a test, build, lint, audit, or runtime flow passed unless it was
  executed in this session and the output was inspected. Label manual checks as
  manual.
- Do not stage or commit changes unless the user explicitly asks.

## 1. Establish scope and baseline

Determine the review mode before reading code:

- **Frontend-wide**: review the client tree, prioritizing authentication and
  session behavior, the exam-attempt path, API boundaries, forms, shared state,
  and test coverage. Sample lower-risk presentational areas and follow evidence;
  do not claim every file was read.
- **Feature or module**: fix the boundary in the request, then trace the complete
  UI flow, its hooks/context, API calls, route guards, and tests.
- **Diff or PR**: use `git diff <base>...HEAD`, then inspect affected callers,
  hooks, shared state, API modules, route configuration, and tests. Do not
  attribute pre-existing issues to the change unless the change makes them
  reachable or worse.
- **Authorized remediation**: record the audit baseline first, then make small,
  approved changes with verification.

Start with:

```bash
git status --short
git branch --show-current
git log --oneline -10
git diff --stat <base>...HEAD   # diff/PR mode only
```

Read `frontend/package.json`, `frontend/vite.config.js`,
`frontend/vitest.config.js`, `frontend/eslint.config.js`,
`frontend/postcss.config.js`, `frontend/tailwind.config.js`,
`frontend/.env.example`, `frontend/src/main.jsx`, `frontend/architecture.md`,
the root `package.json`, and `.github/workflows/ci.yml`. Record exact declared
versions, scripts, Node version, API base URL behavior, test setup, and quality
gates. Note which checks are feasible locally. CI uses Node.js 22.12.0.

Do not treat `frontend/dist/` or `frontend/node_modules/` as source.

## 2. Map the feature execution path

For each review target, build a short map:

- **Entry**: `src/main.jsx` route, `App`/`Layout`, `DashboardPage` role tab, or a
  user action such as registration, submission, or profile update.
- **Render path**: route guard, role dashboard, active tab panel, modal, and
  conditional loading/error/empty/result views.
- **State**: `AuthContext` and `ToastContext`, local component state, refs,
  `localStorage` keys, router state, and server data.
- **Data**: API module function, direct `apiClient` call, endpoint, request
  shape, response normalization, error path, and server authority.
- **Side effects**: fetch on mount, timers, debounce, submit guards, navigation,
  object URLs, PDF preview/download, and cleanup.
- **Tests**: existing unit/integration tests, fixtures, mocks, and untested
  branches.

Follow the actual call chain rather than assuming that a hook or API module is
used everywhere. Some screens still call the shared axios client directly.

## 3. Review by risk order

Apply `references/review-checklist.md` selectively. Inspect the implementation,
its callers, configuration, and tests for each area.

1. **Authentication, session, and client security.** Login/signup, session
   rehydration, token attachment, 401 handling, logout cleanup, protected
   routes, role-driven navigation, direct storage reads, token exposure, unsafe
   rendering, and redirects. Confirm that client checks are described as UX and
   not as authorization.
2. **Exam attempt integrity.** Registration, schedule checks, start/resume,
   question loading, saved-answer restore, debounced and queued saves, flush
   before submit, auto-submit on expiry, duplicate-submit prevention, failed
   saves, result display, and answer-key release. This is the highest-risk
   frontend flow.
3. **API integration and contracts.** Shared client behavior, base URL,
   timeout, auth header, error normalization, response shape, snake_case versus
   camelCase handling, list normalization, direct client usage, multipart/blob
   responses, and 401/403/409/network behavior.
4. **React correctness and effects.** StrictMode double invocation, effect
   dependencies, stale closures, async races, unmounted state updates, timer and
   listener cleanup, refs used to break dependency cycles, controlled inputs,
   keys, and derived state.
5. **Forms and accessibility.** Labels, focus order, modal focus and Escape
   handling, keyboard operation, semantic buttons, loading/disabled states,
   error association, status announcements, image alternatives, and form
   validation feedback.
6. **Performance and rendering.** Unnecessary requests, render/fetch loops,
   polling cost, large lists and charts, image handling, bundle splitting, and
   memoization only where evidence shows a practical cost.
7. **Tests and maintainability.** Behavior-focused RTL tests, fake timers,
   async flushing, over-broad mocks, error/empty/loading states, auth and role
   transitions, duplication, convention drift, stale docs, and dependency
   health.

## 4. Verify and classify findings

Keep a candidate list, then verify each item before reporting it. A finding is
reportable only when you can show:

- the exact file and line (or component/hook when no stable line exists);
- the actual behavior and the caller/config/test that demonstrates it;
- why it matters in this application, not just in React generally;
- the smallest practical fix that preserves existing conventions;
- the regression risk and the test that would prove the fix.

Exclude or downgrade:

- style-only preferences without a practical consequence;
- hypothetical defects with no reachable path;
- duplicated restatements of the same root cause;
- documented deliberate limitations unless the review scope reassesses them or
  a change makes them materially worse;
- recommendations that introduce a new architecture or dependency without a
  concrete failure;
- "missing tests" without naming the behavior, risk, and a realistic test.

Use these severities:

| Severity | Use for |
| --- | --- |
| Critical | Exploitable client-side vulnerability, severe account/session compromise, or equivalent urgent risk |
| High | Significant auth/session, data-integrity, answer-loss, or user-blocking correctness defect |
| Medium | Meaningful accessibility, reliability, API-integration, performance, or test gap |
| Low | Minor issue with a clear practical benefit |
| Informational | Optional improvement with no demonstrated defect |

State confidence separately. A confirmed defect can still be low confidence if
the evidence is incomplete; do not mark it confirmed in that case.

Frontend-specific categories: Architecture-State, Authentication-Session,
Exam-Attempt-Correctness, API-Integration, React-Effects, Accessibility, Forms,
Performance, Security, Testing, Configuration-Dependencies, Documentation.

## 5. Report

Use `references/report-template.md` exactly. Keep the report factual and
concise:

1. Executive summary.
2. Frontend architecture and data-flow assessment.
3. Findings ordered by severity.
4. Authentication, session, and client-security risks.
5. Accessibility and performance assessment.
6. Test and validation results, including commands not run.
7. Recommended remediation plan by priority and dependency.
8. Files changed, or "None — audit only".
9. Remaining risks and limitations.

Do not present a generic React best-practice checklist. Every nontrivial
recommendation must be tied to code evidence or a clearly stated risk in this
repository.

## 6. Remediation only when authorized

If the user authorizes fixes:

- Present the audit findings first, then change only the approved scope.
- Prefer small, reviewable changes that preserve behavior and existing
  component/hook/API boundaries.
- Follow the current patterns for context, hooks, API modules, error toasts,
  Tailwind classes, and RTL tests. Do not introduce parallel abstractions.
- Do not perform broad rewrites, mass formatting, unrelated cleanup, or
  opportunistic dependency upgrades.
- Never weaken authentication, authorization, validation, answer persistence,
  submission guards, error handling, or tests to make a check pass.
- Stop and ask for explicit approval before changing token storage or session
  semantics, route-access rules, API contracts, state-management or form
  libraries, build/test configuration, or making changes with significant
  compatibility impact.
- Run the narrowest relevant tests first, then lint and build where feasible.
  Record the exact command, working directory, result, and skipped checks.
  Manual browser validation must be described as manual.
- Report files changed and verification results. Do not commit unless asked.

## Invocation examples

- `Use frontend-code-quality to audit the whole frontend; report findings only.`
- `Use frontend-code-quality to review the exam-attempt flow for answer loss and submission races.`
- `Use frontend-code-quality to review the current branch against main; focus on auth/session, accessibility, and tests.`
- `Use frontend-code-quality to audit and then fix only the approved High findings; run the relevant tests, lint, and build.`
