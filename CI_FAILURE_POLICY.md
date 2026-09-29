# Canvas CI Failure Policy

Canvas development should distinguish failures that mean the mod is unsafe from failures that only mean an evidence probe did not observe a condition.

## Failure classes

### Class A — Release-blocking correctness failures
Examples: compilation failure, server GameTest failure, world mutation invariant failure, forced chunk loading, serialization corruption, missing required runtime classes/resources.
Action: stop advancement, fix immediately, rerun until green.

### Class B — Release-blocking integration/runtime failures
Examples: Fabric/Loader incompatibility, client crash, mixin failure, companion-mod incompatibility in the admitted stack, missing sound resource, production-client startup failure.
Action: stop advancement, fix or quarantine the incompatible optional companion, rerun until green.

### Class C — Evidence/observability failures
Examples: a CI-only marker does not naturally trigger, a screenshot probe misses a transient presentation, a test scenario fails to create a rare condition even though deterministic policy tests pass.
Action: do not block unrelated roadmap development. Convert the probe to deterministic evidence, downgrade it to advisory, or schedule a dedicated scenario test. Keep the underlying policy covered by a deterministic test.

### Class D — CI/environment noise
Examples: Realms 401 in unauthenticated CI, X11 cursor warnings, headless OpenGL noise that does not affect rendering, transient artifact/upload issues.
Action: suppress/allowlist known harmless noise; retry infrastructure failures; never spend product-development cycles treating environment noise as a Canvas defect.

## Proactive rules

1. Classify every failure before fixing it.
2. Spend at most one short repair cycle on a Class C/D failure before quarantining or redesigning the probe.
3. Keep `main` moving when Class A/B gates are green, even if advisory Class C evidence is pending.
4. Prefer deterministic tests over waiting for a naturally rare runtime condition.
5. Parallelize independent validation: server/core, client/runtime, companion compatibility, and scenario evidence.
6. Preserve the last proven artifact while developing the next tranche.
7. A failed optional companion is quarantined automatically rather than blocking Canvas.
8. Add a regression test for every genuine Class A/B defect fixed.
9. Treat silence as a feature: Nothing Happens is proven primarily by deterministic density-budget tests, not by hoping a live CI session happens to suppress a moment.
