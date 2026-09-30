package net.canvasmod;

public final class SeasonalVillageProfile {
    private SeasonalVillageProfile() { }

    public static float musicVolumeMultiplier(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 1.06f;
            case SUMMER -> 1.02f;
            case AUTUMN -> 0.96f;
            case WINTER -> 0.88f;
            case UNKNOWN -> 1.0f;
        };
    }

    public static float musicPitchMultiplier(SeasonPolicy.Season season) {
        return switch (season) {
            case SPRING -> 1.025f;
            case SUMMER -> 1.01f;
            case AUTUMN -> 0.985f;
            case WINTER -> 0.955f;
            case UNKNOWN -> 1.0f;
        };
    }

    public static int accentArgb(SeasonPolicy.Season season, VillageLifePolicy.Rhythm rhythm) {
        if (!VillageLifePolicy.isMoment(rhythm)) return 0;
        return switch (season) {
            case SPRING -> 0x3A9FE6A8;
            case SUMMER -> 0x3AFFD782;
            case AUTUMN -> 0x3AD88952;
            case WINTER -> 0x3AA9D8F2;
            case UNKNOWN -> switch (rhythm) {
                case WAKE -> 0x38FFE4A6;
                case WIND_DOWN -> 0x38FFB26B;
                case GATHERING -> 0x44FFD28A;
                default -> 0;
            };
        };
    }
}
