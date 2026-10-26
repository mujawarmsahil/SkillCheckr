# Repository Baseline

Observed on 2026-10-26 from the working tree at `main`. This is an orientation
snapshot, not a guarantee. Re-verify versions, paths, commands, and behavior
before relying on them, and say so in the review when something has changed.

## Layout

```text
SkillCheckr/
├── package.json                 root workspace scripts
├── scripts/pre-commit           local pre-commit gate
├── .github/workflows/ci.yml     CI checks
├── .agents/skills/              canonical shared skills
├── .github/skills/              symlinks to .agents/skills
├── architecture.md              system overview (verify against source)
├── report.md                    historical verification report (do not copy)
├── backend/                     Spring Boot API
│   ├── pom.xml
│   ├── checkstyle.xml
│   ├── Dockerfile
│   ├── src/main/java/com/skillcheckr/
│   ├── src/main/resources/
│   └── src/test/java/com/skillcheckr/
└── frontend/                    React + Vite client
    ├── package.json
    ├── eslint.config.js
    ├── vitest.config.js
    └── src/
```

There is no `docs/PROJECT_GUIDE.md` at this snapshot, despite references to it
in older skill files. Use the files listed here.

## Backend

- Spring Boot 3.4.4, Java 21, Maven Wrapper (`./mvnw`).
- Dependencies include Spring MVC, Spring Data JPA, Spring Security, Spring
  Validation, Actuator, Flyway (`flyway-core`, `flyway-mysql`), MySQL driver,
  Lombok, PDFBox, H2 (test), and Testcontainers MySQL (test).
- Persistence is hybrid. Custom interfaces and `*Impl` classes in
  `com.skillcheckr.repository` use `JdbcTemplate` and explicit SQL. JPA
  entities and `JpaRepository` interfaces also exist under
  `com.skillcheckr.persistence.entity` and
  `com.skillcheckr.persistence.repository`, and some custom repositories call
  them. Do not describe the backend as JDBC-only or JPA-only.
- Main schema management: Flyway migrations `V1` through `V8` under
  `src/main/resources/db/migration/`; `spring.jpa.hibernate.ddl-auto=validate`;
  `spring.jpa.open-in-view=false`; `spring.flyway.baseline-on-migrate=true` and
  `baseline-version=1`.
- Unit/context tests use H2 in MySQL mode with Flyway disabled and
  `ddl-auto=create-drop`. MySQL tests use Flyway with validation; Testcontainers
  tests can be disabled when Docker is unavailable.
- Security includes a custom HMAC-SHA256 bearer token (`TokenService`), Spring
  Security (`SecurityConfig`, `TokenAuthenticationFilter`), an `AuthInterceptor`
  mapped over `/api/**`, and controller-side `AuthGuard` checks. The public
  route allowlist appears in both `SecurityConfig` and `AuthInterceptor`.
- Runtime configuration is profile-based: base, `dev`, and `prod` properties.
  `DeploymentConfigurationValidator` rejects a missing/short token secret or a
  development fallback outside development profiles, and rejects wildcard CORS
  origins when credentials are enabled.
- Checkstyle is bound to Maven `validate`. JaCoCo enforces at least 50% line
  coverage during `verify`. The `mysql-integration` profile adds Failsafe tests
  matching `**/*MySqlIT.java`.

## Frontend

- React 19 function components, React Router 7, Vite 6, axios, recharts,
  Tailwind CSS 4 through `@tailwindcss/postcss`.
- `src/api/client.js` is the shared axios instance. It reads `VITE_API_URL`,
  attaches `Authorization: Bearer <localStorage token>`, uses a 15-second
  timeout, and normalizes errors to an `Error` with `message`, `status`,
  `response`, and `code`. On a 401 it removes `token`, `role`, and `user_id`
  from `localStorage`.
- `AuthContext` rehydrates a session only when token, user id, and role are
  present. The token is read from `localStorage`, not stored in React state.
  `ProtectedRoute` guards client routes; backend authorization remains
  authoritative.
