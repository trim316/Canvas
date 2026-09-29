package net.canvasmod;

public final class HomecomingPolicy {
    public enum Flavor { QUIET, FAMILIAR, VILLAGE, LIVED_IN }

    public record Plan(
            Flavor flavor,
            String cueEvent,
            String musicEvent,
            int pulseTicks,
            float cueVolume,
            float cuePitch) { }

    private HomecomingPolicy() { }

    public static Flavor classify(boolean familiarNearby, VillageLifePolicy.Rhythm villageRhythm) {
        boolean villagePresent = villageRhythm != null
                && villageRhythm != VillageLifePolicy.Rhythm.NONE
                && villageRhythm != VillageLifePolicy.Rhythm.QUIET_NIGHT;
        if (familiarNearby && villagePresent) return Flavor.LIVED_IN;
        if (familiarNearby) return Flavor.FAMILIAR;
        if (villagePresent) return Flavor.VILLAGE;
        return Flavor.QUIET;
    }

    public static Plan compose(
            boolean familiarNearby,
            VillageLifePolicy.Rhythm villageRhythm,
            WeatherCharacterPolicy.Character weather,
            int previousReturns) {
        Flavor flavor = classify(familiarNearby, villageRhythm);
        WeatherCharacterPolicy.Character safeWeather =
                weather == null ? WeatherCharacterPolicy.Character.CLEAR : weather;
        int historyTier = Math.min(3, Math.max(0, previousReturns));

        String music = "";
        if (safeWeather == WeatherCharacterPolicy.Character.THUNDER_SHELTERED) {
            music = "music.coming_home_storm";
        } else if (historyTier >= 2 && flavor != Flavor.QUIET) {
            music = "music.coming_home";
        }

        float pitch = switch (safeWeather) {
            case THUNDER_SHELTERED -> 0.92f;
            case RAIN_SHELTERED -> 0.97f;
            case CALM_AFTER_STORM -> 1.04f;
            default -> 1.0f;
        };

        int pulse = pulseTicks(flavor) + historyTier * 10;
        float volume = Math.min(0.58f, volume(flavor) + historyTier * 0.01f);
        return new Plan(flavor, cueEvent(flavor), music, pulse, volume, pitch);
    }

    public static String cueEvent(Flavor flavor) {
        return switch (flavor) {
            case FAMILIAR -> "feel.coming_home_familiar";
            case VILLAGE -> "feel.coming_home_village";
            case LIVED_IN -> "feel.coming_home_lived_in";
            case QUIET -> "feel.coming_home";
        };
    }

    public static int pulseTicks(Flavor flavor) {
        return switch (flavor) {
            case QUIET -> 110;
            case FAMILIAR, VILLAGE -> 130;
            case LIVED_IN -> 160;
        };
    }

    public static float volume(Flavor flavor) {
        return switch (flavor) {
            case QUIET -> 0.46f;
            case FAMILIAR -> 0.48f;
            case VILLAGE -> 0.50f;
            case LIVED_IN -> 0.54f;
        };
    }
}
