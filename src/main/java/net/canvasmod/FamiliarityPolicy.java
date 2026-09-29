package net.canvasmod;

public final class FamiliarityPolicy {
    public static final int SAMPLE_INTERVAL_TICKS = 20;
    public static final int REQUIRED_OBSERVATION_TICKS = 20 * 45;
    public static final int CUE_COOLDOWN_TICKS = 20 * 60 * 10;
    public static final double OBSERVATION_RADIUS = 16.0;

    private FamiliarityPolicy() { }

    public static boolean cueEligible(
            int observedTicks,
            boolean directlyTargeted,
            long ticksSinceLastCue) {
        return observedTicks >= REQUIRED_OBSERVATION_TICKS
                && directlyTargeted
                && ticksSinceLastCue >= CUE_COOLDOWN_TICKS;
    }
}
