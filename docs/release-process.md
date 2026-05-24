# Release Process

## Overview

This document describes how to create a stable release of Juzgon, publish it to GitHub Pages, and verify the deployment. Releases are triggered by pushing a semantic version tag to `main`.

## Prerequisites

- All features for the release are merged into `develop` and passing CI.
- No known regressions on `develop`.
- Data schema version has been bumped if breaking changes were introduced.

## Release Steps

### 1. Promote Develop to Main

```bash
git checkout main
git pull origin main
git checkout develop
git pull origin develop
git checkout main
git merge develop
git push origin main
```

Alternatively, create a release pull request from `develop` to `main` and merge it.

### 2. Create a Version Tag

Use semantic versioning (`vMAJOR.MINOR.PATCH`):

```bash
git checkout main
git pull origin main
git tag v0.1.0
git push origin v0.1.0
```

The tag push triggers the `release-pages.yml` workflow.

### 3. Automated Release Workflow

The workflow performs:
1. Validates the tag format (`v*.*.*`).
2. Runs tests.
3. Builds release APK and AAB.
4. Creates a GitHub Release with attached artifacts.
5. Deploys a download page to `/v0.1.0/` on GitHub Pages.
6. Updates `/latest/` to the new release.
7. Updates `versions.json` index.

### 4. Verify the Release

After the workflow completes:

- [ ] Check [GitHub Releases](https://github.com/tlacahuepec/Juzgon.com/releases) for the new entry.
- [ ] Verify APK and AAB are attached to the release.
- [ ] Visit `/latest/` on GitHub Pages and confirm it points to the new version.
- [ ] Visit `/v0.1.0/` and confirm the download page works.
- [ ] Download and install the APK on a device to verify basic functionality.

## Version Concepts

| Concept | Purpose | Location |
|---------|---------|----------|
| App version | User-visible version (versionName/versionCode) | `build.gradle.kts` |
| Data schema version | Compatibility guard for stored data | `DataSchemaVersion.kt` |
| Backup schema version | JSON export format version | `BackupSchemaContract.kt` |
| Git tag | Triggers release workflow | Git |

When a release includes a breaking data change:
1. Bump `DATA_SCHEMA_VERSION` in `DataSchemaVersion.kt`.
2. Document the breaking change in the release notes.
3. Ensure older app versions show a compatibility warning.

## Published Paths

```text
/Juzgon.com/latest/        → Latest stable release
/Juzgon.com/v0.1.0/        → Immutable versioned release
/Juzgon.com/dev/            → Latest development build
/Juzgon.com/versions.json   → Machine-readable version index
```

## Rollback

If a release has critical issues:

1. **Quick rollback**: Users can access the previous stable version at its versioned URL (e.g., `/v0.0.9/`).
2. **Revert the release**:
   ```bash
   git checkout main
   git revert <merge-commit-sha>
   git push origin main
   git tag v0.1.1
   git push origin v0.1.1
   ```
   This creates a new patch release that reverts the problematic changes.
3. **Delete a broken tag** (emergency only):
   ```bash
   git tag -d v0.1.0
   git push origin :refs/tags/v0.1.0
   ```
   Note: This does not remove already-deployed GitHub Pages content.

## Related Documentation

- [Branching Strategy](branching-strategy.md)
- [Branch Protection Rules](branch-protection.md)
