package net.canvasmod;

public final class LongSessionSoakPolicy {
    public static final int DEFAULT_DAYS = 30;
    private static final long TICKS_PER_DAY = 24000L;
    private static final long ATTEMPT_INTERVAL = 100L;

    private LongSessionSoakPolicy() { }

    public record Result(
            int days,
            long attempts,
            long accepted,
            long suppressed,
            int maxPresentationsInWindow,
            int maxMusicMomentsInWindow) {
        public boolean passed() {
            return days > 0
                    && attempts > 0
                    && accepted > 0
                    && suppressed > 0
                    && maxPresentationsInWindow <= MomentDensityPolicy.MAX_PRESENTATIONS_PER_WINDOW
                    && maxMusicMomentsInWindow <= MomentDensityPolicy.MAX_MUSIC_MOMENTS_PER_WINDOW;
        }
    }

    public static Result run(int days) {
        int safeDays = Math.max(1, days);
        MomentDensityPolicy.Budget budget = new MomentDensityPolicy.Budget();
        long endTick = TICKS_PER_DAY * safeDays;
        long attempts = 0L;
        long accepted = 0L;
        int maxPresentations = 0;
        int maxMusic = 0;
        MomentDensityPolicy.Kind[] kinds = MomentDensityPolicy.Kind.values();

        for (long tick = 0L; tick <= endTick; tick += ATTEMPT_INTERVAL) {
            attempts++;
            int index = (int)((tick / ATTEMPT_INTERVAL) % kinds.length);
            MomentDensityPolicy.Kind kind = kinds[index];
            boolean music = kind == MomentDensityPolicy.Kind.HOMECOMING
                    || kind == MomentDensityPolicy.Kind.VILLAGE_RHYTHM
                    || kind == MomentDensityPolicy.Kind.SEASON_SHIFT
                    || kind == MomentDensityPolicy.Kind.EXPLORATION_MUSIC
                    || kind == MomentDensityPolicy.Kind.COMMUNITY_GATHERING;
            if (budget.tryAcquire(kind, tick, music)) accepted++;
            maxPresentations = Math.max(maxPresentations, budget.presentationsInWindow());
            maxMusic = Math.max(maxMusic, budget.musicMomentsInWindow());
        }

        return new Result(
                safeDays,
                attempts,
                accepted,
                budget.suppressedCount(),
                maxPresentations,
                maxMusic);
    }
}
