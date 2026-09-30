package net.canvasmod;

public final class SeasonalFamiliarityProfile {
    private SeasonalFamiliarityProfile() { }

    public static float cueVolume(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 0.31f;
            case SUMMER -> 0.30f;
            case AUTUMN -> 0.29f;
            case WINTER -> 0.27f;
            case UNKNOWN -> 0.30f;
        };
    }

    public static float cuePitch(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 1.035f;
            case SUMMER -> 1.01f;
            case AUTUMN -> 0.98f;
            case WINTER -> 0.945f;
            case UNKNOWN -> 1.0f;
        };
    }

    public static int pulseArgb(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 0x669FE6A8;
            case SUMMER -> 0x66FFD782;
            case AUTUMN -> 0x66D88952;
            case WINTER -> 0x66B8E2F4;
            case UNKNOWN -> 0x66FFD28A;
        };
    }
}
