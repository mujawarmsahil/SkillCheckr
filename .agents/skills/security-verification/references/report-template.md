# Application Security Verification Report Template

Use exactly one of the three opening outcomes. Do not add a fourth outcome or a
generic preamble.

## Findings identified

```markdown
Findings identified.

## Executive summary

<One or two sentences: what was reviewed, the most important confirmed issue, and
the highest-risk asset or boundary.>

## Scope and method

- Mode: <whole repository / focused feature / control verification / post-remediation>
- Reviewed commit/branch: <ref and working-tree state>
- Environments/profiles: <...>
- Database: <not used / disposable local / Testcontainers / not available>
- In scope: <paths, routes, modules, controls>
- Out of scope: <paths, routes, environments, checks>
- Method: <static source trace / existing tests / local dynamic check / combination>

## Threat model summary

- Assets: <credentials, tokens, PII, exam content, answer keys, results, admin state>
- Actors: <anonymous, student, teacher, admin, disabled/stolen-token attacker>
- Trust boundaries: <browser/API, CORS/security/interceptor/controller, DB, uploads, deployment>
- Relevant invariants: <the rules the application is expected to enforce>

## Findings

### Critical — <concise title>

- ID: SEC-001
- Severity: Critical
- Confidence: Confirmed / Likely / Unverified
- Location: `path/to/file:line-line` or `Class.method`
- Asset: <data or capability at risk>
- Trust boundary: <boundary crossed or missing check>
- Status: Confirmed / Needs validation
- Evidence: <specific code behavior and call path>
- Preconditions: <role, endpoint, data, environment, configuration>
- Attack path: <numbered, safe, concrete steps or test reference>
- Impact: <what an attacker can read/change/break, and for whom>
- Affected data/roles: <...>
- Minimal remediation: <smallest fix preserving conventions>
- Regression test: <specific test/assertion that fails before and passes after>
- Related controls: <route, method security, controller guard, service, repository>
- Notes: <uncertainty or scope limitation, if any>

### High — <concise title>

<Same required fields.>

### Medium — <concise title>

<Same required fields.>

### Low — <concise title>

<Same required fields.>

### Informational — <concise title>

<Same required fields.>

## Security controls verified

| Control | Evidence | Result |
| --- | --- | --- |
| Token signature and expiry | `TokenService` + tests | Verified / Partial / Not verified |
| Public-route allowlists | `SecurityConfig`, `AuthInterceptor` | ... |
| Admin route protection | ... | ... |
| Student object ownership | ... | ... |
| Teacher exam ownership | ... | ... |
| Answer-key release | ... | ... |
| Password hashing | ... | ... |
| SQL parameter binding | ... | ... |
| PDF upload validation | ... | ... |
| CORS/secrets/profiles | ... | ... |
| Frontend storage/XSS posture | ... | ... |

## Authorization coverage matrix

| Route / operation | Anonymous | Student | Teacher | Admin | Effective ownership check | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/...` | <denied/allowed> | ... | ... | ... | ... | ... |

Mark each cell with the observed result and the source path or test. State
routes that were not exercised.

## Validation actually executed

- `<exact command>` in `<working directory>` — passed/failed/blocked; <result>.
- If none: `None.`

## Remaining uncertainties and next steps

- <Unverified endpoints, environment limits, stale baseline, missing database>
- Checks not run: <exact check and specific reason>
- <Recommended targeted follow-up>

## Files changed

None — verification only.

## Remediation plan

| Priority | Finding IDs | Action | Dependency/risk |
| --- | --- | --- | --- |
| <now/next/later> | <SEC-...> | <smallest fix> | <test or migration dependency> |
```

## No confirmed vulnerabilities in reviewed scope

```markdown
No confirmed vulnerabilities in reviewed scope.

No confirmed security defect was identified in the paths and controls that were
inspected. This is not a claim that the application is secure or fully verified.

## Scope and method

- Mode: <...>
- Reviewed commit/branch: <...>
- In scope: <...>
- Out of scope: <...>
- Method: <...>

## Controls verified

| Control | Evidence | Result |
| --- | --- | --- |
| <control> | <source/test> | Verified / Partial / Not verified |

## Authorization coverage matrix

| Route / operation | Anonymous | Student | Teacher | Admin | Ownership check | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| <route> | ... | ... | ... | ... | ... | ... |

## Validation actually executed

- `<exact command>` in `<working directory>` — passed/failed/blocked; <result>.
- If none: `None.`

## Remaining uncertainties and next steps

- <Uninspected paths, unavailable database, unrun tests, report-only controls>
- Checks not run: <exact check and reason>
- <Next verification step>

## Files changed

None — verification only.
```

## Verification incomplete

```markdown
Verification incomplete because of a stated limitation.

Limitation: <missing environment, inaccessible code, unavailable database,
authorization boundary, or other concrete reason>

The findings below are limited to the inspected scope. Absence of findings in an
uninspected area must not be read as evidence that it is safe.

## Scope and method

- Mode: <...>
- Reviewed commit/branch: <...>
- In scope: <...>
- Out of scope: <...>
- Method: <...>

## Findings

<Use the finding block from the first outcome, ordered by severity. If none were
confirmed in the inspected scope, say so explicitly.>

## Controls verified

| Control | Evidence | Result |
| --- | --- | --- |
| <control> | <source/test> | Verified / Partial / Not verified |

## Authorization coverage matrix

| Route / operation | Anonymous | Student | Teacher | Admin | Ownership check | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| <route> | ... | ... | ... | ... | ... | ... |

## Validation actually executed

- `<exact command>` in `<working directory>` — passed/failed/blocked; <result>.
- If none: `None.`

## Remaining uncertainties and next steps

- <What must be available or authorized to finish>
- Checks not run: <exact check and reason>
- <Recommended next action>

## Files changed

None — verification only.
```

## Finding rules

- Order findings: Critical, High, Medium, Low, Informational.
- Include every required field. If a line range is unstable, use the class,
  method, route, or configuration key and say why.
- Separate **confirmed** behavior from **likely** and **unverified** risks. Do
  not put a hypothesis in the findings as if it were demonstrated.
- Merge duplicate findings with the same root cause and cross-reference the
  affected routes.
- Do not include raw secrets, tokens, password hashes, answer keys, exam content,
  or personal data. Use a redacted type/location.
- Do not include a generic checklist, praise, or a claim that the application is
  secure.
- Do not include a finding that depends only on a comment, README, architecture
  document, or test name.
- Do not claim a test, build, audit, scan, migration, or runtime check passed
  unless it was executed and the output was inspected.
- If remediation was explicitly authorized and applied, report those changes
  separately from the pre-fix verification findings, with exact commands and
  results. Otherwise, `Files changed` is `None — verification only`.
