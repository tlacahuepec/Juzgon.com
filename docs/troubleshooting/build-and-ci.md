# Build and CI Troubleshooting

## Summary

Use this guide when Gradle configuration, Kotlin compiler settings, KSP, formatting, static analysis, or Android CI checks fail. It documents the May 8, 2026 build fixes and provides a reusable checklist for similar issues.

## Affected commands and workflows

Local quality checks:

```bash
./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck
```

Local unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

GitHub Actions workflow:

- `.github/workflows/android-ci.yml`
  - `quality` job runs `./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck`
  - `unit-tests` job runs `./gradlew :app:testDebugUnitTest`

## Issue 1: `kotlinOptions` is unresolved

### Symptoms

Gradle script compilation fails while configuring `:app`:

```text
e: app/build.gradle.kts:38:5: Unresolved reference 'kotlinOptions'.
e: app/build.gradle.kts:39:9: Unresolved reference 'jvmTarget'.
```

### Root cause

The project uses Android Gradle Plugin 9 and Kotlin 2.2. In this setup, the old `android { kotlinOptions { ... } }` DSL is not available. JVM target configuration should use the Kotlin compiler options DSL instead.

### Fix

In `app/build.gradle.kts`, keep Java compatibility in the `android` block:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

Then configure Kotlin separately with `compilerOptions`:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

Do not add `org.jetbrains.kotlin.android` if the current AGP/Kotlin setup already registers the `kotlin` extension; applying it again can fail with an extension name conflict.

## Issue 2: Kotlin source sets are rejected with built-in Kotlin

### Symptoms

After fixing `kotlinOptions`, project configuration can fail with this message:

```text
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
Kotlin source set 'debug' contains: [.../app/build/generated/ksp/debug/kotlin, .../app/build/generated/ksp/debug/java]
Solution: Use android.sourceSets DSL instead.
To suppress this error, set android.disallowKotlinSourceSets=false in gradle.properties.
```

### Root cause

KSP registers generated sources through Kotlin source sets. AGP 9 built-in Kotlin mode disallows this by default unless compatibility is explicitly enabled.

### Fix

Add the compatibility setting in `gradle.properties`:

```properties
android.disallowKotlinSourceSets=false
```

This allows the existing KSP-generated source registration to continue working.

## Issue 3: ktlint requires trailing commas

### Symptoms

`ktlintKotlinScriptCheck` fails with a style error similar to:

```text
app/build.gradle.kts:30:37 Missing trailing comma before ")"
```

### Root cause

The configured ktlint rules require trailing commas in multiline Kotlin argument lists, including Gradle Kotlin DSL files.

### Fix

Add the trailing comma in multiline argument lists. Example:

```kotlin
proguardFiles(
    getDefaultProguardFile("proguard-android-optimize.txt"),
    "proguard-rules.pro",
)
```

## Issue 4: Android SDK location is missing locally

### Symptoms

The CI unit-test task can fail locally before tests run:

```text
Could not determine the dependencies of task ':app:testDebugUnitTest'.
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
or by setting the sdk.dir path in your project's local properties file.
```

When running from WSL, this can also happen if `local.properties` contains a Windows-style path such as:

```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

### Root cause

`local.properties` is local machine configuration and should not be committed. Gradle running inside WSL needs a Linux-readable SDK path, or an `ANDROID_HOME` environment variable that points to a Linux-readable SDK directory.

### Fix

Use one of these local-only fixes:

```bash
export ANDROID_HOME=/mnt/c/Users/<user>/AppData/Local/Android/Sdk
```

Or update the untracked `local.properties` file:

```properties
sdk.dir=/mnt/c/Users/<user>/AppData/Local/Android/Sdk
```

Do not commit `local.properties` because it is machine-specific.

## Issue 5: Compose BOM version cannot be resolved

### Symptoms

Gradle fails while resolving the debug runtime classpath:

```text
Could not find androidx.compose:compose-bom:<version>.
Searched in the following locations:
  - https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/<version>/...
  - https://repo.maven.apache.org/maven2/androidx/compose/compose-bom/<version>/...
```

### Root cause

The Compose BOM version in `gradle/libs.versions.toml` must match a version published to the configured repositories. If the version does not exist, Compose dependencies without explicit versions, such as `androidx.compose.material3:material3`, cannot resolve either.

### Fix

Update `composeBom` in `gradle/libs.versions.toml` to a published version:

```toml
composeBom = "2026.05.00"
```

To check published versions, inspect Google Maven metadata:

```bash
python3 - <<'PY'
import urllib.request

