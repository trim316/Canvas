# Canvas rolling GAMEPLAY development queue

Priority: Canvas is second to OceanCanvas. Use ONLY spare engineering time that does
not interfere with OceanCanvas's higher-priority recovery tests. The goal of this
queue is concrete Minecraft play experiences, not more development infrastructure.
Target Minecraft 26.2 Fabric. No Canvas block/state mutations, worldgen, custom
mobs, forced chunk loading, or mandatory quests. Keep every addition optional,
reversible, low-overhead, and compatible with vanilla player-authored worlds.

## Dispatch rules

- Work from this file as the product queue. POST_RC_QUEUE.md remains the separate
  release-evidence queue; do not let release-evidence work consume routine product
  development hours while gameplay tasks are ready.
- Keep **at least 10 distinct independently actionable READY gameplay items**.
  Whenever READY drops below 10, replenish it immediately with the next small,
  testable player-visible slice from the expansion candidates below.
- Each READY item needs a measurable experience, touched implementation surface,
  safe fallback, and a CI/GameTest acceptance check. Put implementation on a
  focused branch; keep separate branches for independent work while another
  feature is validating; do not repeatedly amend a branch with active CI.
- An item is DONE only after merged exact-head CI is green and there is concrete
  execution evidence. CI cannot certify the subjective feel: mark those items
  FIELD-ACCEPTANCE-REQUIRED without blocking other READY coding tasks.
- When an experiment fails, preserve any useful changes and the failing test;
  add a narrow regression, then proceed to a different READY item if independent.
- Never turn this queue into auto-approval for destructive operations or spending
  money. No local Minecraft session may interrupt OceanCanvas.

## Active gameplay handoffs (verify actual state every run)

- [ ] VALIDATING: P6 earned homecoming memory, PR #35; merged on acceptance.
- [ ] VALIDATING: P6 long-known vanilla mob old-friend experience, PR #36.
- [x] INTEGRATED: P6 dimension-appropriate Overworld/End travel presentation.
- [x] INTEGRATED: P6 earned seasonal homecoming music and storm precedence.

## P6 — Home, atmosphere and attachment

- [ ] READY G01: Smooth home ambience handoff between ordinary morning, evening
  and rain. Reuse CanvasFeelClient looping sounds and existing Fade policy; avoid
  overlapping multiple ambience loops across fast phase changes; GameTest policy
  proves repeated transitions respect volume ceiling and settle quietly.
- [ ] READY G02: Make familiar animals contribute to homecoming only if actively
  nearby in a loaded chunk and known in the current world identity. Add a
  conservative proximity/identity policy near CanvasFamiliarityClient and
  CanvasExperienceDirector; GameTest proves different-world animals cannot
  affect homecoming.
- [ ] READY G03: Let first return after a genuine long trip feel distinct from
  routine circling of the base without adding louder effects. Extend
  HomecomingPolicy/CanvasFeelClient with short-vs-long-away separation;
  GameTest proves short trips never trigger the long-trip flavor.
- [ ] READY G04: Give a storm shelter arrival a restrained calm-after-storm
  counterpart without repeating thunder music. Use WeatherCharacterPolicy
  and existing sound events; GameTest checks rain onset, shelter change and
  cooldown across repeated storms.
- [ ] READY G05: Allow a remembered home to sound subtly different after its
  first full seasonal cycle; use only existing CanvasSeasonMemoryStore counters,
  gated by SEASONS and HOME; GameTest proves no reward from fabricated cycles.

## P7 — Village life and familiar faces

- [ ] READY G06: Differentiate genuine village morning activity from isolated
  villagers nearby; refine existing VillageLifePolicy loaded-entity scoring,
  no mob AI control; GameTest separates one villager, established settlement,
  and temporary clustering.
- [ ] READY G07: Make village evening wind-down context-sensitive to sustained
  settlement presence rather than one-frame crowds. Extend
  CanvasSettlementRuntime/CanvasVillageLifeClient using bounded observations;
  GameTest verifies no cue-spam or false recognition.
