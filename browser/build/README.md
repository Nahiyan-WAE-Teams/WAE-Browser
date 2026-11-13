# Chromium POC build

This guide builds the exact source revision in `../revisions/chromium.lock`. The first build is an unmodified Chromium baseline. A second configuration uses WAE's package namespace, but does not yet apply WAE app-label, icon, or browser-UI patches.

## Host requirements

- 64-bit Linux.
- At least 8 GB RAM; more than 16 GB recommended.
- At least 100 GB free disk.
- Git, Python, `depot_tools`, and the Chromium Android build dependencies.
- Use a path without spaces.

Official pinned-revision guide:
https://github.com/chromium/chromium/blob/81ff2496eed313447d34079a6f45c57f9c5611c1/docs/android_build_instructions.md

## Checkout and sync

Run from a clean workspace:

```sh
mkdir -p "$HOME/wae-chromium"
cd "$HOME/wae-chromium"
fetch --nohooks android
cd src
git fetch origin refs/branch-heads/8103
git checkout --detach 81ff2496eed313447d34079a6f45c57f9c5611c1
gclient sync
```

Install host dependencies and run Chromium hooks:

```sh
build/install-build-deps.sh
gclient runhooks
```

## Generate and build ARM64

The baseline GN args are recorded in `browser/build/args/arm64.gn` in the WAE repository. For the upstream checkout:

```sh
gn gen out/WAE --args='target_os="android" target_cpu="arm64"'
autoninja -C out/WAE chrome_public_apk
```

For the official install helper, with an Android device connected and USB debugging enabled:

```sh
out/WAE/bin/chrome_public_apk install
```

## Validation gates

1. Confirm `git rev-parse HEAD` equals the locked revision before building.
2. Confirm `DEPS` pins NDK 30.0.16248370 and `build/config/android/config.gni` reports min SDK 29, target SDK 36, SDK platform 37.2, and Build Tools 37.0.0.
3. Save the complete GN args and build log with the result.
4. Install on an Android 10 / API 29 ARM64 device.
5. Record the APK's version, package metadata, and SHA-256.
6. Do not treat a successful ChromePublic build as proof that WAE's custom UI or engine adapter is implemented.

## WAE package namespace and app-name patch

After the unmodified baseline is reviewed, apply the WAE patch series to a clean checkout at the exact locked revision:

```sh
WAE_REPO=/path/to/WAE-Browser
bash "$WAE_REPO/browser/build/apply_wae_patches.sh" "$HOME/wae-chromium/src"
```

The script reads the expected source SHA from `browser/revisions/chromium.lock`, refuses a mismatched or dirty Chromium checkout, checks each patch before applying it, and runs `git diff --check`.

Then generate a separate WAE package-namespace output directory:

```sh
gn gen out/WAEBrand --args='target_os="android" target_cpu="arm64" chrome_public_manifest_package="com.wae.browser"'
autoninja -C out/WAEBrand chrome_public_apk
```

The GN arg `chrome_public_manifest_package` is declared by Chromium's `chrome/android/chrome_public_apk_tmpl.gni` and changes the Android application package namespace. Patch `0001` changes the manifest's `app_name` resource to WAE Browser. Neither change replaces Chromium launcher icons, remaining product strings, or Chrome's browser UI.

The patch and script are committed but have not yet been run against a Chromium checkout. Do not ship this package-namespace/app-name build as WAE Browser until the source overlay and runtime integration are reviewed.
