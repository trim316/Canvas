# Canvas automated development contract

Canvas development is repository/CI first. The player is not the compiler.

## Mechanical gates before experiential handoff

Every candidate must pass, without user intervention:

1. Java 25 compilation with warnings treated as errors.
2. Real Fabric Loom build against Minecraft 26.2.
3. Fabric Loader 0.19.3 / Fabric API 0.157.0+26.2 compatibility.
4. Runtime JAR metadata and class inspection.
5. Automated unit/self-tests as they are added.
6. Dedicated-server smoke and client/runtime witness gates before a candidate is called playable.
7. Companion-mod compatibility checks.
8. Upgrade/rollback packaging checks.

Only after these gates pass should a candidate be handed to the user for subjective FEEL validation.

## Product invariants

- No block placement/removal/state mutation by Canvas.
- No custom/non-vanilla mobs.
- No forced chunk loading for observation.
- No ownership of weather/time/worldgen.
- Prefer mature companion mods for acoustics, seasons, animation, particles and music engines.
- Canvas owns interpretation, memory, contextual orchestration and restrained reactions.
- Familiarity applies to all vanilla mobs, not only animals.
- Home is semantic evidence, never a timer-only radius.
