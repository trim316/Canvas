package net.canvasmod;

public final class ExplorationMusicPolicy {
    public static final long GENERAL_COOLDOWN_TICKS = 20L * 60L * 5L;
    public static final long VIEWPOINT_COOLDOWN_TICKS = 20L * 60L * 8L;

    private ExplorationMusicPolicy() { }

    public static String eventFor(PlaceFamiliarityPolicy.Kind kind, boolean familiar) {
        if (!familiar || kind == null || kind == PlaceFamiliarityPolicy.Kind.NONE) return "";
        return "music.exploration_place";
    }

    public static float volumeFor(PlaceFamiliarityPolicy.Kind kind) {
        if (kind == null) return 0.0f;
        return switch (kind) {
            case PATH -> 0.11f;
            case DOCK -> 0.15f;
            case FARM -> 0.13f;
            case VIEWPOINT -> 0.17f;
            case GATHERING_SPOT -> 0.14f;
            case NONE -> 0.0f;
        };
    }

    public static float pitchFor(PlaceFamiliarityPolicy.Kind kind) {
        if (kind == null) return 1.0f;
        return switch (kind) {
            case PATH -> 1.02f;
            case DOCK -> 0.97f;
            case FARM -> 1.04f;
            case VIEWPOINT -> 1.07f;
            case GATHERING_SPOT -> 1.0f;
            case NONE -> 1.0f;
        };
    }

    public static long cooldownFor(PlaceFamiliarityPolicy.Kind kind) {
        return kind == PlaceFamiliarityPolicy.Kind.VIEWPOINT
                ? VIEWPOINT_COOLDOWN_TICKS
                : GENERAL_COOLDOWN_TICKS;
    }

    public static boolean shouldPresent(
            PlaceFamiliarityPolicy.Kind previous,
            PlaceFamiliarityPolicy.Kind current,
            boolean familiar,
            boolean atHome,
            long ticksSinceLastMoment) {
        if (atHome || !familiar || current == null || current == PlaceFamiliarityPolicy.Kind.NONE) return false;
        if (previous == current) return false;
        return ticksSinceLastMoment >= cooldownFor(current);
    }
}
