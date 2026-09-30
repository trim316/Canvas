package net.canvasmod;

import java.util.EnumSet;

public final class SeasonsOfHomeScenarioPolicy {
    public enum Experience {
        SPRING_HOME,
        SUMMER_HOME,
        AUTUMN_HOME,
        WINTER_HOME,
        SEASONAL_VILLAGE,
        SEASONAL_FAMILIAR,
        FIRST_SNOW,
        RARE_SEASONAL
    }

    public record Frame(
            SeasonPolicy.Season season,
            boolean atHome,
            VillageLifePolicy.Rhythm villageRhythm,
            boolean familiarNearby,
            SeasonalRareMomentPolicy.Moment rareMoment) { }

    private SeasonsOfHomeScenarioPolicy() { }

    public static EnumSet<Experience> observe(Frame frame) {
        EnumSet<Experience> seen = EnumSet.noneOf(Experience.class);
        if (!frame.atHome() || frame.season() == null || frame.season() == SeasonPolicy.Season.UNKNOWN) {
            return seen;
        }

        switch (frame.season()) {
            case SPRING -> seen.add(Experience.SPRING_HOME);
            case SUMMER -> seen.add(Experience.SUMMER_HOME);
            case AUTUMN -> seen.add(Experience.AUTUMN_HOME);
            case WINTER -> seen.add(Experience.WINTER_HOME);
            case UNKNOWN -> { }
        }

        if (VillageLifePolicy.isMoment(frame.villageRhythm())) seen.add(Experience.SEASONAL_VILLAGE);
        if (frame.familiarNearby()) seen.add(Experience.SEASONAL_FAMILIAR);
        if (frame.rareMoment() == SeasonalRareMomentPolicy.Moment.FIRST_SNOW) {
            seen.add(Experience.FIRST_SNOW);
        }
        if (frame.rareMoment() != null && frame.rareMoment() != SeasonalRareMomentPolicy.Moment.NONE) {
            seen.add(Experience.RARE_SEASONAL);
        }
        return seen;
    }
}
