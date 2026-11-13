# WAE Browser — Module Architecture

**Status:** Proposed architecture; no application modules have been implemented yet.  
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
│       ├── api/                 # Engine abstraction and contracts
│       └── chromium/            # Chromium integration adapter
├── browser/
│   ├── patches/                 # Reviewed Chromium patches only
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
| core/model | Stable domain types | Android UI |
| core/database | Local records and migrations | Sync transport |
| core/datastore | Preferences | Secret cryptographic protocol |
| core/design-system | UI tokens and reusable components | Browser state |
| feature/browser | Tabs and browsing UI state | Direct Chromium internals |
| feature/extensions | User-facing extension management | Unrestricted engine privileges |
| engine/api | Browser engine contracts | Engine-specific implementation |
| engine/chromium | Chromium integration and lifecycle | Product UI |

## 4. Engine boundary

Define a small engine API for:
- Create/destroy tab and renderer lifecycle.
- Navigate, stop, reload, back, and forward.
- Observe navigation, loading, title, security state, and renderer failure.
- Request downloads and file chooser actions.
- Request site permissions through a policy broker.
- Expose only the extension hooks that the pinned Chromium integration actually supports.

Do not assume Android WebView APIs are equivalent to Chromium's full browser embedding surface. The integration strategy must be validated against the chosen Chromium revision before committing to implementation.

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

UI features depend on domain contracts, not Chromium implementation classes. Chromium integration depends on engine contracts and security policy interfaces. Sync depends on domain records and cryptographic interfaces, not UI. No feature module should reach around the permission broker.

## 10. Build and verification strategy

Pin toolchain and Chromium revisions. Record source URLs, commit hashes, build flags, patch hashes, and license inventory. Validate modules with targeted tests first; defer expensive full Chromium CI until integration milestones justify it.
