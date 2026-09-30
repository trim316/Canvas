package net.canvasmod;

public final class ExplorationContextPolicy {
    public enum Moment {
        NONE,
        RAIN_DOCK,
        STORM_OVERLOOK,
        FIELD_AFTER_RAIN
    }

    private ExplorationContextPolicy() { }

    public static boolean musicEligible(
            boolean atHome,
            PlaceFamiliarityPolicy.Kind place,
            long ticksSinceLastMusic) {
        return !atHome
                && place != null
                && place != PlaceFamiliarityPolicy.Kind.NONE
                && ticksSinceLastMusic >= 20L * 60L * 8L;
    }

    public static Moment classify(
            boolean atHome,
            PlaceFamiliarityPolicy.Kind place,
            boolean raining,
            boolean thundering,
            int ticksSinceRainEnded) {
        if (atHome || place == null || place == PlaceFamiliarityPolicy.Kind.NONE) return Moment.NONE;
        if (place == PlaceFamiliarityPolicy.Kind.DOCK && raining && !thundering) return Moment.RAIN_DOCK;
        if (place == PlaceFamiliarityPolicy.Kind.VIEWPOINT && thundering) return Moment.STORM_OVERLOOK;
        if (place == PlaceFamiliarityPolicy.Kind.FARM
                && !raining
                && ticksSinceRainEnded >= 0
                && ticksSinceRainEnded <= 20 * 20) {
            return Moment.FIELD_AFTER_RAIN;
        }
        return Moment.NONE;
    }

    public static String cueEvent(Moment moment) {
        return switch (moment) {
            case RAIN_DOCK -> "exploration.weather_rain_dock";
            case STORM_OVERLOOK -> "exploration.weather_storm_overlook";
            case FIELD_AFTER_RAIN -> "exploration.weather_field_after_rain";
            case NONE -> "";
        };
    }

    public static float cuePitch(Moment moment) {
        return switch (moment) {
            case RAIN_DOCK -> 0.985f;
            case STORM_OVERLOOK -> 0.94f;
            case FIELD_AFTER_RAIN -> 1.035f;
            case NONE -> 1.0f;
        };
    }
}
