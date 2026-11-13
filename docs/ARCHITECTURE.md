# WAE Browser — Module Architecture

**Status:** Android app shell and initial engine API contract module exist; concrete Chromium adapter is not implemented.  
**Minimum Android version:** Android 10 (API 29).  
**Initial ABI:** ARM64.

## 1. Architectural principles

- Keep the browser engine behind a narrow adapter boundary.
- Separate UI, browser orchestration, persistence, networking, and security policy.
- Keep the application local-first; sync is an optional transport.
- Make permission and privacy decisions explicit and auditable.
- Avoid custom cryptography and avoid patching Chromium security invariants for convenience.
- Treat API 29 as a compatibility floor, not as a reason to weaken security or rely on obsolete platform behavior.

## 2. Proposed repository layout

```text
WAE-Browser/
├── android/
│   ├── app/                    # Android application and composition root
│   ├── core/
│   │   ├── common/             # Shared types, dispatchers, result types
│   │   ├── model/              # Domain models
│   │   ├── database/           # Local persistence and migrations
│   │   ├── datastore/          # User preferences
│   │   ├── design-system/      # Tokens, components, themes, motion
│   │   └── testing/            # Test fixtures and utilities
│   ├── feature/
│   │   ├── browser/             # Browser screen, navigation, tabs
│   │   ├── downloads/           # Download queue and file details
│   │   ├── extensions/          # Extension management UI
│   │   ├── privacy/             # Privacy controls and diagnostics
│   │   ├── settings/            # Settings and about
│   │   └── offline/             # Saved pages and reader mode
│   └── engine/
│       └── api/                 # Prototype engine contract only
├── browser/
│   ├── patches/                 # Reviewed Chromium patches only
│   ├── integration/             # Pinned source integration map
│   ├── build/                   # Chromium build configuration
│   └── revisions/               # Exact upstream revision and patch metadata
├── modules/
│   ├── extensions/              # Manifest parsing, permissions, runtime bridge
│   ├── privacy/                 # Rules and policy evaluation
│   ├── networking/              # DNS and network policy integration
│   ├── downloads/               # Download orchestration and validation
│   ├── offline/                 # Snapshot and reader pipeline
│   └── sync-client/             # Client encryption and sync protocol
├── sync-server/                 # Optional self-hosted sync service
├── security/
│   ├── threat-model/             # Threat model and review records
│   ├── supply-chain/             # SBOM and dependency policies
│   └── release/                  # Signing and provenance procedures
├── build/                        # Reproducible build scripts and toolchain pins
├── tests/
│   ├── compatibility/            # Extension and web-platform fixtures
│   ├── security/                  # Security regression tests
│   └── integration/               # Cross-module tests
├── tools/                         # Developer utilities
├── docs/
└── .github/
    ├── ISSUE_TEMPLATE/
    └── workflows/                 # Add after implementation/review milestone
```

## 3. Android module responsibilities

| Module | Responsibility | Must not own |
|---|---|---|
| app | Dependency graph, lifecycle, top-level navigation | Browser policy implementation |
| core/browser | Browser tab/session domain state and address resolution | Android UI and Chromium internals |
| core/model | Stable domain types | Android UI |
| core/database | Local records and migrations | Sync transport |
| core/datastore | Preferences | Secret cryptographic protocol |
| core/design-system | UI tokens and reusable components | Browser state |
| feature/browser | Tabs and browsing UI state | Direct Chromium internals |
| feature/extensions | User-facing extension management | Unrestricted engine privileges |
| engine/api | Browser engine contracts and lifecycle boundary | Engine-specific implementation |
| engine/api | Prototype engine contract only; not a Chromium embedding SDK | Claiming to host the full browser APK |

## 4. Chromium runtime boundary

The current Jetpack Compose `:android:app` module is a product/domain prototype. It is not the final runtime host. Chromium's `chrome_public_apk` is a complete Android browser application, not an AAR or embeddable engine. The selected architecture adapts Chromium's own Android browser application and its `ChromeTabbedActivity`, `ChromeActivity`, tab models, profiles, and UI coordinators.

The `:android:engine:api` module is an initial contract sketch, not a supported third-party Chromium embedding API. Do not implement a parallel WebContents host or attempt to load the full Chromium browser APK inside the Compose Activity. Actual WAE UI and browser policy integration must be implemented in a reviewed patch series inside the pinned Chromium source tree. See [ADR-0001](decisions/ADR-0001-chromium-android-integration.md) and the [pinned Chromium integration map](../browser/integration/README.md).

The engine-independent `:android:core:browser` module remains useful for domain behavior and local persistence. Its address resolver and navigation policy are policy inputs only; Chromium must continue enforcing URL parsing, origin boundaries, redirects, certificates, renderer isolation, and subresource security. The DataStore repository persists only standard-tab session metadata for the prototype. It is not a substitute for Chromium's authoritative tab/profile persistence.

Integration responsibilities:
- Use Chromium's `TabModelSelector`, `TabModel`, and `Tab` as the authoritative tab model.
- Use regular and off-the-record Chromium `Profile` objects; never equate a UI private-mode label with actual OTR isolation.
- Keep `WebContents` and renderer lifecycle inside Chromium's existing tab/view ownership.
- Adapt navigation, permissions, downloads, file chooser, and crash recovery at browser-owned Chromium seams.
- Preserve Chromium's sandbox, site isolation, certificate validation, and helper-process manifest declarations.
- Do not add a WebView fallback or expose privileged browser services to renderer/extension content.

## 5. Android compatibility

- Set the app minimum SDK to API 29 (Android 10).
- Test API 29 behavior for scoped storage, package visibility, background execution, notifications, and document-provider interactions.
- Use runtime feature checks where platform APIs differ across supported Android releases.
- Verify the selected Chromium revision's Android minimum before pinning it; the app's API floor cannot be lower than the engine's actual support floor.
- Keep target SDK current at release time and handle newer permission/platform behavior without breaking API 29.

## 6. Extension subsystem

Proposed components:
- Package importer and bounded archive parser.
- Manifest parser and schema validator.
- Permission model and user-consent UI.
- Extension registry and versioned local storage.
- Runtime host and browser API bridge.
- Origin/host permission enforcement.
- Update checker for user-configured sources.

All extension API calls must be mediated by a permission-aware bridge. The runtime must not expose privileged browser internals directly to extension scripts.

## 7. Privacy/network subsystem

- A single policy model feeds UI, engine integration, and diagnostics.
- Resolver selection is explicit and visible.
- OS VPN/Private DNS state is respected.
- No silent plaintext fallback.
- Ruleset updates are versioned and have source/provenance metadata.

## 8. Sync subsystem

- Local database remains authoritative when sync is off.
- Sync adapter serializes versioned records and encrypts client-side.
- Transport uses authenticated TLS to the configured server.
- Server stores opaque encrypted records and minimal routing metadata.
- Conflict resolution is deterministic and testable.
- Cryptographic protocol selection requires a separate review before code.

## 9. Dependency direction

The current Compose prototype depends on engine-independent domain state. The selected Chromium browser-fork runtime is implemented inside the pinned Chromium Android application, not as a Gradle adapter module wrapping the APK. WAE product UI and policy hooks must integrate at browser-owned Chromium seams. Sync depends on domain records and cryptographic interfaces, not UI. No feature module should reach around the permission broker.

## 10. Build and verification strategy

Pin toolchain and Chromium revisions. Record source URLs, commit hashes, build flags, patch hashes, and license inventory. Validate modules with targeted tests first; defer expensive full Chromium CI until integration milestones justify it.
