# SkillCheckr Architecture

System-wide architecture for SkillCheckr: how the React frontend and the Spring
Boot backend are connected, and which side is authoritative for each rule. This
document describes the code as it exists today.

## 1. Repository layout

```text
SkillCheckr/
├── backend/                  Spring Boot 3.4 API (Java 21, JdbcTemplate, MySQL)
│   ├── src/main/java/com/skillcheckr/
│   │   ├── config/           WebConfig, PasswordConfig, DeploymentConfigurationValidator
│   │   ├── constant/         ExamConstants, RoleConstants
│   │   ├── controller/       Auth, Admin, Exam, Question, Result, Subject, RegistrationRequest
│   │   ├── exception/        GlobalExceptionHandler, ForbiddenException, ExamInUseException
│   │   ├── mapper/           RowMappers
│   │   ├── model/            Entities and DTOs
│   │   ├── repository/       Repository interfaces + *Impl (plain SQL)
│   │   ├── security/         TokenService, AuthInterceptor, AuthGuard, AuthPrincipal
│   │   ├── service/          Business logic interfaces + *Impl
│   │   └── validation/       ExamCreationValidator, ExamAttemptValidator, QuestionValidator
│   └── src/main/resources/   application.properties, db/migration/
├── frontend/                 React 19 + Vite 6 client
│   └── src/{api,components,constants,context,hooks,pages,utils}
└── architecture.md           This file
```

`frontend/architecture.md` documents the client in more detail. This file
covers the boundaries between the two applications.

## 2. Stack

| Concern | Implementation |
| --- | --- |
| API | Spring Boot 3.4, Spring MVC, Java 21 |
| Persistence | `JdbcTemplate` + explicit SQL (no JPA/Hibernate) |
| Database | MySQL (Flyway migrations); H2 in MySQL mode for tests |
| Auth | HMAC-SHA256 signed stateless tokens + `HandlerInterceptor` |
| Client | React 19, Vite 6, react-router-dom 7, axios, Tailwind CSS |
| Tests | JUnit 5 + Mockito (554 backend tests), Vitest 5 + React Testing Library (41 frontend tests), ESLint 9, Checkstyle, Vite build |

## 3. Trust boundary

**The backend is authoritative for every rule that affects a grade, a
schedule, an identity, or a permission.** The browser is a rendering and input
layer only.

| Concern | Owner | Notes |
| --- | --- | --- |
| Credentials, roles, identity | Backend | Token signature, expiry, role claims |
| Authorization | Backend | `AuthGuard` on every protected operation |
| Registration eligibility | Backend | Schedule window + exam status |
| Attempt start / resume | Backend | One attempt per (exam, student) |
| Marking and percentage | Backend | MCQ auto-graded on submit |
| Descriptive grading | Backend | Teacher marks are stored server-side and re-scored |
| Result status | Backend | `Pass` / `Fail` / `Submitted for Evaluation` |
| Proctoring / disqualification | Not implemented | No client or server support exists |
| Input format, required fields | Frontend + backend | Frontend for feedback, backend for enforcement |

The frontend may never set a score, a result status, a submission time, an
identity, or another student's identifier. Those are read from responses.

## 4. Authentication and authorization

### 4.1 Token format

`TokenService` issues and verifies stateless tokens:

```text
sc1.<base64url(userId:roleId:role:issuedAt:expiresAt:username)>.<base64url(HMAC-SHA256(payload, secret))>
```

- The payload is a colon-separated record, not JSON; fields are positional.
- Verification is constant time, checks the expiry, and rejects any token whose
  role is unknown so a role can never be escalated by tampering with the payload.
- Secret comes from `app.security.token-secret`; TTL from
  `app.security.token-ttl-minutes`. There is no sliding refresh: a token is
  re-issued by logging in again. Authenticated requests also check that the
  account remains active, so administrator deactivation invalidates existing tokens.
- The client sends `Authorization: Bearer <token>`; there is no server-side
  session and no client-generated fallback token.
- `AuthPrincipal` distinguishes `userId` (the `user` row) from `roleId` (the
  `student` / `teacher` / `admin` profile row). Exam ownership and attempt
  ownership checks always use `roleId`.

### 4.2 Request pipeline

```text
HTTP request
   → AuthInterceptor      signature + expiry verification
                         method-aware public-route allowlist
   → AuthGuard            role requirement + resource ownership + state rules
   → Controller           thin; no business rules
   → Service              validation and state transitions
   → Repository           parameterized SQL
```

