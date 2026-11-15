# Developing MemeHive

**English** | [Русский](development.ru.md)

[Back to README](../README.md) · [Architecture](architecture.md)

MemeHive is a Kotlin Android application with one Gradle module, `app`. The UI uses Jetpack Compose and Material 3; Room stores metadata, and Coil displays images.

## Setup

1. Clone the repository and open its root folder in Android Studio.
2. Install Android SDK Platform **37**, the compile SDK configured in `app/build.gradle.kts`.
3. Use a **JDK 21** Gradle runtime. The committed daemon criteria request JetBrains JDK 21; CI sets up Java 21. The Java/Kotlin compilation target is separately set to JVM 11.
4. Let Android Studio configure the SDK location in your local `local.properties`, then sync Gradle. Do not commit machine-specific SDK paths.
5. Select an emulator or device running Android 10 / API 29 or later, and run the `app` configuration.

Use an Android Studio version compatible with the repository's Android Gradle Plugin. Dependency and plugin versions are defined in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml); the Gradle distribution is pinned in [`gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties). Use the wrapper rather than a separately installed Gradle.

The current build targets API 36 and compiles against API 37. First-time setup needs access to the configured dependency repositories and toolchain downloads. No backend credentials are required.

### Build and install from a terminal

From the repository root:

```sh
./gradlew assembleDebug
./gradlew installDebug
```

`installDebug` requires an available emulator or USB-debugging device. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. On Windows, use `gradlew.bat` in place of `./gradlew`.

```sh
./gradlew assembleRelease
```

The release build does not configure a signing key. Building a release APK is separate from signing and distributing it. Release minification is currently disabled.

## Checks

Run the same non-device checks as CI:

```sh
./gradlew testDebugUnitTest lintDebug assembleRelease assembleDebugAndroidTest
```

Run instrumentation tests with a connected emulator or device:

```sh
./gradlew connectedDebugAndroidTest
```

The workflow in [`.github/workflows`](../.github/workflows) runs device tests on an API 35 x86_64 emulator. `assembleDebugAndroidTest` only builds the test APK; `connectedDebugAndroidTest` executes the tests.

| Test location | Coverage |
| --- | --- |
| `app/src/test` | Search ranking, archive validation, file cleanup, mappings, state, and feedback helpers |
| `app/src/androidTest` | Room behavior and migrations, persistence and transfers, image sharing, Compose interactions, and activity/draft restoration |

Reports are generated under `app/build/reports/`; device reports are under `app/build/reports/androidTests/`.

## Working on the code

Production Kotlin code lives under `app/src/main/java/com/farywave/memehive/`. Start with [Architecture](architecture.md) for the package map and data flow.

- Keep screen rendering in Compose screens, state and user actions in ViewModels, and coordinated data changes in use cases.
- Access repositories from UI code only through use cases, for both reads and writes. This includes ViewModels and activities.
- Keep repositories independent of one another. Coordinate operations involving several repositories in use cases. These [dependency rules](architecture.md#dependency-rules-and-rationale) prevent circular dependencies and keep the entry point for business logic consistent.
- Wire dependencies in `AppContainer`.
- Put user-facing strings in `app/src/main/res/values/strings.xml` and update the Russian resources in `values-ru/strings.xml`.
- Use the existing image persistence and cleanup mechanisms when adding operations that change images.
- Choose tests for the affected behavior. Database, Android URI, and Compose behavior generally require instrumentation tests.

### Database changes

Room schema snapshots are committed under `app/schemas/`. When changing the schema:

1. Increment the database version and implement/register the migration in `MemeHiveDatabase`.
2. Build to regenerate the exported schema.
3. Add or update migration tests and run them on a device/emulator.
4. Commit the generated schema with the migration.

CI checks that building does not leave changed or untracked schema files. Do not replace migrations with destructive recreation of users' libraries.
