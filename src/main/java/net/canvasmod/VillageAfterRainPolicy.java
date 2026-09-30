package net.canvasmod;

/**
 * A soft after-rain village moment must be earned by observing an actual loaded
 * settlement through sustained rain, followed by clearing weather. No changes
 * to weather, villagers, blocks or Minecraft sound ownership.
 */
public final class VillageAfterRainPolicy {
    public static final int MIN_RAINY_VILLAGE_SAMPLES = 3;
    public static final long COOLDOWN_TICKS = 20L * 60L * 12L;

    private int rainyVillageSamples;
    private boolean previouslyWet;
    private long lastPresentationTick = Long.MIN_VALUE / 4L;

    public boolean observe(
            boolean wet,
            VillageLifePolicy.Rhythm stableRhythm,
            long tick) {
        boolean village = stableRhythm != null
                && stableRhythm != VillageLifePolicy.Rhythm.NONE;
        boolean qualifies = !wet && previouslyWet && village
                && rainyVillageSamples >= MIN_RAINY_VILLAGE_SAMPLES
                && tick - lastPresentationTick >= COOLDOWN_TICKS;

        if (!village) {
            rainyVillageSamples = 0;
        } else if (wet) {
            rainyVillageSamples = Math.min(
                    MIN_RAINY_VILLAGE_SAMPLES, rainyVillageSamples + 1);
        } else {
            rainyVillageSamples = 0;
        }
        previouslyWet = wet;
        if (qualifies) lastPresentationTick = tick;
        return qualifies;
    }

    public void reset() {
        rainyVillageSamples = 0;
        previouslyWet = false;
        lastPresentationTick = Long.MIN_VALUE / 4L;
    }
}
