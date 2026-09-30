package net.canvasmod;

public final class ObservationBudgetPolicy {
    public static final int HOME_RADIUS = 8;
    public static final int HOME_VERTICAL_SAMPLES = 8;
    public static final int HOME_SAMPLE_INTERVAL_TICKS = 100;
    public static final int HOME_MAX_BLOCK_PROBES =
            (HOME_RADIUS * 2 + 1) * (HOME_RADIUS * 2 + 1) * HOME_VERTICAL_SAMPLES;
    public static final int PLACE_RADIUS = 4;
    public static final int PLACE_VERTICAL_SAMPLES = 5;
    public static final int PLACE_SAMPLE_INTERVAL_TICKS = 100;
    public static final int PLACE_MAX_BLOCK_PROBES =
            (PLACE_RADIUS * 2 + 1) * (PLACE_RADIUS * 2 + 1) * PLACE_VERTICAL_SAMPLES;
    public static final int PLACE_EDGE_PROBES = 4 * 3;

    public static final double MAX_EQUIVALENT_BLOCK_PROBES_PER_SECOND = 600.0;

    private ObservationBudgetPolicy() { }

    public static double equivalentBlockProbesPerSecond() {
        double home = HOME_MAX_BLOCK_PROBES * (20.0 / HOME_SAMPLE_INTERVAL_TICKS);
        double places = (PLACE_MAX_BLOCK_PROBES + PLACE_EDGE_PROBES)
                * (20.0 / PLACE_SAMPLE_INTERVAL_TICKS);
        return home + places;
    }

    public static boolean withinBudget() {
        return equivalentBlockProbesPerSecond() <= MAX_EQUIVALENT_BLOCK_PROBES_PER_SECOND;
    }
}
