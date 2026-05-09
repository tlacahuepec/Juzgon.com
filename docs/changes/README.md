# Change Records

Use this directory to trace **why** significant changes were made, what was changed, what was verified, and what risks or follow-ups remain.

Change records are not release notes and are not user-facing. They exist so future contributors can understand the reasoning behind a change without reading every commit.

## When to add a change record

- A build, CI, or tooling fix that affects multiple files.
- A dependency upgrade with a non-obvious rationale.
- A refactor or architectural decision that future maintainers should understand.
- Any fix that required non-obvious investigation.

## When NOT to add a change record

- Routine feature additions (prefer clear commit messages and PR descriptions).
- Minor wording, comment, or formatting-only changes.

## Naming convention

Use the format `YYYY-MM-DD-short-slug.md`, where the date is when the change was finalized.

Example: `2026-05-08-gradle-ci-fixes.md`

## Adding a new change record

1. Copy [`TEMPLATE.md`](TEMPLATE.md) into a new file using the naming convention above.
2. Fill in every section. Delete sections that are not applicable.
3. Link the new record in the **Records** table below.

## Records

| Date | Slug | Summary |
|------|------|---------|
| 2026-05-08 | [gradle-ci-fixes](2026-05-08-gradle-ci-fixes.md) | Fixed Gradle/Kotlin/AGP9 build failures blocking both CI jobs |

