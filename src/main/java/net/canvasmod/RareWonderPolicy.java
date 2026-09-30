package net.canvasmod;

public final class RareWonderPolicy {
    public enum Moment {
        NONE,
        HORIZON_GLOW,
        HARBOR_HUSH,
        GOLDEN_FIELD
    }

    public static final long MIN_DAYS_BETWEEN_WONDERS = 5L;
    public static final long MIN_TICKS_BETWEEN_WONDERS = 20L * 60L * 10L;

    private RareWonderPolicy() { }

    public static Moment classify(
            PlaceFamiliarityPolicy.Kind kind,
            boolean landmark,
            boolean atHome,
            boolean raining,
            boolean thundering,
            long dayTime,
            long worldDay,
            String contextKey,
            long daysSinceLastWonder) {
        if (!landmark || atHome || kind == null || kind == PlaceFamiliarityPolicy.Kind.NONE) {
            return Moment.NONE;
        }
        if (raining || thundering || contextKey == null || contextKey.isBlank()) return Moment.NONE;
        if (daysSinceLastWonder < MIN_DAYS_BETWEEN_WONDERS) return Moment.NONE;
        if (!rareDay(contextKey, worldDay)) return Moment.NONE;

        return switch (kind) {
            case VIEWPOINT -> dayTime >= 11200L && dayTime < 13800L
                    ? Moment.HORIZON_GLOW : Moment.NONE;
            case DOCK -> dayTime >= 16800L && dayTime < 22000L
                    ? Moment.HARBOR_HUSH : Moment.NONE;
            case FARM -> dayTime < 2200L || dayTime >= 23000L
                    ? Moment.GOLDEN_FIELD : Moment.NONE;
            case NONE, PATH, GATHERING_SPOT -> Moment.NONE;
        };
    }

    public static boolean rareDay(String contextKey, long worldDay) {
        long seed = contextKey.hashCode() * 31L + worldDay * 17L;
        return Math.floorMod(seed, 17L) == 6L;
    }

    public static String cueEvent(Moment moment) {
        return switch (moment) {
            case HORIZON_GLOW -> "exploration.wonder_horizon_glow";
            case HARBOR_HUSH -> "exploration.wonder_harbor_hush";
            case GOLDEN_FIELD -> "exploration.wonder_golden_field";
            case NONE -> "";
        };
    }

    public static float pitch(Moment moment) {
        return switch (moment) {
            case HORIZON_GLOW -> 1.06f;
            case HARBOR_HUSH -> 0.95f;
            case GOLDEN_FIELD -> 1.02f;
            case NONE -> 1.0f;
        };
    }

    public static int washArgb(Moment moment) {
        return switch (moment) {
            case HORIZON_GLOW -> 0x12F0B66A;
            case HARBOR_HUSH -> 0x102A5275;
            case GOLDEN_FIELD -> 0x10E8C56A;
            case NONE -> 0;
        };
    }
}
