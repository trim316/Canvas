# Canvas release acceptance — Minecraft 26.2 Fabric

## Release states

**Automated prerelease:** A GitHub prerelease may be published only from the
exact current main commit after the full four-stage `Canvas 26.2 CI` pipeline
succeeds (server GameTests, client/companion GameTests, provenance gate, packaged
and independently audited candidate). The publication job re-downloads that
run's actual audited ZIP and receipt, re-opens the installer manifest, verifies
all SHA-256 paths, matches the audited bundle hash, and uses an immutable
commit-qualified release tag. Never substitute another branch or rebuild
different binaries while claiming the earlier evidence. Publishing a candidate
does **not** mean the release is fully accepted.

**Stable release:** No stable release may be asserted until the human Windows
and backup-world acceptance record below is complete. The published candidate
must be the exact artifact used for these tests. If fixes are required, test a
new candidate and repeat the affected acceptance; do not edit a published
commit-qualified prerelease asset in place.

## Exact candidate proof

Record the published prerelease tag, complete source SHA, GitHub run URL, exact
ZIP SHA-256, `RELEASE-AUDIT.json`, admitted companion versions, and explicitly
quarantined optional companions. The automated receipt proves artifact
integrity and bounded automated simulations, not subjective coziness or every
hardware/third-party profile combination.

## Real Windows release blockers

1. **Transaction test on a throwaway Modrinth profile.** Back up the target
   profile before any operation. Install `Canvas-26.2-PROVEN.zip` using
   `INSTALL-CANVAS.cmd`, launch, verify correct Canvas version and admitted
   companions, repeat install/update, then use `RESTORE-CANVAS.cmd` and verify
   the exact original files and settings. Record Windows version, Java, profile,
   screenshot/log, hashes and rollback result. Use a disposable test profile;
   do not touch OceanCanvas's active profile.
2. **Forever-world safety.** Use a copied, backed-up real survival world with
   player-built houses, farms, village infrastructure and familiar vanilla
   animals. Compare representative block/entity state against the untouched
   backup before and after a long normal session, season/weather changes, world
   reload, Nether/End roundtrip, and disabling all Canvas families. Verify
   Canvas did not write, move or replace world blocks or create mobs. Test
   world-copy/fork isolation and restoration. Record evidence and any anomalies.
3. **Real feel and low-end acceptance.** Over several Minecraft days, walk home
   after both short and long trips, experience morning/evening/night, sheltered
   rain, storm clearing over a real village, a familiar animal, remembered
   authored places and long quiet exploration intervals. Verify cues feel
   restrained and are not repetitive, loops cut cleanly on world swaps, and
   there are no missing audio assets. Capture gameplay notes and frametime
   measurements on an available low-end compatible device; hosted Xvfb is not
   a substitute for human perception or real GPU hardware.
4. **Multiplayer and compatibility.** Join with at least two players at
   different homes and shared settlements; verify private familiarity does
   not leak, no global unwanted broadcasts, clean world/dimension transitions,
   and no unexpected companion sound suppression. Confirm server/client
   configuration off/on behavior and load/save stability.
5. **Decision and evidence.** Record a clear PASS/FAIL/NOT TESTED for each
   requirement with actual artifacts, environment and tester. No stable tag or
   user-facing 'release ready' statement while an essential check is untested
   or failed. Fix, retest, and publish a NEW candidate if needed.

## Operator links

- GitHub Actions → Canvas 26.2 CI: latest proven candidate bundles and evidence.
- GitHub Releases: automatically published **prerelease only** after the
  exact-current-main full CI success and independent package verification.
- GitHub Actions → Canvas Windows installer validation: deterministic Windows
  transaction harness evidence (not the required human installer run).

## Zero-cost boundary

Use free GitHub public repository Actions and release hosting only within
available free limits. No cloud subscriptions, external paid services, or
additional developer tooling purchases are approved. Do not consume the user's
local OceanCanvas test session for Canvas release testing.
