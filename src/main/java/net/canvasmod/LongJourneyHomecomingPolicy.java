package net.canvasmod;

/**
 * A long trip has to be genuinely away from an established HOME, not a loop
 * around the doorstep or time spent AFK in a nearby chunk. This only adapts
 * an already-earned presentation: it never triggers a cue, loads a chunk,
 * invents music or changes a player's world.
 */
public final class LongJourneyHomecomingPolicy {
    public static final long MIN_AWAY_TICKS = 20L * 60L * 4L;
    public static final double MIN_FURTHEST_DISTANCE_SQ = 192.0 * 192.0;

    private LongJourneyHomecomingPolicy() { }

    public static boolean isLongJourney(
            long awayTicks,
            double furthestDistanceSq,
            boolean visitedOtherDimension) {
        return awayTicks >= MIN_AWAY_TICKS
                && (visitedOtherDimension
                    || (Double.isFinite(furthestDistanceSq)
                        && furthestDistanceSq >= MIN_FURTHEST_DISTANCE_SQ));
    }

    public static HomecomingPolicy.Plan adapt(
            HomecomingPolicy.Plan normal,
            long awayTicks,
            double furthestDistanceSq,
            boolean visitedOtherDimension,
            int previousReturns) {
        if (normal == null) throw new IllegalArgumentException("homecoming plan required");
        // Do not manufacture a lived-in feeling before familiarity is earned.
        if (previousReturns < 2
                || normal.flavor() == HomecomingPolicy.Flavor.QUIET
                || !isLongJourney(awayTicks, furthestDistanceSq, visitedOtherDimension)
                // The storm shelter composition must remain distinctive.
                || "music.coming_home_storm".equals(normal.musicEvent())) {
            return normal;
        }

        // A gentler, slightly longer welcome. Reuse the same cue and the
        // existing music choice so the shared music budget remains accurate.
        return new HomecomingPolicy.Plan(
                normal.flavor(),
                normal.cueEvent(),
                normal.musicEvent(),
                Math.min(240, normal.pulseTicks() + 24),
                Math.max(0.0f, normal.cueVolume() * 0.92f),
                Math.max(0.8f, Math.min(1.2f, normal.cuePitch() * 0.97f)));
    }
}
