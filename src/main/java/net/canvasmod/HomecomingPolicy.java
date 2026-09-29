package net.canvasmod;

public final class HomecomingPolicy {
    public enum Flavor { QUIET, FAMILIAR, VILLAGE, LIVED_IN }

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
