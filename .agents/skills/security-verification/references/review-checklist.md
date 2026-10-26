# Security Verification Checklist

Apply the relevant sections selectively. Record evidence for every checked item:
source path, request path, role, precondition, and test or command. A checked box
without evidence is not a finding and does not establish that a control works.

## 1. Scope and evidence

- [ ] Name the review mode: whole repository, focused feature, control
  verification, or post-remediation verification.
- [ ] Record branch, commit, and working-tree state.
- [ ] State active profiles, environment assumptions, database availability, and
  whether the database is disposable.
- [ ] List inspected paths and excluded paths.
- [ ] Identify the request scope and whether a change set is involved.
- [ ] Read current manifests, security classes, configuration, CI, Dockerfile,
  and tests rather than relying on documentation.
- [ ] Record every command actually executed and its result.
- [ ] State what was not verified.

## 2. Threat model and attack surface

- [ ] List assets: credentials, hashes, tokens, signing secret, PII, profiles,
  registration requests, exams, question banks, answer keys, marks, results,
  files, approvals, and admin state changes.
- [ ] List actors: anonymous, student, teacher, admin, disabled user, stolen
  token, malicious client.
- [ ] List trust boundaries: browser/API, route guard/backend, CORS/Security/
  interceptor/method security/controller guard, request/persistence,
  upload/PDF parser, code/build/runtime.
- [ ] Enumerate all routes and methods, including actuator, static, error, file,
  and legacy paths.
- [ ] Identify which routes are intentionally public and why.
- [ ] Identify privileged side effects and the data each route returns.
- [ ] State the expected security invariants before inspecting individual code.

## 3. Endpoint authorization matrix

For each route, record:

| Field | Evidence |
| --- | --- |
| Method and path | |
| Authentication | Anonymous / token required |
| Allowed roles | |
| Object ownership rule | |
| Identity source | Token / path / body / query |
| Request fields | |
| Response sensitivity | |
| Side effect | |
| Rate limit | |
| Effective layers | Security / interceptor / method / controller / service / repository |
| Tests | |

Then verify:

- [ ] `SecurityConfig` URL rules, `AuthInterceptor` public routes, and method
  annotations agree.
- [ ] Every protected route rejects missing and invalid tokens.
- [ ] Every privileged route rejects the wrong role.
- [ ] Every object route rejects an unrelated object ID.
- [ ] Body/path IDs cannot override the authenticated identity.
- [ ] Admin-only routes remain admin-only when called through aliases or
  alternate HTTP methods.
- [ ] Teachers are restricted to their own exams and exam-scoped submissions.
- [ ] Students are restricted to their own attempts, registrations, profile, and
  results.
- [ ] Self checks that use `user.user_id` and role-data checks that use
  `student_id`/`teacher_id` use the correct identifier.
- [ ] Role-ID collisions cannot authorize a different role's record.
- [ ] There is no path from a broad method rule to a narrower guard that
  accidentally grants access, or from a weak controller guard to a service that
  trusts caller-supplied IDs.
- [ ] Public registration cannot create a privileged account or choose an
  admin/teacher-only status.
- [ ] Admin approval and deletion cannot be replayed or performed twice.
- [ ] 401/403/404 behavior does not disclose more than intended.

## 4. Authentication and session lifecycle

- [ ] Token signature verification is HMAC and constant-time.
- [ ] Malformed, truncated, overlong, wrong-prefix, and non-Base64 tokens are
  rejected.
- [ ] Expiry, issued-at, role whitelist, and positive IDs are validated.
- [ ] A valid token for a disabled account is rejected.
- [ ] A token with a missing role profile cannot access protected data.
- [ ] Token TTL is bounded and configured.
- [ ] Secrets are at least 32 characters in non-development environments.
- [ ] Development fallback secret cannot be used outside development profiles.
- [ ] Mixed profile sets are treated as deployment, not development.
- [ ] Login errors do not disclose unnecessary account state.
- [ ] Login and registration rate limits are present and use a trusted client
  address.
- [ ] Proxy headers are trusted only from configured proxies.
- [ ] Per-process rate limits are documented; multi-replica risk is stated.
- [ ] Password hashing uses BCrypt or a stronger approved scheme with an
  appropriate cost.
- [ ] Password policy is enforced server-side, not only in React.
- [ ] A pre-hashed password cannot be submitted to bypass the intended policy.
- [ ] Current-password verification is required for a password change.
- [ ] A copied token stops working after account deactivation.
- [ ] Role changes, password changes, logout, and deletion have the intended
  token-invalidation behavior.
- [ ] No token, password, hash, or secret is logged or returned.

## 5. Input, SQL, and output handling

