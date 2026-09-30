package net.canvasmod;

/**
 * Earned, opt-in-by-attention client presentation for remembered vanilla mobs.
 * The player must deliberately look at a mob; this policy never moves, tames,
 * spawns or modifies the entity. Quiet long-term familiarity avoids chat spam.
 */
public final class FamiliarBondPolicy {
    public enum Bond { UNKNOWN, RECOGNIZED, OLD_FRIEND }

    public record Greeting(
            Bond bond, boolean showText, float volume, float pitchMultiplier,
            int pulseArgb) { }

    public static final int OLD_FRIEND_TICKS = 20 * 60 * 5;

    private FamiliarBondPolicy() { }

    public static Greeting greeting(
            int observedTicks,
            boolean greetedThisSession,
            SeasonPolicy.Season season) {
        if (observedTicks < FamiliarityPolicy.REQUIRED_OBSERVATION_TICKS) {
            return new Greeting(Bond.UNKNOWN, false, 0.0f, 1.0f, 0);
        }
        SeasonPolicy.Season safeSeason = season == null
                ? SeasonPolicy.Season.UNKNOWN : season;
        Bond bond = observedTicks >= OLD_FRIEND_TICKS
                ? Bond.OLD_FRIEND : Bond.RECOGNIZED;
        int seasonalColor = SeasonalFamiliarityProfile.pulseArgb(safeSeason);
        // A long-remembered animal feels gentler rather than louder. Preserve
        // the same existing acoustic cue instead of owning animal behavior.
        if (bond == Bond.OLD_FRIEND) {
            int warmPulse = (seasonalColor & 0x00FFFFFF) | 0x55000000;
            return new Greeting(
                    bond, !greetedThisSession,
                    Math.min(0.28f, SeasonalFamiliarityProfile.cueVolume(safeSeason)),
                    SeasonalFamiliarityProfile.cuePitch(safeSeason) * 0.955f,
                    warmPulse);
        }
        return new Greeting(
                bond, !greetedThisSession,
                SeasonalFamiliarityProfile.cueVolume(safeSeason),
                SeasonalFamiliarityProfile.cuePitch(safeSeason),
                seasonalColor);
    }
}
