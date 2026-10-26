---
name: git-diff-review
description: Review the actual Git change set before commit or pull request by inspecting staged, unstaged, untracked, and base-branch diffs with affected callers, tests, migrations, and dependencies; use when asked to review current changes, a diff, staged changes, or a branch/PR, reporting only defects introduced or exposed by the changes with severity, evidence, and validation results.
---

# Git Diff Review

Use this skill when the developer asks to review current changes, a diff, staged
or unstaged work, or a branch/PR before commit. This is a changed-code review, not
a whole-repository audit. Use `frontend-code-quality`, `backend-code-quality`, or
`repo-code-quality` for broad audits; `module-reviewer` for one module;
`project-explorer` for explanation; and `commit-pr-generator` or `git-summary` for
commit/PR text.

The default mode is review only. Do not modify code, tests, configuration,
dependencies, migrations, documentation, or Git state unless the developer
explicitly authorizes fixes.

## Read first

1. `references/review-checklist.md` before evaluating the changed files.
2. `references/report-template.md` before writing the report.

Re-check the actual manifests and scripts. At authoring time this repository is a
React 19/Vite client and a Spring Boot 3.4.4/Java 21 API with Spring Data JPA plus
`JdbcTemplate`, custom HMAC bearer authentication, Flyway-owned MySQL schema, H2
unit tests, and opt-in/Testcontainers MySQL integration tests.

## Non-negotiable rules

- Establish the current Git state before running any review command.
- Treat Git as read-only. Never overwrite, reset, discard, stash, stage, commit,
  push, or revert the developer's work.
- Do not run `git add`, `git add -N`, `git apply`, `git checkout`, `git switch`,
  `git restore`, `git reset`, `git stash`, `git clean`, `git rm`, `git mv`,
  `git commit`, `git revert`, `git merge`, `git rebase`, `git cherry-pick`,
  `git push`, `git update-index`, or `gh pr checkout`.
- Do not fetch automatically or assume remote-tracking refs are current. Use the
  existing refs and report possible staleness; fetch only if the developer
  explicitly asks for an updated comparison.
- Do not assume all uncommitted changes belong to one commit or one logical
  change. Staged, unstaged, and untracked content can differ and may represent
  separate work.
- Inspect relevant untracked files even though ordinary Git diffs omit their
  contents. Do not use `git add -N` to expose them; read them directly.
- If the intended base branch is unclear, use the available branch/PR context or
  ask the developer. Never silently choose a base that produces a misleading
  comparison.
- Review the complete relevant diff and the surrounding implementation. Do not
  limit analysis to added lines when the change affects callers, contracts,
  configuration, schema, or tests elsewhere.
- Report only defects introduced or exposed by the change. Keep pre-existing
  issues out of the findings unless they block or materially affect the change.
- Do not execute database migrations, `flyway:clean`, destructive SQL, or writes
  against a shared database as part of a review.
- Do not invent line numbers, test results, runtime behavior, or requirements.
  If the evidence is incomplete, state the uncertainty or mark the review
  incomplete.
- Do not print secrets, credentials, tokens, passwords, answer keys, or personal
  data. Report the location and type of exposure without reproducing the value.
- Never claim a test, build, lint, scan, migration, or runtime check passed
  unless it was executed in this session and its output was inspected.
- Do not treat `dist/`, `target/`, `node_modules/`, coverage output, or other
  generated artifacts as source.

## 1. Establish the actual change set

Run from the repository root. Start with read-only context:

```bash
git rev-parse --show-toplevel
git status --porcelain=v2 --branch
git branch --show-current
git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}'
git symbolic-ref refs/remotes/origin/HEAD
git log --oneline --decorate -10
```

If a merge, rebase, cherry-pick, revert, or bisect is in progress, report it
before interpreting the index or working tree as a normal change set.

Choose the scope from the request:

- **Pre-commit / current work**: staged changes, unstaged changes, and relevant
  untracked files.
- **Branch or PR**: committed changes from the merge base to `HEAD`, plus any
  uncommitted changes reviewed separately.
- **Explicit range or paths**: use exactly what the developer requested and state
  what was excluded.
- **Multiple logical changes**: review each group separately; do not present them
  as one coherent commit.

Determine the intended base in this order:

1. The base explicitly named by the developer.
2. The base of a named PR, when `gh pr view <number> --json
   number,baseRefName,headRefName,baseRefOid,headRefOid,url` is available.
