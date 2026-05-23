# Branch Protection Rules

## Overview

Branch protection rules ensure that `main` stays stable and `develop` remains validated before changes are merged. These rules are configured in GitHub repository settings under **Settings → Branches → Branch protection rules**.

## Main Branch Rules

The `main` branch represents the latest stable release. Apply these protection rules:

| Rule | Setting |
|------|---------|
| Require a pull request before merging | Enabled |
| Require approvals | 1 approval minimum |
| Dismiss stale pull request approvals when new commits are pushed | Enabled |
| Require status checks to pass before merging | Enabled |
| Required status checks | `quality`, `unit-tests` (from `android-ci` workflow) |
| Require branches to be up to date before merging | Enabled |
| Restrict direct pushes | Enabled (no one can push directly) |
| Allow force pushes | Disabled |
| Allow deletions | Disabled |

### Who Can Merge to Main

Only maintainers should approve and merge pull requests targeting `main`. In practice, release promotion PRs from `develop` to `main` require maintainer review.

## Develop Branch Rules

The `develop` branch is the integration point for feature work. Apply these protection rules:

| Rule | Setting |
|------|---------|
| Require a pull request before merging | Enabled |
| Require status checks to pass before merging | Enabled |
| Required status checks | `quality`, `unit-tests` (from `android-ci` workflow) |
| Require branches to be up to date before merging | Recommended |
| Restrict direct pushes | Enabled |
| Allow force pushes | Disabled |

### Integration Workflow

Feature branches merge into `develop` through pull requests. CI validation (android-ci workflow) must pass before merge is allowed.

## Required Status Checks

Both branches require the following checks from the `android-ci` workflow:

- **quality** — runs ktlint, detekt, spotless, and dependency boundary checks
- **unit-tests** — runs `testDebugUnitTest`

These job names correspond to the jobs defined in `.github/workflows/android-ci.yml`.

## Configuring Branch Protection

1. Navigate to **Settings → Branches** in the GitHub repository.
2. Click **Add branch protection rule**.
3. Enter the branch name pattern (e.g., `main` or `develop`).
4. Enable the rules listed above.
5. Click **Create** or **Save changes**.

## Hotfix Path

For urgent hotfixes that must bypass normal flow, maintainers with admin privileges can merge directly. This should be rare and documented in the commit/PR description.

## Related Documentation

- [Branching Strategy](branching-strategy.md)
- [Release Process](release-process.md)
