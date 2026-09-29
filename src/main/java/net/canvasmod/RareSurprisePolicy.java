package net.canvasmod;

public final class RareSurprisePolicy {
    public enum Moment { NONE, STORM_BREAK, GOLDEN_HUSH, STARLIT_STILLNESS }
    public static final long MIN_DAYS_BETWEEN = 3L;

    private RareSurprisePolicy() { }

    public static Moment classify(
            boolean atHome, boolean wasRaining, boolean raining, boolean thundering,
            long dayTime, long worldDay, int homeX, int homeZ, long daysSinceLast) {
        if (!atHome || thundering || daysSinceLast < MIN_DAYS_BETWEEN) return Moment.NONE;

        long time = Math.floorMod(dayTime, 24000L);
        if (wasRaining && !raining && time >= 8500L && time < 15000L
                && rareDay(worldDay, homeX, homeZ, 3)) {
            return Moment.STORM_BREAK;
        }
        if (!raining && time >= 11500L && time < 13500L
                && rareDay(worldDay, homeX, homeZ, 5)) {
            return Moment.GOLDEN_HUSH;
        }
        if (!raining && time >= 15000L && time < 21000L
                && rareDay(worldDay, homeX, homeZ, 11)) {
            return Moment.STARLIT_STILLNESS;
        }
        return Moment.NONE;
    }

    public static boolean rareDay(long worldDay, int homeX, int homeZ, int salt) {
        long mixed = worldDay * 31L + homeX * 17L + homeZ * 13L + salt * 19L;
        return Math.floorMod(mixed, 7L) == 0L;
    }

    public static String cueEvent(Moment moment) {
        return switch (moment) {
            case STORM_BREAK -> "feel.rare_storm_break";
            case GOLDEN_HUSH -> "feel.rare_golden_hush";
            case STARLIT_STILLNESS -> "feel.rare_starlit_stillness";
            case NONE -> "";
        };
    }

    public static int washArgb(Moment moment) {
        return switch (moment) {
            case STORM_BREAK -> 0x20B8D8FF;
            case GOLDEN_HUSH -> 0x24FFD47A;
            case STARLIT_STILLNESS -> 0x181B2B55;
            case NONE -> 0;
        };
    }
}
