# SkillCheckr Frontend Architecture

This document describes the frontend architecture as it exists in `frontend/` after the cleanup pass. It is based on the current source and configuration, not on intended future behavior.

## Stack

| Concern | Implementation |
| --- | --- |
| Framework | React 19 (function components only) |
| Build / dev server | Vite 6 |
| Routing | react-router-dom 7 (`createBrowserRouter`) |
| HTTP | axios (single configured instance) |
| Charts | recharts (admin distribution analytics) |
| Styling | Tailwind CSS 3 + PostCSS/autoprefixer |
| Linting | ESLint 9 flat config with `eslint-plugin-react-hooks` and `eslint-plugin-react-refresh` |
| State | React Context + local component state (no external store) |
| Tests | None configured |

There is no Redux store, no global CSS framework beyond Tailwind, no form library, and no test runner.

## Application structure

```text
frontend/src
├── main.jsx              entry point: provider tree + route table
├── App.jsx               public layout shell
├── pages/DashboardPage   role-aware dashboard shell (edit profile, sign out)
├── api/                  axios client + endpoint modules
├── context/              AuthContext, ToastContext
├── hooks/                exam-attempt domain hooks
├── constants/            shared string constants
├── utils/                date/schedule, exam, storage helpers
├── components/
│   ├── layout/           Navbar, Footer, Layout
│   ├── public/           Home, About, Blog, Contact (+ presentational children)
│   ├── auth/             Authentication, Login, Signup, ProtectedRoute,
│   │                     DashboardRedirect, EditProfileModal
│   ├── admin/            AdminDashboard + 8 tab panels
│   ├── teacher/          TeacherDashboard + AddExam, ManageExams
│   ├── student/          StudentDashboard + AvailableExams, StudentResults, TakeExam
│   └── common/Icons      single inline-SVG icon component
└── assets/images         logos, favicons, background, review photos
```

## Provider tree and routing

`main.jsx` mounts `StrictMode > AuthProvider > ToastProvider > RouterProvider`.

Routes:

| Path | Element | Guard |
| --- | --- | --- |
| `/` | `Home` inside `App` → `Layout` (Navbar/Footer) | public |
| `/about`, `/blog`, `/contact` | `About`, `Blog`, `Contact` inside `Layout` | public |
| `/authentication` | `Authentication` (login/signup tabs) | public |
| `/login`, `/signup` | redirect to `/authentication` | public alias |
| `/dashboard` | `DashboardRedirect` | `ProtectedRoute` |
| `/dashboard/:role`, `/user/:role` | `DashboardPage` | `ProtectedRoute` + role match |
| `/take-exam/:examId` | `TakeExam` | `ProtectedRoute allowedRoles=[Student, Admin, Teacher]` |
| `/studentExams`, `/createExam`, `/question` | redirects to the matching dashboard | legacy URL aliases |
| `*` | redirect to `/` | fallback |

`ProtectedRoute` renders a spinner while `AuthContext.loading` is true, redirects
unauthenticated visitors to `/authentication` (remembering `location` in router
state so `Login` can return them), and redirects authenticated users with a
disallowed role to their own dashboard.

`DashboardPage` derives the active role from the `:role` param (falling back to
the session role) and renders `AdminDashboard`, `TeacherDashboard`, or
`StudentDashboard`. Role dashboards are tab containers; only the active tab panel
is mounted.

## Authentication flow

- `AuthContext` holds `user`, `role`, and `loading`. The token is not duplicated
  in React state: `apiClient` reads it from `localStorage` on every request.
- On mount the provider rehydrates the session from `localStorage` keys
  (`role`, `user_id`, `username`, `name`, `email`, `contact`, `profile_image`,
  `teacher_id` / `student_id`).
- `login()` posts to `/api/authentication/login`, persists the resolved profile
  fields to `localStorage`, and returns the raw response so `Login` can route to
  `/dashboard/<role>` (or back to the originally requested location).
- `updateUser()` patches `localStorage` and React state after a profile update.
- `setAuthSession()` establishes a session from an externally supplied payload.
- `logout()` clears all `localStorage` keys owned by the app and resets state.
  `Navbar` and `DashboardPage` navigate to `/authentication` afterwards.

