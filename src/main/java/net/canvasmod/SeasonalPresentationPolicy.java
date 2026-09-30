package net.canvasmod;

/**
 * Season changes are meaningful only after actually observing the previous
 * season in THIS world/session. Receiving a season from a different save or
 * changing dimension is not a transition or an earned musical event.
 */
public final class SeasonalPresentationPolicy {
    private SeasonPolicy.Season previous = SeasonPolicy.Season.UNKNOWN;

    public void reset() { previous = SeasonPolicy.Season.UNKNOWN; }

    public void seed(SeasonPolicy.Season season) {
        if (season != null && season != SeasonPolicy.Season.UNKNOWN) {
            previous = season;
        }
    }

    public boolean observe(SeasonPolicy.Season season) {
        if (season == null || season == SeasonPolicy.Season.UNKNOWN) return false;
        boolean changed = previous != SeasonPolicy.Season.UNKNOWN && previous != season;
        previous = season;
        return changed;
    }
}