- [ ] All SQL values are bound through parameters.
- [ ] Dynamic `IN` lists contain only generated placeholders.
- [ ] Dynamic identifiers and sort fields come from fixed allowlists.
- [ ] Native queries, JPQL, and `@Query` strings are checked.
- [ ] `LIKE` patterns cannot inject wildcards or break query semantics.
- [ ] DTO and `Map` binding cannot mass-assign `userId`, `role`, `roleId`,
  `status`, `teacherId`, `studentId`, `marks`, or ownership fields.
- [ ] Server code overwrites ownership and lifecycle fields.
- [ ] Negative, zero, fractional, huge, null, blank, duplicate, and malformed
  values are handled.
- [ ] JSON duplicate keys, Unicode, and unexpected fields have a defined policy.
- [ ] Error responses do not expose stack traces, SQL, driver, class, table,
  column, or secret details.
- [ ] Error logs do not contain credentials, tokens, answer keys, or PII.
- [ ] User-controlled text is encoded in HTML and React does not inject raw HTML.
- [ ] URLs, redirects, `href`, `src`, filenames, and third-party iframes do not
  allow script execution or open redirects.
- [ ] Frontend rendering of profiles, exams, questions, answers, and PDFs does
  not create XSS.

## 6. Sensitive data and response exposure

- [ ] No endpoint returns the `User` object or its password field directly.
- [ ] Registration list responses omit passwords; approval-only paths keep hashes
  server-side.
- [ ] Profile responses contain only the intended personal data.
- [ ] Students cannot read other students' profiles, contacts, registrations,
  attempts, marks, or results.
- [ ] Teachers cannot read students' data beyond the intended exam scope.
- [ ] Answer keys and correct-option text are absent before release.
- [ ] Question breakdowns are cleared or protected before the release point.
- [ ] Data returned in 404/403/500 responses is minimal.
- [ ] Cache headers prevent sensitive responses or files from being stored.
- [ ] `localStorage` exposure is treated as an XSS impact, not a secure secret
  store.
- [ ] 401 cleanup, logout, and session expiry clear sensitive client state.
- [ ] `VITE_*` variables contain no server secret.
- [ ] PDFs and generated files do not contain hidden answer keys or internal
  metadata beyond the intended release.

## 7. Business-logic and state security

- [ ] Exam creation ignores client-supplied teacher ownership and derives it from
  the authenticated principal.
- [ ] Exam update/delete is limited to the owner or admin.
- [ ] Approval/rejection/cancellation/status changes enforce allowed transitions.
- [ ] An approved or frozen paper cannot be edited.
- [ ] Questions cannot be added after the allowed lifecycle point.
- [ ] A student can create/resume only one attempt under concurrency.
- [ ] Attempt start checks registration, window, status, and ownership.
- [ ] Attempt access checks that the attempt belongs to the student.
- [ ] Submission is idempotent and cannot be replayed for another attempt.
- [ ] Expired attempts are finalized consistently.
- [ ] Marks are integers, bounded, and cannot be changed by a non-owner.
- [ ] Results cannot be read before submission or before release policy allows.
- [ ] Registration is self-only; path/body IDs cannot register another student.
- [ ] Unregistration enforces the same ownership and timing rules.
- [ ] Registration approval creates exactly one account and cannot be replayed.
- [ ] Deletion deactivates accounts with history and does not orphan active data.
- [ ] Concurrent or duplicate operations end in a consistent state.
- [ ] Transaction boundaries and locks cover the state change, not just reads.

## 8. File upload and download

- [ ] Upload requires authentication and exam ownership.
- [ ] `questionOnlyConfirmed` is required server-side.
- [ ] Content is limited by size before expensive parsing.
- [ ] Extension and magic bytes are checked.
- [ ] PDFBox rejects encrypted, empty, unreadable, or malformed files.
- [ ] Parser resource use is bounded (pages, decompression, time).
- [ ] Original filename is not used as a path.
- [ ] Stored filename and content are not trusted by the download layer.
- [ ] Download checks the same authorization as upload/view.
- [ ] Response uses `application/pdf`, attachment disposition, `nosniff`, and
  private/no-store caching where appropriate.
- [ ] Student downloads are limited to visible exams.
- [ ] Generated PDFs contain only student-facing questions.
- [ ] Upload errors do not echo file contents or internal paths.

## 9. Persistence, transactions, and migrations

- [ ] Queries are scoped by authenticated ownership, not only by ID.
- [ ] Transactions cover multi-step state changes and their child rows.
- [ ] Locking prevents duplicate attempts, double submission, and duplicate
  approval.
- [ ] Optimistic version fields are used where concurrent updates are expected.
- [ ] JPA and JdbcTemplate paths agree on the same schema and constraints.
- [ ] Flyway migrations are ordered, immutable once applied, and reviewed for
  destructive operations.
- [ ] Seed data does not contain a usable known credential.
- [ ] Constraints match application assumptions; missing foreign keys are
  documented and tested.
