# Canvas Autonomous Development Queue

This file is the durable queue for the hourly Canvas development automation. The automation should not stop after one green commit. It should work through as many adjacent items as safely fit in one invocation, validating each tranche and continuing until the runtime/tool budget is nearly exhausted or a genuine human decision is required.

## Execution policy

1. Inspect the newest `main` commit and latest GitHub Actions run.
2. If CI is red, fix the actual root cause first. Re-run and continue in the same invocation when possible.
3. If CI is green, take the first incomplete queue item below.
4. Prefer one cohesive tranche over tiny one-line commits, but keep changes reviewable and reversible.
5. After every substantive tranche, push, let CI start, inspect progress, and fix mechanical failures immediately.
6. If a tranche is green and time remains in the same invocation, immediately start the next queue item.
7. Never wait for the user to say “continue” for already-approved roadmap work.
8. Only stop early for a subjective FEEL decision, a product tradeoff not already covered by the vision, external permissions, a dependency/Minecraft/Fabric incompatibility requiring a human choice, or imminent execution limits.
9. Preserve invariants: no Canvas block/world mutation, no custom mobs, no forced chunk loading, no terrain/worldgen ownership, semantic HOME rather than timer-only HOME, familiarity for all vanilla mobs, and mature companion mods for simulation-heavy ambience/acoustics/particles/music where appropriate.
10. Every new experience must gain automated evidence before it is treated as proven.

## Queue

### P0 — Finish Milestone 0 vertical slice
- [x] Weather character v2: rain-on-roof context, thunderstorm shelter mood, calm-after-storm transition, no block ownership. Proven by alpha.13 automated server/client/runtime evidence.
- [x] Coming Home v3: coordinate home ambience, contextual music, familiar mobs, village rhythm, weather, and return-history into one restrained arrival director. Proven by alpha.14 automated server/client/runtime evidence.
- [x] Rare Surprise v2: at least three presentation-only rare moments with deterministic rarity and cooldowns. Proven by alpha.15 automated server/client/runtime evidence.
- [x] Nothing Happens guardrails: explicit density ceilings and anti-spam tests so Canvas regularly stays quiet. Implemented in alpha.16; mark proven only after full CI is green.
- [x] World-memory persistence: persist meaningful HOME returns, familiar-mob continuity, village moments, and rare-surprise history across reloads. Implemented in alpha.16; mark proven only after full CI is green.
- [x] Milestone-0 scenario campaign: automated multi-day settlement scenario covering Coming Home, Rain on the Roof, Village Wakes Up, Village Winds Down, Familiar Face, Gathering, Did You See That?, and Nothing Happens. Implemented in alpha.17; mark proven only after full CI is green.
- [x] Performance gate: prove observation/orchestration stays within budget and never causes forced chunk loads. Implemented in alpha.17 with bounded probe-rate policy plus production-source mutation/chunk-load invariant verification; mark proven only after full CI is green.

### P1 — Seasons of Home
- [x] Season-provider abstraction so Canvas observes an installed season mod rather than owning season simulation. Proven by alpha.18 automated server/client/runtime evidence.
- [x] Spring/summer/autumn/winter atmosphere profiles. Proven by alpha.18 automated server/client/runtime evidence.
- [x] Seasonal HOME ambience and music orchestration. Proven by alpha.18 automated server/client/runtime evidence.
- [x] Seasonal village rhythm interpretation. Implemented in alpha.19 with season-aware presentation only; mark proven after full CI is green.
- [x] Seasonal familiar-mob presentation. Implemented in alpha.19 with season-aware cue tone and pulse; mark proven after full CI is green.
- [x] First-snow moment and other rare seasonal moments. Implemented in alpha.19 with fail-closed snowfall observation, deterministic rarity, and density gating; mark proven after full CI is green.
- [x] Persistence of seasonal memories without punitive crop/calendar mechanics. Implemented in alpha.19 with HOME-scoped season continuity and first-snow memory; mark proven after full CI is green.
- [x] Automated four-season scenario campaign. Implemented in alpha.19 with deterministic coverage of seasonal HOME, village, familiar, first-snow, and rare-moment behavior; mark proven after full CI is green.

