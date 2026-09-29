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
- [ ] Season-provider abstraction so Canvas observes an installed season mod rather than owning season simulation.
- [ ] Spring/summer/autumn/winter atmosphere profiles.
- [ ] Seasonal HOME ambience and music orchestration.
- [ ] Seasonal village rhythm interpretation.
- [ ] Seasonal familiar-mob presentation.
- [ ] First-snow moment and other rare seasonal moments.
- [ ] Persistence of seasonal memories without punitive crop/calendar mechanics.
- [ ] Automated four-season scenario campaign.

### P2 — Exploration, wonder, and place attachment
- [ ] Place familiarity beyond HOME: paths, docks, farms, viewpoints, gathering spots.
- [ ] Exploration music/context director.
- [ ] Weather + terrain semantic moments.
- [ ] Repeated-route familiarity.
- [ ] Quiet landmark recognition.
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
