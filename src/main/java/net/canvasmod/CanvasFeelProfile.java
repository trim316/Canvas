package net.canvasmod;

public final class CanvasFeelProfile {
    public enum Phase {
        AWAY,
        HOME_MORNING,
        HOME_DAY,
        HOME_EVENING,
        HOME_NIGHT,
        HOME_STORM
    }

    private CanvasFeelProfile() { }

    public static Phase classify(
            boolean atHome,
            boolean sheltered,
            boolean raining,
            boolean thundering,
            long dayTime) {
        if (!atHome) return Phase.AWAY;
        if (sheltered && (raining || thundering)) return Phase.HOME_STORM;

        long time = Math.floorMod(dayTime, 24000L);
        if (time >= 22500L || time < 1800L) return Phase.HOME_MORNING;
        if (time >= 11500L && time < 13500L) return Phase.HOME_EVENING;
        if (time >= 13500L && time < 22500L) return Phase.HOME_NIGHT;
        return Phase.HOME_DAY;
    }

    public static String ambienceEvent(Phase phase) {
        return switch (phase) {
            case HOME_MORNING -> "presence.home_morning";
            case HOME_DAY -> "presence.home_day";
            case HOME_EVENING -> "presence.home_evening";
            case HOME_NIGHT -> "presence.home_night";
            case HOME_STORM -> "presence.home_storm";
            case AWAY -> "";
        };
    }

    public static float volume(Phase phase, boolean sheltered) {
        float base = switch (phase) {
            case HOME_MORNING -> 0.16f;
            case HOME_DAY -> 0.13f;
            case HOME_EVENING -> 0.19f;
            case HOME_NIGHT -> 0.21f;
            case HOME_STORM -> 0.18f;
            case AWAY -> 0.0f;
        };
        return sheltered ? base : base * 0.58f;
    }

    public static float pitch(Phase phase) {
        return switch (phase) {
            case HOME_MORNING -> 1.025f;
            case HOME_DAY -> 1.0f;
            case HOME_EVENING -> 0.985f;
            case HOME_NIGHT -> 0.965f;
            case HOME_STORM -> 0.975f;
            case AWAY -> 1.0f;
        };
    }

    public static int washArgb(Phase phase) {
        return switch (phase) {
            case HOME_MORNING -> 0x10FFE7B2;
            case HOME_DAY -> 0x0BFFE0A5;
            case HOME_EVENING -> 0x13FFC174;
            case HOME_NIGHT -> 0x14283A63;
            case HOME_STORM -> 0x122E3D58;
            case AWAY -> 0;
        };
    }

    public static int edgeArgb(Phase phase) {
        return switch (phase) {
            case HOME_MORNING -> 0x20FFD38D;
            case HOME_DAY -> 0x16FFC77A;
            case HOME_EVENING -> 0x24FFAA58;
            case HOME_NIGHT -> 0x203C568B;
            case HOME_STORM -> 0x1E566782;
            case AWAY -> 0;
        };
    }

    public static float transitionPitch(Phase phase) {
        return switch (phase) {
            case HOME_MORNING -> 1.10f;
            case HOME_DAY -> 1.02f;
            case HOME_EVENING -> 0.98f;
            case HOME_NIGHT -> 0.90f;
            case HOME_STORM -> 0.86f;
            case AWAY -> 1.0f;
        };
    }
}
