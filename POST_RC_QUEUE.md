# Canvas Post-RC Continuous Development

This is the durable successor to AUTOMATION_QUEUE.md. RC1 passing automated CI is a
candidate milestone, not a release declaration. Treat this queue as active whenever
P0-P4 in AUTOMATION_QUEUE.md is complete. Never mark a task complete solely
because its implementation exists: record the exact evidence and remaining risks.

## Operating rules

- Begin every run by verifying the exact current main SHA and latest full CI result.
- Never overwrite a green candidate to start another tranche. Use a feature branch
  and merge only when its exact head passed the applicable validation gates.
- If a class A/B failure blocks merging, save a minimal repro and regression test;
  continue unrelated READY work on a separate branch instead of waiting.
- Avoid repeated commits that cancel superseded multi-minute CI runs.
- Distinguish deterministic simulation from real Windows/manual experiential proof.
- When all items in this document are done, add the next scoped follow-up tranche
  based on remaining measurable risk instead of terminating the development queue.

## P5 — Release evidence and operational resilience

- [ ] READY: Audit the Windows install/update and restore scripts as *executable*
  workflows, including missing Modrinth profile, hash mismatch, duplicate mod-ID,
  interrupted copy, backup collision, failed rollback, and repeated restore.
  Build a deterministic Windows CI harness with throwaway profile directories;
  never touch a real user profile. Record exact run and artifacts.
- [ ] READY: Add candidate provenance checks preventing version/SHA/evidence drift
  across server test, client test, companion admission, bundle, and published receipt.
  Use deterministic fixtures for forged or missing evidence and fail closed.
- [ ] READY: Verify automated server/client disable behavior per family, not merely
  config parsing and source-token presence. Add deterministic off/on regression
  scenarios that prove no presentation or observation side effects while disabled.
- [ ] READY: Add save/back-up/fork adversarial tests for corrupted identities,
  mismatched save keys, partial disk writes, and restored worlds. Fail closed on
  uncertainty; never mutate Minecraft blocks or player-authored state.
- [ ] READY: Establish repeatable low-end CPU/memory/frametime measurements on
  the available CI host, label hardware limits, compare to the existing operation
  ceilings, and record regressions rather than claiming universal-device proof.
- [ ] READY: Revalidate the exact 26.2 companion lock and document quarantines,
  update/rollback compatibility and available production integration evidence.
- [ ] EXTERNAL ACCEPTANCE: Playtest the signed-off RC in a backed-up real
  forever-world-like save on Windows. Document actual FEEL, player agency,
  return-home continuity, world safety, long sessions, and multiplayer presence.
  This cannot be asserted solely from headless CI; continue other READY work
  while awaiting external experiential evidence.

## Evidence ledger

For each completed task, append: PR, merged commit, exact full CI run, key
artifact/receipt, remaining limitations, and next independently READY action.
