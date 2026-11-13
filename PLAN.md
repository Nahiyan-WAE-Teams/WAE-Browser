# WAE Browser implementation plan

This plan is the working sequence. Each phase must have explicit acceptance criteria and a review before moving on.

## Phase 0 — Repository and engineering foundation

- [x] Establish product decisions and repository documentation.
- [ ] Select the application license after reviewing Chromium and third-party obligations.
- [ ] Define supported Android API range and minimum device requirements.
- [ ] Verify current stable Android Gradle Plugin, Kotlin, Gradle, NDK, and Chromium revisions from primary sources.
- [ ] Establish reproducible build inputs, dependency pinning, and an SBOM strategy.
- [ ] Define signing-key custody and release provenance.

**Exit:** approved toolchain matrix, build design, license decision, and security review checklist.

## Phase 1 — Android shell and product UI

- [ ] Create the Android application module and baseline build.
- [ ] Implement browser shell, tab model, address/search bar, navigation controls, and settings.
- [ ] Establish design tokens, typography, accessibility, responsive layouts, and restrained motion.
- [ ] Respect Android reduced-motion settings and provide an in-app motion preference.
- [ ] Add UI state and navigation tests.

**Exit:** installable shell with no claim of production browsing until the engine is integrated.

## Phase 2 — Chromium integration

- [ ] Pin an exact Chromium upstream revision and record its security baseline.
- [ ] Build and package Chromium for ARM64 using a documented, reproducible process.
- [ ] Integrate browser tabs, navigation, rendering, downloads, permissions, and lifecycle.
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

- [ ] Build a queue-based download manager with pause, resume, retry, and concurrency limits.
- [ ] Verify downloaded content with hashes where applicable and validate MIME/signature consistency.
- [ ] Warn on executable or suspicious content; never auto-open downloads.
- [ ] Add offline page snapshots and reader mode.
- [ ] Document limitations for authenticated, dynamic, and server-generated pages.

**Exit:** reliable local downloads and useful offline reading with integrity and safety tests.

## Phase 5 — Extension compatibility

- [ ] Define the supported extension manifest and API compatibility matrix.
- [ ] Start with Manifest V3 and a deliberately limited, documented API surface.
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
- [ ] Test upgrade, rollback, data migration, and emergency Chromium update paths.
- [ ] Publish APKs and release notes through GitHub Releases.

**Exit:** signed release candidate with documented security baseline and support policy.

## CI policy

CI is intentionally not introduced as a substitute for implementation. First complete the relevant implementation and code review milestone, then add a small, high-value workflow (format/lint, unit tests, assemble, dependency checks). Avoid repeated full Chromium builds when a narrower validation is sufficient.
