package net.canvasmod;

public final class LowEndPerformanceBudgetPolicy {
    public static final int FAMILIARITY_MAX_MOBS_PER_SAMPLE = 128;
    public static final int VILLAGE_SAMPLE_INTERVAL_TICKS = 40;
    public static final int VILLAGE_MAX_VILLAGERS_PER_SAMPLE = 64;
    public static final int VILLAGE_MAX_CLUSTER_COMPARISONS =
            VILLAGE_MAX_VILLAGERS_PER_SAMPLE * VILLAGE_MAX_VILLAGERS_PER_SAMPLE;
    public static final double MAX_BROAD_ENTITY_QUERIES_PER_SECOND = 1.5;

    private LowEndPerformanceBudgetPolicy() { }

    public static double broadEntityQueriesPerSecond() {
        double familiarity = 20.0 / FamiliarityPolicy.SAMPLE_INTERVAL_TICKS;
        double village = 20.0 / VILLAGE_SAMPLE_INTERVAL_TICKS;
        return familiarity + village;
    }

    public static boolean withinBudget() {
        return ObservationBudgetPolicy.withinBudget()
                && broadEntityQueriesPerSecond() <= MAX_BROAD_ENTITY_QUERIES_PER_SECOND
                && FAMILIARITY_MAX_MOBS_PER_SAMPLE <= 128
                && VILLAGE_MAX_CLUSTER_COMPARISONS <= 4096;
    }
}
