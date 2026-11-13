# ADR-0001: Chromium Android integration strategy

- **Status:** Provisional decision; proof-of-concept required before release commitment.
- **Date:** 2026-11-13
- **Scope:** Browser engine and Android application integration.

## Context

WAE Browser needs a complete browser engine, not an HTML rendering widget. Required browser responsibilities include independent tab contents, navigation/session history, renderer lifecycle and crash recovery, site permissions, downloads, file selection, security UI, and Chromium's Android child-process setup.

Chromium's `content` module provides the core multi-process browser and web-platform implementation, but it does not include Chrome product features such as the browser's extension integration and other browser-level services. The Content API is an embedder interface, not a stable, version-independent Android SDK. A custom embedder would own a large amount of security-sensitive integration and would need to track internal API changes on every Chromium update.

The Android security model is also not just a rendering surface: Chrome launches helper processes as Android services, applies Android isolated-process boundaries to sandboxed helpers, and uses additional Seccomp-BPF restrictions. The exact service declarations and process configuration are part of the engine integration.

## Options considered

### A. Fork and adapt Chromium's Android browser application

Build from the pinned Chromium source and adapt the `chrome/android` application/browser layers to WAE's product requirements.

**Advantages**
- Starts from the upstream Android browser lifecycle and process configuration.
- Keeps browser-level services close to the implementation that Chromium itself ships and maintains.
- Reduces the amount of security-sensitive Content embedder behavior WAE must invent.
- Provides the most direct route to upstream Chromium functionality and security fixes.

**Costs**
- The WAE patch set must be rebased and reviewed for every Chromium security update.
- Replacing or substantially changing Chrome's Android UI is a large ongoing maintenance task.
- Product-specific UI work may need to remain in Java/Kotlin/Android Views until a stable integration seam is proven; Compose must not be forced into an unsupported engine boundary.

### B. Build a custom WAE shell directly over Chromium Content

Create a custom Android application around Chromium's Content Java/JNI interfaces.

**Advantages**
- Maximum freedom over the Compose application shell and product UI.
- Smaller Chrome product-layer dependency surface.

**Costs**
- Content interfaces are internal/version-coupled rather than a stable third-party Android browser SDK.
- WAE must implement and maintain browser-level lifecycle, tab/session integration, permissions, downloads, file chooser, security UI, process management, and other embedder responsibilities.
- The custom embedder becomes responsible for correctly configuring and preserving the Android renderer/helper process sandbox.
- Every Chromium roll may require substantial JNI and native integration changes.

### C. Android WebView or Custom Tabs

Rejected as the primary engine. WebView is not intended as a framework for building a full browser, and Custom Tabs delegates browsing to the user's installed browser rather than giving WAE ownership of its own engine and browser state.

## Decision

**Use option A as the default implementation direction: adapt Chromium's Android browser application layer rather than build a new Content embedder from scratch.**

This is a provisional engineering decision, not a claim that the existing Compose shell can already host Chromium. The proof-of-concept must determine the narrowest maintainable seam between WAE product UI and Chromium's Android browser services. If that seam requires replacing the existing app entry point or moving browser surfaces out of Compose, document the trade-off before changing the application structure.

Do not build a parallel Content embedder merely to preserve Compose. A custom Content integration may be reconsidered only if a pinned-revision POC demonstrates that all required browser responsibilities and Android process protections can be maintained with a materially lower long-term security and rebase burden.

## Non-negotiable security constraints

- Keep Chromium renderer sandboxing and Android isolated helper-process declarations intact.
- Preserve site isolation, origin enforcement, certificate validation, and Chromium navigation security checks.
- Never expose privileged browser services directly to renderer or extension content.
- Route site permissions, downloads, and file chooser requests through explicit WAE policy/UI brokers.
- Do not add a WebView fallback if Chromium initialization fails; fail closed and report the engine failure.
- Keep private browsing data isolated and ensure private profiles are destroyed at the end of their lifetime.

## Proof-of-concept acceptance criteria

The pinned ARM64 source must demonstrate:

1. A reproducible build and install on Android 10 (API 29).
2. HTTPS navigation with valid certificate handling and correct failure behavior for invalid certificates.
3. Multiple independent tabs, including create/destroy and renderer-crash recovery.
4. Correct sandboxed renderer/helper process declarations and runtime process behavior.
5. Permission, download, and file chooser interception through explicit policy callbacks.
6. A documented UI integration seam that does not bypass the browser's security architecture.
7. A practical Chromium security-update/rebase procedure with a reviewable patch set.

Until these criteria are met, the Chromium revision remains a POC candidate and WAE Browser must continue to label real browsing as unavailable.

## Upstream references

- Chromium source architecture: https://new.chromium.org/developers/how-tos/getting-around-the-chrome-source-code/
- Content module and embedder API: https://chromium.googlesource.com/chromium/src/+/HEAD/content/README.md
- Chrome Android sandbox design: https://chromium.googlesource.com/chromium/src/+/HEAD/docs/security/android-sandbox.md
- Chrome Custom Tabs security FAQ: https://github.com/chromium/chromium/blob/main/docs/security/custom-tabs-faq.md
