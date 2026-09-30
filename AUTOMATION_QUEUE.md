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
- [ ] Rare wonder library with strong density limits.

### P3 — Community and multiplayer
- [ ] Shared settlement recognition.
- [ ] Multiplayer-safe familiar faces and place memory.
- [ ] Community gathering interpretation.
- [ ] Shared but non-authoritative world memories.
- [ ] Multiplayer evidence campaign.

### P4 — Release hardening
- [ ] Config surface for every experience family.
- [ ] Disable/reversibility tests.
- [ ] Long-session soak.
- [ ] Save/reload/backup/fork safety tests.
- [ ] Low-end performance budget.
- [ ] Mod compatibility matrix.
- [ ] Proven installer/update path.
- [ ] Release candidate audit.