### P2 — Exploration, wonder, and place attachment
- [x] Place familiarity beyond HOME: paths, docks, farms, viewpoints, gathering spots. Implemented on the P2 branch with loaded-only semantic evidence, repeated recognition, bounded persistent memory, and performance accounting; mark proven after full CI is green.
- [x] Exploration music/context director. Implemented in alpha.20 with familiar-place transitions, place-specific tone, dedicated cooldowns, and the shared Nothing Happens music budget; mark proven after full CI is green.
- [x] Weather + terrain semantic moments. Implemented in alpha.21 with familiar dock rain, viewpoint thunder, and farm-after-rain interpretation; presentation-only, HOME-suppressed, cooldown-bound, and globally density-limited; mark proven after full CI is green.
- [x] Repeated-route familiarity. Implemented in alpha.22 with direction-neutral coarse path segments, repeated traversal thresholds, bounded persistent memory, and zero additional world scans; mark proven after full CI is green.
- [x] Quiet landmark recognition. Implemented in alpha.23 with repeated separated returns to familiar docks, farms, viewpoints, and gathering spots; silent recognition, bounded persistence, and no additional world scans; mark proven after full CI is green.
- [x] Rare wonder library with strong density limits. Implemented in alpha.24 with three landmark-earned context moments, deterministic sparse days, persistent per-landmark cooldown history, ten-minute local spacing, and the shared Nothing Happens ceiling; mark proven after full CI is green.

### P3 — Community and multiplayer
- [x] Shared settlement recognition. Implemented in alpha.25 by clustering durable semantic HOME anchors across players, assigning stable persisted settlement IDs, and requiring no additional world scans; mark proven after full CI is green.
- [x] Multiplayer-safe familiar faces and place memory. Implemented in alpha.26 with stable per-world identity, fail-closed client binding, world-namespaced familiar-mob/HOME/season memory, and runtime packet evidence; mark proven after full CI is green.
- [x] Community gathering interpretation. Implemented in alpha.27 from recognized settlement membership plus live player co-location only, with transition/cooldown gating, shared music density limits, and real client presentation evidence; mark proven after full CI is green.
- [x] Shared but non-authoritative world memories. Implemented in alpha.28 as settlement-scoped observational history for recognized settlements and community gatherings; it never replaces personal memory, names places, assigns ownership, or drives quests; mark proven after full CI is green.
- [x] Multiplayer evidence campaign. Implemented in alpha.29 with an end-to-end deterministic GameTest covering shared settlement formation, cross-world memory isolation, co-located member gathering interpretation, and persisted shared observational history; real client CI separately proves world-identity receipt and gathering presentation.

### P4 — Release hardening
- [x] Config surface for every experience family. Implemented in alpha.30 as restart-loaded `config/canvas-features.properties` toggles for HOME, weather/atmosphere, rare moments, familiar faces, village life, seasons, exploration/wonder, and community/multiplayer; defaults preserve current behavior and deterministic tests cover default-on and explicit-off parsing; mark proven after full CI is green.
- [x] Disable/reversibility tests. Implemented in alpha.31 with an all-family off/on round-trip that proves persisted personal world memory is unchanged, plus a source invariant that fails CI if any declared feature-family toggle becomes disconnected from its server/client execution path; mark proven after full CI is green.
- [x] Long-session soak. Implemented in alpha.32 as a deterministic 30-Minecraft-day orchestration soak covering every moment kind, repeated suppression pressure, and hard verification that presentation/music density ceilings never drift; mark proven after full CI is green.
- [x] Save/reload/backup/fork safety tests. Implemented in alpha.33 with world lineage + branch identity: ordinary reload and same-key backup restore preserve scope, differently keyed forks retain lineage but receive isolated branch scope, and legacy v1 identities migrate forward without losing the world ID; mark proven after full CI is green.
- [ ] Low-end performance budget.
- [ ] Mod compatibility matrix.
- [ ] Proven installer/update path.
- [ ] Release candidate audit.
