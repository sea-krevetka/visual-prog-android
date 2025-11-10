# visual-prog-android (calc)

A small Android application (module: `app`) that contains several activities including a calculator, media player, and a location logger. This repository uses Gradle Kotlin DSL and a version catalog (`gradle/libs.versions.toml`).

## Quick summary
- Package / namespace: `com.example.calc`
- Module: `app`
- Build system: Gradle (wrapper)
- AGP (from version catalog): 8.13.0
- Kotlin (from version catalog): 2.0.21

This README explains how to set up your development environment on Windows (PowerShell), how to build, and common troubleshooting steps.

## Requirements
- JDK 17 (required by AGP 8.x and Kotlin 2.x) — install Temurin / Adoptium / Oracle / Azul Zulu, etc.
- Android SDK (platforms and build-tools)
- Android SDK command-line tools (sdkmanager)
- Android Studio (recommended) or a working Gradle + SDK installation

Note: the repo uses the Gradle wrapper, so you don't need a globally installed Gradle binary.

## Important project notes (recent/required changes)
- The app module's `build.gradle.kts` has been configured to target JVM 17 (source/target compatibility) and Kotlin `jvmTarget = "17"` to match Kotlin 2.0 / AGP 8.x requirements.
- `Gson` was added to the version catalog and declared as a dependency in the `app` module because `LocationActivity.kt` uses `com.google.gson.Gson`.

If you previously had Java 11 set up, please upgrade to JDK 17 (or configure toolchains accordingly) to avoid build errors.

## Setup on Windows (PowerShell)
1. Install JDK 17 and Android SDK.
2. Configure environment variables (replace paths with your actual install locations):

```powershell
# Example temporary (current session only)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = $env:JAVA_HOME + '\\bin;' + $env:Path

# Example permanent (restart shell after running)
setx JAVA_HOME "C:\Program Files\Java\jdk-17"
setx ANDROID_SDK_ROOT "C:\Users\<yourname>\AppData\Local\Android\Sdk"
```

3. Accept SDK licenses if needed:

```powershell
# Run sdkmanager from your Android SDK tools (adjust path to your sdk)
& "$env:ANDROID_SDK_ROOT\\tools\\bin\\sdkmanager.bat" --licenses
```

## Build (PowerShell)
From the project root (where `gradlew.bat` is located):

```powershell
Set-Location -LiteralPath 'C:\Users\Вика\Documents\ideaa\visual-prog-android'
.\gradlew.bat assembleDebug --no-daemon
```

If you want to install and run on a connected device or emulator:

```powershell
.\gradlew.bat installDebug
```

## Run & Permissions
- `LocationActivity` requests runtime location permissions (ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION). Grant them when prompted at runtime.
- The app writes logged locations to the app-specific external files dir (`getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)`) — no special storage permission required on Android 11+ for app-specific external files, but runtime permissions for location are still required.

## Project structure (files of interest)
- `app/src/main/java/com/example/calc/LocationActivity.kt` — logs location and serializes with Gson.
- `app/src/main/java/com/example/calc/*.kt` — other activities: `MainActivity`, `CalculatorActivity`, `MediaPlayerActivity`, `MusicAdapter`, etc.
- `app/src/main/AndroidManifest.xml` — app manifest and declared activities / permissions.
- `app/build.gradle.kts` — module build config, Java/Kotlin targets, dependencies.
- `build.gradle.kts` (root) and `gradle/libs.versions.toml` — plugin and dependency versions.

## Troubleshooting
- ERROR: JAVA_HOME is not set / no 'java' command found
  - Ensure JDK 17 is installed and `JAVA_HOME` and `Path` are set correctly (see Setup section above).

- Build fails with Java/Kotlin/AGP compatibility issues
  - The repo uses Kotlin 2.x + AGP 8.x which require Java 17. If you cannot upgrade JDK immediately, you can (temporarily) downgrade Kotlin/AGP in `gradle/libs.versions.toml` to compatible versions (not recommended long-term).

- Missing dependency errors for Gson
  - Gson was added to the version catalog (`gradle/libs.versions.toml`) and to `app/build.gradle.kts` as `implementation(libs.gson)`. If you see errors, run a Gradle sync/clean:

```powershell
.\gradlew.bat clean assembleDebug
```

- SDK/platform or license errors
  - Use `sdkmanager` to install required SDK platforms and accept licenses. Example:

```powershell
& "$env:ANDROID_SDK_ROOT\\tools\\bin\\sdkmanager.bat" "platforms;android-36" "build-tools;36.0.0"
& "$env:ANDROID_SDK_ROOT\\tools\\bin\\sdkmanager.bat" --licenses
```

## Contribution and development notes
- Prefer using the Gradle wrapper (`gradlew.bat`) to keep consistent Gradle versions.
- If you change Kotlin or AGP versions, update the `gradle/libs.versions.toml` catalog accordingly.

## License
This repository does not include an explicit license file. Add `LICENSE` if you want to make the project open source under a specific license.

---

If you want, I can also add a development checklist or CI workflow (GitHub Actions) that enforces the JDK/AGP/toolchain and runs a build on every PR. I can also run another pass after you confirm JDK/SDK is available on your machine and paste any failing build logs.
