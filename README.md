# WAE Browser

WAE Browser is an Android browser project built around Chromium, with local-first privacy controls, user-installed extensions, a capable download manager, and optional self-hosted synchronization.

## Product decisions

- **Platform:** Android; ARM64 is the first supported ABI.
- **Browser engine:** Chromium. Android System WebView is not the target engine.
- **Distribution:** Direct APK releases and GitHub Releases.
- **Identity:** Anonymous by default; no WAE account required.
- **Sync:** Optional self-hosted server, end-to-end encrypted by the client.
- **Extensions:** User-installed extension packages, including local files; compatibility is staged and explicitly tested rather than assumed.
- **Privacy:** Strong defaults with understandable user controls. Browser security invariants are not user-disableable.
- **Telemetry:** No hidden telemetry and no mandatory cloud dependency.

## Status

This repository is being initialized. The current commit establishes product requirements, architecture, security boundaries, and the implementation roadmap. It does **not** yet contain a working browser, Chromium build, extension runtime, or release APK.

## Documentation

- [Product requirements](docs/PRODUCT_REQUIREMENTS.md)
- [Threat model](docs/THREAT_MODEL.md)
- [Module architecture](docs/ARCHITECTURE.md)
- [Chromium security maintenance](docs/CHROMIUM_MAINTENANCE.md)
- [Implementation plan](PLAN.md)

## Development principles

1. Verify current upstream Chromium and Android toolchain versions before pinning or upgrading dependencies.
2. Keep security-sensitive behavior explicit, reviewable, and covered by tests.
3. Preserve local browsing data by default; make sync opt-in.
4. Do not claim desktop Chrome extension parity without compatibility tests.
5. Complete implementation and code review milestones before spending CI runs; use CI deliberately.
6. Never ship a release with an unknown Chromium security baseline.

## License

The project license has not yet been selected. Do not assume that Chromium's license automatically determines the license of this repository or its original code.
