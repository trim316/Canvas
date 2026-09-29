package net.canvasmod.client;

import net.canvasmod.HomecomingPolicy;
import net.canvasmod.VillageLifePolicy;

final class CanvasExperienceDirector {
    private VillageLifePolicy.Rhythm villageRhythm = VillageLifePolicy.Rhythm.NONE;
    private boolean familiarNearby;

    void setVillageRhythm(VillageLifePolicy.Rhythm value) {
        villageRhythm = value == null ? VillageLifePolicy.Rhythm.NONE : value;
    }

    void setFamiliarNearby(boolean value) {
        familiarNearby = value;
    }

    HomecomingPolicy.Flavor homecomingFlavor() {
        return HomecomingPolicy.classify(familiarNearby, villageRhythm);
    }
}
