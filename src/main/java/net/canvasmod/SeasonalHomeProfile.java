package net.canvasmod;

public final class SeasonalHomeProfile {
    private SeasonalHomeProfile() { }

    public static String ambienceEvent(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> "presence.season_spring";
            case SUMMER -> "presence.season_summer";
            case AUTUMN -> "presence.season_autumn";
            case WINTER -> "presence.season_winter";
            case UNKNOWN -> "";
        };
    }

    public static float ambienceVolume(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 0.055f;
            case SUMMER -> 0.050f;
            case AUTUMN -> 0.060f;
            case WINTER -> 0.052f;
            case UNKNOWN -> 0.0f;
        };
    }

    public static float ambiencePitch(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 1.035f;
            case SUMMER -> 1.01f;
            case AUTUMN -> 0.975f;
            case WINTER -> 0.94f;
            case UNKNOWN -> 1.0f;
        };
    }

    public static int washArgb(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 0x050FCB74;
            case SUMMER -> 0x04FFD36A;
            case AUTUMN -> 0x06D77A2A;
            case WINTER -> 0x061F6FA8;
            case UNKNOWN -> 0;
        };
    }

    public static float homecomingMusicPitchMultiplier(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 1.025f;
            case SUMMER -> 1.01f;
            case AUTUMN -> 0.985f;
            case WINTER -> 0.96f;
            case UNKNOWN -> 1.0f;
        };
    }

    public static String transitionMusicEvent(SeasonPolicy.Season season) {
        return season == SeasonPolicy.Season.UNKNOWN ? "" : "music.season_home_shift";
    }
}