- Exam-attempt behavior is split across `useExamAttempt`, `useExamTimer`, and
  `useAttemptAnswers`.
- Vitest 5, React Testing Library, and jsdom run tests matching
  `src/**/*.test.{js,jsx}`. At this snapshot there are four test files:
  `dateUtils`, `useAttemptAnswers`, `TakeExam`, and `QuestionBankViewer`.
  Recount before citing coverage.
- ESLint 9 flat config includes React Hooks and React Refresh rules.

## Commands

| Purpose | Command | Notes |
| --- | --- | --- |
| Frontend install | `npm ci` in `frontend/` | Uses the lockfile; CI does this |
| Frontend lint | `npm run lint` in `frontend/` | ESLint |
| Frontend tests | `npm test` in `frontend/` | Vitest run mode |
| Frontend build | `npm run build` in `frontend/` | Vite production build |
| Frontend dependency audit | `npm audit --audit-level=low` in `frontend/` | CI gate |
| Backend tests | `./mvnw test` in `backend/` | H2/Testcontainers behavior depends on environment |
| Backend full gate | `./mvnw verify` in `backend/` | Checkstyle, tests, JaCoCo coverage check |
| Checkstyle only | `./mvnw checkstyle:check` in `backend/` | |
| MySQL integration tests | `./mvnw -Pmysql-integration verify` in `backend/` | `*MySqlIT`; Docker may be required |
| Backend local run | `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` in `backend/` | Needs MySQL; optional `.env` |
| Root lint | `npm run lint` | Frontend then backend |
| Root tests | `npm test` | Frontend then backend |
| Root build | `npm run build` | Frontend build plus backend JAR, tests skipped |
| Pre-commit | `npm run install:hooks`, then `npm run precommit` | Lint and backend `test-compile`; not full tests/build |

CI uses Node.js 22.12.0 and Java 21 (Temurin). The README states lower Node
minimums, so CI is the stricter reference.

## Environment constraints

- Runtime database is MySQL 8.x; Flyway runs on startup. Local runs need MySQL
  or a compatible configured database.
- Without a real `TOKEN_SECRET`, use `SPRING_PROFILES_ACTIVE=dev` locally.
  Production and mixed profile sets require a real secret.
- `backend/.env` is optional and git-ignored; `backend/.env.example` is the
  credential-free template. The frontend reads only `VITE_API_URL`; server
  secrets must not appear in `VITE_*` variables.
- Testcontainers MySQL tests may be skipped or disabled when Docker is absent.
  Record the actual behavior rather than assuming the suite ran.
- The frontend production build has had a chunk-size advisory above 500 kB.
  Treat it as a performance note unless the review establishes a user-facing
  requirement.

## Documented limitations and review boundaries

The repository documents these as deliberate limitations. Do not report them as
new defects unless the review scope explicitly reassesses them or a change makes
them materially worse:

- No proctoring or anti-cheat implementation.
- Login/registration rate limiting is per process and in-memory; multi-replica
  deployments need ingress-level limits.
- Legacy plaintext passwords are accepted and upgraded to BCrypt on successful
  sign-in.
- The frontend bundle is not code-split.

## Known documentation drift to verify

At this snapshot, the architecture documents disagreed with the source:

- `architecture.md` and `backend/architecture.md` say persistence is
  `JdbcTemplate`-only with no JPA/Hibernate, but `backend/pom.xml` includes
  `spring-boot-starter-data-jpa` and `persistence/` contains JPA entities and
  repositories.
- `backend/architecture.md` says there is no Spring Security HTTP filter chain,
  but `security/SecurityConfig.java` and `TokenAuthenticationFilter.java` exist
  and are active.
- `WebConfig` retains a comment saying there is no Spring Security filter chain,
  while the application has one; verify the effective request pipeline.
- `frontend/architecture.md` describes Tailwind CSS 3 and two test files, while
  the current package uses Tailwind CSS 4 tooling and there are additional test
  files.
- `report.md` contains historical test/build results. They are not current
  evidence.

Report documentation drift only when it has a practical maintenance or review
consequence; cite the conflicting source and documentation.
