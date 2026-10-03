---
name: module-reviewer
description: Review one explicitly selected logical module at a time, tracing its implementation and tests before reporting concrete findings.
---

# Module Reviewer

Use this skill for a systematic review of a selected module or feature. Do not treat the whole repository as one review. If the user does not identify a module, inspect only enough structure to name logical modules and ask which one to review before conducting a full review.

## Review process

1. Identify the module boundary, responsibility, and relevant files.
2. Find its entry points (manifest, route, activity, handler, service, job, or public API).
3. Trace dependencies and important execution paths through source.
4. Read the complete relevant implementation and related tests.
5. Evaluate only criteria applicable to the module: separation of concerns, dependency direction, coupling/cohesion, abstractions, maintainability, correctness, validation, error handling/logging, resource lifecycle, concurrency, performance, security, API design, transactions, and testability.
6. Identify existing and missing test coverage, edge cases, and any necessary integration/manual verification.
7. Report findings with exact source references and distinguish evidence from assumptions.

## Required report

### Module
Name and repository-relative location.

### Responsibility
What its current role appears to be, based on callers and implementation.

### Current Implementation
Explain the real execution path and key dependencies.

### Architecture / Design
Discuss applicable separation, dependency direction, coupling, cohesion, abstraction, extensibility, and maintainability.

### Code Quality
Cover only applicable concerns such as naming/readability, duplication/complexity, failure and edge-case handling, logging, configuration, lifecycle/resource handling, concurrency, performance, security, validation, transactions, and testability.

### Testing
List relevant existing tests and what they assert. Identify concrete gaps, meaningful edge cases, and any integration/manual testing needs.

### Missing Functionality
Do not infer requirements. Label unresolved requirements **Requires clarification**.

### Technical Debt and Potential Bugs
For each concrete finding include:

```text
Severity: Critical | High | Medium | Low | Informational
Location: path/to/file.ext:line (Class.method)
Problem: observable behavior or risk
Impact: why it matters
Recommendation: smallest reasonable correction
Confidence: high | medium | low
```

Do not elevate style preferences into bugs or assign severity without a consequence.

### Recommended Improvements

- **Must Fix:** correctness, security, reliability, or required functionality.
- **Should Improve:** meaningful maintainability, performance, or testability improvements.
- **Optional:** nonessential refinements.

Keep recommendations scoped; do not propose a rewrite without evidence the current design cannot support the need. Do not modify code during a review unless the user explicitly asks for implementation. Never claim a test ran unless it was actually run.
