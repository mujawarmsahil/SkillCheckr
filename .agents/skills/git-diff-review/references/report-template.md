# Git Diff Review Report Template

Use one of the three opening outcomes exactly. Do not add a fourth outcome or a
generic preamble.

## Findings identified

```markdown
Findings identified.

## Findings

### High — <concise title>

- Severity: High
- Location: `path/to/file:line-line`
- Defect: <concrete bug, regression, security issue, or maintainability problem>
- Trigger: <conditions and reachable path>
- Impact: <what breaks, who is affected, and what data/contract is at risk>
- Minimal fix: <smallest correction that preserves existing conventions>
- Regression test: <specific test or assertion that proves the fix>

### Medium — <concise title>

- Severity: Medium
- Location: `path/to/file:line-line`
- Defect: ...
- Trigger: ...
- Impact: ...
- Minimal fix: ...
- Regression test: ...

## Changed files and relevant risks

- `path/to/file` — <what changed and the relevant risk>
- `path/to/test` — <coverage added or missing>
- `path/to/migration` — <schema/deployment risk>

Include new, deleted, renamed, staged, unstaged, untracked, and base-branch
changes in the scope that applies. Group files by logical change when the change
set contains unrelated work.

## Missing or inadequate tests

- <Behavior changed>: <missing case or inadequate assertion>.
- <If no gap was identified>: No specific test gap was identified for the
  inspected behavior; this does not prove complete coverage.

## Commands actually executed

- `<exact command>` in `<working directory>` — passed/failed/blocked; <exit
  status or concise result>.
- If none: `None.`

## Remaining uncertainties and next steps

- <Base ref, uncommitted/untracked scope, environment, or generated-file
  limitation>
- Checks not run: <exact check and specific reason>
- <Follow-up needed to confirm a candidate or verify a dependent path>
```

## No actionable findings

```markdown
No actionable findings.

No actionable issues were identified in the reviewed scope.

## Changed files and relevant risks

- `path/to/file` — <what changed; no issue confirmed in the inspected paths>

Clearly state the reviewed scope and any paths or change sets that were not
inspected. Do not claim the code is bug-free or secure.

## Missing or inadequate tests

- <Any concrete gap, or "No specific test gap was identified in the inspected
  scope.">

## Commands actually executed

- `<exact command>` in `<working directory>` — passed; <result>.
- If none: `None.`

## Remaining uncertainties and next steps

- <Unverified paths, stale base ref, unavailable environment, or unrun checks>
- Checks not run: <exact check and specific reason>
```

## Review incomplete

```markdown
Review incomplete because of a stated limitation.

Limitation: <missing base branch, inaccessible file, unavailable environment, or
other concrete reason>

The findings below are limited to the scope that was inspected. Do not interpret
the absence of findings in an uninspected area as evidence that it is correct.

## Findings

<Findings from the inspected scope, ordered by severity, using the finding block
above. If none were confirmed in the inspected scope, say so explicitly.>

## Changed files and relevant risks

- <Files actually inspected and relevant risks>
- <Files or change sets not inspected and why>

## Missing or inadequate tests

- <Gaps in the inspected behavior, and tests that could not be assessed>

## Commands actually executed

- `<exact command>` in `<working directory>` — passed/failed/blocked; <result>.
- If none: `None.`

## Remaining uncertainties and next steps

- <What must be resolved before the review can be completed>
- Checks not run: <exact check and specific limitation>
- <Recommended next action, such as supplying the base branch or environment>
```

## Finding rules

- Order findings by severity: Critical, High, Medium, Low, Informational.
- Include every required field for every finding. If a line range is not stable,
  use the class/method or exact configuration key and say why.
- Do not repeat the same root cause under multiple severities; merge duplicates
  and cross-reference them.
- Do not report a speculative issue as a confirmed defect. Put unresolved
  possibilities under remaining uncertainties.
- Do not include secrets, tokens, passwords, answer keys, or personal data in the
  report.
- Do not include a generic checklist or praise. Focus on changed behavior and its
  direct effects.
- Do not claim tests, builds, scans, migrations, or runtime flows passed unless
  the command was executed and the output inspected.
- If fixes were authorized and applied, report them separately from the review
  findings and include the post-fix validation actually run.
