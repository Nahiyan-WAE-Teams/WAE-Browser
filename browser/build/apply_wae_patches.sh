#!/usr/bin/env bash
set -euo pipefail

CHROMIUM_SRC="${1:-${CHROMIUM_SRC:-}}"
if [[ -z "$CHROMIUM_SRC" ]]; then
  echo "Usage: bash browser/build/apply_wae_patches.sh /path/to/chromium/src" >&2
  exit 2
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WAE_REPO="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
PATCH_DIR="$WAE_REPO/browser/patches"
LOCK_FILE="$WAE_REPO/browser/revisions/chromium.lock"

if [[ ! -d "$CHROMIUM_SRC/.git" && ! -f "$CHROMIUM_SRC/.git" ]]; then
  echo "Not a Chromium Git checkout: $CHROMIUM_SRC" >&2
  exit 2
fi

EXPECTED_REVISION="$(sed -n 's/^revision=//p' "$LOCK_FILE")"
ACTUAL_REVISION="$(git -C "$CHROMIUM_SRC" rev-parse HEAD)"
if [[ -z "$EXPECTED_REVISION" || "$ACTUAL_REVISION" != "$EXPECTED_REVISION" ]]; then
  echo "Chromium revision mismatch." >&2
  echo "Expected: $EXPECTED_REVISION" >&2
  echo "Actual:   $ACTUAL_REVISION" >&2
  exit 1
fi

if [[ -n "$(git -C "$CHROMIUM_SRC" status --porcelain)" ]]; then
  echo "Refusing to patch a dirty Chromium checkout. Use a clean locked revision." >&2
  exit 1
fi

shopt -s nullglob
patches=("$PATCH_DIR"/*.patch)
if (( ${#patches[@]} == 0 )); then
  echo "No WAE patches found in $PATCH_DIR" >&2
  exit 1
fi

# Preflight the complete series before modifying the checkout. A later
# patch conflict must not leave an earlier subset silently applied.
for patch in "${patches[@]}"; do
  echo "Checking $(basename "$patch")"
  git -C "$CHROMIUM_SRC" apply --check "$patch"
done

for patch in "${patches[@]}"; do
  echo "Applying $(basename "$patch")"
  git -C "$CHROMIUM_SRC" apply "$patch"
done

git -C "$CHROMIUM_SRC" diff --check
echo "Applied ${#patches[@]} WAE patch(es) to Chromium $ACTUAL_REVISION."
echo "Review with: git -C \"$CHROMIUM_SRC\" diff"
