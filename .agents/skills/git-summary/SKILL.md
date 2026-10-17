---
name: git-summary
description: Generate concise Git commit messages and PR descriptions from staged changes (git diff --cached).
---

# Git Summary Skill

## Core Instruction
Analyze only `git diff --cached` (staged changes) and relevant staged files.

---

## Output Requirements

Generate exactly two sections:

### 1. Commit Message
- `<type>: <short summary>`
- 2–5 concise bullets highlighting key changes.

Common conventional `<type>` prefixes:
- `feat`: A new feature
- `fix`: A bug fix
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `style`: Changes that do not affect the meaning of code (formatting, CSS, etc.)
- `docs`: Documentation only changes
- `test`: Adding or correcting tests
- `chore`: Build process, tooling, or dependency updates

### 2. Pull Request Description
```markdown
## Summary
<Concise overview of what was changed and why>

## Changes
- <Key change 1>
- <Key change 2>
- <Key change 3>

## Testing
<Details of executed tests, or "Not run">
```

---

## Strict Rules
1. **Analyze Staged Only**: Ignore all unstaged or unrelated changes. Focus exclusively on `git diff --cached`.
2. **Factual Accuracy**: Never invent functionality, tests, fixes, or database modifications that are not present in the diff.
3. **Explicit Callouts**: Explicitly mention API changes, database schema modifications, or runtime behavior changes when present in the staged files.
4. **Testing Verification**: If testing is not evident from the staged files or verification output, state `Not run`.
5. **Conciseness**: Keep descriptions clear, precise, and concise without boilerplate fluff.
6. **Output Constraint**: Return only the commit message and PR description.
