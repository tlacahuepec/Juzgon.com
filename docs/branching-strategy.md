# Branching Strategy

## Branch Roles

| Branch | Purpose | Deploys To |
|--------|---------|------------|
| `main` | Latest stable release | GitHub Pages `/latest/` |
| `develop` | Integration branch for new work | GitHub Pages `/dev/` |
| `feature/*` | Individual work items | — |
| `hotfix/*` | Urgent fixes to stable | — |

## Main — Latest Stable

The `main` branch always represents the latest stable, production-ready state of the application. Only tested, reviewed code reaches `main` through controlled promotion from `develop` or hotfix merges.

Direct pushes to `main` are not allowed. All changes arrive via pull request.

## Develop — Integration Branch

The `develop` branch is the integration point for all new work. Feature branches merge into `develop` through pull requests. CI validation runs on every PR to `develop`.

When `develop` is ready for a stable release, it is promoted to `main` through a release pull request.

## Feature Branches

All new work happens on feature branches created from `develop`.

### Naming Convention

```
feature/issue-<number>-<short-description>
```

Examples:
- `feature/issue-145-branching-strategy`
- `feature/issue-141-data-schema-compatibility`

### Workflow

1. Create branch from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/issue-123-description
   ```
2. Implement with TDD (RED → GREEN → REFACTOR).
3. Push branch and open a pull request targeting `develop`.
4. CI validation must pass before merge.
5. Squash-merge into `develop` after approval.

## Release Promotion

Promotion from `develop` to `main` follows a controlled process:

1. Ensure `develop` is stable (CI passing, no known regressions).
2. Open a pull request from `develop` to `main`.
3. Review and merge the release PR.
4. Tag the merge commit on `main`:
   ```bash
   git checkout main
   git pull origin main
   git tag v<major>.<minor>.<patch>
   git push origin v<major>.<minor>.<patch>
   ```
5. The tag push triggers the release workflow which publishes artifacts.

## Hotfix Flow

For urgent fixes that must reach stable without waiting for the next release:

1. Branch from `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b hotfix/description
   ```
2. Implement the fix with tests.
3. Open a pull request targeting `main`.
4. After merge to `main`, tag a patch release.
5. Cherry-pick or merge the fix back into `develop` to keep branches in sync.

## Branch Flow Diagram

```
feature/issue-123   feature/issue-456
        \                  /
         \                /
          ↓ PR          ↓ PR
         develop ←──────────
              \
               ↓ release PR
              main
               \
                ↓ tag
              v1.0.0
```

## Related Documentation

- [Branch Protection Rules](branch-protection.md)
- [Release Process](release-process.md)
