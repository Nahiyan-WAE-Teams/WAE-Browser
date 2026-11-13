# Chromium Android integration investigation

**Verification date:** 2026-11-13  
**Status:** Integration route not yet pinned; do not package a WebView substitute.

## 1. Upstream baseline observed

The latest fully announced Chrome for Android Stable build found during this review is 157.0.8103.78 (November 11, 2026). Chrome 158.0.8127.36 was announced as an early Stable rollout on November 12, so it is not treated as the broad Stable baseline for this investigation.

Sources:
- Chrome for Android 157.0.8103.78: https://chromereleases.googleblog.com/2026/11/
- Early Stable Chrome 158 announcement: https://chromereleases.googleblog.com/
- Chrome release channels and stability policy: https://www.chromium.org/chrome-release-channels/

This version observation is not yet an upstream Chromium commit pin. Before a production pin, resolve the exact Android stable branch-head commit, inspect its Android build configuration and security fixes, and record the full SHA.

## 2. Official Android build facts

The Chromium Android build guide currently documents:
- A 64-bit Linux host; Windows and macOS are not supported for this build path.
- At least 100 GB free disk space; more than 16 GB RAM is recommended.
- `depot_tools`, Git, Python, Chromium hooks, and Android build dependencies.
- ARM64 GN configuration using `target_os = "android"` and `target_cpu = "arm64"`.
- The `chrome_public_apk` build target.
- API 29 support for the ChromePublic Android bundle.

Official guide: https://chromium.googlesource.com/chromium/src/+/main/docs/android_build_instructions.md

The Android 10 minimum is therefore consistent with the documented ChromePublic bundle target, but the exact selected revision must still be checked before it is pinned.

## 3. Important packaging distinction

`chrome_public_apk` produces a complete Chromium browser application. It is not an AAR or a drop-in engine dependency that can simply be added to the existing Jetpack Compose app.

Do not:
- Replace Chromium with Android System WebView.
- Treat a Chrome/Chromium APK as an embeddable library.
- Claim that desktop Chrome extension APIs are available just because the project builds Chromium.
- Disable sandboxing, site isolation, certificate checks, or renderer process protections to simplify integration.

## 4. Integration paths to evaluate

### Path A — WAE-branded Chromium Android browser fork

Build from the Chromium Android browser target and adapt the Android browser UI/application layer to WAE product requirements. This keeps browser lifecycle, tabs, permissions, downloads, and engine internals close to the upstream implementation, but requires maintaining a reviewed Chromium patch set and rebasing it on every security update.

### Path B — Custom WAE Android shell over Chromium Content

Build Chromium native/content components and create a separate WAE Android shell using the available Content Java/JNI interfaces. This could allow more freedom for a Compose-first product UI, but the Content interfaces are not a stable, version-independent third-party embedding SDK. Every Chromium revision can require integration changes, and browser-level responsibilities (tabs, permissions, downloads, file chooser, process lifecycle, accessibility, security UI) remain WAE's responsibility.

### Decision gate

Do not choose based on a demo or API name alone. Create a small proof-of-concept on one pinned stable revision and compare:
1. Can it load arbitrary HTTPS pages with Chromium's certificate validation and sandboxing intact?
2. Can it create and destroy independent tabs and recover from renderer crashes?
3. Can it mediate permissions, file chooser, downloads, and navigation without bypassing browser security?
4. Can it build reproducibly for ARM64 and install on Android 10?
5. Can the integration be updated on a two-week Chromium release cadence without unreviewable patch drift?

The first POC should compare both integration paths. A full Chrome APK is a useful upstream build validation, not by itself a WAE Browser release.

## 5. Immediate implementation sequence

1. Resolve and pin the latest fully Stable Android Chromium branch-head commit, not a moving `main` ref or early Stable build.
2. Verify API 29 support, ARM64 build flags, NDK/SDK revisions, toolchain requirements, license inventory, and relevant security fixes at that exact commit.
3. Build the unmodified upstream ARM64 Android browser target on a suitable Linux host.
4. Prototype the narrowest WAE integration on top of that exact source revision.
5. Review the resulting architecture and patch set before moving the app shell or UI into the selected engine integration.
6. Record the exact source SHA, build arguments, patch hashes, third-party notices, and update/rebase procedure.

No Chromium source has been vendored or built in this repository yet.
