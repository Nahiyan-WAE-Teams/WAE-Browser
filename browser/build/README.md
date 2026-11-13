# Chromium POC build

This guide builds the exact source revision in `../revisions/chromium.lock`. It does not produce a WAE-branded browser; it validates the upstream ARM64 Android build environment and pinned source.

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

The initial GN args are also recorded in `browser/build/args/arm64.gn` in the WAE repository. For the upstream checkout:

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

Do not add remote execution, production signing, custom patches, or a release GN configuration until the baseline build is reviewed. No command in this guide has been run by this repository's current build environment.
