---
name: commit-pr-generator
description: Generate accurate commit and pull request documentation from the actual current changes, with appropriate testing and evidence requirements.
---

# Commit and PR Generator

Use this skill when asked to generate commit or pull request documentation for current changes.

## Source of truth

1. Inspect the current diff and relevant surrounding source; use the repository's branch/base information when available. Do not include unrelated or uncommitted changes as if they belong to the requested change.
2. Base every claim only on actual code/documentation changes. Do not claim an issue is fixed unless the diff implements the fix.
3. Inspect project build/test configuration and `docs/PROJECT_GUIDE.md` when available to determine the appropriate exact commands and manual flows.
4. Clearly distinguish commands actually executed and their results from commands that should be run. Never claim tests, lint, builds, or manual checks passed unless observed.
5. Identify whether the project guide needs updating when a change affects architecture, configuration, execution flow, APIs, data schema, or developer setup. Name the exact section; do not silently imply it was updated.

## Commit message format

Generate a specific, concise, imperative subject and a short heading with factual bullets. Avoid generic subjects and unnecessary implementation details. Use this exact layout:

```text
PR name which we can use for this changes:

<PR name>

Commit message:

<Short imperative commit message>

<Heading>

- <actual change>
- <actual change>
```

## Testing decisions

Determine appropriate automated checks from the affected code and repository scripts/tasks: relevant unit, integration, API, component, E2E, regression, build, and lint/static-analysis checks. Give exact commands when they can be established. Report actual results separately; mark unexecuted checks as not run or recommended.

Write manual scenarios specific to the change. Depending on the affected behavior, consider happy path, validation/error handling, persistence/transactions, permissions, authentication/authorization, retries/idempotency, and regressions. Mention environment/device/service prerequisites.

For UI changes, say whether before/after screenshots or a short recording is warranted. Include responsive/device and accessibility checks only when relevant. For API changes, specify request, expected response, error cases, and auth checks. For database changes, cover migration, old-data compatibility, constraints, and rollback where relevant. For configuration changes, cover environment-specific values and missing-configuration behavior. Include an application-specific smoke test only when appropriate.

When a smoke test is appropriate, include concrete checks (for example, startup and the primary affected flow) under `Manual:` in the required PR structure and label them `Smoke testing`; do not use a generic checklist when a more specific flow is known.

Select only relevant evidence from:

```text
Evidence required:
[ ] No evidence required
[ ] Screenshot
[ ] Before/after screenshots
[ ] Short screen recording
[ ] API request/response
[ ] Logs
[ ] Test output
[ ] Other: <specific requirement>
```

For visual/interaction UI fixes, normally request before/after evidence; use a short recording only when a state transition or interaction cannot be shown adequately in screenshots. Do not request UI evidence for backend-only changes without a concrete reason.

## PR description format

Return a concise description with this exact structure:

```text
PR description:

Summary:

<short factual summary and reason>

Description:

- <actual change>
- <actual change>

Testing:

Automated:
- <executed check and result, or explicitly not run / recommended command>

Manual:
- <specific scenario(s) still requiring verification>

UI validation:
- <applicable UI/device validation, or Not applicable>

Evidence:
- <selected evidence and why, or No evidence required>
```

Use a change-appropriate smoke test when needed and identify remaining manual checks. Keep claims and evidence aligned with the inspected diff and test output.

Generate commit text only; do not stage files, create/amend a commit, or otherwise modify Git history unless separately requested.
