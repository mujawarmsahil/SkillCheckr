# SkillCheckr Verification Report

Every result below was produced by running the command shown, in this working
tree, on the date in the last section. No result is copied from an earlier run.

## Commands

| # | Command | Result |
| --- | --- | --- |
| 1 | `cd backend && ./mvnw verify` | **554 tests, 0 failures, 0 errors, 0 skipped** — coverage gate passed |
| 2 | `cd backend && ./mvnw -q -DskipTests compile` | Exit code 0, no output |
| 3 | `cd backend && ./mvnw checkstyle:check` | 0 Checkstyle violations — BUILD SUCCESS |
| 4 | `cd frontend && npm test` | **2 test files, 41 tests passed** (Vitest 5.0.3) |
| 5 | `cd frontend && npm run lint` | Clean, no errors or warnings |
| 6 | `cd frontend && npm run build` | Built in ~3.9s, 767 modules transformed |
| 7 | `cd frontend && npm audit` | **0 vulnerabilities** |

## Backend test breakdown

`./mvnw verify` (554 total across 37 test classes):

| Area | Classes | Tests |
| --- | --- | --- |
| Context / integration | `DevCorsIntegrationTest` (4), `CorsIntegrationTest` (4), `SkillCheckrApplicationTests` (1), `ActuatorHealthTest` (1) | 10 |
| Config | `DeploymentConfigurationValidatorTest` (10) | 10 |
| Security | `TokenServiceTest` (11), `AuthInterceptorTest` (12) | 23 |
| Controllers | `ExamControllerTest` (75), `AdminControllerTest` (24), `RegistrationRequestControllerTest` (20), `AuthControllerTest` (19), `QuestionControllerTest` (15), `ResultControllerTest` (16), `SubjectControllerTest` (12) | 181 |
| Services | `ExamServiceTest` (34), `AuthServiceImplTest` (22), `RegistrationRequestServiceTest` (16), `QuestionServiceTest` (18), `ExamSubmissionServiceTest` (16), `AdminServiceTest` (15), `ResultServiceTest` (6), `AttemptAnswerServiceTest` (5), `SubjectServiceTest` (3) | 135 |
| Repositories | `AdminRepositoryImplTest` (31), `ExamRepositoryImplTest` (25), `ExamQuestionRepositoryImplTest` (4), `AuthRepositoryImplTest` (21), `QuestionRepositoryImplTest` (17), `SubjectRepositoryImplTest` (11), `ResultRepositoryImplTest` (11), `RegistrationRequestRepositoryImplTest` (9) | 129 |
| Validation | `ExamCreationValidatorTest` (21), `RequestValueParserTest` (15) | 36 |
| Exception handling | `GlobalExceptionHandlerTest` | 16 |
| Mappers | `ExamResultRowMapperTest` (4), `ExamAttemptRowMapperTest` (4) | 8 |
| JSON serialization | `JacksonNumberBindingTest` | 6 |
| **Total** | | **554** |

## Frontend test breakdown

`npm test` (Vitest, 41 total across 2 test files):

| File | Tests |
| --- | --- |
| `src/utils/dateUtils.test.jsx` | 25 |
| `src/hooks/useAttemptAnswers.test.jsx` | 16 |
| **Total** | **41** |

## Frontend build output

```text
✓ 767 modules transformed.
dist/index.html                             0.93 kB │ gzip:   0.52 kB
dist/assets/index-De724Jwj.css             49.04 kB │ gzip:   8.32 kB
dist/assets/index-COErKPw3.js             942.31 kB │ gzip: 260.13 kB
✓ built in 3.85s
```

One non-blocking notice remains:

- the main chunk exceeds the 500 kB advisory limit (no code splitting configured);

## Issues found and resolved

