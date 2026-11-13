# Chromium-owned download integration design

**Target source:** Chromium `81ff2496eed313447d34079a6f45c57f9c5611c1`  
**Status:** Architecture specification only; no download adapter or UI patch is implemented.

## Decision

WAE will use Chromium's active Android download pipeline and download-item model. It will not create a parallel HTTP client, background downloader, file writer, or download database. The Android app's system DownloadManager integration, Chromium security checks, and browser-owned UI remain authoritative.

The pinned source's Android download build target includes `DownloadManagerBridge`, `DownloadDelegateImpl`, `DownloadManagerCoordinator`, `DownloadToolbarButtonController`, dialogs, and file-provider components. The legacy `DownloadManagerService.DownloadObserver` path is deprecated and must not be adopted as a new WAE integration contract.

## Ownership boundaries

| Concern | Owner | WAE responsibility |
|---|---|---|
| Download request creation | Chromium navigation/download pipeline | Observe the browser-owned item lifecycle; do not intercept and re-download the URL independently. |
| Destination and Android DownloadManager handoff | Chromium bridge/backend | Preserve the upstream destination, content URI, and lifecycle semantics. |
| Dangerous/insecure file checks | Chromium security/download UI | Preserve all warnings and confirmation flows. Never auto-open or auto-execute. |
| MIME and filename handling | Chromium backend plus Android content resolver | Display validated metadata; do not infer trust from extension or server-provided filename. |
| Progress and terminal state | Chromium download item/backend | Project state into WAE UI without maintaining a second source of truth. |
| Pause/resume/cancel | Active backend capability | Expose only operations supported by the exact pinned backend and item state. |
| File access/sharing | Chromium file-provider integration and Android URI grants | Use content URIs and scoped grants; never expose raw filesystem paths to other apps. |
| Persistence | Chromium download subsystem | Do not persist duplicate item state in the WAE Compose prototype. |

## UI projection contract

A WAE download row may display only metadata exposed by the live Chromium download item and approved for the current profile:

- Stable in-memory item identity for UI diffing; do not assume it is a durable identifier across browser restarts.
- Display name after Chromium filename sanitization.
- Download URL/origin only when permitted by profile privacy rules.
- Received and total bytes, with unknown total represented explicitly.
- Progress and state: pending, in progress, paused, interrupted, complete, cancelled, or dangerous/blocked where the backend distinguishes them.
- MIME type and completion destination metadata supplied by the backend.

Do not log full URLs, local paths, referrers, or private-profile download metadata. Off-the-record downloads must follow Chromium's OTR lifecycle and must not leak into regular-profile history, persistent WAE stores, or optional sync.

## Action rules

- Pause, resume, retry, and cancel must call the active Chromium download item/backend operation, only when that operation is available and allowed for the current state.
- A retry must be a Chromium-owned retry of the original item, not a new WAE HTTP request.
- Completed downloads open through Chromium's existing intent/content-URI flow, with Android URI permissions and MIME handling preserved.
- Dangerous or insecure downloads remain blocked until the user completes Chromium's existing confirmation flow.
- WAE must not silently change download directories, strip security prompts, or grant broad storage access.

## Integration sequence

1. In the full pinned source checkout, inspect the Android download GN target and trace item creation, bridge ownership, observer registration/removal, and Activity/profile lifecycle.
2. Identify the active download-item abstraction and supported state-changing operations at the lock SHA. Record exact method signatures and source paths before writing adapter code.
3. Add a read-only projection adapter in Chromium's Android browser source set. Attach/detach it with the owning browser coordinator; handle Activity recreation and profile destruction.
4. Add WAE download-list presentation inside Chromium's existing UI composition boundary. Do not replace the download manager's warning dialogs or file-provider flows.
5. Add action wiring only after capability/state mapping is verified against the pinned backend.
6. Test regular and OTR behavior, interrupted network, process death, storage pressure, duplicate filenames, unknown MIME, malicious filenames, insecure origins, dangerous file types, and Android 10 scoped-storage behavior.

## Acceptance criteria

- No independent WAE network downloader or duplicate download database exists.
- Every UI state corresponds to the active Chromium item/backend state.
- Every exposed action is supported by the pinned backend and has correct state guards.
- Dangerous/insecure download warnings and user confirmation remain unchanged.
- File opening/sharing uses safe content URIs and scoped grants.
- OTR download metadata does not enter regular-profile persistence, logs, or sync.
- Interrupted, resumed, cancelled, completed, and failed downloads are correctly represented.
- API 29 and a current Android version are covered before release claims.

## Explicit non-goals

- Downloading through a separate OkHttp/HttpURLConnection stack.
- Replacing Chromium's file provider or Android DownloadManager bridge.
- Auto-opening APKs, executables, scripts, or other downloaded content.
- Claiming pause/resume support before the active pinned backend exposes and validates it.
