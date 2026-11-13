# Chromium security baseline and maintenance record

**Review date:** 2026-11-13  
**Pinned WAE source:** Chromium `81ff2496eed313447d34079a6f45c57f9c5611c1`  
**Pinned version:** `157.0.8103.78`  
**Branch position:** `refs/branch-heads/8103@{#1503}`  
**Status:** candidate only; not built, not release-approved.

## Current official release comparison

As of this review, the latest broadly released Chrome for Android Stable update is **157.0.8103.78**, announced on 2026-11-11. Google's Android release note says Android receives the same security fixes as the corresponding Desktop release unless otherwise noted. The pinned source SHA is the exact version-bump commit for 157.0.8103.78, and its commit message records branch position `refs/branch-heads/8103@{#1503}`.

Chrome 158.0.8127.36 for Android was announced on 2026-11-12 as an **Early Stable** release to a small percentage of users. It is tracked, but it is not being substituted for the broadly released Stable baseline solely because it has a higher version number.

The corresponding 157.0.8103.78/.79 Stable desktop security update, announced 2026-11-11, lists **83 security fixes**: five Critical and 78 High. The five Critical issues listed include:

| CVE | Area | Severity |
|---|---|---|
| CVE-2026-126885 | ServiceWorker use-after-free | Critical |
| CVE-2026-126895 | CSS use-after-free | Critical |
| CVE-2026-126879 | Layout use-after-free | Critical |
| CVE-2026-126846 | Skia use-after-free | Critical |
| CVE-2026-126911 | Accessibility use-after-free | Critical |

The update also lists High-severity fixes across V8, Skia, ANGLE, media, extensions, filesystem, browser, and other components. Chromium issue details may remain restricted while the fixes roll out; this record does not infer exploitability or claim that any listed issue was exploited.

## Decision for WAE

- **Keep the current pin for the POC candidate.** It matches the latest broadly released Android Stable build verified in this review.
- **Do not advance to 158 Early Stable yet.** Re-evaluate when a corresponding broadly released Android Stable update is announced and its source SHA, branch head, Android applicability, and security notes can be verified.
- **Do not treat the current pin as permanently current.** A later Stable security release must trigger a source update review before any public APK release.
- **Do not cherry-pick security changes blindly.** Prefer the exact official Stable branch revision after checking the full intervening branch history, dependency rolls, DEPS changes, build configuration, and WAE patch compatibility.
- **Do not ship a stale Chromium build.** If a newer security release is available and has not been integrated and reviewed, block release rather than weakening sandbox, site isolation, certificate validation, renderer protections, or other browser security boundaries.

## Required maintenance procedure

For every Chromium maintenance run:

1. Read the official Chrome Releases Android posts and identify the latest broadly released Stable Android version separately from Early Stable, Beta, and Dev.
2. Read the matching official Stable desktop security post and its full listed security fixes; check Android parity notes and exceptions.
3. Resolve the exact source version-bump commit, full Git SHA, branch name, and `Cr-Commit-Position` from official Chromium branch metadata.
4. Compare the pinned SHA against the target Stable branch head. Review every relevant intervening security fix, cherry-pick, DEPS roll, and toolchain/build change.
5. Verify the target revision still supports API 29 and re-check SDK, Build Tools, NDK, GN args, and `chrome_public_apk` build graph from that exact source.
6. Rebase and review every WAE patch against the exact new source revision. Reject fuzzy or partial patch application.
7. Re-audit the tab/profile, navigation, download, permission, extension, and renderer integration seams before writing adapters.
8. Record the evidence, decision, source SHA, branch position, and unresolved risks here and in `chromium.lock`.

A release update is not complete merely because the version string changed. It is complete only after source provenance, Android applicability, patch compatibility, security impact, and build configuration have been reviewed. Build and device validation remain separate required gates before release.

## Primary sources

- [Chrome for Android 157.0.8103.78 release, 2026-11-11](https://chromereleases.googleblog.com/2026/11/chrome-for-android-update_02063106877.html)
- [Chrome Stable 157.0.8103.78/.79 security update, 2026-11-11](https://chromereleases.googleblog.com/2026/11/stable-channel-update-for-desktop_01744469280.html)
- [Chrome for Android 158.0.8127.36 Early Stable release, 2026-11-12](https://chromereleases.googleblog.com/2026/11/chrome-for-android-update_012297226.html)
- [Official Chromium branch log for 157.0.8103.51..157.0.8103.79](https://chromium.googlesource.com/chromium/src/+log/157.0.8103.51..157.0.8103.79?n=10000&pretty=fuller)
- [Chromium source repository refs and release tags](https://chromium.googlesource.com/chromium/src.git/+refs)

## Source inspection limitation

The exact pinned Gitiles source-file URLs for `ChromeTabbedActivity`, `TabModelSelector`, and `TabModel` were not retrievable during this review. Therefore this review does **not** validate their current signatures or authorize a guessed Java bridge. A full checkout at the exact SHA is still required before implementing or claiming the native bridge.
