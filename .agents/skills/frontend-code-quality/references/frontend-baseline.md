# Frontend Baseline

Observed on 2026-10-26 from the working tree at `main`. This is an orientation
snapshot, not a guarantee. Re-verify versions, paths, commands, and behavior
before relying on them, and say so in the review when something has changed.

## Layout

```text
frontend/
├── package.json                 scripts and declared dependency ranges
├── package-lock.json            npm lockfile
├── vite.config.js               React plugin only
├── vitest.config.js             jsdom, globals, test include/setup
├── eslint.config.js             ESLint 9 flat config
├── postcss.config.js            @tailwindcss/postcss
├── tailwind.config.js           content globs
├── .env.example                 VITE_API_URL only
├── architecture.md              partially stale; verify against source
└── src/
    ├── main.jsx                 providers and route table
    ├── App.jsx                  public layout shell
    ├── pages/DashboardPage.jsx  role-aware dashboard shell
    ├── api/                     shared axios client and endpoint modules
    ├── context/                 AuthContext, ToastContext
    ├── hooks/                   exam-attempt behavior
    ├── constants/               shared exam/result strings
    ├── utils/                   date and exam helpers
    ├── test/setup.js            RTL cleanup and mock clearing
    └── components/
        ├── layout/              Navbar, Footer, Layout
        ├── public/              Home, About, Blog, Contact and presentational children
        ├── auth/                Authentication, Login, Signup, ProtectedRoute,
        │                        DashboardRedirect, EditProfileModal
        ├── admin/               AdminDashboard and tab panels
        ├── teacher/             TeacherDashboard, AddExam, ManageExams, EvaluateSubmission
        ├── student/             StudentDashboard, AvailableExams, StudentResults, TakeExam
        └── common/              Icons, QuestionBankViewer
```

`frontend/dist/` is build output and `frontend/node_modules/` is installed
dependencies. Neither is source.

## Stack and configuration

- React 19 function components, React Router 7, Vite 6, axios, recharts, and
  Tailwind CSS 4 through `@tailwindcss/postcss`. The package declares caret
  ranges; consult `package-lock.json` when an exact installed version matters.
- ESLint 9 flat config with browser globals, `eslint-plugin-react-hooks`, and
  `eslint-plugin-react-refresh`. There is no Prettier or TypeScript config.
- `vite.config.js` contains only the React plugin. It has no dev proxy, path
  alias, manual chunking, or code-splitting configuration.
- `vitest.config.js` uses `jsdom`, `globals: true`, includes
  `src/**/*.test.{js,jsx}`, and loads `./src/test/setup.js`.
- `src/test/setup.js` runs React Testing Library `cleanup()` and
  `vi.clearAllMocks()` after each test.
- `frontend/.env.example` documents only `VITE_API_URL`; the default is
  `http://localhost:8080`. Vite variables are public and bundled into the client.
- CI uses Node.js 22.12.0, even though the README states a lower minimum.

## Provider tree and routing

`src/main.jsx` mounts:

```text
StrictMode > AuthProvider > ToastProvider > RouterProvider
```

Routes:

| Path | Element | Guard |
| --- | --- | --- |
| `/` | `Home` inside `App`/`Layout` | public |
| `/about`, `/blog`, `/contact` | public pages inside `Layout` | public |
| `/authentication` | `Authentication` login/signup tabs | public |
| `/login`, `/signup` | redirect to `/authentication` | public alias |
| `/dashboard` | `DashboardRedirect` | `ProtectedRoute` |
| `/dashboard/:role`, `/user/:role` | `DashboardPage` | `ProtectedRoute` plus client role match |
| `/take-exam/:examId` | `TakeExam` | `ProtectedRoute allowedRoles=[Student]` |
| `/studentExams`, `/createExam`, `/question` | redirects to matching dashboards | legacy aliases |
| `*` | redirect to `/` | fallback |