- `AuthInterceptor` matches public routes by HTTP method, so `POST` on an
  otherwise public path is still authenticated. Anonymous login and registration
  requests have per-process rate limits (10/minute and 5/hour per remote address);
  production ingress should enforce shared limits when running multiple replicas.
- `AuthGuard` expresses each authorization decision explicitly:

  | Guard | Rule |
  | --- | --- |
  | `requireAdmin` | Admin only |
  | `requireStudent` / `requireStudentId` | Student only; returns `roleId` |
  | `requireSelfOrStaff(request, ownerId)` | Student must match the owner; staff may act on anybody |
  | `requireSelfOrAdmin(request, ownerId)` | Compares against `user.user_id` (account data) |
  | `requireSelfOrAdminForRoleData(request, ownerRoleId)` | Compares against `student_id` / `teacher_id` (role data) |
  | `requireExamAccess(request, exam)` | Owning teacher, or any admin |

  The two "self" variants are deliberately separate: account data is keyed by
  `user_id` and role data by `student_id` / `teacher_id`, so comparing the wrong
  one would either leak data or deny legitimate access.
- `GlobalExceptionHandler` maps domain exceptions to HTTP status codes;
  `ExamSubmissionException` carries 404/409/400/500 explicitly, and
  `ForbiddenException` becomes 403.

### 4.3 Roles

`RoleConstants` is the single source for the persisted role names
(`Admin`, `Teacher`, `Student`). Student-only surfaces such as
`/take-exam/:examId` are also guarded in the router.

## 5. Exam lifecycle

```text
Teacher creates exam ──> Pending ──admin approve──> Upcoming ──exams run──> Completed
                            │                           │
                            └──admin reject──> Rejected └──admin cancel──> Cancelled
```

- Teacher-created exams are forced to `Pending`; the client cannot choose a
  status. Status changes go through admin-only endpoints.
- `ADMIN_SETTABLE_EXAM_STATUSES` is the allowlist for admin status updates.
- Registration is open only while `isOpenForRegistration(status)` is true, i.e.
  `Upcoming` or `Approved`.
- Creation rules in `ExamCreationValidator`: exam name pattern, at least
  `EXAM_MIN_LEAD_TIME_DAYS` (10) days of lead time, duration at most
  `EXAM_MAX_DURATION_MINUTES` (600), and consistent marks.
- Questions are attached through `POST /api/exams/{exam_id}/questions` and
  carry the generated `exam_id`; `QuestionValidator` rejects incomplete MCQ
  options and inconsistent marks, and an attached question must belong to the
  exam's subject.
- A question paper is frozen once the exam window opens
  (`ExamInUseException`), and a question already assigned to an exam cannot be
  deleted.

### 5.1 Attempt lifecycle

```text
(registered) → start → IN_PROGRESS ──submit──> SUBMITTED ──grade──> result row
                  │                              │
                  └── resume same attempt ────────┘   expiry auto-submits
```

- `POST /api/exams/{exam_id}/attempts` derives the student from the token, so a
  client cannot start an attempt for someone else.
- One attempt per (exam, student): a duplicate start returns **409**; a repeated
  submit is idempotent and returns the stored result.
- Answers are saved per question
  (`PUT /api/exams/{exam_id}/attempts/{attempt_id}/answers/{question_id}`) and
  reloaded from the server on resume.

### 5.2 Grading

- On submit, MCQ answers are auto-graded and written to `attempt_answer`.
- If any descriptive question still lacks marks, the result status is
  `Submitted for Evaluation`; otherwise it is `Pass` or `Fail` from
  `marks_obtained >= passing_marks`.
- A teacher awards marks per question
  (`PUT /api/exams/{exam_id}/attempts/{attempt_id}/answers/{question_id}/marks`).
  The service re-derives the total, re-evaluates the status, and updates the
  result row in the same transaction, so the client never posts a score.
- `QuestionBreakdownService` rebuilds the per-question review from stored
  answers on every result read; the result row only holds the score.

## 6. Result access

| Caller | Scope |
| --- | --- |
| Student | Own attempts and own results (`requireSelfOrAdminForRoleData`) |
| Teacher | Results and submissions for exams they own (`requireExamAccess`) |
| Admin | All results |

