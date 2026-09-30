package net.canvasmod;

public final class SeasonalRareMomentPolicy {
    public enum Moment {
        NONE,
        FIRST_SNOW,
        SPRING_CHORUS,
        SUMMER_AFTERGLOW,
        AUTUMN_HUSH,
        WINTER_STILLNESS
    }

    public static final long MIN_DAYS_BETWEEN_MOMENTS = 4L;

    private SeasonalRareMomentPolicy() { }

    public static Moment classify(
            SeasonPolicy.Season season,
            boolean atHome,
            boolean snowing,
            long worldDay,
            long daysSinceLast,
            boolean firstSnowSeen) {
        if (!atHome || season == null || season == SeasonPolicy.Season.UNKNOWN) return Moment.NONE;
        if (season == SeasonPolicy.Season.WINTER && snowing && !firstSnowSeen) return Moment.FIRST_SNOW;
        if (daysSinceLast < MIN_DAYS_BETWEEN_MOMENTS) return Moment.NONE;

        return switch (season) {
            case SPRING -> Math.floorMod(worldDay, 11L) == 2L ? Moment.SPRING_CHORUS : Moment.NONE;
            case SUMMER -> Math.floorMod(worldDay, 13L) == 4L ? Moment.SUMMER_AFTERGLOW : Moment.NONE;
            case AUTUMN -> Math.floorMod(worldDay, 11L) == 7L ? Moment.AUTUMN_HUSH : Moment.NONE;
            case WINTER -> Math.floorMod(worldDay, 17L) == 9L ? Moment.WINTER_STILLNESS : Moment.NONE;
            case UNKNOWN -> Moment.NONE;
        };
    }

    public static String cueEvent(Moment moment) {
        return switch (moment) {
            case FIRST_SNOW -> "season.rare_first_snow";
            case SPRING_CHORUS -> "season.rare_spring_chorus";
            case SUMMER_AFTERGLOW -> "season.rare_summer_afterglow";
            case AUTUMN_HUSH -> "season.rare_autumn_hush";
            case WINTER_STILLNESS -> "season.rare_winter_stillness";
            case NONE -> "";
        };
    }

    public static float cuePitch(Moment moment) {
        return switch (moment) {
            case FIRST_SNOW -> 1.03f;
            case SPRING_CHORUS -> 1.06f;
            case SUMMER_AFTERGLOW -> 1.015f;
            case AUTUMN_HUSH -> 0.975f;
            case WINTER_STILLNESS -> 0.94f;
            case NONE -> 1.0f;
        };
    }

    public static int washArgb(Moment moment) {
        return switch (moment) {
            case FIRST_SNOW -> 0x12D8F1FF;
            case SPRING_CHORUS -> 0x109BE6AD;
            case SUMMER_AFTERGLOW -> 0x10FFD58A;
            case AUTUMN_HUSH -> 0x12D67B42;
            case WINTER_STILLNESS -> 0x102A5275;
            case NONE -> 0;
        };
    }
}
