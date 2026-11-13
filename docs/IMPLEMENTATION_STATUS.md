# Implementation status

**Updated:** 2026-11-13  
**Branch:** `feat/android-foundation`

This document distinguishes committed source work from functionality that is not yet implemented or verified.

## Available in the repository

- Android application module targeting API 36 with a minimum API of 29.
- Compose-based dark browser workspace.
- Compose prototype tab strip is connected to the engine-independent session controller: create, select, close, and reset tabs. Standard/private are currently domain labels only; actual Chromium regular/OTR profile isolation is not implemented.
- Address drafts are kept per tab; entering text does not initiate network navigation.
- Application-facing browser engine contracts for standard/private profile lifecycle, tab lifecycle, navigation commands and interception, state observation, download requests, site permission requests, file chooser requests, and renderer crashes.
- Chromium POC source revision, baseline ARM64 GN args, WAE package-namespace GN args, a pinned-source integration map, UI adaptation boundary notes, an initial WAE app-name/widget-label patch, and a revision-guarded patch application script (not yet run).
- Engine-independent browser session controller with standard/private tab modes and standard-tab-only persistence projection.
- Durable session repository contract plus a concrete AndroidX DataStore 1.3.0 adapter. The Activity restores on startup and saves tab changes/onStop on an IO dispatcher; writes are serialized through a process-wide mutex. Private-mode domain tabs, unsent address drafts, and transient loading/security state are excluded. This does not yet establish Chromium OTR profile isolation. A single process-wide DataStore instance is reused.
- Address-bar resolver for HTTP(S) destinations and configurable search providers.
- Central top-level navigation policy and coordinator; unsupported schemes are rejected and external mail/tel actions require a direct user gesture.

## Not implemented yet

- A Chromium source checkout/build, validated WAE app-name/launcher icon branding, actual toolbar/tab-switcher UI patches, and WAE-specific Chromium runtime integration.
- Real network navigation/search execution, engine-enforced navigation interception, browser history, and bookmarks.
- Production download execution, offline snapshots, extension runtime, privacy/network enforcement, or sync.
- Release signing and production packaging.

The Compose Activity is a prototype for product/domain behavior, not the final Chromium host. Chromium's `chrome_public_apk` is a complete browser application; WAE runtime integration is selected to adapt `ChromeTabbedActivity` and Chromium's own tab/profile/UI lifecycle rather than embed the APK in the Compose app. The engine API remains a contract, not an implementation. The UI intentionally labels browsing unavailable.

## Verification

No Android build, emulator run, device run, or CI workflow has been executed for this branch. The current environment lacks the Android SDK and configured JDK 17 toolchain. These checks remain deferred under the repository's implementation-first policy.

## Next implementation sequence

1. Complete source-level review of the Chromium patch boundary, then build the unmodified pinned ARM64 baseline and the separate WAE package-namespace variant on a suitable Linux host.
2. Implement WAE branding and browser controls inside the Chromium Android application, preserving ChromeActivity and tab/profile lifecycles.
3. Adapt navigation, downloads, permissions, and renderer recovery using the active APIs at the pinned revision.
4. Implement privacy, offline, extensions, and optional self-hosted sync.
5. Complete security review and documentation.
6. Introduce CI only after the implementation and code-review milestone.
