# Frontend Review Checklist

Use only the sections relevant to the scope. For every candidate finding, inspect
the implementation, its callers, shared state, API module, and tests. Report a
problem only with evidence and a practical impact.

## 1. Architecture, state, and data flow

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Route and provider ownership | `src/main.jsx`, `App.jsx`, `Layout.jsx`, `DashboardPage.jsx`, `ProtectedRoute.jsx` | A route reaches data or actions without the required provider/guard; two places disagree about the active role or layout |
| Feature boundaries | Components, `src/hooks`, `src/context`, `src/api`, `src/utils` | A component owns multi-step domain logic that already lives in a hook; a hook performs unrelated presentation work |
| State ownership | `AuthContext`, `ToastContext`, local state, refs, router state | The same session/attempt/status state is stored in several places that can diverge |
| Storage ownership | `AuthContext`, `api/client.js`, `EditProfileModal.jsx`, `ManageExams.jsx` | A component reads or writes a storage key it does not own; cleanup and rehydration disagree about the session shape |
| API boundary | `src/api/*.js`, direct `apiClient` calls | A screen duplicates endpoint/normalization logic; a direct call assumes a shape the API module already normalizes |
| Side-effect ownership | Fetches, timers, debounce, submit, navigation, object URLs | Several owners trigger the same effect, or cleanup lives in a different lifecycle than setup |
| Duplication | Error extraction, snake/camel handling, schedule/status checks, role names, date formatting | Multiple copies must change together, especially where one copy already differs |
| Convention fit | Existing context/hook/API/component/Tailwind/test patterns | A new abstraction or dependency conflicts with the repository without a concrete failure |
| Documentation | `frontend/architecture.md`, comments, test names | A stale claim would mislead a reviewer or maintainer about current behavior |

Do not recommend a new state library, form library, directory layer, or design
pattern merely because it is familiar. Point to concrete duplication, coupling,
or change risk.

## 2. Authentication, session, and client security

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Session rehydration | `AuthContext` mount effect | A partial/corrupt session is treated as authenticated; ids become `NaN`; a stale role survives a new login |
| Login/session creation | `AuthContext.login`, `setAuthSession`, `Login.jsx` | Missing token/user id is accepted; storage and React state disagree; old role/teacher/student ids survive a role change |
| 401 handling | `api/client.js`, `AuthContext`, `ProtectedRoute`, calling components | Credentials are cleared while the UI remains authenticated; only some owned keys are removed; no redirect or context update occurs |
| Logout | `AuthContext.logout`, `Navbar`, `DashboardPage` | An owned key, timer, or in-flight request survives logout and can repopulate state |
| Route protection | `ProtectedRoute`, `DashboardPage`, redirects | Client-side role checks are described as security; an authenticated user can render another role's panel; redirect loops |
| Role identity | `role`, `roleId`, `student_id`, `teacher_id`, URL `:role` | A role-table id is used as a user id (or the reverse); the URL role is trusted over the session role |
| Token exposure | `localStorage` token, logs, error rendering, `VITE_*` variables | A secret is bundled into the client, logged, rendered, or sent somewhere other than the API |
| Redirects | `location.state.from`, login navigation, role dashboard fallback | A crafted or stale path sends the user outside the intended app routes or loses required query/hash state |
| Unsafe rendering | `dangerouslySetInnerHTML`, URLs, PDFs, user names/text | Untrusted content is rendered as HTML or navigated without validation |
| Sensitive data | Profile fields, results, answer keys, localStorage, console output | Another user's data, a token, a password, or unreleased answers are exposed client-side |
| Destructive actions | Admin/teacher delete/status/approval actions | A destructive request has no confirmation, no pending guard, or no recovery/error path |
| Dependencies | `package-lock.json`, `npm audit` | A known advisory or unnecessary client dependency, or an audit claimed without being run |

Client-side guards are UX. Do not report "authorization bypass" unless the
frontend exposes data or an action the server would otherwise deny, or the UI
misrepresents the server's decision in a way that causes a concrete failure.

