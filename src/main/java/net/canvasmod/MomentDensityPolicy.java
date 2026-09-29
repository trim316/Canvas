package net.canvasmod;

public final class MomentDensityPolicy {
    public static final long MIN_MAJOR_GAP_TICKS = 20L * 45L;
    public static final int MAX_MAJOR_MOMENTS_PER_DAY = 4;

    private MomentDensityPolicy() { }

    public static boolean allowMajor(
            long currentTick,
            long lastMajorTick,
            int majorMomentsToday) {
        if (majorMomentsToday >= MAX_MAJOR_MOMENTS_PER_DAY) return false;
        if (lastMajorTick <= Long.MIN_VALUE / 8L) return true;
        return currentTick - lastMajorTick >= MIN_MAJOR_GAP_TICKS;
    }
}