3. The current branch's upstream, if it is a meaningful comparison branch.
4. The remote default branch already known to Git.
5. Ask the developer if none of the above is unambiguous.

If the current branch is the default branch, a branch comparison may be empty;
use the working-tree scope unless the developer names another base. Do not fetch
to "fix" a base without permission; say that the local remote ref may be stale.

Capture the relevant diffs:

```bash
git --no-pager diff --no-ext-diff --no-color --find-renames
git --no-pager diff --no-ext-diff --no-color --cached --find-renames
git --no-pager diff --no-ext-diff --no-color --stat
git --no-pager diff --no-ext-diff --no-color --cached --stat
git --no-pager diff --no-ext-diff --no-color --find-renames <base>...HEAD
git --no-pager diff --no-ext-diff --no-color --name-status -M <base>...HEAD
git --no-pager diff --no-ext-diff --no-color --stat <base>...HEAD
git ls-files --others --exclude-standard
```

Interpret the commands correctly:

- `git diff` shows unstaged tracked changes only.
- `git diff --cached` shows staged changes only.
- Neither command includes untracked files.
- `<base>...HEAD` shows committed changes since the merge base and excludes
  uncommitted work. Review uncommitted work separately.
- `--name-status -M` exposes new, deleted, and renamed files. A rename can hide
  edits; compare the old and new content.
- Ignored files are outside the Git change set. Mention one only when it is
  relevant to the requested review, without treating it as tracked work.

For untracked files, read the complete file. If a diff view is useful, use
`git diff --no-index -- /dev/null <path>`; exit status `1` is normal when files
differ. For old content, use `git show <base>:<path>` or `git show HEAD:<path>`.
For staged content, use `git show :<path>`.

If the developer names a PR and `gh` is available, the read-only metadata command
above can establish its base. Do not use `gh` to modify the PR or local branch.

## 2. Review the diff in context

Group changed files into logical changes and build a short map for each:

1. Read the complete relevant diff, including deleted lines and hunk context.
2. Read the changed file around the change and the pre-change version.
3. Find affected callers, imports, routes, interfaces, DTOs, queries, schemas,
   configuration, and tests.
4. Compare the new behavior with the old behavior and the intended contract.
5. Check new, deleted, and renamed files for reachability and dangling
   references.
6. Check related tests and configuration even when they are not in the diff.

Useful checks:

```bash
git show <base>:<path>
git show HEAD:<path>
git show :<path>
```

Search callers and consumers rather than assuming a symbol is used only where it
was edited. For this repository, follow the relevant paths across
`frontend/src/api/*`, `frontend/src/context/*`, `frontend/src/hooks/*`,
`frontend/src/App.jsx`, backend controllers, services, repositories,
`persistence/entity`, security classes, and Flyway migrations.

Treat a change as a regression when it makes a previously valid path fail,
changes a public contract without coordination, bypasses validation, weakens
authorization, loses data, or leaves a new file unreachable. Pre-existing defects
are context unless the change makes them reachable or materially worse.

## 3. Apply the review checks

Use `references/review-checklist.md` selectively for the files and technologies
affected. The minimum review covers:

- **General correctness**: conditions, null and empty values, boundaries,
  invalid input, exception handling, async behavior, duplicate work, dead code,
  API/configuration drift, dependencies, generated files, secrets, and incomplete
  placeholders.
- **React**: component and state consistency, hook dependencies and cleanup,
  server-state synchronization, loading/error/empty states, forms and duplicate
  submissions, authentication storage, API compatibility, accessibility, and
  performance.
- **Spring Boot and security**: controller/service/repository responsibility,
  validation and exception mapping, authentication and object-level
  authorization, status codes, transaction boundaries, password/token/session
  handling, response exposure, and profile/deployment configuration.
- **JPA, SQL, and Flyway**: entity/schema agreement, query correctness and
  parameter binding, locking and data integrity, constraints/indexes, migration
  ordering and existing-data compatibility, destructive operations, backfills,
  deployment order, and migration tests.
- **Cross-stack contracts**: changed REST routes, methods, statuses, fields,
  serialization, auth behavior, file handling, and the matching frontend/backend
  callers and tests.
- **Tests**: new or changed behavior has a meaningful assertion; security,
  concurrency, migration, and error paths are covered where the change makes
  them relevant; existing tests were not weakened to accommodate the change.

Do not run a generic best-practice checklist. Tie every nontrivial concern to the
changed code and a practical consequence.

