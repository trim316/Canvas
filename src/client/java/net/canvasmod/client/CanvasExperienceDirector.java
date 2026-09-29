package net.canvasmod.client;

import net.canvasmod.HomecomingPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.WeatherCharacterPolicy;

final class CanvasExperienceDirector {
    private final MomentDensityPolicy.Budget momentBudget = new MomentDensityPolicy.Budget();
    private VillageLifePolicy.Rhythm villageRhythm = VillageLifePolicy.Rhythm.NONE;
    private WeatherCharacterPolicy.Character weatherCharacter = WeatherCharacterPolicy.Character.CLEAR;
    private boolean familiarNearby;
    private int meaningfulReturns;
    private boolean ciGuardrailAnnounced;

    void setVillageRhythm(VillageLifePolicy.Rhythm value) {
        villageRhythm = value == null ? VillageLifePolicy.Rhythm.NONE : value;
    }

    void setFamiliarNearby(boolean value) { familiarNearby = value; }

    void setWeatherCharacter(WeatherCharacterPolicy.Character value) {
        weatherCharacter = value == null ? WeatherCharacterPolicy.Character.CLEAR : value;
    }

    boolean allowMoment(MomentDensityPolicy.Kind kind, long tick, boolean music) {
        boolean allowed = momentBudget.tryAcquire(kind, tick, music);
        if (!allowed && !ciGuardrailAnnounced
                && Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"))) {
            System.out.println("CANVAS_CI_NOTHING_HAPPENS_GUARDRAIL_ACTIVE");
            ciGuardrailAnnounced = true;
        }
        return allowed;
    }

    HomecomingPolicy.Plan previewHomecoming() {
        return HomecomingPolicy.compose(familiarNearby, villageRhythm, weatherCharacter, meaningfulReturns);
    }

    HomecomingPolicy.Plan nextHomecoming() {
        HomecomingPolicy.Plan plan =
                HomecomingPolicy.compose(familiarNearby, villageRhythm, weatherCharacter, meaningfulReturns);
        meaningfulReturns = Math.min(1000000, meaningfulReturns + 1);
        return plan;
    }

    int meaningfulReturns() { return meaningfulReturns; }
    long suppressedMoments() { return momentBudget.suppressedCount(); }
}
