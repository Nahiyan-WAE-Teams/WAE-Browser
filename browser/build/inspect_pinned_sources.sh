#!/usr/bin/env bash
# Read-only source audit helper for the exact Chromium revision pinned by WAE.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WAE_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
LOCK_FILE="$WAE_ROOT/browser/revisions/chromium.lock"

usage() {
  echo "Usage: $0 /path/to/chromium/src" >&2
  exit 2
}

[[ $# -eq 1 ]] || usage
CHROMIUM_SRC="$(cd -- "$1" && pwd)"
git -C "$CHROMIUM_SRC" rev-parse --is-inside-work-tree >/dev/null

lock_value() {
  local key="$1"
  sed -n "s/^${key}=//p" "$LOCK_FILE" | tail -n 1
}

EXPECTED_REVISION="$(lock_value revision)"
EXPECTED_VERSION="$(lock_value version)"
ACTUAL_REVISION="$(git -C "$CHROMIUM_SRC" rev-parse HEAD)"

if [[ "$ACTUAL_REVISION" != "$EXPECTED_REVISION" ]]; then
  printf 'ERROR: Chromium checkout revision mismatch\nExpected: %s\nActual:   %s\n' \
    "$EXPECTED_REVISION" "$ACTUAL_REVISION" >&2
  exit 1
fi

ACTUAL_VERSION="$(
  git -C "$CHROMIUM_SRC" show HEAD:chrome/VERSION |
    awk -F= '
      $1 == "MAJOR" { major=$2 }
      $1 == "MINOR" { minor=$2 }
      $1 == "BUILD" { build=$2 }
      $1 == "PATCH" { patch=$2 }
      END {
        if (major == "" || minor == "" || build == "" || patch == "") exit 1
        print major "." minor "." build "." patch
      }'
)"

if [[ "$ACTUAL_VERSION" != "$EXPECTED_VERSION" ]]; then
  printf 'ERROR: chrome/VERSION mismatch\nExpected: %s\nActual:   %s\n' \
    "$EXPECTED_VERSION" "$ACTUAL_VERSION" >&2
  exit 1
fi

echo "WAE Chromium source audit"
echo "Revision: $ACTUAL_REVISION"
echo "Version:  $ACTUAL_VERSION"
echo "Commit position:"
git -C "$CHROMIUM_SRC" log -1 --format=%B | grep -E 'Cr-Commit-Position:' || true

echo
echo "Working-tree status (inspection is read-only):"
git -C "$CHROMIUM_SRC" status --short

echo
echo "Relevant Java source paths:"
mapfile -t JAVA_FILES < <(
  git -C "$CHROMIUM_SRC" ls-files |
    grep -E '(^|/)(ChromeTabbedActivity|TabbedRootUiCoordinator|ChromeActivity|TabModelSelector|TabModel|TabObserver|Tab|Profile|DownloadManagerBridge|DownloadDelegateImpl|DownloadManagerCoordinator|DownloadManagerService|PermissionPromptAndroid|PermissionDialogDelegate)\.java$' ||
    true
)
if (("${#JAVA_FILES[@]}" == 0)); then
  echo "No expected Java source files were found at this revision."
else
  printf '%s\n' "${JAVA_FILES[@]}"
  echo
  echo "Lifecycle, tab-model, profile, download, and permission symbols:"
  git -C "$CHROMIUM_SRC" grep -n -E \
    'createRootUiCoordinator|initializeToolbar|TabModelSelector|addObserver|removeObserver|getCurrentModel|getModel\(|isOffTheRecord|getOrCreatePrimaryOtrProfile|DownloadManagerBridge|DownloadDelegateImpl|DownloadManagerCoordinator|PermissionPromptAndroid|PermissionDialogDelegate' \
    HEAD -- "${JAVA_FILES[@]}" || true
fi

echo
echo "Relevant GN/build files:"
mapfile -t GN_FILES < <(
  git -C "$CHROMIUM_SRC" ls-files |
    grep -E '(^|/)(chrome_public_apk_tmpl\.gni|BUILD\.gn|buildflags\.gni|chrome_build\.gni)$' |
    grep -E '(^chrome/android/|^chrome/browser/(tabmodel|download)/android/|^extensions/buildflags/|^build/config/)' ||
    true
)
if (("${#GN_FILES[@]}" == 0)); then
  echo "No expected GN files were found."
else
  printf '%s\n' "${GN_FILES[@]}"
  echo
  echo "Relevant target and feature-flag declarations:"
  git -C "$CHROMIUM_SRC" grep -n -E \
    'chrome_public_apk|chrome_public_manifest_package|enable_extensions|enable_extensions_core|enable_desktop_android_extensions|DownloadManagerBridge|DownloadDelegateImpl|DownloadManagerCoordinator' \
    HEAD -- "${GN_FILES[@]}" || true
fi

echo
echo "Audit complete. No source files were changed."
echo "Review the exact declarations and their owning BUILD.gn targets before authoring a WAE adapter."
