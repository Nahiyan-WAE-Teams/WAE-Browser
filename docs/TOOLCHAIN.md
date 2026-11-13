# Android toolchain baseline

**Verification date:** 2026-11-13  
**Status:** Initial Android shell baseline. Re-check all versions before Chromium integration and before release.

## Selected versions

| Component | Pin | Reason |
|---|---:|---|
| Android minimum SDK | 29 | Android 10; matches current Chromium Android default minimum |
| Android compile SDK | 36 | Current stable Android platform API baseline selected for this app shell |
| Android target SDK | 36 | Align target behavior with current stable Android platform |
| Android Gradle Plugin | 9.5.0 | Current stable AGP release found in Android Developers release notes |
| Gradle | 9.6.0 | Minimum/default Gradle version required by AGP 9.5.0 |
| JDK | 17 | AGP 9.5.0 documented JDK requirement |
| Kotlin | 2.4.21 | Latest stable Kotlin version on verification date |
| Jetpack Compose BOM | 2026.09.00 | Current stable Compose BOM in Android Developers setup guidance |
| Activity Compose | 1.13.0 | Version shown in current Android Developers Compose setup guidance |

## Primary-source verification

- Android Gradle Plugin 9.5.0 release notes list Gradle 9.6.0, SDK Build Tools 36.0.0, and JDK 17: https://developer.android.com/build/releases/agp-9-5-0-release-notes
- Android Gradle Plugin compatibility table: https://developer.android.com/build/releases/about-agp
- Kotlin release history identifies 2.4.21 as released October 8, 2026: https://kotlinlang.org/docs/releases.html
- Compose setup guidance specifies Compose BOM 2026.09.00 and Activity Compose 1.13.0: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- Chromium Android build configuration currently sets default_min_sdk_version to 29: https://chromium.googlesource.com/chromium/src/+/master/build/config/android/config.gni
- Android API level mapping identifies Android 10 as API 29: https://source.android.com/docs/setup/reference/build-numbers

## Important constraints

1. The Chromium source link above is a moving upstream branch. Before integrating Chromium, resolve and record an exact commit SHA, then verify the minimum SDK from that pinned revision.
2. AGP 9.5.0 supports up to API 37, but this app shell intentionally compiles and targets API 36 rather than targeting a preview platform.
3. The Android application shell does not yet require an NDK. The NDK version must be selected from the pinned Chromium build requirements, not guessed from the app plugin default.
4. Keep all plugin and library versions pinned. Do not use dynamic versions such as `9.5.+`.
5. Re-check the stable toolchain before release. A version that is current today may be superseded before the first public APK.
6. The toolchain has been source-checked but has not yet been built or validated by Gradle in this repository.

## Build environment

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Gradle 9.6.0
- Android Gradle Plugin 9.5.0
