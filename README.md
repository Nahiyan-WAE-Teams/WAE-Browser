# WAE Browser

WAE Browser is a privacy-focused Android browser project built around Chromium, with local-first controls, user-installed extensions, a capable download manager, offline reading, and optional self-hosted synchronization.

## Product decisions

- **Platform:** Android 10 (API 29) minimum; ARM64 first.
- **Browser engine:** Chromium. Android System WebView is not the target engine.
- **Distribution:** Direct APK releases and GitHub Releases.
- **Identity:** Anonymous by default; no WAE account required.
- **Sync:** Optional self-hosted server, with client-side encryption.
- **Extensions:** User-installed extension packages, including local files; compatibility is staged and explicitly tested rather than assumed.
- **Privacy:** Strong defaults with understandable user controls. Browser security invariants are not user-disableable.
- **Telemetry:** No hidden telemetry and no mandatory cloud dependency.

## Current status

The repository contains the product/security baseline and an initial Android application shell using Jetpack Compose. The shell is a UI foundation only: Chromium navigation, extension execution, privacy enforcement, downloads, and sync are not yet integrated. No release APK has been produced.

Active implementation branch: [feat/android-foundation](https://github.com/Nahiyan-WAE-Teams/WAE-Browser/tree/feat/android-foundation).

## Documentation

- [Product requirements](docs/PRODUCT_REQUIREMENTS.md)
- [Threat model](docs/THREAT_MODEL.md)
- [Module architecture](docs/ARCHITECTURE.md)
- [Verified Android toolchain baseline](docs/TOOLCHAIN.md)
- [Chromium security maintenance](docs/CHROMIUM_MAINTENANCE.md)
- [Implementation plan](PLAN.md)

## Android build baseline

- Minimum SDK: 29 (Android 10)
- Compile / target SDK: 36
- Android Gradle Plugin: 9.5.0
- Gradle: 9.6.0
- Kotlin: 2.4.21
- JDK: 17
- Compose BOM: 2026.09.00

See [docs/TOOLCHAIN.md](docs/TOOLCHAIN.md) for source references and constraints. These versions have been checked against upstream documentation but have not yet been validated by a Gradle build in this repository.

## Development principles

1. Verify current upstream Chromium and Android toolchain versions before pinning or upgrading dependencies.
2. Keep security-sensitive behavior explicit, reviewable, and covered by tests.
3. Preserve local browsing data by default; make sync opt-in.
4. Do not claim desktop Chrome extension parity without compatibility tests.
5. Complete implementation and code review milestones before spending CI runs; use CI deliberately.
6. Never ship a release with an unknown Chromium security baseline.

## License

The project license has not yet been selected. Do not assume that Chromium's license automatically determines the license of this repository or its original code.
