# Implementation status

**Updated:** 2026-11-13  
**Branch:** `feat/android-foundation`

This document distinguishes committed source work from functionality that is not yet implemented or verified.

## Available in the repository

- Android application module targeting API 36 with a minimum API of 29.
- Compose-based dark browser workspace.
- Compose tab strip is connected to the engine-independent browser session controller: create, select, close, and reset tabs; standard and private tabs can be created separately.
- Address drafts are kept per tab; entering text does not initiate network navigation.
- Application-facing browser engine contracts for standard/private profile lifecycle, tab lifecycle, navigation commands and interception, state observation, download requests, site permission requests, file chooser requests, and renderer crashes.
- Chromium POC source revision and ARM64 build notes.
- Engine-independent browser session controller with standard/private tab modes and standard-tab-only persistence projection.
- Durable session repository contract plus a concrete AndroidX DataStore 1.3.0 adapter. The Activity restores on startup and saves tab changes/onStop on an IO dispatcher; private tabs, unsent address drafts, and transient loading/security state are excluded. A single process-wide DataStore instance is reused.
- Address-bar resolver for HTTP(S) destinations and configurable search providers.
- Central top-level navigation policy and coordinator; unsupported schemes are rejected and external mail/tel actions require a direct user gesture.

## Not implemented yet

- A concrete Chromium engine adapter and rendered web content.
- Real network navigation/search execution, engine-enforced navigation interception, browser history, and bookmarks.
- Production download execution, offline snapshots, extension runtime, privacy/network enforcement, or sync.
- Release signing and production packaging.

The UI intentionally labels the engine as offline and does not pretend that address-bar text has been loaded. The engine API is a contract, not an implementation.

## Verification

No Android build, emulator run, device run, or CI workflow has been executed for this branch. The current environment lacks the Android SDK and configured JDK 17 toolchain. These checks remain deferred under the repository's implementation-first policy.

## Next implementation sequence

1. Review and harden the session persistence implementation, then build the pinned Chromium ARM64 target and validate the provisional integration decision in ADR-0001.
2. Implement the engine adapter and connect real navigation/rendering.
3. Add browser lifecycle, permissions, downloads, and history.
4. Implement privacy, offline, extensions, and optional self-hosted sync.
5. Complete security review and documentation.
6. Introduce CI only after the implementation and code-review milestone.
