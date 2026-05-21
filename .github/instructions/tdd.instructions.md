---
description: "Use when writing, modifying, or reviewing any Kotlin/Android code. Enforces TDD Red-Green-Refactor cycle, SOLID principles, and test coverage for touched code."
applyTo: "**/*.kt"
---

# TDD & SOLID Rules

## Red-Green-Refactor (mandatory)

1. **RED** — write a failing test that describes the desired behavior.
2. **GREEN** — write the minimum production code to make it pass.
3. **REFACTOR** — clean up duplication and design issues while keeping tests green.

Never write implementation before the test exists.

## Test Coverage for Touched Code

If you edit a function or class that has no unit test, add one before finishing the task.

## Test Location

| Production code | Test file |
|-----------------|-----------|
| `app/src/main/java/**/*.kt` | `app/src/test/java/**/*Test.kt` |

## SOLID Checklist (apply to every change)

- **SRP**: Does this class/function have exactly one reason to change?
- **OCP**: Are you extending via a new class rather than editing an existing one?
- **LSP**: Do subclasses honor the parent contract without surprises?
- **ISP**: Are interfaces small and role-specific?
- **DIP**: Do higher layers depend on interfaces, not concrete implementations?

## Lint & Tests Gate

After every change, run:
```
./gradlew :app:testDebugUnitTest :app:ktlintCheck :app:detekt :app:spotlessCheck
```
Fix all failures before declaring the task done.
