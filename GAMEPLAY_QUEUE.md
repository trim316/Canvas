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


## P10 — The homestead feels inhabited (new independent tranche)

- [ ] READY G21: Recognize a *lived-in* home only after separate meaningful returns on separate Minecraft days, not merely repeat cues in one session. Extend world-scoped home memory and HomecomingPolicy; fallback to current QUIET/FAMILIAR/VILLAGE behavior. GameTest: multiple same-day returns do not simulate established history; save/reload preserves legitimate progress.
- [ ] READY G22: Make an authored greenhouse or covered growing area feel quieter during rain **only** when loaded domestic evidence and shelter support that interpretation. Reuse HomeEvidenceDetector and current rain ambience; unknown structures stay generic. GameTest: open farm, natural cave, roofed bed/work area, and unloaded edges.
- [ ] READY G23: Give first-light-at-home a subtle one-time-per-morning environmental transition after a genuine overnight presence, using existing home-morning sound. No alarm, notifications or forced sleep. GameTest: time skips, reconnects, and repeated loaded ticks never spam cues.
- [ ] READY G24: Detect a repeated return to a player-built dock/harbor and slightly adapt the existing exploration ambience with stored place familiarity, not boat-specific invented structures. GameTest: harbor-qualified authored evidence and repeated visits vs open shoreline; no chunk loads.
- [ ] READY G25: Make long, undisturbed evenings at home permit longer quiet intervals, while preserving accessibility and weather transitions. Tune existing moment-budget/home phase policies rather than adding content. GameTest: long idle, sudden storm, return cue, and no-sound-on-disable.

## P11 — Seasonal rhythm without seasonal chores

- [ ] READY G26: Seasonal village wake-up variation that uses already-known village and season states, existing approved cues, and no villager scheduling changes. GameTest: spring/winter present differently, UNKNOWN season retains ordinary village rhythm.
- [ ] READY G27: Make first genuine autumn evening in an established home a rare atmospheric micro-moment using existing seasonal audio, not a checklist or popup. Persist once-per-season/world scope. GameTest: saves, dimension changes, and duplicated server packets cannot replay it.
- [ ] READY G28: When a known home experiences consecutive rainy visits, vary rain ambience subtly without increasing total loudness or manufacturing weather. GameTest: repeated rain, sheltered/exposed changes, and cooldown behavior.
- [ ] READY G29: Give a familiar domestic animal's spring reappearance a very restrained old-friend accent *after* the animal has actually been absent; extend familiarity memory, no behavioral control. GameTest: false absences due to unloaded chunks do not create a reunion.
- [ ] READY G30: Preserve seasonal sound continuity across portal trips and returns: suppress seasonal Overworld audio in Nether/End and resume only once the same world-scoped home is confirmed. Client GameTest: rapid dimension crossing and stale packets.

## P12 — Quiet discovery and shared world character

- [ ] READY G31: Add a rare 'recognize this route' return moment once the existing RouteFamiliarityTracker has sustained evidence, reusing available approved audio rather than map markers or generated trails. GameTest: one-off visit vs established path, world fork isolation, throttling.
- [ ] READY G32: Recognize a player-built overlook after repeated observation from a stable loaded position and nearby authored evidence, without scanning distant terrain. Use existing place/landmark engine; GameTest: real dwell vs fly-by and unloaded boundary.
- [ ] READY G33: Add restrained twilight exploration ambience for known player-authored places, distinct from first-discovery wonder. Use current exploration music budget and sound assets; GameTest: no additional music when another moment is active.
- [ ] READY G34: Make multiplayer return to a shared settlement recognize *shared* context without disclosing another player's private familiarity records. Use current shared settlement/world-memory payloads; GameTest: two players, private records unchanged, dimension separation.
- [ ] READY G35: Give a repeated visit to an authored gathering spot a subtle sense of anticipation **only when vanilla villagers are actually present**, not via new festivals, calendar mechanics or spawned entities. GameTest: empty settlement, loaded villagers, repeated events/cooldown.

