# Chromium Android integration investigation

**Verification date:** 2026-11-13  
**Status:** A stable Chromium source revision is pinned for the first proof-of-concept; the integration path remains under evaluation.

## 1. Stable source baseline pinned for the POC

The latest fully announced Chrome for Android Stable build found during this review is **157.0.8103.78**, released November 11, 2026. Android releases carry the same security fixes as their corresponding desktop releases (157.0.8103.78/.79). Chrome 158.0.8127.36 was announced on November 12 as an early Stable rollout to a small percentage of users, so it is not used as the broad Stable baseline here.

- Chromium source commit: `81ff2496eed313447d34079a6f45c57f9c5611c1`
- Version commit position: `refs/branch-heads/8103@{#1503}`
- Branch: `refs/branch-heads/8103`
- Source: https://chromium.googlesource.com/chromium/src.git
- Mirror commit metadata: https://github.com/chromium/chromium/commit/81ff2496eed313447d34079a6f45c57f9c5611c1

The exact revision and toolchain values are recorded in [browser/revisions/chromium.lock](../browser/revisions/chromium.lock). This is a POC pin, not a claim that WAE Browser is already built or release-ready.

Sources:
- Chrome for Android 157.0.8103.78 release and security-fix parity: https://chromereleases.googleblog.com/2026/
- Chrome 158 early Stable rollout: https://chromereleases.googleblog.com/2026/
- Chromium commit metadata and branch position: https://chromium.googlesource.com/chromium/src/+/81ff2496eed313447d34079a6f45c57f9c5611c1

## 2. Toolchain verified at the exact Chromium commit

The following values were read from the pinned revision's own source files, not inferred from the current moving branch:

| Setting | Pinned value | Source |
|---|---:|---|
| Chromium default minimum SDK | 29 | `build/config/android/config.gni` |
| Chromium default target SDK | 36 | `build/config/android/config.gni` |
| Android SDK platform | 37.2 | `build/config/android/config.gni` |
| Android Build Tools | 37.0.0 | `build/config/android/config.gni` |
| Android NDK | 30.0.16248370 | `DEPS` |
| Initial ABI | ARM64 | Chromium Android build guide |
| Build target | `chrome_public_apk` | Chromium Android build guide |

Pinned source links:
- [Android config at the pinned commit](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/build/config/android/config.gni)
- [DEPS at the pinned commit](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/DEPS)
- [Android build guide at the pinned commit](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/docs/android_build_instructions.md)

The Android 10 minimum is therefore confirmed at this exact revision. The app's existing compile SDK 36 is retained for the independent Compose shell; the Chromium toolchain itself uses the pinned SDK platform 37.2.

## 3. Official Android build requirements

The pinned Chromium Android build guide documents:
- A 64-bit Linux host; Windows and macOS are not supported for this build path.
- At least 8 GB RAM, with more than 16 GB highly recommended.
- At least 100 GB free disk space.
- Git, Python, `depot_tools`, Chromium hooks, and Android build dependencies.
- Siso and GN for generating and executing the build.
- ARM64 GN configuration using `target_os = "android"` and `target_cpu = "arm64"`.
- `autoninja -C out/Default chrome_public_apk` as the documented build command.

The minimal initial GN arguments are committed in [browser/build/args/arm64.gn](../browser/build/args/arm64.gn). Do not add remote-execution or release flags until the host environment and reproducibility requirements are selected.

## 4. Important packaging distinction

`chrome_public_apk` produces a complete Chromium browser application. It is not an AAR or a drop-in engine dependency that can simply be added to the existing Jetpack Compose app.

Do not:
- Replace Chromium with Android System WebView.
- Treat a Chrome/Chromium APK as an embeddable library.
- Claim that desktop Chrome extension APIs are available just because the project builds Chromium.
- Disable sandboxing, site isolation, certificate checks, or renderer process protections to simplify integration.

## 5. Integration paths to evaluate

### Path A — WAE-branded Chromium Android browser fork

Build from the Chromium Android browser target and adapt the Android browser UI/application layer to WAE product requirements. This keeps browser lifecycle, tabs, permissions, downloads, and engine internals close to the upstream implementation, but requires maintaining a reviewed Chromium patch set and rebasing it on every security update.

### Path B — Custom WAE Android shell over Chromium Content

Build Chromium native/content components and create a separate WAE Android shell using the available Content Java/JNI interfaces. This could allow more freedom for a Compose-first product UI, but the Content interfaces are not a stable, version-independent third-party embedding SDK. Every Chromium revision can require integration changes, and browser-level responsibilities (tabs, permissions, downloads, file chooser, process lifecycle, accessibility, security UI) remain WAE's responsibility.

### WebLayer / WebEngine status

Chromium's newer WebEngine work is also worth tracking, but it is not an automatic production choice. Upstream documents that the WebLayer API was deprecated as of M108 and is being merged into WebEngine. The current WebEngine README says the Android browser-process sandbox boundary does not yet exist; its limited/makeshift sandbox mode is described as testing-only. That is not sufficient evidence for WAE's production security requirements.

Source: https://chromium.googlesource.com/chromium/src/+/f5be305cce08118a4983ac2fa7441ff30eca8aad/weblayer/README.md

Treat WebEngine as a research candidate only. Reconsider it only after its public API, supported release status, Android 10 compatibility, renderer/browser process isolation, and production sandbox guarantees are verified at the exact pinned revision.

### Decision gate

Do not choose based on a demo or API name alone. Create a small proof-of-concept on the pinned revision and compare:
1. Can it load arbitrary HTTPS pages with Chromium's certificate validation and sandboxing intact?
2. Can it create and destroy independent tabs and recover from renderer crashes?
3. Can it mediate permissions, file chooser, downloads, and navigation without bypassing browser security?
4. Can it build reproducibly for ARM64 and install on Android 10?
5. Can the integration be updated on the two-week Chromium release cadence without unreviewable patch drift?

The first POC should compare both integration paths. A full Chrome APK is a useful upstream build validation, not by itself a WAE Browser release.

## 6. Immediate implementation sequence

1. Build the unmodified pinned Chromium ARM64 Android browser target on a suitable Linux host.
2. Prototype the narrowest WAE integration on top of that exact source revision.
3. Review the resulting architecture and patch set before moving the app shell or UI into the selected engine integration.
4. Record the exact source SHA, build arguments, patch hashes, third-party notices, and update/rebase procedure.

No Chromium source has been vendored or built in this repository yet.
