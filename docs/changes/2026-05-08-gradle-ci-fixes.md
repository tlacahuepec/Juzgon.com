# 2026-05-08 — Gradle and CI build fixes

## Context

Both GitHub Actions jobs in `.github/workflows/android-ci.yml` were failing on every run:

- **`quality` job**: `./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck` failed at Gradle script compilation before any task ran.
- **`unit-tests` job**: `./gradlew :app:testDebugUnitTest` failed at Android resource linking.

**Environment at time of fix:**

| Tool | Version |
|------|---------|
| AGP | 9.0.0 |
| Kotlin | 2.2.10 |
| KSP | 2.2.10-2.0.2 |
| Gradle | 9.5.0 |
| Compose BOM | 2026.05.00 |
| Hilt | 2.59.2 |
| Room | 2.8.0 |

## What changed

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Replaced deprecated `android { kotlinOptions { jvmTarget } }` with top-level `kotlin { compilerOptions { jvmTarget.set(...) } }`. Added trailing comma in `proguardFiles(...)` to satisfy ktlint. |
| `gradle.properties` | Added `android.disallowKotlinSourceSets=false` to allow KSP to register generated source sets in AGP 9 built-in Kotlin mode. |
| `gradle/libs.versions.toml` | Added `material = "1.13.0"` version and `material` library entry. |
| `app/src/main/res/values/colors.xml` | Created. Defines `ic_launcher_background` color used by adaptive launcher icons. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Created. Vector drawable used as the adaptive icon foreground. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Created. Adaptive icon definition referenced by `AndroidManifest.xml` `android:icon`. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Created. Adaptive icon definition referenced by `AndroidManifest.xml` `android:roundIcon`. |
| `README.md` | Added `Troubleshooting` and `Changes` sections with links to the new docs. |
| `docs/troubleshooting/README.md` | Created. Troubleshooting index with conventions for adding future guides. |
| `docs/troubleshooting/TEMPLATE.md` | Created. Reusable template for troubleshooting guides. |
| `docs/troubleshooting/build-and-ci.md` | Created. Documents all seven issues found and fixed in this session. |
| `docs/changes/README.md` | Created. Change record index. |
| `docs/changes/TEMPLATE.md` | Created. Reusable template for change records. |
| `docs/changes/2026-05-08-gradle-ci-fixes.md` | Created. This file. |

## Verification

Quality checks:

```bash
./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck --stacktrace
```

Expected result:

```text
BUILD SUCCESSFUL
```

Unit tests:

```bash
./gradlew :app:testDebugUnitTest --stacktrace
```

Expected result:

```text
BUILD SUCCESSFUL
```

Both commands confirmed passing locally on May 8, 2026.

## Risks and known limitations

- `android.disallowKotlinSourceSets=false` is marked experimental by AGP 9. The default will eventually become `true` and enforced. A future migration will need to switch KSP-registered source sets to the `android.sourceSets` DSL or wait for KSP to handle this natively.
- Gradle deprecation warnings indicate incompatibility with Gradle 10. These will become errors in a future toolchain upgrade.
- Launcher icon resources use a minimal vector drawable intended as a placeholder. Production-quality icons should replace these before publishing.

## Follow-ups

- [ ] Replace placeholder launcher icons with production-quality assets before release.
- [ ] Migrate away from `android.disallowKotlinSourceSets=false` once KSP natively supports the `android.sourceSets` DSL.
- [ ] Address Gradle deprecation warnings before upgrading to Gradle 10.

## References

- [Build and CI troubleshooting guide](../troubleshooting/build-and-ci.md)
- [Android CI workflow](../../.github/workflows/android-ci.yml)
- [KSP GitHub issue tracker](https://github.com/google/ksp/issues)

