# 🎓 SkillCheckr — Academic Examination Platform

> **Next-Generation Dual-Engine Skill Assessment & Academic Testing Platform**  
> Supporting both automated **Multiple Choice Questions (MCQ)** and in-depth **Question-Answer (Descriptive/Theory)** evaluations.

---

## ⚡ Quick Start: How to Run the Application

You can run the frontend, backend, or both from the root workspace or from their respective directories.

### 1. Run Everything (Root Directory)

From the root project directory (`/SkillCheckr`), you can run:

```bash
# 1. Install frontend dependencies (first time only)
cd frontend && npm install && cd ..

# 2. Start Frontend Dev Server (http://localhost:5173)
npm run dev:frontend

# 3. Start Backend Spring Boot Server (http://localhost:8080) in another terminal
npm run dev:backend
```

---

### 2. Run Only the Frontend

Navigate to the `frontend/` directory:

```bash
cd frontend

# Install dependencies (first time)
npm install

# Start development server with Hot Module Replacement (HMR)
npm run dev
# -> App will be live at http://localhost:5173

# Run frontend ESLint checks
npm run lint

# Build frontend for production
npm run build
```

---

### 3. Run Only the Backend

Navigate to the `backend/` directory:

```bash
cd backend

# Start Spring Boot Application (the dev profile is required; without it startup
# fails because a real TOKEN_SECRET is mandatory outside a development profile)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
# -> API server will be live at http://localhost:8080

# Run Checkstyle linter
./mvnw checkstyle:check

# Compile backend classes
./mvnw clean compile

# Build executable JAR file
./mvnw clean package -DskipTests

# Run unit tests
./mvnw test
```

---

## 📋 Complete Commands Cheat Sheet

| Task / Purpose | From Root (`/`) | From Frontend (`/frontend`) | From Backend (`/backend`) |
|---|---|---|---|
| **Start Frontend** | `npm run dev:frontend` | `npm run dev` | — |
| **Start Backend** | `npm run dev:backend` | — | `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` |
| **Build Frontend** | `npm run build:frontend` | `npm run build` | — |
| **Build Backend** | `npm run build:backend` | — | `./mvnw clean package` |
| **Build Both (Production)** | `npm run build` | — | — |
| **Run Linting (Frontend)** | `npm run lint:frontend` | `npm run lint` | — |
| **Run Linting (Backend)** | `npm run lint:backend` | — | `./mvnw checkstyle:check` |
| **Run All Tests** | `npm test` | — | — |
| **Run Frontend Tests** | `npm run test:frontend` | `npm test` | — |
| **Run Backend Tests** | `npm run test:backend` | — | `./mvnw test` |
| **Test Quality Gate / Pre-commit**| `npm run precommit` | — | — |
| **Install Git Hooks** | `npm run install:hooks` | — | — |

---

## 🛠️ Prerequisites & Environment Setup

Before running the application, make sure you have installed:

- **Java JDK:** Version 21 or higher (`java -version`)
- **Node.js:** Version 18 or higher (`node -v`) & npm (`npm -v`)
- **MySQL Database Server:** Version 8.x running on port `3306`

---

## 🗄️ Database Setup

1. Start your local MySQL server.
2. Create an empty MySQL database. Flyway creates the tables and the disabled
   administrator profile automatically when the backend starts:

```bash
CREATE DATABASE exam_application_system
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

3. Configure your database and secret credentials via environment variables. The backend env template lives at [`backend/.env.example`](backend/.env.example) — copy it to `backend/.env` and fill in your real values (`.env` is git-ignored and must never be committed):
   ```bash
   cp backend/.env.example backend/.env      # then fill in the values
   ```
   ```properties
   DB_URL=jdbc:mysql://localhost:3306/exam_application_system
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password
   ```

   The frontend reads only the public API location, configured separately in [`frontend/.env.example`](frontend/.env.example) (`VITE_API_URL`). Never place a server credential in a `VITE_*` variable — those are compiled into the browser bundle.

### Administrator provisioning

The schema includes an **inactive** `admin` account so the known repository
password cannot authenticate on a fresh database. Before first use, provision a
new, unique password with your approved password-management process, store only
its BCrypt hash, and activate the account:

```sql
UPDATE `user`
SET `password` = '<BCrypt hash for a new, unique administrator password>',
    `status` = 'Active'
