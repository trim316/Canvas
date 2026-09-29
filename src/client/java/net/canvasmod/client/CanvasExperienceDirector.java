package net.canvasmod.client;

import net.canvasmod.HomecomingPolicy;
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.WeatherCharacterPolicy;

final class CanvasExperienceDirector {
    private VillageLifePolicy.Rhythm villageRhythm = VillageLifePolicy.Rhythm.NONE;
    private WeatherCharacterPolicy.Character weatherCharacter = WeatherCharacterPolicy.Character.CLEAR;
    private boolean familiarNearby;
    private int meaningfulReturns;

    void setVillageRhythm(VillageLifePolicy.Rhythm value) {
        villageRhythm = value == null ? VillageLifePolicy.Rhythm.NONE : value;
    }

    void setFamiliarNearby(boolean value) {
        familiarNearby = value;
    }

    void setWeatherCharacter(WeatherCharacterPolicy.Character value) {
        weatherCharacter = value == null ? WeatherCharacterPolicy.Character.CLEAR : value;
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

    int meaningfulReturns() {
        return meaningfulReturns;
    }
}
