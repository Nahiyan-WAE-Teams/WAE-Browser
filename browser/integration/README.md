# Chromium Android integration map

**Pinned source:** Chromium `81ff2496eed313447d34079a6f45c57f9c5611c1`  
**Version:** 157.0.8103.78  
**Status:** Source-level map only; no Chromium build or runtime proof-of-concept has been run.

The source/security review record is maintained in [Chromium security baseline](../revisions/SECURITY_BASELINE.md). Review it before changing the source pin or creating a release build.

This document defines the intended ownership boundary for the selected browser-fork approach. It is not a claim that a WAE adapter already exists. See [UI adaptation boundaries](UI_ADAPTATION.md) for the root Activity, toolbar, and tab-switcher integration rules, and [native tab-state bridge design](TAB_STATE_BRIDGE.md) for the observer contract, privacy invariants, lifecycle, and acceptance criteria.

## Runtime host and application identity

- GN target: `//chrome/android:chrome_public_apk`.
- Runtime tabbed Activity: `org.chromium.chrome.browser.ChromeTabbedActivity`.
- Activity base: `org.chromium.chrome.browser.app.ChromeActivity`.
- Manifest: `chrome/android/java/AndroidManifest.xml`.
- Public package namespace GN arg: `chrome_public_manifest_package`, declared in `chrome/android/chrome_public_apk_tmpl.gni`.
- WAE package namespace args: `browser/build/args/wae-arm64.gn` (`com.wae.browser`).

The standalone Compose `MainActivity` is not the eventual Chromium host. The WAE product UI must be adapted inside the Chromium browser application. Do not add Chromium native libraries to the current app module and assume that this creates a supported embedder.

