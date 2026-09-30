# Canvas

Automated Minecraft 26.2 Fabric development line for the Canvas cozy vanilla+ runtime.

The repository is intentionally built around an evidence-first pipeline: compile, test, package, inspect, smoke-check, then hand off only proven candidates for experiential playtesting.


## Feature configuration

Canvas writes `config/canvas-features.properties` on first launch. Configuration is loaded at startup; restart the client/server after changing it.

Every current experience family defaults to `true` and can be disabled independently:

- `family.home`
- `family.weather`
- `family.rare_moments`
- `family.familiar_faces`
- `family.village_life`
- `family.seasons`
- `family.exploration`
- `family.community`

These switches disable Canvas interpretation/presentation for the selected family. They do not mutate world blocks, terrain, entities, or vanilla mechanics.


## Companion compatibility

The production companion contract is checked in at `compatibility/companion-matrix.json`. Required companions must pass the production-client launch; trial companions may be automatically quarantined if incompatible. The proven playtest bundle includes both this matrix and the exact CI admission report used for that candidate.
