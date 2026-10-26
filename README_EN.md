# Full Charge Alert

[简体中文](README.md) | [English](README_EN.md)

[![Flutter Android](https://github.com/Kuugo2002/phone-charge-notice/actions/workflows/flutter-android.yml/badge.svg)](https://github.com/Kuugo2002/phone-charge-notice/actions/workflows/flutter-android.yml)

A Flutter and Material 3 Android app that wakes the screen and sends a notification when a connected phone first reaches 100% battery.

## Features

- Live battery level and charging status.
- An Android foreground service monitors the battery independently of the Flutter screen.
- Screen wake, vibration, and lock-screen notifications at full charge; one alert per charging session, reset when unplugged.
- Monitoring resumes after reboot and app updates.
- Built-in test alert and system notification settings.
- Optional Root enhancement: Doze allowlisting and an additional screen wake command.
- Four theme colors: green, blue, purple, and orange.
- System, light, and dark appearance modes, applied immediately and saved.

## Installation and use

Requires Android 8.0 (API 26) or later.

1. Install the APK, open the app, and allow notifications.
2. Tap the test alert button to check screen wake and notifications.
3. Choose a theme color and appearance mode in Settings. Enable Root enhancement if desired on a rooted device.

Some manufacturers also require allowing auto-start, background activity, and lock-screen notifications. Root is optional for basic alerts.

## Download an APK from GitHub Actions

Open the [Flutter Android workflow](https://github.com/Kuugo2002/phone-charge-notice/actions/workflows/flutter-android.yml), select a successful run, and download `charge-notice-debug-RUN_NUMBER` from **Artifacts**. Extract `app-debug.apk` to install it. Artifact downloads require signing in to GitHub; artifacts are retained for 14 days.

Each push, pull request, and manual run installs locked dependencies, checks formatting, runs analysis and tests, builds the Android debug APK, and uploads it. CI uses Flutter **3.47.8 stable** and JDK **17**, matching the locally verified environment.

CI provides debug-signed APKs for testing. Configure your own stable signing key for releases; APKs with different signatures cannot update one another directly.

## Local development

Install Flutter **3.47.8 stable**, JDK **17**, and Android SDK **36**. Open the project root in Android Studio with the Flutter plugin or VS Code.

```sh
flutter doctor
flutter pub get --enforce-lockfile
flutter run
```

Run the same checks and build as CI:

```sh
dart format --output=none --set-exit-if-changed lib test
flutter analyze --no-pub
flutter test --no-pub
flutter build apk --debug --no-pub
```

The APK is generated at `build/app/outputs/flutter-apk/app-debug.apk`. Commit `pubspec.lock` and update it alongside dependency changes. The app version is maintained in `pubspec.yaml`.

Before distributing a release, configure signing in `android/app/build.gradle` and run `flutter build apk --release`. Keep signing keys and local configuration out of version control.

## Project structure

```text
.github/workflows/flutter-android.yml  Checks and APK builds
lib/main.dart                         Flutter UI and themes
lib/charge_controller.dart            State and platform calls
android/                              Android host and background service
test/                                 Flutter tests
pubspec.yaml                          Version and dependencies
pubspec.lock                          Locked dependencies
```

MethodChannel handles Android operations; EventChannel streams battery changes to Flutter. Background monitoring runs independently of the UI. The package name `com.kuugo.chargenotice` and native preferences are preserved, allowing existing settings to survive updates signed with the same key. Only Android is supported.

Build output, caches, SDK paths, and signing files are excluded through `.gitignore`. APKs are distributed through Actions rather than committed to the source repository.

## Build troubleshooting

- **Incompatible Java version:** run `flutter config --jdk-dir="path to JDK 17"` and rebuild.
- **LSP JSON parsing errors on Windows paths containing Chinese characters:** map the project with `subst Q: "absolute project path"`, run checks from `Q:\`, then switch to another directory and remove the mapping with `subst Q: /D`.
- **Slow first build:** Gradle installs the required NDK and CMake when needed. Accept Android SDK licenses with `flutter doctor --android-licenses`.
