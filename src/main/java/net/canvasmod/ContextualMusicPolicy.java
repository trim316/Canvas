package net.canvasmod;

public final class ContextualMusicPolicy {
    public static final long GENERAL_COOLDOWN_TICKS = 20L * 60L * 3L;
    public static final long GATHERING_COOLDOWN_TICKS = 20L * 60L * 8L;

    private ContextualMusicPolicy() { }

    public static String eventFor(VillageLifePolicy.Rhythm rhythm) {
        return switch (rhythm) {
            case WAKE -> "music.village_wake";
            case WIND_DOWN -> "music.village_wind_down";
            case GATHERING -> "music.community_gathering";
            default -> "";
        };
    }

    public static long cooldownFor(VillageLifePolicy.Rhythm rhythm) {
        return rhythm == VillageLifePolicy.Rhythm.GATHERING
                ? GATHERING_COOLDOWN_TICKS
                : GENERAL_COOLDOWN_TICKS;
    }

    public static boolean shouldPresent(
            VillageLifePolicy.Rhythm previous,
            VillageLifePolicy.Rhythm current,
            long ticksSinceLastMoment) {
        if (previous == current || !VillageLifePolicy.isMoment(current)) return false;
        return ticksSinceLastMoment >= cooldownFor(current);
    }
}