## 3. Exam-attempt correctness

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Registration and schedule | `useExamAttempt`, `AvailableExams`, `parseExamSchedule` | A failed request is read as "not registered"; the client opens an exam outside its window; date/time zones disagree with the server |
| Start/resume | `useExamAttempt`, `startExamAttempt`, route state | A missing attempt id is accepted; route state is treated as authoritative; refresh starts a second attempt or loses the current one |
| Saved-answer restore | `getAttemptAnswers`, `restoreSavedAnswers`, `TakeExam` effect | Restored values are matched by the wrong id/option; restore runs after user edits and overwrites them; empty answers reappear |
| Debounced saves | `useAttemptAnswers` | A stale debounce wins, a clear is lost, a failed save is reported as success, or timers survive unmount |
| Per-question ordering | `inFlightRef`, `enqueueSave`, `flushPendingSaves` | Two saves for one question race; a slower older save overwrites newer text; flush resolves before in-flight saves finish |
| Submit ordering | `TakeExam.submitExam`, `flushPendingSaves`, `suspendSaves` | Submit starts before pending answers are saved; a failed flush still submits; saves continue after submission begins |
| Duplicate submission | `isSubmittingRef`, submit buttons, timer callback | Double click or timer/manual submit sends two requests or renders a stale result |
| Expiry | `useExamTimer`, `parseServerTimestamp`, server `expiresAt` | Bare timestamps are guessed in browser local time; `onExpire` fires repeatedly; the interval is not cleared; expired UI still accepts answers |
| Navigation/unmount | `TakeExam`, `QuestionBankViewer`, route changes | An in-flight request or timer updates unmounted state; an object URL leaks; exiting drops unsaved text without warning |
| Error paths | `useExamAttempt`, registration, save, submit, result load | An error leaves a blank screen, an endless spinner, a false success, or a dead-end with no retry |
| Result and answer key | `TakeExam` result view, `RESULT_STATUS` | The client decides pass/fail or reveals answers the server has not released; result fields are read with the wrong shape |
| Tests | `useAttemptAnswers.test.jsx`, `dateUtils.test.jsx`, `TakeExam.test.jsx` | The highest-risk path is covered only by mocked hooks, or timer/save/submit races are untested |

## 4. API integration and contracts

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Base URL and environment | `api/client.js`, `.env.example`, Vite config | Environment change breaks all requests; a production URL or secret is hardcoded |
| Auth header | Request interceptor | A missing/expired token is treated as authenticated; header is attached to the wrong requests; refresh behavior is undefined |
| Error normalization | Response interceptor, callers | Callers read a shape the interceptor does not provide; a raw backend object is rendered; network vs HTTP errors are conflated |
| 401/403/409 semantics | Interceptor, components | Retry loops, silent failure, or a generic message where the user must re-authenticate or resolve a conflict |
| Response shape | API modules, direct calls | snake_case/camelCase mismatch; optional fields are assumed; list/detail shapes differ between endpoints |
| List normalization | `examApi`, `resultApi`, `attemptApi` | A non-array payload is rendered as a list; a valid empty response becomes a loading/error state |
| Direct client usage | Admin/teacher/profile/signup components | Duplicated paths, inconsistent error handling, or a bypassed normalization that causes a real display/action failure |
| Multipart and blobs | Question-bank upload/download, `QuestionBankViewer` | Wrong `Content-Type`, missing auth, unrevoked object URL, wrong filename encoding, or broken error handling for a blob response |
| Cancellation/races | Load effects, answer saves, PDF fetch | An older response overwrites a newer one; a request continues after unmount; no way to retry a failed load |
| Compatibility | Request fields, status codes, DTO names, route aliases | A frontend change breaks an existing backend/client contract without a migration plan |

