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
- Chromium POC source revision, baseline ARM64 GN args, WAE package-namespace GN args, a pinned-source integration map, UI adaptation boundary notes, native tab-state and Chromium-owned download bridge design specifications (observer lifecycle, OTR privacy, safe download URI handling, action boundaries, and acceptance criteria), an initial WAE app-name/widget-label patch, a tabbed-window system-theme patch, and a revision-guarded patch application script with a non-mutating ordered-series preflight mode (neither mode has been run against a Chromium checkout).
- Engine-independent browser session controller with standard/private tab modes, bounded tab IDs, non-negative creation timestamps, and standard-tab-only persistence projection. Restore rejects private tabs, duplicate IDs, invalid active-tab references, unsafe destinations, and more than 50 tabs before mutating current state; it discards unsent address drafts and transient loading progress.
- Durable session repository contract plus a concrete AndroidX DataStore 1.3.0 adapter. The Activity restores on startup and saves tab changes/onStop on an IO dispatcher. Background writes and explicit session clearing share one process-wide mutex, preventing an already-queued save from being applied after a confirmed reset. Stored-format failures (malformed JSON, unsupported schema, or invalid persisted values) are distinguished from transient DataStore I/O failures: only invalid stored data offers a destructive reset; transient failures offer retry and exit without changing stored data. The recovery UI displays actionable error feedback, preserves data until explicit reset, and rethrows coroutine cancellation. Reset clears only the session key before creating a new tab. Private-mode domain tabs, unsent address drafts, and transient loading/security state are excluded. This does not yet establish Chromium OTR profile isolation. A single process-wide DataStore instance is reused.
- Address-bar resolver for HTTP(S) destinations and configurable search providers; explicit destination-port validation rejects port zero and values outside the valid TCP/UDP port range; address input and top-level navigation URLs are bounded to 8,192 characters.
- Bounded, device-local bookmark storage using a dedicated DataStore file, with add/update, list, and remove operations; HTTP(S)-only URL validation, duplicate URL handling, unique bounded IDs, non-negative creation timestamps, valid destination-port checks, rejection of control characters in persisted IDs/titles, and removal of invisible control/bidirectional override characters from user-entered bookmark titles, safe-ID validation on removal, and cancellation-preserving error handling for reads, plus 500-item and payload-size limits. Bookmark policy tests are authored but not executed. Selecting a bookmark copies its URL into the address field; it does not navigate until a real engine is connected.
- Central top-level navigation policy and coordinator; unsupported top-level schemes are rejected and external mail/tel actions require a direct user gesture. Subframe navigation is deliberately delegated to Chromium's own origin/renderer policy.
- JVM unit test source covering HTTP(S)-only address resolution, invalid ports and URL lengths, top-level versus subframe policy boundaries, private-tab persistence projection, restore validation and state preservation, draft stripping, bounded per-tab input, HTTP(S)-only navigation commits, and gesture-gated external actions (rejecting unsafe HTTP destinations); test code is authored but has not been executed.

## Not implemented yet

- A Chromium source checkout/build, validated WAE app-name/launcher icon branding, actual toolbar/tab-switcher UI patches, and WAE-specific Chromium runtime integration. The new system-theme patch is presentation-only and has not been applied or built. The pinned-source map now records the actual Android download bridge and the mobile extension buildflag limitation.
- Real network navigation/search execution, engine-enforced navigation interception, and browser history.
- Production download execution, offline snapshots, extension runtime, privacy/network enforcement, or sync. The pinned Chromium source audit also found that the normal Android phone GN target disables both the full desktop extension platform and `enable_extensions_core` by default; mobile Manifest V3 support needs additional source/build/runtime work and cannot be claimed from an install UI. The native tab-state and download bridge documents are specifications, not implemented adapters.
- Release signing and production packaging.

The Compose Activity is a prototype for product/domain behavior, not the final Chromium host. Chromium's `chrome_public_apk` is a complete browser application; WAE runtime integration is selected to adapt `ChromeTabbedActivity` and Chromium's own tab/profile/UI lifecycle rather than embed the APK in the Compose app. The engine API remains a contract, not an implementation. The UI intentionally labels browsing unavailable.

## Verification

No Android build, emulator run, device run, or CI workflow has been executed for this branch. The current environment lacks the Android SDK and configured JDK 17 toolchain. These checks remain deferred under the repository's implementation-first policy. The recovery changes are source-reviewed only; they have not been compiled or exercised against DataStore.

## Next implementation sequence

1. Inspect the exact pinned Chromium source and GN targets in a full checkout; verify the tab selector/model observer signatures and choose the correct owning coordinator.
2. Implement the read-only native tab-state bridge from `browser/integration/TAB_STATE_BRIDGE.md`, with attach/detach coverage and OTR privacy guarantees.
3. Build the unmodified pinned ARM64 baseline and the separate WAE package-namespace variant on a suitable Linux host.
4. Implement WAE branding and browser controls inside the Chromium Android application, preserving ChromeActivity and tab/profile lifecycles.
5. Adapt navigation, downloads, permissions, and renderer recovery using the active APIs at the pinned revision; use `browser/integration/DOWNLOAD_BRIDGE.md` for the download ownership and security contract.
6. Implement privacy, offline, extensions, and optional self-hosted sync.
7. Complete security review and documentation.
8. Introduce CI only after the implementation and code-review milestone.
