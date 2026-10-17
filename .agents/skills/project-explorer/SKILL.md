---
name: project-explorer
description: Explain this repository's purpose, architecture, modules, and feature execution paths using the project guide and verified source references.
---

# Project Explorer

Use this skill when asked to explain how this project or a feature works, where behavior starts, what a module owns, how components depend on each other, or which files/tests are relevant before a change.

## Sources and verification

1. Read `docs/PROJECT_GUIDE.md` first for the repository overview and navigation map.
2. Treat the guide as a map, not as authority for current implementation. Inspect the referenced manifest, build file, source, resource, or test whenever the question depends on current behavior or the guide may be stale.
3. Search source when the guide does not cover the requested feature. Follow calls and data across modules instead of inferring behavior from directory names or class names.
4. Cite repository-relative file paths and actual class, method, resource, and test names. Add line numbers when the inspected tool output provides reliable line locations.
5. Distinguish **documented fact**, **verified source behavior**, and **inference/assumption**. State uncertainty plainly; do not invent business requirements.

## Exploration method

- Identify the feature's entry point: manifest activity/service, UI callback, receiver, sensor callback, or other caller.
- Trace the execution in order through the actual methods, dependencies, persistence/external APIs, and result.
- Explain each module's responsibility and relevant dependency direction.
- Name tests that exercise the path; if none exist, say that rather than implying coverage.
- For “where should I change this?” questions, identify the narrowest likely owner and the related callers/tests/docs that need checking. Do not modify files unless implementation is explicitly requested.

Prefer a compact flow:

```text
Entry point (path and method)
    → component (path and method)
    → dependency / persistence / external system
    → result and caller
```

Answer the question directly, explain only relevant modules, and include precise source links/paths where possible.