## P13 — Cohesive vanilla+ presentation and release feel

- [ ] READY G36: Introduce an optional low-intensity presentation mode for players sensitive to repeated HUD pulses while preserving recognition and existing audio. Extend independent feature configuration safely; GameTest: pulse alpha and disabled/normal modes; never modify global game settings.
- [ ] READY G37: Prioritize dialogue-free environmental sound over repeated Canvas system-chat narration after first discovery of home/landmark/familiar face. Persist acknowledgement per world branch; GameTest: no chatter on reconnect or fork-leak.
- [ ] READY G38: Make cross-feature music arbitration coherent when homecoming, village rhythm, and exploration discoveries coincide; use a single pre-existing moment budget and defer or suppress lower-salience music without introducing another music player. GameTest: deterministic simultaneous triggers, no overlap.
- [ ] READY G39: Tune low-end visual overlays to fade rather than layer competing weather, seasonal and rare-event full-screen washes; retain existing coloring and never override Minecraft's own render settings. Client test: multiple concurrent overlays and bounded composite alpha.
- [ ] READY G40: Ensure an all-features-off configuration produces ordinary vanilla gameplay with no residual looping Canvas audio, overlays, network-driven presentation, or persistent recognition writes. Client/server integration acceptance: off/on/off toggle across restart, world swap, and weather change.

## New tranche handoff

Treat G21–G40 as an additional pool, not a reason to leave G01–G20 unfinished.
Whenever fewer than 10 total items across both tranches are independently READY,
derive more precise gameplay slices from playtest evidence and the product pillars
(Home, Seasons, Village, Familiar Vanilla Mobs, Travel, Weather, Sound, Wonder,
Multiplayer, World Memory). Avoid duplicate work, unsupported mod claims, and
new build-system development. Implement one cohesive playable change per branch.


## P14 — Homestead moments and player-authored places

- [ ] READY G41: Differentiate a *busy workshop* from a resting home using only repeated loaded evidence of vanilla crafting/storage/work blocks around an already-confirmed HOME; reuse existing ambience at restrained volume. Extend HomeEvidencePolicy and CanvasFeelClient; GameTest empty shelter, ordinary home, authored workshop, and unloading mid-observation.
- [ ] READY G42: Recognize returning to a repeatedly used *home entrance* from the outside, not merely standing within a wide HOME radius. Derive stable arrival from existing home coordinates and movement history; require real departure. GameTest walking past, circling inside, and genuine return.
- [ ] READY G43: Give a player-authored covered porch a gentle storm-observation presentation if shelter and HOME evidence both persist, without inferring a porch from a naturally sheltered cave. Reuse approved existing weather assets; GameTest roof-only cave, established house, wet-to-sheltered transition.
- [ ] READY G44: Suppress HOME audio and overlays immediately when the player explicitly leaves the recognized world, including during prolonged loading screens; never bleed old memories into the next server. Extend CanvasClient disconnect/world identity cleanup; client GameTest reconnect to a different world.
- [ ] READY G45: Let a long-established HOME gradually favor quieter repeat homecoming cues after many actual visits; use capped world-memory history and existing cue variants, no reward inflation. GameTest first, fifth, twentieth, and overflow returns.

## P15 — Vanilla villagers and the sense of community

- [ ] READY G46: Distinguish an active village market period from an ordinary cluster using repeated observation of *loaded* vanilla villagers plus work infrastructure; no trading or villager behavior overrides. Extend VillageLifePolicy/CanvasVillageLifeClient; GameTest fleeting crowd, village workday, and unloaded edge.
- [ ] READY G47: Introduce a restrained village-after-rain atmosphere only for an established, currently loaded settlement and a real observed storm ending; reuse existing approved ambience. GameTest village storm end vs an unrecognized roadside stop; prove cooldown.
- [ ] READY G48: Make shared community-gathering presentation responsive to genuine proximity rather than broadcasting equivalent HUD/audio to every connected player. Reuse CommunityGatheringPayload and CanvasCommunityClient; multiplayer GameTest near/far players and dimension boundaries.
- [ ] READY G49: Prevent a village from constantly feeling 'busy' when the player camps near a stable crowd. Introduce stable-state decay and meaningful change triggers in village presentation, no entity scheduling. GameTest hour-long stable observation vs legitimate new gathering.
- [ ] READY G50: Mark return to a previously visited vanilla village with a gentle familiarity variation when shared world-memory evidence supports it; distinguish player-authored settlement from a chance encounter. GameTest known/unknown village, fork isolation, and opt-out.

