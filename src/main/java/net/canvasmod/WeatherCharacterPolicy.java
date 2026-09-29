package net.canvasmod;

public final class WeatherCharacterPolicy {
    public enum Character {
        CLEAR,
        RAIN_EXPOSED,
        RAIN_SHELTERED,
        THUNDER_SHELTERED,
        CALM_AFTER_STORM
    }

    public static final int AFTER_STORM_TICKS = 20 * 18;

    private WeatherCharacterPolicy() { }

    public static Character classify(
            boolean atHome,
            boolean sheltered,
            boolean raining,
            boolean thundering,
            int ticksSinceRainEnded) {
        if (raining) {
            if (!sheltered) return Character.RAIN_EXPOSED;
            return thundering ? Character.THUNDER_SHELTERED : Character.RAIN_SHELTERED;
        }
        if (atHome && sheltered
                && ticksSinceRainEnded >= 0
                && ticksSinceRainEnded <= AFTER_STORM_TICKS) {
            return Character.CALM_AFTER_STORM;
        }
        return Character.CLEAR;
    }

    public static String ambienceEvent(Character character) {
        return switch (character) {
            case RAIN_SHELTERED -> "presence.rain_on_roof";
            case THUNDER_SHELTERED -> "presence.thunder_shelter";
            default -> "";
        };
    }

    public static float ambienceVolume(Character character) {
        return switch (character) {
            case RAIN_SHELTERED -> 0.115f;
            case THUNDER_SHELTERED -> 0.14f;
            default -> 0.0f;
        };
    }

    public static float ambiencePitch(Character character) {
        return switch (character) {
            case RAIN_SHELTERED -> 0.99f;
            case THUNDER_SHELTERED -> 0.94f;
            default -> 1.0f;
        };
    }

    public static int washArgb(Character character) {
        return switch (character) {
            case RAIN_SHELTERED -> 0x0C38526A;
            case THUNDER_SHELTERED -> 0x16404A66;
            case CALM_AFTER_STORM -> 0x12A8D6D9;
            default -> 0;
        };
    }

    public static int edgeArgb(Character character) {
        return switch (character) {
            case RAIN_SHELTERED -> 0x164D6C82;
            case THUNDER_SHELTERED -> 0x225D6582;
            case CALM_AFTER_STORM -> 0x1CC6E7D9;
            default -> 0;
        };
    }

    public static boolean usesCompanionAcousticsOnly(Character character) {
        return character == Character.RAIN_EXPOSED;
    }
}
