# WAE native tab-state bridge design

**Target:** Chromium `81ff2496eed313447d34079a6f45c57f9c5611c1` (157.0.8103.78)  
**Status:** implementation specification; no bridge source patch has been authored or built.

## Decision

Chromium remains the sole owner of tabs, profiles, navigation, WebContents, and tab persistence. WAE must not maintain a second authoritative tab list. The first integration slice is a read-only state bridge from Chromium's tab models into WAE-owned browser controls. User actions will be routed back through Chromium's existing tab/profile APIs in a later slice.

The bridge is intentionally not implemented in the Compose application. It belongs in the Chromium Android source tree and must be compiled into `chrome_public_apk`.

## Data contract

A WAE tab row may project only these values from the live Chromium `Tab`:

- Stable Chromium tab ID (identity only; never persist as a restorable tab ID).
- Current title and URL, with empty/loading states represented explicitly.
- Whether the tab belongs to the regular or off-the-record model.
- Whether it is the selected tab in its model.
- Loading/progress and crash state, where the upstream observer exposes them.

Do not copy `WebContents`, `Profile`, native pointers, authentication state, page contents, or private URLs into the WAE UI model. A tab's privacy classification comes from its owning model/profile and Chromium's off-the-record state, not an app-maintained boolean or the incognito-branded presentation flag.

## Observer ownership and lifecycle

1. Attach only after Chromium has created the tab-model selector and its regular/OTR models. Startup can expose suppliers before the models are ready; the bridge must handle the not-yet-initialized state without assuming a non-null current tab.
2. Observe selector changes to detect regular/OTR model switches and model initialization.
3. Observe both tab models for add, select, move, close, and removal events. Also observe the selected tab for title, URL, load progress, navigation completion, and renderer crash changes.
4. Reconcile from the current selector/model snapshot after observer registration and after model switches. Events are invalidation signals; the current Chromium model is authoritative.
5. Remove every selector, model, and tab observer when the bridge is destroyed or the Activity's owning coordinator is torn down. Rebind the selected-tab observer whenever selection changes.
6. Keep all observer work on Chromium's required UI thread. Coalesce UI projection updates; never block callbacks on disk, network, or Compose recomposition.

The exact owning coordinator and registration callback must be selected only after inspecting the full pinned source and its GN target dependencies. Do not add observers from an arbitrary Activity lifecycle callback merely because it is convenient.

## User actions (subsequent slice)

- New tab: invoke Chromium's tab creator with the correct regular/OTR model and existing launch-type semantics.
- Select tab: select by current Chromium tab identity in its owning model.
- Close tab: use Chromium's `TabRemover` / closure parameters so undo, close confirmation, and model policy remain intact.
- Switch privacy mode: switch the selector's active model; do not synthesize or persist OTR tabs.
- Restore: let Chromium restore regular tabs and profile state. WAE's existing Compose session persistence is prototype-only and must not become a competing restore mechanism.

No direct `WebContents` construction, independent renderer, or manual tab serialization is allowed.

## Security and privacy invariants

- Off-the-record tabs and their URLs/titles must never enter WAE persistent storage, sync, logs, crash breadcrumbs, or analytics.
- Closing the last OTR tab must follow Chromium's OTR profile lifecycle and cleanup.
- Do not weaken sandboxing, site isolation, renderer process assignment, certificate validation, Safe Browsing, permission prompts, or download warnings.
- A crash callback updates presentation only; Chromium remains responsible for recovery/discard/reload behavior.
- Navigation policy must use a cancellable pre-commit browser-owned hook. Tab observers are not a substitute for request interception.

## Acceptance criteria before calling the bridge implemented

- Cold start, warm resume, and tab restoration with zero, one, and multiple tabs.
- Add/select/move/close/undo/close-all in both regular and OTR models.
- Repeated regular-to-OTR switching with correct active-model and selected-tab state.
- Last OTR tab closure and profile cleanup.
- Title/URL/progress updates, redirects, same-document navigation, and renderer crash/recovery.
- Activity recreation, background/foreground, multi-window, and process recreation.
- Automated observer attach/detach and no-leaked-observer coverage.
- Manual confirmation that private metadata never reaches the regular persistence path.

## Work still required

- Inspect the exact pinned `ChromeTabbedActivity`, selector/model implementations, and owning coordinator in a full Chromium checkout.
- Verify the public/protected selector accessors, model observer signatures, tab-observer registration APIs, and GN source-set dependencies at the lock SHA.
- Implement the bridge as a small Chromium Java source patch with unit tests.
- Apply the complete ordered patch series to a clean checkout, build baseline and WAE variants, then exercise the acceptance criteria on API 29 and a current Android device.

Until those steps are complete, the Compose tab strip remains a domain prototype and WAE has no Chromium-backed tab UI.
