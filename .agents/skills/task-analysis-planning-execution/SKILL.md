---
name: task-analysis-planning-execution
description: Analyze user tasks, determine the real objective, break complex work into dependency-aware verifiable stages, and execute through completion. Covers features, bugs, refactoring, security, migrations, architecture, reviews, and docs.
---

# Task Analysis, Planning, and Execution

Use this skill when the user provides a task, issue, requirement, error, document, code snippet, or implementation request that needs analysis, planning, and execution. Do not start editing code before understanding the problem.

## 1. Understand the input

1. Read and analyze all relevant information provided.
2. Identify the primary objective and expected outcome.
3. Extract explicit requirements, constraints, assumptions, and acceptance criteria.
4. Identify relevant existing implementation, architecture, configuration, tests, and documentation.
5. Determine what is already implemented and what is missing.
6. Identify ambiguous requirements, conflicting instructions, dependencies, and risks.
7. Distinguish the requested outcome from the user's proposed implementation approach.

When the task depends on repository files, inspect the actual files rather than relying on filenames, summaries, or assumptions. When supporting documents are provided, use their relevant content as part of the analysis.

## 2. Decide whether staging is necessary

### Simple tasks
For small, self-contained tasks: analyze, make the change, validate, report. Do not introduce unnecessary planning documents, stages, or approval checkpoints.

### Complex tasks
For tasks involving multiple components, architectural changes, security-sensitive functionality, database migrations, or significant dependencies: divide into logical, independently verifiable stages with dependency-aware ordering. Define purpose, scope, prerequisites, implementation tasks, expected outputs, acceptance criteria, validation, risks, and decisions requiring user input.

## 3. Repository and implementation analysis

Inspect repository structure and module boundaries, existing implementation and call sites, framework and dependency versions, configuration and environment behavior, tests and validation scripts, database schema and migration history when relevant, conventions and reusable abstractions, and git status and existing developer changes before editing.

Understand the current implementation before deciding what to change. Prefer extending or correcting existing functionality over duplicate implementations. Do not assume a feature is missing because it is not present in the first file inspected.

## 4. Create a staged execution plan

For each stage define: number and title; objective; scope and likely affected files/components; prerequisites and dependencies; specific implementation tasks; expected deliverables; acceptance criteria; tests and validation required; risks, compatibility concerns, and rollback/recovery considerations where applicable.

Keep stages small enough to review and validate, but large enough to deliver meaningful progress. A typical complex task may follow:

1. Requirement analysis and current-state assessment.
2. Architecture and implementation planning.
3. Foundational changes and configuration.
4. Core backend or business-logic implementation.
5. Database and persistence changes, where required.
6. Frontend and API integration, where required.
7. Security, edge cases, and error handling.
8. Testing, regression verification, and final review.
9. Documentation and completion report.

This sequence is illustrative, not mandatory. Adapt to the actual dependency graph.

## 5. Decide when to ask questions

Ask only when missing information materially affects correctness, security, architecture, data integrity, or the requested outcome. Before asking, inspect available context and repository, determine whether a safe reasonable assumption is possible, and avoid questions already answered. If ambiguity is minor, state the assumption and proceed. If an unresolved decision could cause destructive changes, security weaknesses, incompatible APIs, or irreversible data changes, pause the affected operation and obtain approval. Do not block unrelated work while waiting.

## 6. Execute the plan

Unless the user explicitly requests analysis or plan only, treat implementation as part of the task. After preparing the plan:

1. Identify the next executable stage.
2. Check its prerequisites.
3. Implement the required changes.
4. Validate the stage against its acceptance criteria.
5. Review regressions and unintended side effects.
6. Record completed work and unresolved issues.
7. Proceed to the next stage when prerequisites are satisfied.

Do not ask for approval after every stage by default. Continue independently when within the user's request and no consequential decision is required. Pause only when required information, explicit authorization, an external dependency, or a safety constraint prevents responsible continuation. If interrupted, report current state and precise next steps instead of restarting completed work.

## 7. Technology-specific execution rules