WHERE `username` = 'admin' AND `user_role` = 'Admin';
```

Do not enable this account with any password published in source control. Use a
separate credential and controlled database access in each environment.

### Existing database adoption

Back up the database before deploying. Flyway is configured to baseline a
non-empty schema at version 1, which records the existing schema as the initial
version without re-running the table-creation migration. On startup Flyway then
applies later versioned migrations, including the compatibility migration that
disables only the unchanged repository-seeded administrator password.

This automatic baseline is safe only when the existing database already
contains the complete schema corresponding to version 1. Verify the tables
against [`backend/src/main/resources/db/migration/V1__initial_schema.sql`](backend/src/main/resources/db/migration/V1__initial_schema.sql)
and take a restorable backup first. Do not deploy this version to an empty or
partially initialized production database expecting it to infer missing tables:
empty databases receive V1, while non-empty databases are baselined and skip
V1.

Flyway creates and maintains its `flyway_schema_history` table. Future schema
changes belong in a new, ordered migration under
`backend/src/main/resources/db/migration/` and must not modify a migration
already applied in an environment.

### Student question-bank PDFs

Teachers add the structured questions required by the online exam in the
**Add Exam** workflow. They may optionally upload a separate question-only PDF
(maximum 10 MB); if they do not, the backend generates a student PDF from the
structured questions. The generated PDF omits correct options and descriptive
sample answers. The application cannot reliably detect answer keys inside
arbitrary uploaded PDFs, so the teacher must confirm the uploaded PDF contains
questions only.

The question-bank download is available to students only after an exam is
approved. Flyway applies
`backend/src/main/resources/db/migration/V3__add_exam_question_bank.sql`
automatically on startup; no manual SQL migration is required.

---

## 🛡️ Automated Pre-Commit Quality Gate

This project has a **Git Pre-Commit Hook** configured. Whenever `git commit` is executed by a developer or AI agent:
1. It runs **Frontend ESLint** (`npm run lint`).
2. It runs **Backend Checkstyle & Compilation** (`./mvnw checkstyle:check test-compile`).
3. If any linting error or syntax issue exists, the commit is **blocked** until resolved.

To manually re-install or activate the hook on any machine:
```bash
npm run install:hooks
```

GitHub Actions runs the full CI checks on pull requests, pushes to `main`, and
manual dispatches. The frontend job installs from the lockfile, audits
dependencies, lints, tests, and builds using Node.js 22.12; the backend job runs
Maven `verify` with Java 21, including tests and the JaCoCo coverage gate. Set
these jobs as required status checks in repository branch protection to prevent
merging when they fail.

---

## 📂 Project Architecture Overview

```
SkillCheckr/
├── package.json              # Unified root workspace scripts
├── scripts/
│   └── pre-commit            # Git pre-commit quality gate script
├── backend/                  # Spring Boot 3.4 Backend (Java 21)
│   ├── product-spec.json     # Product specification & command registry
│   ├── checkstyle.xml        # Java code quality & linting ruleset
│   ├── pom.xml               # Maven configuration & plugins
│   └── src/
│       ├── main/java/com/skillcheckr/
│       │   ├── controller/   # REST API Controllers (Auth, Exams, Admin, Results)
│       │   ├── service/      # Business logic interfaces & implementations
│       │   ├── repository/   # Data access layer (JDBC Templates)
│       │   └── model/        # DTOs & Domain entities
│       └── main/resources/
│           ├── db/migration/ # Ordered Flyway schema migrations
│           └── application.properties
└── frontend/                 # React 19 + Vite Frontend (Tailwind CSS)
    ├── package.json          # Frontend dependencies & scripts
    ├── eslint.config.js      # Modern ESLint configuration
    ├── vite.config.js        # Vite bundler configuration
    └── src/
        ├── api/client.js     # Centralized Axios API client
        ├── context/          # AuthContext & ToastContext providers
        ├── components/
        │   ├── auth/         # Login, Signup, Role ProtectedRoute
        │   ├── teacher/      # AddExam (MCQ vs Q&A), ManageExams, EvaluateSubmission (grading)
        │   ├── student/      # AvailableExams, TakeExam (server-timed attempt), StudentResults
        │   ├── admin/        # AdminDashboard + Approvals, Users, Results, Questions, Subjects, Stats
        │   ├── layout/       # Navbar, Footer, Layout
        │   └── public/       # Home, About, Blog, Contact
        └── pages/            # Role Dashboard Page router
```
