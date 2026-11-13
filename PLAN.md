# WAE Browser implementation plan

This plan is the working sequence. Each phase must have explicit acceptance criteria and a review before moving on.

## Phase 0 — Repository and engineering foundation

- [x] Establish product decisions and repository documentation.
- [x] Set Android 10 (API 29) as the minimum supported OS.
- [ ] Select the application license after reviewing Chromium and third-party obligations.
- [ ] Define minimum device requirements beyond OS version (RAM, storage, CPU/ABI).
- [x] Verify current stable Android Gradle Plugin, Kotlin, Gradle, and Compose versions from primary sources; record them in docs/TOOLCHAIN.md.
- [x] Add official Gradle 9.6.0 Wrapper scripts/JAR and pin the distribution SHA-256.
- [x] Verify and pin the NDK and exact Chromium POC revision from primary sources.
- [x] Validate that the selected Chromium revision supports the Android 10 (API 29) floor.
- [ ] Establish reproducible build inputs, dependency pinning, and an SBOM strategy.
- [ ] Define signing-key custody and release provenance.

**Exit:** approved toolchain matrix, verified Android API floor, build design, license decision, and security review checklist.

## Phase 1 — Android shell and product UI

- [x] Create the initial Android application module and Compose baseline.
- [x] Add the Gradle Wrapper and split out the engine API contract module (actual Android build validation still pending).
- [x] Implement initial browser workspace, local tab model, and per-tab address drafts.
- [x] Add engine-independent session models, standard/private tab modes, and safe HTTP(S)/search address resolution.
- [x] Define a persistence boundary that excludes private tabs and retains the active standard tab.
- [x] Implement a bounded DataStore persistence adapter; restore and save standard tabs through activity lifecycle callbacks, excluding private tabs and unsent address drafts.
- [x] Implement bounded local bookmark persistence and a bookmark management surface with HTTP(S)-only validation, duplicate URL updates, and remove support (navigation remains pending Chromium integration).
- [ ] Connect persisted session state to real engine navigation.
- [ ] Complete navigation controls and settings.
- [ ] Establish design tokens, typography, accessibility, responsive layouts, and restrained motion.
- [ ] Respect system animation settings and provide an in-app motion preference.
- [ ] Add UI state and navigation tests, including API 29 coverage.

**Exit:** installable shell with no claim of production browsing until the engine is integrated.

## Phase 2 — Chromium integration

- [x] Investigate official Chromium Android build requirements and distinguish a full browser APK from an embeddable engine library.
- [x] Resolve and pin an exact fully Stable Android Chromium upstream revision and record its security baseline.
- [x] Verify the pinned revision's API 29 floor, Android SDK, Build Tools, NDK, and ARM64 build target.
- [x] Select Chromium's Android browser application as the runtime host rather than building a new Content embedder (ADR-0001); record that the standalone Compose Activity is a prototype, not an embeddable engine host.
- [x] Add a separate ARM64 GN args profile for the WAE package namespace (`com.wae.browser`) using Chromium's public manifest-package argument.
- [x] Author the first minimal Chromium source patch to set the Android launcher label to WAE Browser (patch application/build verification still pending).
- [x] Author a narrow WAE tabbed-window system theme patch for dark system bars and the WAE accent, without replacing Chromium browser controls (patch application/build verification still pending).\n- [x] Specify the Chromium-native tab-state bridge contract, observer ownership/lifecycle, OTR privacy invariants, and acceptance criteria (implementation remains pending).
- [ ] Build and review the unmodified ARM64 Chromium baseline, then build the WAE package-namespace/app-name variant and validate the browser-fork integration seam with an Android 10 proof of concept.
- [ ] Implement WAE branding and product browser controls inside Chromium's Android browser application using a small, reviewable patch series.
- [ ] Integrate WAE tab/session UI with Chromium's native tab models, profiles, navigation, rendering, downloads, permissions, and lifecycle.
- [ ] Preserve Chromium sandboxing, site isolation, certificate validation, and process boundaries.
- [ ] Add smoke, navigation, crash, and security regression tests.

**Exit:** stable ARM64 browsing prototype with documented Chromium provenance.

## Phase 3 — Privacy and network controls

- [ ] Add configurable tracker and ad-blocking rules with clear update provenance.
- [ ] Add third-party-cookie controls, HTTPS-first behavior, referrer controls, and WebRTC controls.
- [ ] Add DNS-over-HTTPS and supported DNS-over-TLS resolver options.
- [ ] Respect Android VPN and Private DNS behavior; never silently downgrade to plaintext DNS.
- [ ] Add per-site permissions and privacy diagnostics.

**Exit:** tested privacy controls with clear limits and no breakage of core browser security.

## Phase 4 — Downloads and offline

- [x] Specify Chromium-owned Android download integration, active backend ownership, OTR privacy, safe URI handling, and acceptance criteria (adapter/UI implementation remains pending).\n- [ ] Adapt Chromium's pinned Android download-item backend, `DownloadManagerBridge`, and download UI; preserve Chromium warning/confirmation and file-provider flows.
- [ ] Add WAE queue presentation and user controls (pause/resume/retry only where the pinned backend supports them); do not create a parallel network downloader.
- [ ] Verify downloaded content with hashes where applicable and validate MIME/signature consistency.
- [ ] Warn on executable or suspicious content; never auto-open downloads.
- [ ] Add offline page snapshots and reader mode.
- [ ] Document limitations for authenticated, dynamic, and server-generated pages.

**Exit:** reliable local downloads and useful offline reading with integrity and safety tests.

## Phase 5 — Extension compatibility

- [x] Audit pinned Chromium Android extension buildflags: the default ARM64 phone build has neither the desktop extensions platform nor `enable_extensions_core` enabled; enabling the desktop-Android path is not equivalent to full mobile MV3 support.
- [ ] Define the supported extension manifest and API compatibility matrix from a source-level audit of the exact pinned revision.
- [ ] Start with Manifest V3 and a deliberately limited, documented API surface, after proving the required runtime services can be built and operated on Android phones.
- [ ] Support explicit installation from local extension packages and trusted release sources.
- [ ] Validate package structure, manifest, permissions, origin, and integrity before install.
- [ ] Isolate extension execution and storage; provide per-extension disable/remove controls.
- [ ] Add compatibility fixtures and regression tests.

**Exit:** a measured compatibility report. Do not advertise universal Chrome Web Store or desktop parity.

## Phase 6 — Optional self-hosted sync

- [ ] Specify a minimal sync protocol and server deployment model.
- [ ] Keep the client fully functional without a server or account.
- [ ] Encrypt data on the client before upload; do not invent cryptographic primitives.
- [ ] Define key recovery, device enrollment, conflict resolution, deletion, and backup behavior.
- [ ] Add protocol versioning, abuse controls, and interoperability tests.

**Exit:** independently deployable server and tested encrypted multi-device sync.

## Phase 7 — Release hardening

- [ ] Complete threat-model review and security testing.
- [ ] Add dependency and license audits, SBOM generation, and reproducible-build checks.
- [ ] Verify APK signing and release artifact provenance.
- [ ] Publish security contact and vulnerability-reporting process.
- [ ] Test upgrade, rollback, data migration, and emergency Chromium update paths on API 29 and a current Android release.
- [ ] Publish APKs and release notes through GitHub Releases.

**Exit:** signed release candidate with documented security baseline and support policy.

## CI policy

CI is intentionally not introduced as a substitute for implementation. First complete the relevant implementation and code review milestone, then add a small, high-value workflow (format/lint, unit tests, assemble, dependency checks). Avoid repeated full Chromium builds when a narrower validation is sufficient.
