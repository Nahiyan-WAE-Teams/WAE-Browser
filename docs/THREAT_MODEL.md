# WAE Browser — Security Threat Model

**Status:** Initial design baseline; must be revised as implementation details become concrete.

## 1. Security objectives

1. Protect users from malicious websites and compromised content.
2. Preserve Chromium's process isolation, sandboxing, and certificate validation.
3. Prevent extensions from receiving broader access than users approved.
4. Protect local browsing data and downloaded files from unintended disclosure or execution.
5. Keep optional sync confidential from the sync-server operator.
6. Make security state and permission decisions understandable to users.

## 2. Assets

- Browsing history, bookmarks, tabs, saved pages, and site data.
- Authentication cookies, session tokens, and web credentials handled by Chromium.
- Downloaded files and their integrity metadata.
- Extension packages, extension storage, and granted permissions.
- Sync encryption keys, device identity material, and encrypted sync records.
- APK signing keys, build credentials, source dependencies, and release artifacts.

## 3. Trust boundaries

- Web content ↔ Chromium renderer process.
- Renderer ↔ browser process and privileged Android services.
- Extension runtime ↔ browser APIs and web origins.
- App ↔ Android OS, document provider, and external storage.
- App ↔ configured DNS resolver and remote websites.
- App ↔ user-operated sync server.
- Source repository/build environment ↔ signed release artifacts.

## 4. Threat actors

- Malicious website operators and advertisers.
- Attackers exploiting browser or renderer vulnerabilities.
- Malicious or compromised extension authors.
- Attackers controlling public Wi-Fi, DNS, or network intermediaries.
- Malicious files or deceptive download sources.
- Compromised build dependencies, CI credentials, or release infrastructure.
- A curious or compromised self-hosted sync server.

## 5. Threats and required mitigations

| Threat | Impact | Required mitigation |
|---|---|---|
| Renderer exploit | Code execution or data access | Preserve Chromium sandboxing, site isolation, and timely security updates |
| Malicious extension | History, page, or credential exposure | Permission review, least privilege, isolated storage/runtime, explicit install consent |
| Tampered extension package | Arbitrary extension code | Validate package format, manifest, source, and integrity; warn on unsigned/untrusted packages |
| Network interception | Content or metadata exposure | TLS validation, HTTPS-first, no certificate-error bypass |
| DNS manipulation | Redirects and tracking | Encrypted DNS options, clear resolver state, respect OS VPN/Private DNS |
| Tracking/fingerprinting | Cross-site profiling | Configurable blocking and privacy controls; document residual fingerprinting |
| Malicious download | Device compromise or data loss | MIME/signature checks, safe filenames, no auto-execution, user confirmation |
| Local data theft | History/session disclosure | Android app sandbox, minimize exported components, protect backups and local stores |
| Sync-server compromise | Disclosure or deletion of sync records | Client-side authenticated encryption, versioned protocol, replay/conflict handling, backup/export |
| Supply-chain compromise | Malicious browser build | Pinned revisions, dependency audit, SBOM, protected signing keys, release provenance |
| Stale Chromium | Exploitation of known vulnerabilities | Defined update SLAs, emergency update path, visible Chromium version |

## 6. Security invariants

These controls are not user-disableable:
- Chromium sandbox and site isolation must not be weakened to make a feature work.
- TLS certificate validation must remain enabled.
- Extension package parsing and permission enforcement must remain enabled.
- Downloads must not execute automatically.
- Sync encryption must not be replaced by server-side plaintext storage.
- Release signing keys must not be embedded in source code or ordinary CI logs.

## 7. Privacy settings versus security invariants

Users may adjust compatibility-sensitive privacy behavior such as tracker blocking, third-party-cookie handling, referrer reduction, and WebRTC policy. The UI must explain trade-offs. A setting must never imply protection beyond what is actually enforced.

## 8. Residual risks

- Browser zero-days can exist before a patch is available.
- Fingerprinting defenses cannot guarantee anonymity.
- A malicious extension with broad permissions can still abuse the permissions it receives.
- A compromised Android OS can access app data and runtime state.
- A self-hosted server can deny service, delete ciphertext, or observe connection metadata.
- Page snapshots may contain sensitive content and need local retention controls.

## 9. Required review gates

Before release, review:
- Chromium revision and patch status.
- Extension API permission mapping and isolation.
- Exported Android components and intent handling.
- Local data encryption/backup behavior and key storage.
- DNS fallback behavior and VPN interactions.
- Download path handling and file validation.
- Sync cryptographic design and recovery flow.
- Build provenance, dependency inventory, and signing-key custody.

This document is a threat-model starting point, not a claim that these mitigations are already implemented.