## P16 — Exploration, travel and understated surprises

- [ ] READY G51: Adapt quiet travel ambience during genuine boat travel through a familiar, loaded, player-recognized water route without changing boat speed or sea generation. Use existing RouteFamiliarityTracker and TravelAtmospherePolicy; GameTest boat vs walking and unknown route.
- [ ] READY G52: Give a mature vanilla minecart route an understated return-home transition as it approaches an established authored destination; never change minecart mechanics or force rails/chunks. GameTest near/far known destination and unloaded rails.
- [ ] READY G53: Distinguish exploring a known place during clear weather versus after a *naturally observed* rainstorm using existing ExplorationWeatherPolicy cues. GameTest rain-start/recovery timing, unknown location, and disabled weather family.
- [ ] READY G54: Permit one rare long-session discovery in the End using only directly observed loaded landmarks and existing sound assets, with no generated features and minimal overlays. GameTest no fabricated landmark evidence, rarity limits, dimension transitions.
- [ ] READY G55: Prevent repeated exploration surprise cues when repeatedly crossing a dimension portal or chunk boundary near the same recognized location. Extend existing exploration surprise/history cooldown; GameTest rapid portal/edge oscillation and genuine later revisit.

## P17 — Pack feel, restraint and long-lived worlds

- [ ] READY G56: Introduce configurable ambient-audio *ducking* when Canvas plays its own short music moment, affecting only Canvas-owned loops (never Minecraft or companion music). Refine CanvasFeelClient/CanvasSeasonClient sound targets; client acceptance overlaps home, storm, and music with smooth recovery.
- [ ] READY G57: Make HOME recognition messaging unobtrusive in multiplayer where multiple players concurrently establish separate homes. Keep private client-side feedback, no public chat broadcast, and strict world-scope separation. GameTest two simultaneous homes and reconnect.
- [ ] READY G58: Reduce surprising visual pulses for movement-sensitive players by making pulse strength and duration follow existing client feature settings while preserving the audio-only moment. Client acceptance compares reduced/normal/off and reload persistence.
- [ ] READY G59: Define an actual low-end dense-village experience budget that prioritizes existing audio/context over cosmetic overlay updates under load, without reducing base Minecraft frame performance. Instrument only Canvas client update work; test bounded work in synthetic high-density snapshots.
- [ ] READY G60: Complete a full day/night/rain/return-home playable scenario in cloud client GameTests with only approved companion stack: verify audible cue selection, restrained repeated presentation, no missing resources, and no accidental world edits. Preserve machine evidence; separate human FEEL review remains outstanding.

## P14–P17 dispatch notes

G41–G60 are **additional implementation work**, not replacements for G01–G40.
Dispatch one cohesive feature at a time; use independent gameplay branches while
other branches validate. Each item must either use an existing sound or wait
for a separately approved, distributable Canvas-owned audio asset. Do not claim
that a proposed sound, companion mod, or gameplay behavior already exists. A
failed gameplay experiment should leave a testable learning and a narrow fix,
not another infrastructure program. Continue replenishing as READY work drops.

## Release acceptance still required

Manual Windows Modrinth profile test, long-play FPS/stutter, forever-world
backup/restore, multiplayer and subjective home/village/wonder FEEL cannot be
declared proven from CI. Defer any use of the user's local session until it
cannot interfere with OceanCanvas; keep doing independent gameplay work.
