# Copilot Instructions — Engineering Constitution

This repository is governed by the **tlacahuepec Engineering Constitution**.
Full document: https://github.com/tlacahuepec/Constitution/blob/main/CONSTITUTION.md
**Project Tier**: 🔧 Tier 2 — Personal Tool

## Immutable Rules

1. Verification-first is mandatory — use TDD for executable code and VDD for non-executable artifacts.
2. Never push directly to `main` or `develop`.
3. All changes go through PRs with CI passing.
4. No secrets ever in the repository.
5. No force-pushes on any branch — protected or feature. History is immutable once pushed.
6. No `git commit --amend`, `git rebase`, or history rewrites after push. Create a new commit.
7. One logical change per PR (one issue per branch).
8. CI must pass before any merge.

## Architecture & Code Style

- Android/Kotlin project. Follow patterns in `app/src/main/` and tests in `app/src/test/`.
- Clean Architecture with strict layered boundaries: `ui` / `feature` (presentation) → `domain` (business logic) ← `data` (infrastructure).
- Respect DIP at layer boundaries — depend on interfaces, not concrete classes.
- Architectural boundaries are validated at compile-time by `./gradlew :app:checkDependencyBoundaries`.

## Build and Test Commands

```bash
./gradlew :app:testDebugUnitTest          # unit tests
./gradlew :app:checkDependencyBoundaries  # clean architecture boundary validation
./gradlew :app:ktlintCheck                # Kotlin formatting
./gradlew :app:detekt                     # static analysis
./gradlew :app:spotlessCheck              # spotless formatting
```

Run lint, static analysis, boundary check, and tests before considering any task done. Fix all failures — do not leave tests or lint in a broken state.

## Conventions

### TDD (non-negotiable)
- Write the failing test **first** (RED), then the implementation (GREEN), then refactor.
- If you touch code that has no test, add one before or alongside your change.
- Never mark a task complete while tests are failing.

### SOLID
- SRP: one reason to change per class/function.
- OCP: extend via new classes/interfaces, not by editing existing ones.
- LSP: subtypes must honor their parent's contract.
- ISP: small, focused interfaces — no fat interfaces.
- DIP: high-level modules depend on abstractions, not details.

### Small Changes
- Each PR addresses a single concern.
- No out-of-scope refactors or unrelated features.
- Follow the Boy Scout Rule only on local touched lines.

### Destructive Actions
- **NEVER** delete, overwrite, or bulk-modify files unless the user explicitly requests it.
- When a task does require a destructive action, **highlight it clearly** before proceeding so the developer can review.

### Branching & PR Workflow
- Always synchronize base before branching:
  - `git fetch origin`
  - `git checkout develop`
  - `git pull origin develop`
- Branch naming: `feat/issue-number-description`, `fix/...`, `hotfix/...`
- Normal feature work targets `develop`. Only release promotions and hotfixes target `main`.
- Close issues only via a merged PR that references the issue (`Closes #N`).
- Use the PR template in `.github/pull_request_template.md`.
