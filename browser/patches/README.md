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

No Chromium source patches have been authored or applied yet. The current WAE package namespace is selected through the GN args file at `../build/args/wae-arm64.gn`; it does not rename the app label, icons, or Chrome UI.