`ProtectedRoute` waits for `AuthContext.loading`, redirects unauthenticated
visitors to `/authentication` with `state.from`, and redirects a disallowed role
to its own dashboard. `DashboardPage` also redirects when the URL role differs
from the session role. These are UX guards; backend authorization remains
authoritative.

Role dashboards are tab containers and mount only the active panel. Relevant
surfaces:

- Student: available exams, exam registration, question-bank PDF preview,
  exam attempt, results.
- Teacher: exam creation, exam/question management, submission evaluation.
- Admin: overview/statistics, exam approval, users, subjects, questions,
  results.

## Session and state

- `AuthContext` holds `user`, `role`, and `loading`. The token is not duplicated
  in React state.
- On mount, `AuthContext` rehydrates a session only when `token`, `user_id`, and
  `role` are present. It reads profile fields and optional `teacher_id` /
  `student_id` from `localStorage`.
- `login()` posts to `/api/auth/login`, requires a token and user id, persists
  profile/session fields, sets React state, and returns the raw response.
- `updateUser()` writes selected profile fields to storage and React state.
  `setAuthSession()` establishes a session from a supplied payload and requires
  a token. `logout()` removes the app-owned storage keys and clears context.
- `ToastContext` owns transient notifications, suppresses duplicate messages,
  and removes each toast on a timer or user action.
- Everything else is local component state or derived render data. There is no
  Redux, Zustand, TanStack Query, SWR, or form library.
- `localStorage` is read outside `AuthContext` by `api/client.js` (token),
  `EditProfileModal.jsx` (profile/user id fallbacks), and
  `ManageExams.jsx` (teacher id fallback).

## API boundary

```text
Component / hook
      |
      v
api/*Api.js                     endpoint functions, usually returning response data
      |
      v
api/client.js                   axios instance: base URL, auth header, 15s timeout,
      |                         401 failure cleanup, error normalization
      v
Spring Boot backend
```

- `src/api/client.js` reads `VITE_API_URL`, defaults to
  `http://localhost:8080`, sets `Content-Type: application/json`, and attaches
  `Authorization: Bearer <localStorage token>`.
- On HTTP 401 it removes `token`, `role`, and `user_id` from `localStorage`. It
  does not update `AuthContext` state directly.
- Errors are normalized to an `Error` carrying `message`, `response`, `status`,
  and `code`. Components generally surface `err.message`.
- `examApi.js` covers exam lookup, questions, question-bank upload/download,
  lists, approval, cancellation, deletion, registration, submissions, and
  evaluation. It normalizes some snake_case evaluation payloads to camelCase.
- `attemptApi.js` covers start/resume, save answer, get answers, and submit.
- `resultApi.js` covers student results, submitted-status check, and all results.
- API modules normalize list responses to arrays; some detail responses pass
  through unchanged.
- Nine components plus `AuthContext` also call `apiClient` directly for endpoints
  without a module: admin overview/questions/subjects/requests/stats/users,
  teacher exam creation, signup, and profile editing.

## Exam-attempt flow

`TakeExam` is the only stateful exam surface and delegates behavior to:

- `useExamAttempt`: checks submitted status, loads the exam, checks registration
  and the schedule window, starts/resumes the attempt, loads questions, and
  restores saved answers. It reports `NOT_REGISTERED`, `NOT_STARTED`, or
  `EXPIRED` as `accessBlocked`.
- `useExamTimer`: derives a countdown from the server `expiresAt`, requires an
  explicit timezone designator, fires `onExpire` at most once per attempt, and
  clears its interval.
- `useAttemptAnswers`: holds MCQ/text state, saves MCQ immediately, debounces
  text saves by 500 ms, serializes saves per question, flushes pending and
  in-flight saves before submission, and suspends saves after submission starts.
- `TakeExam`: guards duplicate submission with a ref, flushes unsaved answers
  before calling submit, renders registration/schedule/error/result states, and
  shows the answer key only when the server says it is released.

`dateUtils.js` deliberately avoids `toISOString()` for local date inputs and
rejects bare server timestamps without an explicit offset. `examUtils.js`
provides exam-end checks and option answer-id lookup.

