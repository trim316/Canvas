package net.canvasmod;

/**
 * Stable, loaded-observation-only village presence. A passing crowd should
 * never trigger village music; sustained ordinary vanilla villager presence
 * earns a morning, gathering or wind-down presentation. Does not alter mobs.
 */
public final class VillageContinuityPolicy {
    public static final int REQUIRED_CONSECUTIVE_SAMPLES = 3;

    private VillageLifePolicy.Rhythm pending = VillageLifePolicy.Rhythm.NONE;
    private VillageLifePolicy.Rhythm stable = VillageLifePolicy.Rhythm.NONE;
    private int consecutive;

    public VillageLifePolicy.Rhythm observe(VillageLifePolicy.Rhythm sample) {
        VillageLifePolicy.Rhythm next = sample == null
                ? VillageLifePolicy.Rhythm.NONE : sample;
        if (next == VillageLifePolicy.Rhythm.NONE) {
            reset();
            return stable;
        }
        if (next == stable) {
            pending = next;
            consecutive = 0;
            return stable;
        }
        if (next != pending) {
            pending = next;
            consecutive = 1;
        } else {
            consecutive = Math.min(REQUIRED_CONSECUTIVE_SAMPLES, consecutive + 1);
        }
        if (consecutive >= REQUIRED_CONSECUTIVE_SAMPLES) {
            stable = pending;
            consecutive = 0;
        }
        return stable;
    }

    public VillageLifePolicy.Rhythm stable() { return stable; }

    public void reset() {
        pending = VillageLifePolicy.Rhythm.NONE;
        stable = VillageLifePolicy.Rhythm.NONE;
        consecutive = 0;
    }
}