url = "https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml"
print(urllib.request.urlopen(url, timeout=20).read().decode())
PY
```

## Issue 6: XML Material3 theme parent is missing

### Symptoms

Android resource linking fails with a missing theme parent:

```text
Android resource linking failed
ERROR: AAPT: error: resource style/Theme.Material3.DayNight.NoActionBar not found
```

### Root cause

`app/src/main/res/values/themes.xml` extends `Theme.Material3.DayNight.NoActionBar`, which is provided by Material Components for Android (`com.google.android.material:material`). Jetpack Compose Material 3 (`androidx.compose.material3:material3`) does not provide XML theme parents.

### Fix

Declare Material Components in `gradle/libs.versions.toml`:

```toml
material = "1.13.0"
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
```

Then add it to `app/build.gradle.kts`:

```kotlin
implementation(libs.material)
```

## Issue 7: Launcher icon resources are missing

### Symptoms

The CI unit-test task fails during Android resource processing before tests run:

```text
> Task :app:processDebugResources FAILED
Android resource linking failed
ERROR: AndroidManifest.xml: AAPT: error: resource mipmap/ic_launcher not found.
ERROR: AndroidManifest.xml: AAPT: error: resource mipmap/ic_launcher_round not found.
```

### Root cause

`app/src/main/AndroidManifest.xml` references launcher icon resources through `android:icon` and `android:roundIcon`, but the corresponding resources do not exist under `app/src/main/res`.

### Fix

Add launcher icon resources that match the manifest references. For adaptive icons, include:

```text
app/src/main/res/values/colors.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```

The adaptive icon XML files should define a background and foreground drawable:

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

## Issue 8: Gradle wrapper distribution cannot be downloaded

### Symptoms

Running any Gradle command (including `./gradlew`) fails with:

```text
Could not install Gradle distribution from 'https://services.gradle.org/distributions/gradle-X.X-XXX.zip'.
Reason: java.lang.RuntimeException: Could not create parent directory for lock file /home/.gradle/wrapper/dists/gradle-X.X-XXX/hash/gradle-X.X-XXX.zip.lck
```

### Root cause

The Gradle wrapper cache directory (`~/.gradle/wrapper/dists/`) does not exist or is not writable by the current user. This commonly happens when:

1. The home directory or `.gradle` folder has restrictive permissions.
2. Running Gradle inside WSL when the home directory is mounted from Windows with limited permissions.
3. Running Gradle as a different user than who owns `~/.gradle/`.

### Fix

**Option 1: Create and fix permissions (preferred)**

```bash
mkdir -p ~/.gradle/wrapper/dists
chmod 755 ~/.gradle
chmod 755 ~/.gradle/wrapper
chmod 755 ~/.gradle/wrapper/dists
```

**Option 2: Clean the Gradle cache and retry**

```bash
rm -rf ~/.gradle
./gradlew --version
```

This will re-download the wrapper distribution with correct permissions.

**Option 3: WSL-specific fix**

If running inside WSL and the home directory is on Windows, ensure the mount uses Unix-friendly permissions:

```bash
# In /etc/wsl.conf (if it exists, create if needed)
[interop]
appendWindowsPath=true

[automount]
options = "metadata,umask=0022"
```

Then restart WSL:

```bash
wsl --terminate Ubuntu
```

### Verification

After applying the fix, verify by running:

```bash
./gradlew --version
```

Expected result:

```text
------------------------------------------------------------
Gradle X.X
------------------------------------------------------------
```

## Issue 9: `git-town` command is not recognized in PowerShell

### Symptoms

Running Git Town commands from PowerShell fails with:

```text
git-town : The term 'git-town' is not recognized as the name of a cmdlet, function,
script file, or operable program.
```

### Root cause

Git Town is not installed, or the installed executable is not on the current PowerShell `PATH`.

### Fix

Follow the Windows setup guide for installation and verification steps:

- [`docs/android-studio-windows-ssh-setup.md`](../android-studio-windows-ssh-setup.md)

That guide includes package-manager install options, command verification, and the repository-root `git-town sync` workflow.

## Verification

Run the same quality command used by CI:

```bash
./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck --stacktrace
```

Expected result:

```text
BUILD SUCCESSFUL
```

Run the unit-test task used by CI:

```bash
./gradlew :app:testDebugUnitTest --stacktrace
```

Expected result:

```text
BUILD SUCCESSFUL
```

## Known warnings

These warnings may appear after the fixes and do not necessarily mean the build failed:

```text
WARNING: The option setting 'android.disallowKotlinSourceSets=false' is experimental.
The current default is 'true'.
```

```text
Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.
```

For deprecation details, run:

```bash
./gradlew :app:ktlintCheck :app:detekt :app:spotlessCheck --warning-mode all
```

## Related files

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/values/colors.xml`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `.github/workflows/android-ci.yml`

## References

- [Troubleshooting index](README.md)
- [Troubleshooting guide template](TEMPLATE.md)
- [Android CI workflow](../../.github/workflows/android-ci.yml)

