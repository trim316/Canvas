package net.canvasmod;

public final class ExplorationWeatherPolicy {
    public enum Moment {
        NONE,
        RAIN_ON_DOCK,
        STORM_OVERLOOK,
        FIELD_AFTER_RAIN
    }

    public static final long COOLDOWN_TICKS = 20L * 60L * 4L;

    private ExplorationWeatherPolicy() { }

    public static Moment classify(
            PlaceFamiliarityPolicy.Kind place,
            boolean familiar,
            boolean atHome,
            boolean raining,
            boolean thundering,
            boolean rainJustEnded) {
        if (!familiar || atHome || place == null || place == PlaceFamiliarityPolicy.Kind.NONE) {
            return Moment.NONE;
        }
        if (place == PlaceFamiliarityPolicy.Kind.VIEWPOINT && thundering) {
            return Moment.STORM_OVERLOOK;
        }
        if (place == PlaceFamiliarityPolicy.Kind.DOCK && raining && !thundering) {
            return Moment.RAIN_ON_DOCK;
        }
        if (place == PlaceFamiliarityPolicy.Kind.FARM && rainJustEnded) {
            return Moment.FIELD_AFTER_RAIN;
        }
        return Moment.NONE;
    }

    public static String cueEvent(Moment moment) {
        return switch (moment) {
            case RAIN_ON_DOCK -> "exploration.weather_rain_dock";
            case STORM_OVERLOOK -> "exploration.weather_storm_overlook";
            case FIELD_AFTER_RAIN -> "exploration.weather_field_after_rain";
            case NONE -> "";
        };
    }

    public static float volume(Moment moment) {
        return switch (moment) {
            case RAIN_ON_DOCK -> 0.22f;
            case STORM_OVERLOOK -> 0.26f;
            case FIELD_AFTER_RAIN -> 0.20f;
            case NONE -> 0.0f;
        };
    }

    public static float pitch(Moment moment) {
        return switch (moment) {
            case RAIN_ON_DOCK -> 0.98f;
            case STORM_OVERLOOK -> 0.93f;
            case FIELD_AFTER_RAIN -> 1.05f;
            case NONE -> 1.0f;
        };
    }

    public static int washArgb(Moment moment) {
        return switch (moment) {
            case RAIN_ON_DOCK -> 0x102C617A;
            case STORM_OVERLOOK -> 0x14283B56;
            case FIELD_AFTER_RAIN -> 0x0E88B86A;
            case NONE -> 0;
        };
    }
}
