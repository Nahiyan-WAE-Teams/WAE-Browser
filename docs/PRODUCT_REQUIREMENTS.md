# WAE Browser — Product Requirements

**Status:** Baseline specification  
**Platform:** Android  
**Initial ABI:** ARM64

## 1. Product vision

Deliver a privacy-focused Android browser based on Chromium that remains useful without an account, vendor cloud, or mandatory synchronization. Users retain control over browser data, extension installation, network privacy settings, and downloads.

## 2. Product decisions

| Area | Decision |
|---|---|
| Engine | Chromium, not Android System WebView |
| Distribution | Direct APK and GitHub Releases |
| Identity | Anonymous by default; no account |
| Sync | Optional self-hosted server |
| Sync privacy | Client-side encryption before network transfer |
| Extensions | Local package installation plus supported external sources |
| Initial ABI | ARM64 |
| Telemetry | No hidden telemetry |
| Cloud dependency | None required for normal browsing |

## 3. Functional requirements

### 3.1 Core browsing
- Navigate to URLs and search queries.
- Support tabs, tab switching, private tabs, back/forward, reload, and stop.
- Support downloads, file uploads, permissions, and Android lifecycle changes.
- Handle crashes and renderer-process termination without corrupting unrelated local state.
- Provide clear security indicators for HTTPS, certificate errors, and permission prompts.

### 3.2 Privacy and security
- Privacy protections are enabled by default and configurable where technically safe.
- Provide controls for tracker blocking, third-party cookies, referrer behavior, WebRTC exposure, and per-site permissions.
- HTTPS-first behavior must not bypass certificate validation.
- DNS configuration must respect Android VPN and Private DNS settings.
- Never silently fall back from encrypted DNS to plaintext DNS.
- Browser sandboxing, site isolation, certificate validation, and extension package verification are mandatory security invariants.

### 3.3 Extensions
- Support a documented subset of Chromium extension APIs, beginning with Manifest V3.
- Permit users to select a local extension package through Android's document picker.
- Show requested permissions and explain their implications before installation.
- Verify package structure and reject malformed or unsafe packages.
- Provide extension enable/disable, update, remove, and per-extension data controls.
- Do not promise full desktop Chrome compatibility until an API-by-API compatibility suite supports that claim.

### 3.4 Downloads
- Provide a queue with pause, resume, retry, cancellation, and configurable concurrency.
- Show progress, source URL, destination, size, and final status.
- Validate file type and MIME consistency where possible.
- Do not automatically execute or open downloaded files.
- Keep original downloaded bytes unchanged; integrity metadata is separate.

### 3.5 Offline browsing
- Save readable page snapshots for offline access.
- Provide reader mode and local search over saved content.
- Clearly indicate that dynamic pages, authenticated sessions, streaming media, and server-side interactions may not be preserved.

### 3.6 Optional synchronization
- No account is required to use the browser.
- Users configure their own compatible sync server.
- Encrypt sync payloads on the client before transmission.
- Define device enrollment, key management, conflict resolution, deletion, and recovery before implementation.
- The server must not need plaintext browsing data to perform storage and synchronization.

## 4. UX requirements

- Premium, coherent Android UI with accessible contrast and touch targets.
- Fast tab and navigation interactions.
- Restrained motion for navigation, tab changes, downloads, and settings.
- Respect system animation scale/reduced-motion preferences and provide an in-app motion control.
- Privacy controls must explain what they do and what may break.
- No dark patterns, hidden opt-ins, or misleading security badges.

## 5. Non-functional requirements

- ARM64 is the first release target.
- Keep Chromium revision and security patch level visible in diagnostics/about.
- Pin all build-critical dependencies.
- Make release builds reproducible to the extent supported by the Chromium toolchain.
- Minimize background work and battery use.
- Keep core browsing functional when sync is unavailable.
- Provide data export and clear local-data deletion controls.

## 6. Out of scope for the first release

- Guaranteed compatibility with every desktop Chrome extension.
- Mandatory WAE account or WAE-operated sync service.
- Cloud-based browsing history analytics.
- Automatic execution of downloaded files.
- Claims of perfect anonymity or complete fingerprinting resistance.

## 7. Acceptance criteria

A feature is complete only when its behavior, failure modes, permissions, data handling, and tests are documented. Security-sensitive features additionally require threat-model review. Marketing claims must not exceed tested behavior.
