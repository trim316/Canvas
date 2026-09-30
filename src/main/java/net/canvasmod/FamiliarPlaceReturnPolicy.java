package net.canvasmod;

/**
 * A familiar authored place may feel gentler on returning after genuinely
 * being away. This adds no music or discoveries: it only colors an existing
 * exploration cue after the shared moment gate has already accepted it.
 */
public final class FamiliarPlaceReturnPolicy {
    public static final long REQUIRED_AWAY_TICKS = 20L * 60L * 2L;

    private long leftPlaceAt = -1L;
    private boolean seenFamiliarPlace;

    public void reset() {
        leftPlaceAt = -1L;
        seenFamiliarPlace = false;
    }

    public boolean observe(PlaceFamiliarityPolicy.Kind kind, boolean familiar, long tick) {
        if (kind == null || kind == PlaceFamiliarityPolicy.Kind.NONE) {
            if (seenFamiliarPlace && leftPlaceAt < 0L) leftPlaceAt = tick;
            return false;
        }
        boolean valid = familiar;
        boolean returned = valid && seenFamiliarPlace && leftPlaceAt >= 0L
                && tick >= leftPlaceAt
                && tick - leftPlaceAt >= REQUIRED_AWAY_TICKS;
        // Unfamiliar stops cannot silently inherit a prior landmark's credit.
        seenFamiliarPlace = valid;
        leftPlaceAt = -1L;
        return returned;
    }

    public static float volume(float existing, boolean earnedReturn) {
        return earnedReturn ? Math.max(0.0f, existing * 0.82f) : existing;
    }

    public static float pitch(float existing, boolean earnedReturn) {
        return earnedReturn ? Math.max(0.8f, existing * 0.96f) : existing;
    }
}
