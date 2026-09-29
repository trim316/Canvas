package net.canvasmod.client;

import net.canvasmod.CanvasWorldMemoryStore;
import net.canvasmod.HomecomingPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.WeatherCharacterPolicy;
import net.fabricmc.loader.api.FabricLoader;

final class CanvasExperienceDirector {
    private final MomentDensityPolicy.Budget momentBudget = new MomentDensityPolicy.Budget();
    private final CanvasWorldMemoryStore worldMemory = new CanvasWorldMemoryStore(
            FabricLoader.getInstance().getConfigDir().resolve("canvas-world-memory-v1.properties"));
    private boolean memoryBound;
    private VillageLifePolicy.Rhythm villageRhythm = VillageLifePolicy.Rhythm.NONE;
    private WeatherCharacterPolicy.Character weatherCharacter = WeatherCharacterPolicy.Character.CLEAR;
    private boolean familiarNearby;
    private boolean ciGuardrailAnnounced;

    void bindHome(String dimension, int x, int y, int z) {
        worldMemory.bind(dimension + "|" + x + "|" + y + "|" + z);
        memoryBound = true;
    }

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
        return HomecomingPolicy.compose(
                familiarNearby,
                villageRhythm,
                weatherCharacter,
                meaningfulReturns());
    }

    HomecomingPolicy.Plan nextHomecoming() {
        HomecomingPolicy.Plan plan =
                HomecomingPolicy.compose(familiarNearby, villageRhythm, weatherCharacter, meaningfulReturns());
        if (memoryBound) worldMemory.noteHomecoming();
        return plan;
    }

    void noteFamiliarMoment() {
        if (memoryBound) worldMemory.noteFamiliarMoment();
    }

    void noteVillageMoment() {
        if (memoryBound) worldMemory.noteVillageMoment();
    }

    void noteRareMoment(long worldDay) {
        if (memoryBound) worldMemory.noteRareMoment(worldDay);
    }

    int meaningfulReturns() {
        return memoryBound ? worldMemory.snapshot().homecomings() : 0;
    }

    long lastSurpriseDay() {
        return memoryBound ? worldMemory.snapshot().lastSurpriseDay() : Long.MIN_VALUE / 4;
    }

    long suppressedMoments() { return momentBudget.suppressedCount(); }
}