## Tests

At this snapshot there are four test files:

- `src/utils/dateUtils.test.jsx` — timestamp parsing, countdown, fake timers,
  timezone-sensitive assertions, and `useExamTimer`.
- `src/hooks/useAttemptAnswers.test.jsx` — debounce, clear, flush, in-flight
  serialization, failure, suspension/resume, and MCQ saves.
- `src/components/student/TakeExam.test.jsx` — answer-key release rendering;
  the component's hooks, router, toast, and APIs are mocked.
- `src/components/common/QuestionBankViewer.test.jsx` — PDF preview/download
  and object URL cleanup.

There is no Playwright/Cypress E2E suite, no `@testing-library/jest-dom`
dependency, and no coverage threshold for the frontend. Recount before citing
coverage or suite size.

## Commands

| Purpose | Command | Notes |
| --- | --- | --- |
| Install | `npm ci` in `frontend/` | Uses the lockfile; CI does this |
| Dev server | `npm run dev` in `frontend/` | Needs a reachable backend for data |
| Lint | `npm run lint` in `frontend/` | ESLint |
| Tests | `npm test` in `frontend/` | Vitest run mode |
| Watch tests | `npm run test:watch` in `frontend/` | Vitest watch mode |
| Build | `npm run build` in `frontend/` | Vite production build |
| Preview build | `npm run preview` in `frontend/` | Serves the production build |
| Dependency audit | `npm audit --audit-level=low` in `frontend/` | CI gate |
| Root frontend lint | `npm run lint:frontend` at repository root | Delegates to frontend |
| Root frontend tests | `npm run test:frontend` at repository root | Delegates to frontend |
| Root frontend build | `npm run build:frontend` at repository root | Delegates to frontend |

CI runs `npm ci`, audit, lint, tests, and build for the frontend. The pre-commit
hook runs frontend lint but not the frontend tests or build.

## Known documentation drift

At this snapshot, `frontend/architecture.md` disagrees with the source in at
least these ways:

- It says Tailwind CSS 3 with PostCSS/autoprefixer; the package and
  `postcss.config.js` use Tailwind CSS 4 through `@tailwindcss/postcss`.
- It lists two test files; there are four.
- It describes a `vite.config.js` with no code splitting, which is still true,
  but should be re-checked as build configuration changes.

`docs/PROJECT_GUIDE.md` does not exist, despite references in older skills.

`report.md` contains historical verification results. They are not current
evidence.

## Deliberate limitations and review boundaries

- Proctoring/anti-cheat is not implemented; the server owns attempt state,
  scoring, and final result status.
- The frontend bundle is not code-split. Treat it as a performance note unless
  the review establishes a user-facing requirement or a change makes it worse.
- Frontend validation is immediate-feedback UX, not the authoritative rule set.
- Login/registration rate limiting is backend/in-memory or ingress-level work,
  not a frontend defect.

## Review hotspots to verify, not findings

The following are reachable patterns worth checking. They are not confirmed
defects and must not be reported without source, caller, and test evidence:

- The 401 interceptor clears only three storage keys and does not notify
  `AuthContext`; check the resulting session/UI state and whether other keys
  remain.
- Session rehydration checks presence but parses ids with `parseInt`; check
  corrupt/legacy storage behavior.
- Several components call `apiClient` directly; check response-shape handling,
  error normalization assumptions, and duplication against API modules.
- `window.confirm` is used for destructive actions; check consistency with the
  accessible modal/dialog patterns.
- `ToastContext` schedules removal timers without tracking them; check provider
  unmount and stale-timer behavior.
- `TakeExam.test.jsx` mocks its own hooks, so it does not exercise the attempt
  integration path.
- `QuestionBankViewer` uses a dialog role and object URL; check focus management,
  Escape behavior, and revoke timing.
- The same date/schedule rules exist in `dateUtils.js` and `examUtils.js`; check
  whether their behavior can diverge in a reachable path.
