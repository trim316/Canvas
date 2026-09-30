package net.canvasmod;

public final class MomentDensityPolicy {
    public enum Kind { HOMECOMING, FAMILIAR_FACE, VILLAGE_RHYTHM, RARE_SURPRISE, PHASE_SHIFT, WEATHER_TRANSITION, SEASON_SHIFT, SEASONAL_RARE, EXPLORATION_MUSIC, EXPLORATION_WEATHER }

    public static final long GLOBAL_MIN_GAP_TICKS = 20L * 10L;
    public static final long WINDOW_TICKS = 20L * 60L * 5L;
    public static final int MAX_PRESENTATIONS_PER_WINDOW = 5;
    public static final int MAX_MUSIC_MOMENTS_PER_WINDOW = 2;

    public static final long MIN_MAJOR_GAP_TICKS = 20L * 45L;
    public static final int MAX_MAJOR_MOMENTS_PER_DAY = 4;

    private MomentDensityPolicy() { }

    public static boolean allowMajor(long currentTick, long lastMajorTick, int majorMomentsToday) {
        if (majorMomentsToday >= MAX_MAJOR_MOMENTS_PER_DAY) return false;
        if (lastMajorTick <= Long.MIN_VALUE / 8L) return true;
        return currentTick - lastMajorTick >= MIN_MAJOR_GAP_TICKS;
    }

    public static final class Budget {
        private long lastPresentationTick = Long.MIN_VALUE / 4L;
        private long windowStartTick = Long.MIN_VALUE / 4L;
        private int presentations;
        private int musicMoments;
        private long suppressed;

        public boolean tryAcquire(Kind kind, long tick, boolean music) {
            if (windowStartTick <= Long.MIN_VALUE / 8L || tick - windowStartTick >= WINDOW_TICKS) {
                windowStartTick = tick;
                presentations = 0;
                musicMoments = 0;
            }
            if (lastPresentationTick > Long.MIN_VALUE / 8L
                    && tick - lastPresentationTick < GLOBAL_MIN_GAP_TICKS) {
                suppressed++;
                return false;
            }
            if (presentations >= MAX_PRESENTATIONS_PER_WINDOW) {
                suppressed++;
                return false;
            }
            if (music && musicMoments >= MAX_MUSIC_MOMENTS_PER_WINDOW) {
                suppressed++;
                return false;
            }
            lastPresentationTick = tick;
            presentations++;
            if (music) musicMoments++;
            return true;
        }

        public int presentationsInWindow() { return presentations; }
        public int musicMomentsInWindow() { return musicMoments; }
        public long suppressedCount() { return suppressed; }
    }
}