Storage keys are written and read in `AuthContext`; other modules read
`localStorage` directly for identifiers only (`user_id`, `role`, `student_id`,
`teacher_id`) and for the exam draft key.

## API layer

```text
Component / hook
      |
      v
api/*Api.js          endpoint functions, return response data
      |
      v
api/client.js        axios instance: base URL, auth header, error normalization
      |
      v
Spring Boot backend
```

- `api/client.js` reads `VITE_API_URL` (falling back to `http://localhost:8080`),
  sets `Content-Type: application/json` and a 15s timeout, attaches
  `Authorization: Bearer <localStorage.token>`, and normalizes failures into a
  single `Error` carrying `message`, `response`, `status`, and `code`. Callers
  therefore use `err.message` and `err.status`.
- Exam/attempt/result flows go through the API modules (`examApi`, `attemptApi`,
  `resultApi`). Modules return unwrapped data and normalize list responses to
  arrays.
- Admin, teacher, and profile screens still call the shared axios instance
  directly for endpoints that have no API module. Both styles share the same
  client, interceptors, and error shape.
- No API module performs business transformation; formatting and status
  interpretation happen in components/hooks/utilities.

## Exam attempt flow

`TakeExam` is the only stateful exam surface and delegates its logic to three
hooks:

- `useExamAttempt` – loads the exam, checks whether the student already
  submitted, verifies registration and the schedule window, starts (or resumes)
  the server-side attempt, loads questions, and restores saved answers. It
  reports `accessBlocked` (`NOT_REGISTERED`, `NOT_STARTED`, `EXPIRED`) instead of
  failing.
- `useExamTimer` – one interval driven by the server-provided `expiresAt`;
  fires `onExpire` (auto submit) exactly once per attempt.
- `useAttemptAnswers` – MCQ/text answer state, per-question debounced
  persistence to the backend, restore of server-saved answers, and the answered
  count.

Proctoring lives in `TakeExam` itself: webcam capture, browser
`FaceDetector`/pixel-sampling presence detection, fullscreen tracking, focus and
clipboard listeners, strike counters, and the screen shield overlay. Answers are
also mirrored into a `localStorage` draft keyed by exam and user, which is
cleared on successful submission.

## State management

- Two contexts: `AuthContext` (session) and `ToastContext` (global
  notifications, with duplicate-message suppression).
- Everything else is local component state.
- Data lists are fetched by `useCallback`-wrapped loaders invoked from a single
  `useEffect`; the loader is the effect dependency, so identity is stable.
- Derived values (filtered lists, counts, schedule status) are computed during
  render rather than stored.
- `AvailableExams` keeps a 5-second `currentTime` tick so schedule state and
  countdowns stay live without refetching.

## Validation responsibilities

Frontend validation exists only for immediate feedback and input constraints:
required fields, email format, 10-digit contact, minimum username/password
length, password confirmation, non-empty question text, MCQ option completeness,
and marks relationships. The backend remains authoritative for credentials,
schedules, registration, and grading; the frontend does not reimplement those
rules.

## Error handling

All requests pass through the axios response interceptor, which converts
failures into a plain `Error` with a user-safe message. Components surface
`err.message` through `showError` and fall back to a domain-specific message.
Lists reset to empty on failure; no failure is silently swallowed, and no raw
backend stack traces are rendered. Storage and media failures are caught locally
because they are best-effort concerns.

## Environment and build configuration

- `.env` / `.env.example`: only `VITE_API_URL` is read (by `api/client.js`).
- `vite.config.js`: React plugin only. No dev proxy, aliases, or code splitting.
- `tailwind.config.js`: content globs for `index.html` and `src/**`.
- `postcss.config.js`: Tailwind + autoprefixer.
- `eslint.config.js`: browser globals, `react-hooks` recommended rules,
  `react-refresh/only-export-components` with `useAuth`/`useToast` allowed as
  non-component exports.
- Scripts: `dev`, `build`, `preview`, `lint`. There is no test script.

## Notes for future changes

- Add new backend calls as functions in the matching `api/*Api.js` module rather
  than calling `apiClient` from a component; keep business rules in components,
  hooks, or utils.
- Update this document when routing, providers, the API layer, or the exam
  attempt flow change.
