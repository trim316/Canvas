package net.canvasmod;

public final class ObservationBudgetPolicy {
    public static final int HOME_RADIUS = 8;
    public static final int HOME_VERTICAL_SAMPLES = 8;
    public static final int HOME_SAMPLE_INTERVAL_TICKS = 100;
    public static final int HOME_MAX_BLOCK_PROBES =
            (HOME_RADIUS * 2 + 1) * (HOME_RADIUS * 2 + 1) * HOME_VERTICAL_SAMPLES;
    public static final double MAX_EQUIVALENT_BLOCK_PROBES_PER_SECOND = 600.0;

    private ObservationBudgetPolicy() { }

    public static double equivalentBlockProbesPerSecond() {
        return HOME_MAX_BLOCK_PROBES * (20.0 / HOME_SAMPLE_INTERVAL_TICKS);
    }

    public static boolean withinBudget() {
        return equivalentBlockProbesPerSecond() <= MAX_EQUIVALENT_BLOCK_PROBES_PER_SECOND;
    }
}
