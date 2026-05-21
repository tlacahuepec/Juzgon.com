---
description: "Use when starting work on a GitHub issue, story, or task. Enforces the in-progress → merged PR → closed workflow."
---

# Issue / Story Workflow

## Starting work

1. Find the relevant GitHub issue.
2. Move it to **In Progress** (assign yourself or update the project board status).
3. Create a feature branch from `main`.

## During development

- Reference the issue number in every commit that relates to it.
- Use the PR template at `.github/pull_request_template.md`.
- Keep the PR focused: one issue → one PR.

## Closing the issue

- Include `Closes #<issue-number>` in the PR description so GitHub auto-closes it on merge.
- Do **not** close the issue manually before the PR is merged.
- After the PR merges, verify the issue moved to **Done / Closed** on the board.

## PR checklist reminder

Before requesting review, confirm:
- [ ] All tests pass (`./gradlew :app:testDebugUnitTest`)
- [ ] Lint passes (`./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck`)
- [ ] PR template filled in (TDD evidence, SOLID checks, scope check)
- [ ] `Closes #N` is in the PR body
