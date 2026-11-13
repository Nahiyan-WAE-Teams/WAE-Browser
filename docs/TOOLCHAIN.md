# Android toolchain baseline

**Verification date:** 2026-11-13  
**Status:** Reproducible Gradle bootstrap committed; Android build not yet executed.

## Selected versions

| Component | Pin | Reason |
|---|---:|---|
| Android minimum SDK | 29 | Android 10; Chromium Android target documentation confirms API 29 support for ChromePublic bundle |
| Android compile SDK | 36 | Stable Android platform API baseline |
| Android target SDK | 36 | Stable Android platform target |
| Android Gradle Plugin | 9.5.0 | Current stable release; requires Gradle 9.6.0 and JDK 17 |
| Gradle Wrapper | 9.6.0 | Exact AGP minimum/default; binary distribution SHA-256 pinned |
| JDK | 17 | AGP 9.5.0 documented requirement |
| Kotlin compiler | AGP built-in Kotlin | AGP 9 enables built-in Kotlin; legacy kotlin-android plugin is intentionally not applied |
| Compose compiler plugin | 2.4.10 | Matches the Kotlin/AGP compatibility example documented for AGP 9.5.0 |
| Jetpack Compose BOM | 2026.09.00 | Current stable Compose BOM in Android Developers setup guidance |
| Activity Compose | 1.13.0 | Version shown in current Android Developers Compose setup guidance |
| AndroidX DataStore Preferences | 1.3.0 | Current stable DataStore release; used for bounded standard-tab session persistence |
| Lifecycle Runtime KTX | 2.12.0 | Current stable Lifecycle release; supplies Activity lifecycle coroutine scope |

## Compatibility decisions

Kotlin 2.4.21 is the newest stable Kotlin release found on this verification date, but its published compatibility table does not list AGP 9.5.0 as supported. Android's AGP 9.5.0 guidance demonstrates Kotlin plugin 2.4.10. This project therefore uses AGP's built-in Kotlin support and pins the Compose compiler plugin to 2.4.10 rather than combining an unsupported latest-version pair. Re-evaluate this matrix before the first release.

AGP 9.0+ enables built-in Kotlin by default. Do not also apply `org.jetbrains.kotlin.android`; doing so can cause duplicate Kotlin extension/plugin failures. The built-in Kotlin compiler target defaults to the Java compile target, which is pinned to JVM 17 above.

## Primary-source verification

- AGP 9.5.0 release notes list Gradle 9.6.0, SDK Build Tools 36.0.0, and JDK 17: https://developer.android.com/build/releases/agp-9-5-0-release-notes
- Android's AGP compatibility page demonstrates Kotlin plugin 2.4.10 with AGP 9.5.0: https://developer.android.com/build/releases/about-agp
- AGP built-in Kotlin migration guidance says not to apply `org.jetbrains.kotlin.android` on AGP 9+: https://developer.android.com/build/migrate-to-built-in-kotlin
- Kotlin release history identifies 2.4.21 as released October 8, 2026: https://kotlinlang.org/docs/releases.html
- Compose setup guidance specifies Compose BOM 2026.09.00 and Activity Compose 1.13.0: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- AndroidX DataStore 1.3.0 stable release: https://developer.android.com/jetpack/androidx/releases/datastore
- AndroidX Lifecycle 2.12.0 release information: https://developer.android.com/jetpack/androidx/releases/lifecycle
- Gradle 9.6.0 binary distribution SHA-256: `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`, published at https://gradle.org/release-checksums/
- Gradle 9.6.0 Wrapper JAR SHA-256: `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`, published at https://gradle.org/release-checksums/
- Chromium Android build instructions: https://chromium.googlesource.com/chromium/src/+/main/docs/android_build_instructions.md
- Android API level mapping: https://source.android.com/docs/setup/reference/build-numbers

## Wrapper security

The repository includes the official Gradle 9.6.0 Wrapper scripts and Wrapper JAR from the Gradle `v9.6.0` source tag. The Wrapper JAR SHA-256 was computed from the fetched binary and matches Gradle's published `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7` value. The Wrapper distribution URL is pinned to the official Gradle HTTPS host, and `distributionSha256Sum` is set to the independently published distribution checksum. Treat changes to Wrapper scripts or JAR as security-sensitive.

## Chromium toolchain constraints

Chromium is built separately from the Android Gradle project. The pinned revision's Android build instructions require a 64-bit Linux host, at least 8 GB RAM (more than 16 GB recommended), at least 100 GB free disk space, Git and Python, and `depot_tools` plus Chromium hooks. The POC pin in `browser/revisions/chromium.lock` specifies NDK 30.0.16248370, Android SDK platform 37.2, Build Tools 37.0.0, and ARM64 GN arguments `target_os = "android"` and `target_cpu = "arm64"`. These are Chromium's own pinned build inputs and are separate from the Android app shell's AGP SDK configuration.

The pinned Chromium source identifies `chrome_public_apk` as a complete browser APK target and sets the default Android minimum SDK to API 29. It is not a standalone embeddable AAR. WAE Browser must not treat that APK as a drop-in engine library.

## Build environment and validation status

- Required app build environment: JDK 17, Android SDK Platform 36, Build Tools 36.0.0, Gradle 9.6.0.
- The current execution environment has no installed Gradle command or Android SDK, so an Android assemble has not been run here.
- No APK has been produced and no CI workflow has been created.
- Do not mark the app shell as build-verified until a clean checkout can run `./gradlew :android:app:assembleDebug`.