Student-wide result reads are permitted only when the caller is the owning
student; a student cannot read another student's result by guessing an id, and a
teacher is not granted blanket access to every result just by being staff.

## 7. API surface (exam domain)

Canonical REST paths are authoritative across the API surface.

| Method | Path (canonical) | Purpose |
| --- | --- | --- |
| POST | `/api/exams` | Create exam (teacher, forced `Pending`) |
| POST | `/api/exams/{exam_id}/questions` | Attach questions |
| GET | `/api/exams/{exam_id}` | Exam detail |
| GET | `/api/exams/upcoming` | Student exam list |
| GET | `/api/exams/teacher/{teacher_id}` | Teacher's own exams |
| POST | `/api/exams/{exam_id}/approve` · `/reject` · `/cancel` | Admin decisions |
| PUT | `/api/exams/{exam_id}/status` | Admin status update |
| POST | `/api/exams/{exam_id}/register` | Self-registration (token identity) |
| GET | `/api/exams/{exam_id}/isRegistered/{student_id}` | Registration check |
| GET | `/api/exams/registrations/student/{student_id}` | Student registrations |
| POST | `/api/exams/{exam_id}/attempts` | Start or resume attempt |
| PUT | `/api/exams/{exam_id}/attempts/{attempt_id}/answers/{question_id}` | Save answer |
| GET | `/api/exams/{exam_id}/attempts/{attempt_id}/answers` | Restore answers |
| POST | `/api/exams/{exam_id}/attempts/{attempt_id}/submit` | Submit (idempotent) |
| GET | `/api/exams/{exam_id}/submissions` | Teacher submission list |
| GET | `/api/exams/{exam_id}/attempts/{attempt_id}/evaluation` | Answers to grade |
| PUT | `/api/exams/{exam_id}/attempts/{attempt_id}/answers/{question_id}/marks` | Award marks |
| DELETE | `/api/exams/{exam_id}` | Delete an exam the caller owns |

## 8. JSON contract

- `ExamController` is mapped to `/api/exams`; the client
  uses lowercase.
- DTOs that the teacher evaluation screens consume
  (`ExamSubmissionSummaryDTO`, `EvaluationItemDTO`) serialize with snake_case
  `@JsonProperty` names. `examApi.js` normalizes those two responses to
  camelCase at the API boundary so components use one naming convention.
  `EvaluationItemDTO.evaluated` is exposed as `is_evaluated`, so it arrives as
  `isEvaluated`.
- `ExamResultDTO` and the question breakdown maps are read defensively
  (`marks_obtained ?? marksObtained`) in the result screens.
- Breakdown map keys from `QuestionBreakdownService` are camelCase
  (`selectedAnswer`, `textAnswer`, `awardedMarks`, `isCorrect`, …).

## 9. Client behaviour

- `apiClient` attaches the bearer token, normalizes errors into a single
  `Error` with `message` / `status` / `code`.
- `AuthContext` rehydrates only when token, user id, and role are all present;
  a partial legacy session is discarded rather than upgraded.
- `useExamAttempt` derives the student from `useAuth`, checks server
  registration and schedule state, then starts or resumes the attempt.
- `useExamTimer` counts down to the server-supplied `expiresAt` and auto-submits
  once. `useAttemptAnswers` persists answers to the backend with no local draft.
- `registerForExam(examId)` sends no student identifier; the server uses the
  token.
- `TakeExam` contains no proctoring logic and no client-side scoring or
  disqualification.
- `EvaluateSubmission` (teacher) lists submissions, shows in-progress attempts
  as not submitted, and posts per-question marks only.

## 10. Database

- Flyway migrations under `backend/src/main/resources/db/migration/` are the
  source of truth for the database schema. Fresh databases apply the initial
  migration on startup; existing complete schemas are baselined at version 1.
  The seeded administrator is inactive and must be provisioned with a new
  password before activation.

## 11. Verification

```bash
cd backend  && ./mvnw test          # 554 tests, 0 failures, 0 errors
cd backend  && ./mvnw -q -DskipTests compile
cd backend  && ./mvnw checkstyle:check
cd frontend && npm test             # 41 tests, 2 test files
cd frontend && npm run lint
cd frontend && npm run build
```

Current status: backend **554/554 passing** with 0 Checkstyle violations,
frontend **41/41 passing** with lint clean, production build succeeds (only the
pre-existing >500 kB chunk-size and Browserslist notices).