## 5. React correctness and effects

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| StrictMode behavior | `main.jsx`, effects with fetches/start/timers | A mount effect starts an attempt, submits, or registers twice in development and the logic is not idempotent |
| Effect dependencies | `TakeExam`, `useExamAttempt`, `useExamTimer`, `AvailableExams` | A missing dependency uses stale values; an unstable dependency causes a fetch/render loop |
| Stale closures | Timers, submit callbacks, async handlers, refs | A timer submits an old attempt; a callback reads old answers or role; ref indirection hides a real dependency problem |
| Cleanup | Intervals, timeouts, listeners, object URLs, async guards | A timer fires after unmount; a blob URL leaks; state is set after the component is gone |
| Async state transitions | Loaders, registration, save, submit | Loading never clears; an error is overwritten by a later success; a retry duplicates an action |
| Derived state | Filters, counts, schedule status, answered count | Derived data is stored separately and can disagree with its source |
| Keys and lists | Exam cards, question navigator, result breakdown | A mutable/reordered list uses an unstable key and causes wrong state or DOM reuse |
| Controlled inputs | Login, signup, profile, exam answer fields | An input switches controlled/uncontrolled; typing loses characters; a disabled state blocks recovery |
| Context values | `AuthContext`, `ToastContext` | A new object/function every render causes broad re-renders with a measurable user impact |
| Rendering safety | Raw HTML, URLs, user text | Untrusted content becomes executable markup or an unsafe navigation target |
| Error containment | Route/feature boundaries | A render error blanks the whole app where a local fallback is practical and expected |

Do not flag a missing `useMemo`/`useCallback`/`React.memo` without a measured or
clearly reachable rendering cost. Do not flag a dependency-free effect merely
because it fetches on mount.

## 6. Forms and accessibility

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Labels and names | Login, Signup, EditProfileModal, AddExam, question forms | An input has no programmatic label; a button/icon has no accessible name; placeholder is the only label |
| Error association | Form validation and toast/banner errors | A field error is visual-only, not linked to the input, or not announced |
| Keyboard and focus | Modals, PDF viewer, custom option buttons, destructive dialogs | Focus is not moved/trapped/restored; Escape does not close a modal; a custom control cannot be operated by keyboard |
| Dialog semantics | `QuestionBankViewer`, submit/confirm modals | Missing `role`/`aria-modal`/title wiring, background remains interactive, or focus escapes |
| Status feedback | Loading, submitting, empty, error states | A status change is not announced; a disabled button gives no reason; a spinner replaces the only action |
| Semantics | Cards, tabs, navigation, question options, tables | Clickable `div`s instead of buttons/links; tabs lack selected state; headings skip levels in a way that breaks navigation |
| Images/media | Profile image, public pages, PDF iframe | Missing/incorrect `alt`; an iframe lacks a title; a media control is unreachable |
| Contrast and targets | Tailwind color classes, icon buttons, mobile layout | A control is unreadable or too small in a reachable context; do not report subjective palette preferences |
| Responsive behavior | Dashboard tabs, exam header/navigator, modals | Content/action becomes unreachable at a supported viewport; overflow hides the primary action |
| Validation UX | Login/signup, exam creation, evaluation forms | Client validation contradicts server rules or blocks a valid input; error recovery is impossible |

Do not claim WCAG conformance or run an accessibility audit unless the tool or
manual check was actually performed. Report concrete user impact, not a generic
"accessibility could be improved".

## 7. Performance and rendering

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Bundle splitting | `vite.config.js`, route imports, build output | The initial bundle carries an avoidable large feature; the documented no-splitting limitation is not automatically a defect |
| Request waterfalls | Dashboards, `AvailableExams`, profile load, evaluation | A screen serializes independent requests or refetches on every render |
| Polling and intervals | `AvailableExams`, `useExamTimer`, public pages | A timer continues off-screen/unmounted or forces broad re-renders without need |
| Large lists/charts | Admin/teacher/student tabs, recharts | An unbounded list or chart recomputes/re-renders with a measurable cost; do not demand pagination without reachable volume evidence |
| Expensive work | Filters, sorts, derived breakdowns, PDF/blob handling | Work repeats per render or per row with a practical user-visible cost |
| Images | Profile images, public assets | Large/remote images have no sizing/loading strategy where it clearly affects the page |
| Memoization | Context values, callbacks, expensive components | A recommendation must name the measured or obvious re-render path and the smallest fix |
| Build warnings | `npm run build` output | A chunk-size warning is context; report it as a defect only with a concrete user-facing consequence |

