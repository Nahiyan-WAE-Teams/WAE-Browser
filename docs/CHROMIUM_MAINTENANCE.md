# Chromium Security and Maintenance Policy

**Status:** Required operating policy; exact cadence and upstream revision must be verified at implementation time.

## 1. Revision pinning

Every build must identify:
- Chromium repository and exact commit hash.
- Corresponding milestone/version and security patch level.
- Local patch set and patch hashes.
- Build flags, toolchain versions, and target ABI.
- Third-party dependency and license inventory.

Never build release artifacts from an unpinned moving branch.

## 2. Monitoring

Monitor official Chromium release announcements, security fixes, branch updates, and Android build documentation. Verify the current stable branch and toolchain requirements from primary Chromium sources before each baseline update. Record the verification date and source URLs in the repository.

## 3. Internal response targets

These are project targets, not guarantees:
- **Critical actively exploited issue:** begin triage immediately; target a patched release within 24–72 hours after a usable upstream fix and validated build path are available.
- **High severity security fix:** target a release within 7 calendar days after a usable upstream fix is available.
- **Routine Stable baseline update:** target validation and release within 7 calendar days of the selected upstream stable update, subject to build and regression validation.

If upstream has not supplied a usable fix, publish a risk assessment and mitigation/update status rather than claiming the issue is resolved.

## 4. Release gates

A browser release must not ship unless:
- Its Chromium revision and security baseline are recorded.
- Relevant upstream security fixes are included or explicitly assessed.
- Local Chromium patches have been reviewed.
- Build inputs and dependencies are pinned.
- APK signing and release provenance are verified.
- Smoke tests and security regression tests pass.
- Known limitations are documented in release notes.

## 5. Emergency process

1. Identify affected Chromium revision and exposure.
2. Determine whether upstream fix is available.
3. Build a candidate with the smallest justified patch set.
4. Run focused regression and security tests.
5. Review signing and provenance.
6. Publish a clearly marked security release and user-facing advisory.
7. Verify rollout and document any remaining exposure.

## 6. Update policy

Do not silently disable security features to preserve compatibility. Do not backport complex security patches without review. Do not describe a build as current or secure solely because it compiles; state its exact Chromium revision and known patch status.