| Issue | Cause | Fix |
| --- | --- | --- |
| Context tests failed with `IllegalArgumentException: No Statement specified` | The startup migrations are `ApplicationRunner`s; those tests wire a **mocked** `DataSource`, so `Connection.prepareStatement(...)` returned `null` and Spring's `DataSourceUtils.applyTimeout` rejected it | Migrations deleted; the mocked-`DataSource` `@TestConfiguration` blocks removed from the context tests. The fresh `schema.sql` declares every constraint directly, so no startup migration is needed |
| Teacher grading modal read undefined values | `ExamSubmissionSummaryDTO` and `EvaluationItemDTO` serialize with snake_case `@JsonProperty` names, but the component read camelCase (`studentName`, `pendingEvaluation`, `awardedMarks`, …) | `examApi.js` normalizes those two responses to camelCase at the API boundary; `EvaluateSubmission` updated accordingly. `EvaluationItemDTO.evaluated` is exposed as `is_evaluated`, so it now reads `isEvaluated` |
| A failed exam submission could never be retried | `isSubmittingRef.current` was set to `true` on entry and never cleared | Reset in the `finally` block of `submitExam` |
| Exam UI icons fell back to a placeholder circle | `lock`, `alert-triangle`, and `x-circle` were used but not defined in `Icons.jsx` | Added the three missing cases |
| Submission list showed a misleading status | An attempt with no result row (still `IN_PROGRESS`) fell back to a hardcoded `"Graded"` label | Status is now derived from `resultStatus`, with `In Progress` and a "Not submitted" action for attempts that have not been submitted |
| Documentation contradicted the code | `frontend/architecture.md` described webcam proctoring, a `localStorage` answer draft, and unconditional session rehydration; `README.md` called `TakeExam` a "proctoring" surface | Rewritten to match the implementation; new root `architecture.md` documents the system |
| Bare `./mvnw spring-boot:run` no longer started the app | `DeploymentConfigurationValidator` requires a real `TOKEN_SECRET` outside a development profile, so a profile-less start aborted | Documented `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` in `README.md` and `backend/product-spec.json` |
| Documentation understated the implementation | `backend/architecture.md` still claimed mock-token authentication, attempt-only authentication, no role-based authorization, a legacy result submission path, and answer/submission-only transaction coverage; `frontend/architecture.md` claimed no test runner or test script | Corrected against the implementation: 7 of 7 controllers use `AuthGuard`, `ResultController` is read-only, 16 `@Transactional` methods span 7 services, and the frontend runs Vitest with 41 tests |

## Known limitations

These are deliberate boundaries, not defects:

- **No proctoring or anti-cheat.** There is no webcam, focus, fullscreen, or
  clipboard enforcement, and no `Disqualified` result status. Nothing in the
  backend or frontend can produce one.
- **Rate limiting is per process.** Login and registration are limited to 10
  requests/minute and 5 requests/hour per remote address. Multi-replica
  deployments still need a shared ingress limiter.
- **Legacy plaintext passwords are still accepted.** They are migrated to
  BCrypt on the next successful sign in.
- **Chunk size.** The single 908 kB bundle could be code-split, but this is a
  performance note, not a correctness problem.

## Coverage note

JaCoCo produces a report during `test` and enforces a 50% line-coverage rule
during `verify`. The `test` goal above does not itself enforce that threshold, so
`./mvnw verify` is the command that fails a build on a coverage regression.

## Schema rebuild verification

`backend/src/main/resources/schema.sql` was rewritten as a fresh-install schema and
verified against an isolated empty MySQL 8.x instance (temporary datadir and port,
since removed):

| Check | Result |
| --- | --- |
| Tables created | 14 |
| Foreign keys created | 16 |
| Repository SQL statements validated with `PREPARE` | 154 (139 direct + 15 expanded dynamic-table) |
| Unique / `NOT NULL` / restrictive-FK constraints | Enforced as declared |
| Seeded admin row | Present, inactive by default; activate only after setting a new BCrypt password |
| End-to-end flow against a database created only from `schema.sql` | **30/30 checks passed before the seed was made inactive; rerun after secure administrator provisioning** |

