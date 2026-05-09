# Juzgon.com
Juzgón is a modern Android app where users create custom rating systems for anything: soccer players, cars, movies, pets, and more. Create categories, define attributes, and score items from 1–10. Built with Kotlin, Jetpack Compose, Room, Hilt, and Material 3.

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