## 4. Confirm findings and severity

Before reporting a candidate, confirm:

- the exact file and changed line or range, or the class/method and configuration
  key when no stable line exists;
- the concrete trigger and reachable path;
- the impact in this application;
- that the change introduced the defect or made it reachable/worse;
- the smallest practical fix that preserves existing conventions;
- the regression test that would fail before the fix and pass after it.

Do not report a hypothesis as a confirmed defect. If the trigger depends on an
unverified environment or missing code, put it under remaining uncertainties
instead of inventing certainty.

Use these severities:

| Severity | Use for |
| --- | --- |
| Critical | Severe security compromise or imminent serious data loss |
| High | Authorization bypass, major correctness defect, or substantial data-integrity risk |
| Medium | Meaningful regression or reliability problem |
| Low | Minor defect with a concrete impact |
| Informational | Optional improvement, not a confirmed defect |

Prioritize actionable findings over stylistic suggestions. Merge duplicate
findings with the same root cause. Do not inflate severity for style, naming, or
an unproven edge case.

## 5. Run relevant validation

Inspect the available scripts before selecting commands. Run the narrowest check
that can substantiate the review, then broaden only when the change warrants it.

Frontend, from `frontend/`:

```bash
npm run lint
npm test -- <test-file>
npm run build
npm audit --audit-level=low
```

Use `npm audit` only when dependency or security changes make it relevant and the
network is available. Do not install or update dependencies, regenerate the
lockfile, or run a full suite by default. If dependencies are unavailable, report
that limitation instead of changing the lockfile or build configuration.

Backend, from `backend/`:

```bash
./mvnw -q -DskipTests compile
./mvnw -Dtest=<ClassName>#<method> test
./mvnw checkstyle:check test-compile -q
./mvnw --batch-mode --no-transfer-progress verify
```

`verify` is the broader gate and includes Checkstyle plus the JaCoCo threshold
and tests; run it when the change warrants a full backend check. Target the
relevant unit, controller, security, repository, or integration tests first.

MySQL integration checks are environment-dependent:

```bash
./mvnw -Pmysql-integration verify
```

Run this only with a disposable database, the required environment variables or
Docker/Testcontainers support. Inspect `application-mysql-it.properties` and the
test guards first. Never point it at a shared, staging, or production database.
Do not run migrations as a side effect of ordinary review. For migration changes,
inspect SQL, ordering, existing-data assumptions, and test coverage; execute
against a disposable MySQL instance only when the developer requests migration
validation and a disposable environment is available.

For a REST contract change, run the relevant frontend tests and backend
controller/security tests that exercise the changed request and response shape.
If no contract test exists, say so rather than implying compatibility was
verified.

Record the exact command, working directory, exit status, and concise result.
Preserve failure output relevant to the finding. A command that was not run is
not a pass, and a skipped test is not coverage.

## 6. Fix only when explicitly requested

If the developer explicitly requests fixes:

- Fix only confirmed issues within the agreed scope.
- Preserve unrelated work, staged/unstaged state, and existing changes.
- Keep changes minimal and logically grouped.
- Add or update regression tests.
- Do not perform unrelated formatting, refactoring, or dependency upgrades.
- Do not change public API contracts, authentication behavior, or database
  semantics without justification and approval.
- Re-run the relevant validation after fixes and report actual results.

Never stage, commit, push, or create a pull request unless explicitly requested.

## 7. Report

Use `references/report-template.md` exactly. Start with one of:

- `No actionable findings.`
- `Findings identified.`
- `Review incomplete because of a stated limitation.`

Then provide, in order:

1. Findings ordered by severity.
2. Changed files and the relevant risks.
3. Missing or inadequate tests.
4. Commands actually executed and their results.
5. Remaining uncertainties and recommended next steps.

For each finding include:

- severity;
- file path and changed line or range where possible;
- concise description of the defect;
- concrete conditions under which it occurs;
- practical impact;
- minimal recommended fix;
- relevant regression test.

If there are no findings, state: "No actionable issues were identified in the
reviewed scope." Do not claim the code is completely bug-free or secure. If the
review is incomplete, name the limitation and what was inspected; do not present a
partial review as complete.

## 8. Writing standards

Use concise, direct technical language. Avoid AI fluff, generic praise,
repetitive summaries, and speculative recommendations. Do not restate the entire
diff or explain unchanged code. Cite actual paths and line ranges, separate facts
from assumptions, and keep the report focused on the developer's changes.