The end-to-end flow covered admin login, subject creation, teacher/student
registration and approval, question creation, exam creation, exam approval,
student registration, question attachment, attempt creation, answer autosave
(2 answers persisted), submission, and result creation/retrieval
(`Submitted for Evaluation`), plus the duplicate-registration and
second-attempt guards.

### Finding-specific runtime verification

| Finding | Check | Result |
| --- | --- | --- |
| 1 | Edit an already-attempted question | **409** — "This question has already been attempted and cannot be edited." |
| 2 | Change `subject_id` of a question attached to an exam | **400** — "This question is attached to an exam with a different subject and cannot be moved." |
| 2 | Control: same-subject edit on an attached question | **200** |
| 4 | Uniqueness constraint declared directly in `schema.sql` as `uq_exam_attempt`; migration deleted | No duplicate constraint possible |

## Code changes

| Area | File | Change |
| --- | --- | --- |
| 1 | `AttemptAnswerRepository` / `AttemptAnswerRepositoryImpl` | Added `hasAttemptHistory(questionId)` |
| 1 | `QuestionServiceImpl.updateQuestion` | Rejects editing a question that has attempt history (`ExamInUseException`, 409) before any data is modified; added `@Transactional` |
| 2 | `ExamQuestionRepository` / `ExamQuestionRepositoryImpl` | Added `findSubjectIdsByQuestionId(questionId)` |
| 2 | `QuestionServiceImpl.updateQuestion` | Rejects a subject change that would detach a question from an exam with a different subject (`BadRequestException`, 400) |
| 4 | `schema.sql` | `uq_exam_attempt` uniqueness constraint declared directly; migrations deleted |
| 5 | `ExamRepository` / `ExamRepositoryImpl` / `AdminRepositoryImpl` | Duplicated completed-status SQL extracted to `ExamRepository.syncExamStatuses()` |
| 6 | `DBConfig` → `PasswordConfig` | Renamed; it configures BCrypt, not database connectivity |
| — | `ExamResultRowMapper` | Removed `ResultSetMetaData`/`hasColumn` probing; binds the known query shape |
| — | `ResultRepositoryImpl` | All four queries project `student.name AS student_name` via `LEFT JOIN student` |
| — | `RegistrationRequestRepositoryImpl` | Uses `jdbcTemplate.update(...)`; BCrypt-prefix detection moved to `RegistrationRequestServiceImpl` |
| — | `QuestionServiceImpl.deleteQuestionById` | Message now names the actual remedy (delete the exam first) |
| — | Controllers | Legacy route aliases removed; canonical paths only |
| — | `AuthInterceptor` | Public-route allowlist reduced to `/api/auth/login` and `/api/requests` |
| — | `AuthGuard`, `ResultController` | Require student role for self-result access, preventing cross-role ID collisions |
| — | `AuthInterceptor`, `AuthService`, `AuthRepository` | Re-check account status on protected requests so deactivation invalidates existing tokens |
| — | `PublicEndpointRateLimiter` | Apply bounded per-process limits to login and public registration |
| — | `schema.sql`, `README.md` | Disable the seeded admin until a unique BCrypt password is provisioned |
| — | `disable-known-admin.sql` | Disable the unchanged default account in existing databases before deployment |
| — | `AuthController`, `Signup`, `EditProfileModal` | Require at least eight characters for new and changed passwords |
| — | `frontend/package.json`, `frontend/package-lock.json` | Update vulnerable frontend dependencies; `npm audit` reports zero advisories |
| — | Frontend | API clients and components updated to canonical paths; dead assets deleted |

No database referential integrity was weakened. The
`attempt_answer.selected_answer_id → answer` FK remains restrictive (no cascade).

## Date

Original schema rebuild and end-to-end verification: 2026-09-29. Security remediations and test verification: 2026-10-01.