Adapt implementation and validation to the actual technologies present.

### React.js
Preserve existing component and state-management conventions. Verify hooks, state updates, API integration, validation, loading states, and error handling. Check accessibility and avoid unnecessary rendering or abstractions. Verify compatibility with backend API contracts.

### Spring Boot and REST APIs
Follow existing architectural boundaries between controllers, services, repositories, and DTOs. Verify request validation, response contracts, error handling, and backward compatibility. Check authentication and authorization for affected endpoints. Avoid exposing persistence entities or sensitive fields without justification.

### Spring Security
Preserve or improve authentication and authorization guarantees. Verify access-control rules, ownership checks, session or token behavior, and relevant CSRF/CORS configuration. Do not weaken security controls. Add regression tests for security-sensitive changes.

### Spring Data JPA and SQL
Verify entity mappings, transaction boundaries, query correctness, constraints, and concurrency behavior. Preserve existing data semantics and historical records. Check performance implications where relevant. Avoid unintended cascade deletes or unsafe bulk operations.

### Flyway
Inspect existing migrations and schema assumptions before making database changes. Prefer new forward migrations when modifying an already-applied schema. Verify migration ordering, compatibility, and data preservation. Do not run destructive migrations against production or shared databases without explicit authorization.

### Testing and build validation
Inspect available project scripts and build configuration before selecting commands. Run targeted tests first and broader checks when appropriate. Add regression tests for confirmed defects. Never claim a build, test, migration, or security check passed unless actually executed and its result inspected. Clearly report checks that could not be performed.

## 8. Scope control and change safety

Implement only changes required by the task or necessary to make them correct and safe. Avoid unrelated refactoring, mass formatting, unnecessary dependency upgrades, and speculative improvements. Preserve existing user changes and uncommitted work. Do not reset, discard, overwrite, or revert unrelated changes. Do not silently change public APIs, database semantics, authentication behavior, or deployment configuration. Never expose secrets in logs, reports, or generated documentation. Obtain explicit approval for consequential destructive operations or changes exceeding requested scope. Keep modifications small, understandable, and reviewable.

If additional problems are discovered, distinguish blockers from optional follow-up improvements. Fix additional issues only when within scope or necessary for correctness and safety; otherwise report them separately.

## 9. Handle blockers and failed validation

When a stage cannot be completed: identify the exact blocker; record what has already been completed; explain impact on subsequent stages; attempt a safe alternative if one exists; ask for user input only when genuinely required; continue independent tasks where possible.

When a test or build fails: inspect the failure output; determine whether introduced by current changes, existed previously, or remains uncertain; fix failures within scope where possible; do not hide, suppress, or weaken tests merely to obtain a passing result. Never report partial work as fully completed.

## 10. Progress tracking

Maintain a concise execution checklist for complex tasks. Track each stage as Pending, In progress, Completed, Blocked, or Skipped with reason. For each completed stage record: changes made; important implementation decisions; validation performed and results; remaining risks or dependencies. Use an existing project tracking document if appropriate. Create a new progress document only when it adds practical value or the user requests one. Avoid repeatedly presenting the entire plan when a concise progress update is sufficient.

## 11. Final deliverable

At completion, provide a factual summary containing:

1. Original objective.
2. Stages completed and any stages skipped or blocked.
3. Important implementation decisions.
4. Files or components changed.
5. Tests, builds, and other checks actually executed.
6. Results and unresolved failures.
7. Security, compatibility, and migration considerations where relevant.
8. Remaining risks and follow-up work.
9. Any user decisions or approvals still required.

Clearly distinguish implemented work from recommendations and unverified assumptions. If the user requested a plan only, provide the plan without modifying application code. If analysis only, provide findings and recommendations without implementing changes.

## 12. Communication standards

Use concise, natural technical language. Avoid AI-generated fluff, repetitive summaries, unnecessary greetings, and generic best-practice lists. Explain technical decisions in the context of the actual task. Prefer actionable findings over lengthy theoretical explanations. Do not claim completion without evidence. Report meaningful blockers promptly instead of waiting until the end.