## 8. Testing and quality gates

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Behavior coverage | All `src/**/*.test.{js,jsx}` | A high-risk auth, answer-save, submit, expiry, registration, or result behavior has no meaningful test |
| Test realism | `TakeExam.test.jsx` mocks | The test mocks the exact hooks/APIs that contain the risk, so it cannot catch integration defects |
| Timer/async tests | `useAttemptAnswers`, `dateUtils` | Fake timers are not restored; `act`/flush handling hides races; a test passes without exercising the claimed path |
| Session tests | Auth, 401, logout, role redirect | No unauthenticated/forbidden/stale-session/401 case, or the test asserts storage without asserting the resulting UI |
| Error/empty/loading | Screens and hooks | Failure paths are untested, or a test asserts only that a mock was called |
| Accessibility assertions | Forms, dialogs, PDF viewer | No role/label/focus coverage where the interaction is central; do not assume `jest-dom` is installed |
| Timezone tests | `dateUtils.test.jsx` | A timestamp test passes only in one ambient timezone or relies on a zone that is not actually set |
| E2E/manual coverage | Exam attempt, login, PDF preview, destructive actions | A multi-page flow has no automated or documented manual verification |
| Test quality | Queries, fixtures, cleanup, mocks | Over-broad mocks, index-based queries, unnecessary snapshots, or assertions that cannot fail for the right reason |
| Gates | ESLint, Vitest, build, CI, pre-commit | A changed area is outside the gate or the documented gate is weaker than claimed |

## 9. Configuration, dependencies, and documentation

| Check | Inspect | Evidence of a real problem |
| --- | --- | --- |
| Environment | `.env.example`, `api/client.js`, README | A required value is undocumented, a secret is exposed, or the documented default differs from behavior |
| Build config | `vite.config.js`, `postcss.config.js`, `tailwind.config.js` | A config file is unused/misleading, or the documented toolchain differs from the installed one |
| Lint/test config | `eslint.config.js`, `vitest.config.js`, `src/test/setup.js` | A rule/setup silently excludes changed files or hides a real failure |
| Dependencies | `package.json`, `package-lock.json`, CI audit | Unused/duplicate dependency, missing peer, deprecated API, or an unpinned build tool with reproducibility impact |
| Documentation | `frontend/architecture.md`, README, comments | A stale statement affects routing, auth, tests, Tailwind, or commands; verify before repeating it |
| Deliberate limitations | Proctoring, code splitting, client-side validation | Reporting an accepted limitation as a new defect without scope justification |
| Artifacts | `dist/`, `node_modules/`, generated files | Reviewing build output or installed code as if it were source |

## Review anti-patterns

- Do not call a client route guard "authorization" or a client validation
  "security".
- Do not report "missing tests" without naming the behavior, risk, and a
  realistic assertion.
- Do not report "no code splitting" as a new defect when it is a documented
  limitation; first establish the user-facing cost.
- Do not demand `useMemo`, `useCallback`, `React.memo`, pagination, or a new
  state library without evidence of a reachable performance or correctness
  problem.
- Do not flag a `useEffect` fetch, a local `setTimeout`, or `window.confirm`
  solely because another pattern exists.
- Do not treat Tailwind class style, line count, comment density, or naming taste
  as a defect.
- Do not assume a backend contract from the frontend alone; inspect the API
  module and, when relevant, the backend contract.
- Do not reproduce a command result from the README, `frontend/architecture.md`,
  `report.md`, or a previous conversation as current evidence.
