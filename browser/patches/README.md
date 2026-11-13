# Chromium patch series

This directory contains only reviewed, reproducible patches applied to the exact source revision in `../revisions/chromium.lock`.

## Patch rules

- Keep patches small and ordered; name them `0001-short-purpose.patch`, `0002-short-purpose.patch`, and so on.
- Every patch must apply cleanly to the locked Chromium SHA and include a short rationale, source paths, expected behavior, security impact, and rebase notes.
- Do not patch Chromium sandbox, site-isolation, certificate-validation, renderer, or helper-process security behavior to simplify product integration.
- Keep third-party license and attribution changes explicit.
- Do not silently update the lock revision when rebasing patches. Review and update the Chromium lock, toolchain matrix, source map, and security baseline together.
- A patch is not considered validated until it applies to a clean checkout and the affected Chromium target is built and reviewed.

## Current patch inventory

### 0001 — WAE app name

- File: `0001-wae-app-name.patch`
- Source change: changes the manifest's `@string/app_name` from Chromium to WAE Browser in `chrome/android/java/res_chromium_base/values/channel_constants.xml`.
- Base: exact Chromium lock revision `81ff2496eed313447d34079a6f45c57f9c5611c1`.
- Validation: patch authored against the exact pinned file contents; `git apply --check` and Chromium build have not yet been run.

The WAE package namespace is selected separately through `../build/args/wae-arm64.gn`. Launcher icons, product strings, and browser UI remain Chromium defaults until separately patched and reviewed.