- [ ] READY G08: Give a previously familiar neutral mob a restrained
  reunion cue after the player has been away long enough; use existing
  all-Mob UUID memory and targeting, no entity mutation; GameTest checks
  cooldown and same-world identity.
- [ ] READY G09: Make familiarity feedback quieter during crowded village
  gatherings. Route familiar-mob cues through the existing moment density
  guardrail with village context; GameTest enforces one salient moment at a time.
- [ ] READY G10: Restore predictable familiarity after save/reload without
  flooding greetings on relog. Improve current client persistence/session policy;
  GameTest uses temporary stores and verifies memory continuity and silence.

## P8 — Exploration, weather and wonder

- [ ] READY G11: Remember repeat walks past already-known player-authored
  landmarks and distinguish them from first discovery. Reuse existing
  LandmarkFamiliarityTracker and CanvasExplorationClient; GameTest verifies
  minimum revisit spacing and no generated landmarks.
- [ ] READY G12: Add a rare, silent exploration 'nothing happens' interval
  after consecutive discoveries so wonder remains exceptional. Extend
  MomentDensityPolicy/rare exploration selection; GameTest stress-tests a
  long exploration session with tight event caps.
- [ ] READY G13: Make sheltered rain at home perceptibly different from
  exposed rain during exploration through existing weather sound assets;
  no weather changes; GameTest checks loaded-only shelter behavior.
- [ ] READY G14: Expand End travel with an occasional sparse, non-intrusive
  context cue using existing approved audio; Nether should not inherit
  Overworld weather; GameTest verifies dimension/rarity/cooldown.
- [ ] READY G15: Improve seasonal exploration restraint so a season transition
  and a rare landmark moment do not both compete for music simultaneously.
  Reuse central density policy; GameTest proves music window budget.

## P9 — Actual playable pack cohesion

- [ ] READY G16: Audit production companion audio overlap against Canvas
  music/ambience and reduce simultaneous playback via in-game ownership
  policy, without suppressing vanilla music by default; CI client witness.
- [ ] READY G17: Verify all currently admitted 26.2 companion JARs actually
  contribute to the intended cozy experience; document which trial integrations
  were quarantined and avoid shipping inert or unnecessary content.
- [ ] READY G18: Improve first-play setup with one concise in-game, opt-out
  informational cue for Canvas HOME recognition and feature toggles; avoid
  tutorials/quests, repeated system messages, or new items.
- [ ] READY G19: Test world switching from Overworld to Nether/End and back:
  no stale home overlays, seasonal ambience or familiar-mob cues bleed between
  worlds/dimensions. Use existing identity hooks and client GameTests.
- [ ] READY G20: Preserve quietness on low-end devices under high settlement
  density: lower sampling cadence before dropping essential atmosphere.
  Test bounded sampling and no forced chunk loads.

## Expansion seeds (refill only after reviewing overlapping implementations)

- Village gathering invitation through subtle existing soundscape and villager
  presence; not a calendar, festival quest or newly spawned NPC.
- Four-season return-home texture from known weather/season/mob context.
- Optional no-overlay minimal FEEL mode that retains audio and memory.
- Multiplayer shared settlement ambience that never leaks private
  familiarity or overrides another player's world interpretation.
- Restrained homecoming when returning by minecart/boat, using only player
  travel context already observed in loaded simulation.
- Complement existing advancement packs with exploration atmosphere, not
  duplicate task checklists.
- Nether and End equivalent context semantics where evidence is reliable;
  UNKNOWN rather than invented interpretation otherwise.

## Release acceptance still required

Manual Windows Modrinth profile test, long-play FPS/stutter, forever-world
backup/restore, multiplayer and subjective home/village/wonder FEEL cannot be
declared proven from CI. Defer any use of the user's local session until it
cannot interfere with OceanCanvas; keep doing independent gameplay work.
