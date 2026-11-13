# Chromium Android integration map

**Pinned source:** Chromium `81ff2496eed313447d34079a6f45c57f9c5611c1`  
**Version:** 157.0.8103.78  
**Status:** Source-level map only; no Chromium build or runtime proof-of-concept has been run.

This document defines the intended ownership boundary for the selected browser-fork approach. It is not a claim that a WAE adapter already exists. See [UI adaptation boundaries](UI_ADAPTATION.md) for the root Activity, toolbar, and tab-switcher integration rules, and [native tab-state bridge design](TAB_STATE_BRIDGE.md) for the observer contract, privacy invariants, lifecycle, and acceptance criteria.

## Runtime host and application identity

- GN target: `//chrome/android:chrome_public_apk`.
- Runtime tabbed Activity: `org.chromium.chrome.browser.ChromeTabbedActivity`.
- Activity base: `org.chromium.chrome.browser.app.ChromeActivity`.
- Manifest: `chrome/android/java/AndroidManifest.xml`.
- Public package namespace GN arg: `chrome_public_manifest_package`, declared in `chrome/android/chrome_public_apk_tmpl.gni`.
- WAE package namespace args: `browser/build/args/wae-arm64.gn` (`com.wae.browser`).

The standalone Compose `MainActivity` is not the eventual Chromium host. The WAE product UI must be adapted inside the Chromium browser application. Do not add Chromium native libraries to the current app module and assume that this creates a supported embedder.

## Ownership map

| WAE responsibility | Pinned Chromium owner / seam | Integration rule |
|---|---|---|
| Activity and startup lifecycle | `ChromeTabbedActivity`, `ChromeActivity`, `TabbedRootUiCoordinator` | Preserve Chromium startup, native initialization, lifecycle dispatch, and window setup. |
| Standard/private tab models | `TabModelSelector`, `TabModel`, `Tab`, `TabObserver` | Use Chromium's tab models; do not mirror them as a second authoritative tab store. |
| Profiles and private isolation | `Profile`, `ProfileManager`, `OtrProfileId` | Use regular and off-the-record profiles. A private tab must use an OTR profile and must not be restored or serialized. |
| Page contents and rendering | `Tab.getWebContents()`, `WebContents`, Chromium compositor/view ownership | Keep WebContents attached through Chromium's tab/view lifecycle. Do not create a second renderer host. |
| Downloads | `chrome/browser/download/android` Java library: `DownloadManagerBridge`, `DownloadDelegateImpl`, `DownloadManagerCoordinator`, `DownloadToolbarButtonController`, and Chromium download-item/backend services | Adapt Chromium's download-item lifecycle and existing Android DownloadManager bridge. Preserve dangerous/insecure download dialogs, file-provider URI handling, MIME validation, and user confirmation. Do not create a second network download stack or use the deprecated `DownloadManagerService.DownloadObserver` API as the WAE contract. |
| Site permissions | `components/permissions/android/permission_prompt/PermissionPromptAndroid` and `PermissionDialogDelegate` | Adapt the existing prompt UI/delegate. Preserve Chromium's origin/content-setting checks and Android runtime permission flow; do not add a second grant path. |
| Navigation and security | Chromium navigation stack, `WebContents`, `NavigationHandle`, certificate/security UI | Add WAE policy at a verified browser-owned pre-commit navigation hook. `TabObserver` callbacks are useful for state observation but must not be mistaken for a cancellable request hook. Retain Chromium redirects, origin checks, certificate validation, and subresource security. |
| Renderer failure | `TabObserver.onCrash` and Chromium tab/WebContents lifecycle | Reuse Chromium crash/recovery behavior and test that dead contents cannot be reused as live tabs. |

## Important API observations at the pinned revision

- `TabModelSelector` exposes separate regular and off-the-record models, current-model suppliers, observer registration, and profile access.
- `TabModel` exposes model observers, tab lookup, and a `TabRemover`; WAE tab UI should consume those events rather than own a parallel list.
- The pinned `Tab` interface exposes `getWebContents()`, `getUrl()`, `getTitle()`, `getId()`, and explicit `isOffTheRecord()` / `isIncognitoBranded()` methods. Use off-the-record semantics for privacy; branding is not a privacy boundary.
- The pinned `TabObserver` exposes title/URL updates, load progress, primary-main-frame navigation lifecycle notifications, and `onCrash`. These are state/lifecycle observations; they do not by themselves provide a synchronous pre-commit cancellation contract.
- `NavigationHandle` exposes `isInPrimaryMainFrame()` and `hasUserGesture()`. Any WAE top-level navigation policy must be attached at an upstream hook that can actually cancel or redirect before commit; do not rely on post-start observer callbacks.
- `Profile` exposes `getOrCreatePrimaryOtrProfile()` and `isOffTheRecord()`. The lifecycle of the OTR profile must remain under Chromium's profile manager.
- The existing `DownloadManagerService.DownloadObserver` is marked deprecated after the new download backend. The implementation must identify and use the active download backend for this exact source revision.
- The pinned permission pipeline includes `PermissionPromptAndroid` and `PermissionDialogDelegate`; the existing prompt is the integration seam to preserve, rather than exposing raw permission decisions to page content.

