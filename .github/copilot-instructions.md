# Project Guidelines

## Code Style

Android/Kotlin project. Follow patterns in `app/src/main/` and tests in `app/src/test/`.

## Architecture

Clean architecture with layered boundaries (data / domain / presentation). Respect DIP at layer boundaries — depend on interfaces, not concrete classes.

## Build and Test

```
./gradlew :app:testDebugUnitTest          # unit tests
./gradlew :app:ktlintCheck                # Kotlin formatting
./gradlew :app:detekt                     # static analysis
./gradlew :app:spotlessCheck              # spotless formatting
```

Run lint and tests before considering any task done. Fix all failures — do not leave tests or lint in a broken state.

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

### Destructive Actions
- **NEVER** delete, overwrite, or bulk-modify files unless the user explicitly requests it.
- When a task does require a destructive action, **highlight it clearly** before proceeding so the developer can review.

### Issue / Story Workflow
- When picking up a GitHub issue, move it to **In Progress** first.
- Close issues only via a merged PR that references the issue (`Closes #N`).
- Use the PR template in `.github/pull_request_template.md`.
