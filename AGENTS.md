# AGENTS.md — Engineering Constitution

This repository is governed by the **tlacahuepec Engineering Constitution**.
Full document: https://github.com/tlacahuepec/Constitution/blob/main/CONSTITUTION.md

## Immutable Rules

1. Verification-first is mandatory — use TDD for executable code and VDD for non-executable artifacts.
2. Never push directly to `main` or `develop`.
3. All changes go through PRs with CI passing.
4. No secrets ever in the repository.
5. No force-pushes on any branch — protected or feature. History is immutable once pushed.
6. No `git commit --amend`, `git rebase`, or history rewrites after push. Create a new commit.
7. One logical change per PR (one issue per branch).
8. CI must pass before any merge.

## Verification Protocol (mandatory)

### For executable code

1. Write a failing test that defines the expected behavior.
2. Run the test and confirm it fails.
3. Write the minimum code to make the test pass.
4. Run the test and confirm it passes.
5. Refactor if needed, ensuring tests still pass.

Verification commands:
```bash
./gradlew :app:testDebugUnitTest          # unit tests
./gradlew :app:checkDependencyBoundaries  # clean architecture boundaries
./gradlew :app:ktlintCheck                # Kotlin formatting
./gradlew :app:detekt                     # static analysis
./gradlew :app:spotlessCheck              # spotless formatting
```

### For artifacts, workflows, prompts, templates, and docs

1. Write or update the spec, checklist, schema, README, or acceptance criteria first.
2. Define validation before changing the artifact.
3. Automate validation where practical.
4. Document manual validation in the PR when automation is not practical.
5. Preserve reproducibility and safety.

## Workflow

1. **Check Project Tier**: Read `README.md` for project tier (🔧 Tier 2 — Personal Tool).
2. **Synchronize Base (Mandatory)**: Before creating any branch, ALWAYS run:
   - `git fetch origin`
   - `git checkout develop`
   - `git pull origin develop`
3. **Branch**: `git checkout -b feat/your-feature` using `feat/`, `fix/`, `hotfix/`, or `release/` prefix.
4. **Keep Updated**: Regularly merge `develop` into your feature branch while working.
5. **Commit**: Imperative mood, reference issue, < 72 chars.
6. **Pre-Push Validation**: Run linter, formatter, tests, and boundary checks.
7. **PR**: Must include issue link, summary, test/validation plan, and checklist.

## DO NOT

- Do NOT commit directly to `main` or `develop`.
- Do NOT use `git push --force` on any branch.
- Do NOT use `git commit --amend` or rebase after pushing to a remote.
- Do NOT bypass CI or skip hooks (`--no-verify`).
- Do NOT mix refactoring and feature behavior changes in the same PR (Pure Refactoring PRs).
- Do NOT create catch-all `utils/` or `common/` junk drawers; use single-purpose semantic modules.
- Do NOT exceed folder depth limit of 4 levels from source root.
- Do NOT suppress lint, PMD, or static analysis warnings. Fix the issue instead.
- Do NOT self-approve PRs — wait for human review.
- Do NOT commit secrets, credentials, or `.env` files.
- Do NOT commit large model files, generated output batches, private images, or machine-specific artifacts.
- Do NOT make destructive changes without explicit human authorization.
- Do NOT skip tests or validation because the repository has no traditional production code.

## Always

- Always check project tier in `README.md` before applying standards.
- Always add tests for executable code (TDD).
- Always add validation for non-executable artifacts (VDD).
- Write characterization tests before refactoring existing code.
- Package by Feature and enforce Clean Architecture inward dependency flow.
- Follow the Boy Scout Rule only for local touched lines.
- Use SOLID principles where software design is involved.
- Prefer small, focused changes.

## Full Standards

Read `CONSTITUTION.md` in this repository for complete standards on branching, CI/CD, security, documentation, and technology-specific extensions.