## First patch-series boundaries

1. **Package identity:** GN arg only; no claim of WAE branding.
2. **Product identity:** change app label, launcher/adaptive icons, about/version surfaces, and Chromium product strings through a small source patch set.
3. **Browser controls:** adapt the toolbar/root UI coordinator and tab switcher while preserving ChromeActivity lifecycle and content hosting.
4. **Privacy defaults:** configure browser-owned settings/policies; preserve mandatory certificate, sandbox, origin, and renderer protections.
5. **WAE-specific features:** add extensions and optional sync only after browser profile, storage, permission, and process boundaries are reviewed.

Every patch must be based on the locked Chromium SHA, have a stable patch ID, include rationale and security impact, and be rebased/reviewed on every Chromium security update.

## Source references at the exact pinned revision

- [ChromeTabbedActivity](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/android/java/src/org/chromium/chrome/browser/ChromeTabbedActivity.java)
- [ChromeActivity](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/android/java/src/org/chromium/chrome/browser/app/ChromeActivity.java)
- [Android manifest](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/android/java/AndroidManifest.xml)
- [Public APK GN template](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/android/chrome_public_apk_tmpl.gni)
- [TabModelSelector](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/browser/tabmodel/android/java/src/org/chromium/chrome/browser/tabmodel/TabModelSelector.java)
- [TabModel](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/browser/tabmodel/android/java/src/org/chromium/chrome/browser/tabmodel/TabModel.java)
- [Tab](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/browser/tab/java/src/org/chromium/chrome/browser/tab/Tab.java)
- [Profile](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/browser/profiles/android/java/src/org/chromium/chrome/browser/profiles/Profile.java)
- [DownloadManagerService](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/android/java/src/org/chromium/chrome/browser/download/DownloadManagerService.java)
- [Chrome Android sandbox design](https://chromium.googlesource.com/chromium/src/+/HEAD/docs/security/android-sandbox.md)

## Extension feasibility on Android phones

The pinned source does **not** enable the full desktop extensions platform for a normal Android phone build by default. This is a concrete compatibility gate, not just a UI task:

- `extensions/buildflags/buildflags.gni` sets `enable_extensions = !is_android && ...`, so it defaults to false on Android.
- `enable_desktop_android_extensions` defaults to `is_desktop_android`; `build/config/chrome_build.gni` defaults `is_desktop_android` to false.
- `enable_extensions_core` defaults to `enable_extensions || enable_desktop_android_extensions`, so it is also false for the current ARM64 phone target.
- The same buildflags file explains that `enable_extensions_core` contains the majority of extension code ported to Android, while code still gated by `enable_extensions` is not yet ported or is explicitly excluded from desktop Android.

Therefore, adding `enable_extensions_core=true` or enabling the desktop-Android flag is not proof of full Manifest V3 compatibility. Do not force either flag in the production GN args until the exact pinned source has been audited for extension loading/unpacking, MV3 service-worker lifecycle, permissions/host access, content scripts, storage, declarativeNetRequest, action UI, background events, incognito behavior, and update/uninstall paths. The target must build and pass Android 10 and current-device regression coverage with Chromium security boundaries intact.

Primary pinned-source references:
- [Extension buildflags](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/extensions/buildflags/buildflags.gni)
- [Desktop Android build defaults](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/build/config/chrome_build.gni)
- [Chrome extensions build graph](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/browser/extensions/BUILD.gn)

## Android download integration at the pinned revision

The pinned `chrome/browser/download/android/BUILD.gn` includes Chromium-owned `DownloadManagerBridge`, `DownloadDelegateImpl`, `DownloadManagerCoordinator`, `DownloadToolbarButtonController`, download dialogs, and file-provider components. WAE should adapt these browser-owned services and the active download-item backend rather than implement a parallel HTTP downloader. Preserve Chromium's dangerous/insecure download warnings and Android DownloadManager URI/file handling.

- [Pinned Android download build graph](https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/chrome/browser/download/android/BUILD.gn)