- [ ] Delete/deactivate behavior preserves required history.
- [ ] Database credentials and URL flags are appropriate for the environment.
- [ ] No destructive SQL, mass update, or migration is run during review.

## 10. Frontend and browser security

- [ ] Browser routes and hidden UI are treated as usability only.
- [ ] API base URL is trusted or configured; no untrusted redirect.
- [ ] Token storage implications are documented.
- [ ] The client does not log tokens, passwords, or sensitive responses.
- [ ] 401 clears the session consistently.
- [ ] Logout clears all sensitive keys.
- [ ] Error messages displayed to users are not raw server internals.
- [ ] User-controlled text cannot become HTML, script, or an unsafe URL.
- [ ] Profile images cannot inject HTML or executable content.
- [ ] PDF blob URLs are revoked and not shared across users.
- [ ] Third-party embeds are intentional and constrained.
- [ ] CSP enforcement versus report-only status is stated accurately.
- [ ] CORS credentials and origins match the browser contract.
- [ ] Client checks do not claim to enforce authorization.

## 11. Configuration, dependencies, and deployment

- [ ] Secrets come from environment/runtime, not source or `VITE_*`.
- [ ] Missing production secret fails startup.
- [ ] Development fallback is rejected outside development.
- [ ] CORS origins are exact; wildcards with credentials are rejected.
- [ ] CSRF is disabled only with a sound bearer-token rationale.
- [ ] Security headers and CSP are evaluated correctly.
- [ ] Actuator exposure and health detail are minimal.
- [ ] TLS termination and forwarded headers are understood.
- [ ] Rate limiting is effective behind proxies and across replicas.
- [ ] Database credentials and TLS flags are production-appropriate.
- [ ] Container runs as non-root and uses the production profile.
- [ ] CI uses least-privilege permissions and fails on audit/check failures.
- [ ] Frontend and backend dependency audits are available and current.
- [ ] No historical report or generated artifact is treated as current evidence.
- [ ] No real secret appears in examples, logs, screenshots, or the report.

## 12. Test verification

- [ ] Existing security tests are identified by behavior, not just file name.
- [ ] Tests exercise negative authorization paths, not only happy paths.
- [ ] Token tampering, expiry, unknown role, and disabled account are covered.
- [ ] Each privileged route has at least an unauthenticated and wrong-role test.
- [ ] Object-ownership tests use different user and role IDs to catch ID
  confusion.
- [ ] Method-security and controller-guard interaction is tested.
- [ ] File upload authorization and validation are tested.
- [ ] Answer-key release timing is tested before and after release.
- [ ] Concurrency and idempotency are tested where state can race.
- [ ] Tests do not weaken controls to pass.
- [ ] Test output was inspected; a test name is not evidence of an assertion.
- [ ] Test gaps are reported as coverage limitations, not vulnerabilities.

## 13. Finding validation

For every candidate, record:

- [ ] Exact file and line/range or class/method/configuration key.
- [ ] Asset and trust boundary.
- [ ] Required role, endpoint, parameters, and environment.
- [ ] Effective control path and why it fails.
- [ ] Safe reproduction or test reference.
- [ ] Concrete impact in this application.
- [ ] Smallest practical fix.
- [ ] Regression test.
- [ ] Confirmed / likely / unverified.
- [ ] Severity and rationale.

Reject or downgrade:

- [ ] Style-only advice.
- [ ] Generic best practice without a reachable path.
- [ ] Duplicate root causes.
- [ ] Unreachable code.
- [ ] Documented deliberate limitations unless reassessed.
- [ ] Claims based only on comments, docs, or test names.
- [ ] A hypothesis presented as a confirmed exploit.

## 14. Severity and report

Use the repository convention unless the developer supplies another:

| Severity | Criteria |
| --- | --- |
| Critical | Unauthenticated/trivial admin compromise, mass sensitive-data exposure, destructive loss, or equivalent |
| High | Auth bypass, privilege escalation, exploitable injection, answer-key/substantial PII exposure, serious integrity flaw |
| Medium | Meaningful security defect with preconditions or limited scope, or missing control with demonstrated impact |
| Low | Narrow impact, difficult preconditions, or concrete defense-in-depth gap |
| Informational | Observation/optional hardening, no demonstrated vulnerability |

Before publishing:

- [ ] Findings are ordered by severity.
- [ ] Every finding has evidence, impact, remediation, and regression test.
- [ ] Confidence is separate from severity.
- [ ] No secrets, tokens, hashes, answer keys, or PII appear.
- [ ] Controls verified and not verified are both listed.
- [ ] Reviewed scope and exclusions are explicit.
- [ ] Commands actually run are listed with results.
- [ ] The report never claims the application is "secure" or "fully verified".
- [ ] Files changed says "None — verification only" unless remediation was
  explicitly authorized.
