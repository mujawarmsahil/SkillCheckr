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

# Start Spring Boot Application
./mvnw spring-boot:run
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
| **Start Backend** | `npm run dev:backend` | — | `./mvnw spring-boot:run` |
| **Build Frontend** | `npm run build:frontend` | `npm run build` | — |
| **Build Backend** | `npm run build:backend` | — | `./mvnw clean package` |
| **Build Both (Production)** | `npm run build` | — | — |
| **Run Linting (Frontend)** | `npm run lint:frontend` | `npm run lint` | — |
| **Run Linting (Backend)** | `npm run lint:backend` | — | `./mvnw checkstyle:check` |
| **Run All Linters** | `npm run lint` | — | — |
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
2. Run the provided schema script to set up tables and default admin seed data:
   - File location: [`backend/src/main/resources/schema.sql`](file:///Users/sahilmujawar/Desktop/projects/SkillCheckr/backend/src/main/resources/schema.sql)

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

3. Update database credentials if needed in [`backend/src/main/resources/application.properties`](file:///Users/sahilmujawar/Desktop/projects/SkillCheckr/backend/src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/exam_application_system
   spring.datasource.username=root
   spring.datasource.password=S@h!l2803
   ```

### 🔑 Default Administrator Login
- **Username:** `Admin1`
- **Password:** `Admin@1234`
- **Role:** `Admin`

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
│           ├── schema.sql    # Complete MySQL relational database schema
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
        │   ├── teacher/      # AddExam (MCQ vs Q&A), ManageExams
        │   ├── student/      # AvailableExams, TakeExam (Timer & Scorecards), StudentResults
        │   ├── admin/        # RegisterUser, TotalUsers, AcceptExam, TotalExams
        │   ├── layout/       # Navbar, Footer, Layout
        │   └── public/       # Home, About, Blog, Contact
        └── pages/            # Role Dashboard Page router
```
