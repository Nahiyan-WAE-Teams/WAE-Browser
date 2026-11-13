# Chromium Android UI adaptation boundary

**Source baseline:** `81ff2496eed313447d34079a6f45c57f9c5611c1`  
**Status:** Source-level implementation guidance; no custom Activity or toolbar patch has been built.

## Existing upstream UI ownership

At the pinned revision:

- `ChromeTabbedActivity.createRootUiCoordinator()` constructs a `TabbedRootUiCoordinator`.
- `TabbedRootUiCoordinator.initializeToolbar()` locates the existing browser control container and toolbar views, calls the base `RootUiCoordinator.initializeToolbar()`, and wires browser-control/side-panel behavior.
- `ChromeTabbedActivity` owns the tab creators, tab-model suppliers, layout manager, hub/tab-switcher dependencies, and tab delegate factory.
- `TabbedRootUiCoordinator` observes the current activity tab and coordinates browser UI that reacts to tab changes, title updates, and navigation.

These are not disposable presentation details. They are part of the browser's lifecycle and integration between the toolbar, tab models, compositor, browser controls, and WebContents.

## WAE adaptation rules

1. Keep `ChromeTabbedActivity` as the first runtime baseline until a source-built alternative proves it preserves startup, intents, multi-window behavior, native initialization, and lifecycle.
2. Keep `TabbedRootUiCoordinator` (or a carefully reviewed subclass) responsible for the browser root UI. Do not replace its content container or toolbar with a standalone Compose Activity.
3. Reuse the existing `ToolbarManager`, location bar/omnibox, tab-model selector, tab switcher, and profile suppliers. WAE controls should be added at their existing browser-owned seams.
4. Use Chromium's `TabModelSelector` and regular/off-the-record models as the authoritative tab strip and private-session state. UI selection must never manufacture an OTR profile independently.
5. Keep the existing compositor, browser controls, fullscreen, inset, keyboard, accessibility, and back-press coordination intact.
6. Do not remove upstream security UI, certificate interstitials, permission prompts, download confirmation, or renderer recovery to achieve a simpler WAE screen.
7. Treat a UI change as incomplete until tab creation/switch/close, regular/private transitions, navigation, keyboard/omnibox, permission prompts, download flows, background/foreground, multi-window, and renderer crashes have been exercised on-device.

## Proposed first WAE UI slice

The first meaningful product UI patch should be deliberately narrow:

- Apply WAE application/widget labels from patch `0001-wae-app-name.patch`.
- Keep the upstream toolbar and tab switcher initially; establish a stable WAE theme and restrained color/resource overrides without changing browser behavior.
- Replace only clearly branded surfaces after identifying their owning coordinator and verifying the relevant resource overlays at the pinned revision.
- Do not add a second tab strip or duplicate address-bar state in the existing Compose prototype.
- Add WAE-specific controls one at a time, each wired to Chromium's existing action/delegate and profile lifecycle.

A full replacement toolbar or new root Activity is a later design decision, not a prerequisite for the first branded Chromium build.

## Review gate

Before creating a UI patch, record the exact upstream class/resource paths, the overridden method or resource precedence, lifecycle ownership, and tests affected. Build the pinned baseline first, then build the patch variant. The Compose prototype must not be presented as a Chromium integration proof.
