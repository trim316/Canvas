package net.canvasmod;

public final class VillageLifePolicy {
    public enum Rhythm {
        NONE,
        WAKE,
        ACTIVE,
        WIND_DOWN,
        QUIET_NIGHT,
        GATHERING
    }

    private VillageLifePolicy() { }

    public static Rhythm classify(
            int nearbyVillagers,
            int clusteredVillagers,
            long dayTime,
            boolean storming) {
        if (nearbyVillagers < 2) return Rhythm.NONE;

        long time = Math.floorMod(dayTime, 24000L);
        if (!storming
                && nearbyVillagers >= 4
                && clusteredVillagers >= 3
                && time >= 5000L
                && time < 11000L) {
            return Rhythm.GATHERING;
        }
        if (time >= 23000L || time < 2500L) return Rhythm.WAKE;
        if (time >= 10500L && time < 13500L) return Rhythm.WIND_DOWN;
        if (time >= 13500L && time < 23000L) return Rhythm.QUIET_NIGHT;
        return Rhythm.ACTIVE;
    }

    public static boolean isMoment(Rhythm rhythm) {
        return rhythm == Rhythm.WAKE
                || rhythm == Rhythm.WIND_DOWN
                || rhythm == Rhythm.GATHERING;
    }
}
