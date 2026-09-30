package net.canvasmod.client;

import net.canvasmod.CanvasWorldMemoryStore;
import net.canvasmod.HomecomingPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.WeatherCharacterPolicy;
import net.canvasmod.WorldMemoryScopePolicy;
import net.fabricmc.loader.api.FabricLoader;

final class CanvasExperienceDirector {
    private final MomentDensityPolicy.Budget momentBudget = new MomentDensityPolicy.Budget();
    private final CanvasWorldMemoryStore worldMemory = new CanvasWorldMemoryStore(
            FabricLoader.getInstance().getConfigDir().resolve("canvas-world-memory-v1.properties"));
    private final net.canvasmod.CanvasSeasonMemoryStore seasonMemory = new net.canvasmod.CanvasSeasonMemoryStore(
            FabricLoader.getInstance().getConfigDir().resolve("canvas-season-memory-v1.properties"));
    private boolean memoryBound;
    private String worldIdentity = "";
    private String pendingHomeDimension = "";
    private int pendingHomeX;
    private int pendingHomeY;
    private int pendingHomeZ;
    private VillageLifePolicy.Rhythm villageRhythm = VillageLifePolicy.Rhythm.NONE;
    private WeatherCharacterPolicy.Character weatherCharacter = WeatherCharacterPolicy.Character.CLEAR;
    private boolean familiarNearby;
    private net.canvasmod.SeasonPolicy.Season season = net.canvasmod.SeasonPolicy.Season.UNKNOWN;
    private boolean ciGuardrailAnnounced;

    void setWorldIdentity(String worldId) {
        String next = worldId == null ? "" : worldId.trim();
        if (next.equals(worldIdentity)) return;
        worldIdentity = next;
        memoryBound = false;
        pendingHomeDimension = "";
        pendingHomeX = 0;
        pendingHomeY = 0;
        pendingHomeZ = 0;
    }

    void bindHome(String dimension, int x, int y, int z) {
        pendingHomeDimension = dimension == null ? "" : dimension;
        pendingHomeX = x;
        pendingHomeY = y;
        pendingHomeZ = z;
        if (!worldIdentity.isBlank()) bindPendingHome();
    }

    private void bindPendingHome() {
        String local = pendingHomeDimension + "|" + pendingHomeX + "|" + pendingHomeY + "|" + pendingHomeZ;
        String scope = WorldMemoryScopePolicy.scope(worldIdentity, local);
        if (scope.isBlank()) return;
        worldMemory.bind(scope);
        seasonMemory.bind(scope);
        memoryBound = true;
    }

    void setVillageRhythm(VillageLifePolicy.Rhythm value) {
        villageRhythm = value == null ? VillageLifePolicy.Rhythm.NONE : value;
    }

    void setFamiliarNearby(boolean value) { familiarNearby = value; }

    void setSeason(net.canvasmod.SeasonPolicy.Season value) {
        season = value == null ? net.canvasmod.SeasonPolicy.Season.UNKNOWN : value;
        if (memoryBound && season != net.canvasmod.SeasonPolicy.Season.UNKNOWN) {
            seasonMemory.observeSeason(season);
        }
    }

    net.canvasmod.SeasonPolicy.Season season() { return season; }

    void noteSeasonalMoment(net.canvasmod.SeasonalRareMomentPolicy.Moment moment, long worldDay) {
        if (memoryBound) seasonMemory.noteSeasonalMoment(moment, worldDay);
    }

    long lastSeasonalMomentDay() {
        return memoryBound ? seasonMemory.snapshot().lastSeasonalMomentDay() : Long.MIN_VALUE / 4L;
    }

    boolean firstSnowSeen() {
        return memoryBound && seasonMemory.snapshot().firstSnowSeen();
    }

    int seasonTransitions() {
        return memoryBound ? seasonMemory.snapshot().seasonTransitions() : 0;
    }

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
