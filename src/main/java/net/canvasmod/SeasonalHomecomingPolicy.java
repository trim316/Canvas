package net.canvasmod;

/**
 * A small season-aware variation of an earned homecoming, not a new event
 * source. Existing homecoming cooldowns and shared music budget still decide
 * whether anything is presented. No season information means vanilla Canvas
 * homecoming behavior is retained unchanged.
 */
public final class SeasonalHomecomingPolicy {
    private SeasonalHomecomingPolicy() { }

    public static HomecomingPolicy.Plan adapt(
            HomecomingPolicy.Plan original,
            SeasonPolicy.Season season,
            int previousReturns,
            int seasonTransitions) {
        if (original == null) throw new IllegalArgumentException("homecoming plan required");
        if (season == null || season == SeasonPolicy.Season.UNKNOWN) return original;

        // The player has to have an established relationship with this place.
        // Simply entering a base in a new world is not a seasonal music trigger.
        boolean earned = previousReturns >= 2
                && original.flavor() != HomecomingPolicy.Flavor.QUIET;
        if (!earned) return original;

        float multiplier = SeasonalHomeProfile.homecomingMusicPitchMultiplier(season);
        float cuePitch = Math.max(0.80f, Math.min(1.15f, original.cuePitch() * multiplier));

        // Thunder at home is already authored as its own musical moment. Never
        // replace its storm composition with a seasonal cue.
        boolean stormMusic = "music.coming_home_storm".equals(original.musicEvent());
        String music = !stormMusic && seasonTransitions > 0
                ? SeasonalHomeProfile.transitionMusicEvent(season)
                : original.musicEvent();
        return new HomecomingPolicy.Plan(
                original.flavor(), original.cueEvent(), music,
                original.pulseTicks(), original.cueVolume(), cuePitch);
    }
}
