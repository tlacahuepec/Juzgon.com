# Juzgon.com

This project follows the [Engineering Constitution](https://github.com/tlacahuepec/Constitution).

Juzgón is a modern Android app where users create custom rating systems for anything: soccer players, cars, movies, pets, and more. Create categories, define attributes, and score items from 1–10. Built with Kotlin, Jetpack Compose, Room, Hilt, and Material 3.

## Key features

- **Custom rating categories**: Define any evaluation subject with heterogeneous attribute types (numeric ratings, dates, booleans, dropdowns, URLs, notes, nationalities, and social platforms).
- **Multi-perspective score profiles**: Score the same items through different attribute weighting profiles (e.g., evaluating a player as a "Striker" vs "Playmaker").
- **Visual ranking & radar charts**: Canvas-rendered diamond/radar charts, glowing cards, and real-time aggregate score calculations.
- **AI-powered attribute enrichment**: Google Gemini API integration with Google Search Grounding to automatically discover and suggest metadata values (birth dates, positions, nationalities).
- **Data integrity & portable backups**: Room database (v17) with incremental migrations, automated foreign-key integrity repairs, and versioned JSON export/import.

## Architecture

This project strictly adheres to Clean Architecture:
- **Presentation Layer** (`ui`, `feature`, `navigation`): Jetpack Compose, Material 3, and custom visual tokens.
- **Domain Layer** (`domain`): Core models, calculations, and use cases with zero framework dependencies.
- **Data Layer** (`data`): Room database, encrypted API key storage, Gemini REST client, and backup serializer.

Architectural boundaries are validated at compile-time by `./gradlew :app:checkDependencyBoundaries` (`feature` cannot import `data`, `domain` cannot import `feature` or `data`).

For complete details on the architecture, Room database schema, AI enrichment pipeline, and design tokens, see **[docs/architecture.md](docs/architecture.md)**.

## Getting started

### Prerequisites

- **Java**: 17 or later
- **Android SDK**: API level 26 (Android 8.0) or later
- **Android Studio**: latest stable version recommended

On Windows/WSL, set `ANDROID_HOME` environment variable:

```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Build and run

Build the debug APK:

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Install and run on a connected device or emulator:

```bash
./gradlew :app:installDebug
adb shell am start -n com.juzgon/.MainActivity
```

Or open the project in Android Studio and click **Run** (Shift+F10).

## Code quality and formatting
- Run code format check: `./gradlew :app:spotlessCheck`
- Apply formatting automatically: `./gradlew :app:spotlessApply`
- Run Kotlin lint: `./gradlew :app:ktlintCheck`
- Run static analysis: `./gradlew :app:detekt`

## Troubleshooting
- See [docs/troubleshooting](docs/troubleshooting/README.md) for reusable guides covering build, CI, Gradle, Kotlin, and tooling issues.

## Git workflow (optional: Git Town)

From the repository root, you can sync your branch stack with:

```powershell
git-town sync
```

If PowerShell reports `git-town` as not recognized, see [docs/android-studio-windows-ssh-setup.md](docs/android-studio-windows-ssh-setup.md).

## Change records
- See [docs/changes](docs/changes/README.md) for a log of significant changes, with context, verification, and follow-ups.

