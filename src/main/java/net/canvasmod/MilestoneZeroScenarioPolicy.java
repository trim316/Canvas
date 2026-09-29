package net.canvasmod;

import java.util.EnumSet;

public final class MilestoneZeroScenarioPolicy {
    public enum Moment {
        COMING_HOME,
        RAIN_ON_ROOF,
        VILLAGE_WAKES_UP,
        VILLAGE_WINDS_DOWN,
        FAMILIAR_FACE,
        GATHERING,
        DID_YOU_SEE_THAT,
        NOTHING_HAPPENS
    }

    public record Frame(
            boolean homecoming,
            boolean atHome,
            boolean sheltered,
            boolean raining,
            VillageLifePolicy.Rhythm villageRhythm,
            boolean familiarTargeted,
            RareSurprisePolicy.Moment rareMoment,
            boolean densitySuppressed) { }

    private MilestoneZeroScenarioPolicy() { }

    public static EnumSet<Moment> observe(Frame frame) {
        EnumSet<Moment> moments = EnumSet.noneOf(Moment.class);
        if (frame.homecoming() && frame.atHome()) moments.add(Moment.COMING_HOME);
        if (frame.atHome() && frame.sheltered() && frame.raining()) moments.add(Moment.RAIN_ON_ROOF);
        if (frame.villageRhythm() == VillageLifePolicy.Rhythm.WAKE) moments.add(Moment.VILLAGE_WAKES_UP);
        if (frame.villageRhythm() == VillageLifePolicy.Rhythm.WIND_DOWN) moments.add(Moment.VILLAGE_WINDS_DOWN);
        if (frame.familiarTargeted()) moments.add(Moment.FAMILIAR_FACE);
        if (frame.villageRhythm() == VillageLifePolicy.Rhythm.GATHERING) moments.add(Moment.GATHERING);
        if (frame.rareMoment() != null && frame.rareMoment() != RareSurprisePolicy.Moment.NONE) {
            moments.add(Moment.DID_YOU_SEE_THAT);
        }
        if (frame.densitySuppressed()) moments.add(Moment.NOTHING_HAPPENS);
        return moments;
    }
}
