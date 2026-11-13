# Implementation status

**Updated:** 2026-11-13  
**Branch:** `feat/android-foundation`

This document distinguishes committed source work from functionality that is not yet implemented or verified.

## Available in the repository

- Android application module targeting API 36 with a minimum API of 29.
- Compose-based dark browser workspace.
- Compose tab strip is connected to the engine-independent browser session controller: create, select, close, and reset tabs.
- Address drafts are kept per tab; entering text does not initiate network navigation.
- Application-facing browser engine interfaces for tab lifecycle, navigation commands, state observation, download requests, site permission requests, file chooser requests, and renderer crashes.
- Chromium POC source revision and ARM64 build notes.\n- Engine-independent browser session controller with standard/private tab modes and standard-tab-only persistence projection.\n- Address-bar resolver for HTTP(S) destinations and configurable search providers.\n- Central top-level navigation policy and coordinator; unsupported schemes are rejected and external mail/tel actions require a direct user gesture.

## Not implemented yet

- A concrete Chromium engine adapter and rendered web content.
- Real network navigation/search execution, engine-enforced navigation interception, browser history, bookmarks, and persistent tab sessions.
- Production download execution, offline snapshots, extension runtime, privacy/network enforcement, or sync.
- Release signing and production packaging.

The UI intentionally labels the engine as offline and does not pretend that address-bar text has been loaded. The engine API is a contract, not an implementation.

## Verification

No Android build, emulator run, device run, or CI workflow has been executed for this branch. The current environment lacks the Android SDK and configured JDK 17 toolchain. These checks remain deferred under the repository's implementation-first policy.

## Next implementation sequence

1. Define durable session/profile persistence interfaces and connect the navigation coordinator to the future engine event stream.
2. Build the pinned Chromium ARM64 target and validate the provisional integration decision in ADR-0001.
3. Implement the engine adapter and connect real navigation/rendering.
4. Add browser lifecycle, permissions, downloads, history, and persistence.
5. Implement privacy, offline, extensions, and optional self-hosted sync.
6. Complete security review and documentation.
7. Introduce CI only after the implementation and code-review milestone.
