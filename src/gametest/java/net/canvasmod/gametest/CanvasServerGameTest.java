package net.canvasmod.gametest;

import java.lang.reflect.Method;
import net.canvasmod.CanvasFeelProfile;
import net.canvasmod.ContextualMusicPolicy;
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.WeatherCharacterPolicy;
import net.canvasmod.FamiliarityPolicy;
import net.canvasmod.HomeEvidenceDetector;
import net.canvasmod.HomecomingPolicy;
import net.canvasmod.RareSurprisePolicy;
import net.canvasmod.HomeEvidencePolicy;
import net.canvasmod.HomeRecognitionAccumulator;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

public final class CanvasServerGameTest implements CustomTestMethodInvoker {
    @GameTest
    public void canvasServerBootsAndRuns(GameTestHelper context) {
        context.assertBlockPresent(Blocks.AIR, 0, 0, 0);
        context.succeed();
    }

    @GameTest
    public void canvasDoesNotMutateUntouchedProbeBlock(GameTestHelper context) {
        context.setBlock(1, 1, 1, Blocks.OAK_PLANKS);
        context.runAtTickTime(20, () -> {
            context.assertBlockPresent(Blocks.OAK_PLANKS, 1, 1, 1);
            context.succeed();
        });
    }

    @GameTest
    public void semanticHomeRejectsTimerOnlyLocation(GameTestHelper context) {
        HomeEvidencePolicy.Evidence emptyShelter =
                new HomeEvidencePolicy.Evidence(true, 0, 0, 0, 0);
        context.assertFalse(emptyShelter.qualifies(),
                "Shelter without domestic evidence must not become HOME");
        context.succeed();
    }

    @GameTest
    public void semanticHomeAcceptsBedAndDomesticInfrastructure(GameTestHelper context) {
        HomeEvidencePolicy.Evidence home =
                new HomeEvidencePolicy.Evidence(true, 1, 1, 1, 2);
        context.assertTrue(home.qualifies(),
                "Sheltered bed + domestic infrastructure should qualify as HOME evidence");
        context.succeed();
    }

    @GameTest
    public void loadedWorldHomeDetectorRecognizesAuthoredHome(GameTestHelper context) {
        BlockPos center = new BlockPos(3, 2, 3);

        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlock(x, 1, z, Blocks.OAK_PLANKS);
                context.setBlock(x, 4, z, Blocks.OAK_PLANKS);
            }
        }
        for (int y = 2; y <= 3; y++) {
            for (int i = 0; i <= 6; i++) {
                context.setBlock(0, y, i, Blocks.OAK_PLANKS);
                context.setBlock(6, y, i, Blocks.OAK_PLANKS);
                context.setBlock(i, y, 0, Blocks.OAK_PLANKS);
                context.setBlock(i, y, 6, Blocks.OAK_PLANKS);
            }
        }

        context.setBlock(2, 2, 2, BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "red_bed")));
        context.setBlock(4, 2, 2, Blocks.CHEST);
        context.setBlock(4, 2, 4, Blocks.CRAFTING_TABLE);
        context.setBlock(2, 2, 4, Blocks.LANTERN);

        HomeEvidencePolicy.Evidence evidence = HomeEvidenceDetector.scan(
                context.getLevel(), context.absolutePos(center));

        context.assertTrue(evidence.qualifies(),
                "A loaded, sheltered, player-authored home with bed and infrastructure must qualify: " + evidence.summary());
        context.assertTrue(evidence.beds() > 0, "Detector must see the bed");
        context.assertTrue(evidence.storage() > 0, "Detector must see storage");
        context.assertTrue(evidence.work() > 0, "Detector must see work infrastructure");
        context.succeed();
    }

    @GameTest
    public void loadedWorldHomeDetectorRejectsRoofOnlyShelter(GameTestHelper context) {
        BlockPos center = new BlockPos(3, 2, 3);
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlock(x, 4, z, Blocks.STONE);
            }
        }

        HomeEvidencePolicy.Evidence evidence = HomeEvidenceDetector.scan(
                context.getLevel(), context.absolutePos(center));

        context.assertFalse(evidence.qualifies(),
                "A roof/cave-like shelter with no domestic anchor must remain UNKNOWN");
        context.succeed();
    }

    @GameTest
    public void semanticHomeRequiresRepeatedEvidence(GameTestHelper context) {
        HomeRecognitionAccumulator accumulator = new HomeRecognitionAccumulator();
        HomeEvidencePolicy.Evidence home =
                new HomeEvidencePolicy.Evidence(true, 1, 1, 0, 0);

        boolean recognized = false;
        for (int i = 0; i < HomeRecognitionAccumulator.DEFAULT_REQUIRED_GOOD_SAMPLES; i++) {
            recognized = accumulator.observe("minecraft:overworld", 0, 64, 0, home,
                    HomeRecognitionAccumulator.DEFAULT_REQUIRED_GOOD_SAMPLES);
        }

        context.assertTrue(recognized, "Repeated qualifying evidence should recognize HOME");
        context.succeed();
    }

    @GameTest
    public void semanticHomeResetsOnContradictoryEvidence(GameTestHelper context) {
        HomeRecognitionAccumulator accumulator = new HomeRecognitionAccumulator();
        HomeEvidencePolicy.Evidence home =
                new HomeEvidencePolicy.Evidence(true, 1, 1, 0, 0);
        HomeEvidencePolicy.Evidence notHome =
                new HomeEvidencePolicy.Evidence(false, 0, 0, 0, 0);

        for (int i = 0; i < 3; i++) {
            accumulator.observe("minecraft:overworld", 0, 64, 0, home,
                    HomeRecognitionAccumulator.DEFAULT_REQUIRED_GOOD_SAMPLES);
        }
        accumulator.observe("minecraft:overworld", 0, 64, 0, notHome,
                HomeRecognitionAccumulator.DEFAULT_REQUIRED_GOOD_SAMPLES);

        context.assertTrue(accumulator.goodSamples() == 0,
                "Contradictory evidence must reset the HOME candidate");
        context.succeed();
    }

    @GameTest
    public void familiarityPolicySupportsAnyMobClass(GameTestHelper context) {
        context.assertTrue(
                FamiliarityPolicy.cueEligible(
                    FamiliarityPolicy.REQUIRED_OBSERVATION_TICKS,
                    true,
                    FamiliarityPolicy.CUE_COOLDOWN_TICKS),
                "Any Mob reaching familiarity threshold and directly targeted should be eligible");
        context.assertFalse(
                FamiliarityPolicy.cueEligible(
                    FamiliarityPolicy.REQUIRED_OBSERVATION_TICKS,
                    false,
                    FamiliarityPolicy.CUE_COOLDOWN_TICKS),
                "A familiar mob must be directly targeted before presenting a cue");
        context.succeed();
    }

    @GameTest
    public void homeFeelChangesAcrossDayAndWeather(GameTestHelper context) {
        context.assertTrue(
                CanvasFeelProfile.classify(true, true, false, false, 500L)
                        == CanvasFeelProfile.Phase.HOME_MORNING,
                "HOME should have a morning phase");
        context.assertTrue(
                CanvasFeelProfile.classify(true, true, false, false, 12500L)
                        == CanvasFeelProfile.Phase.HOME_EVENING,
                "HOME should have an evening phase");
        context.assertTrue(
                CanvasFeelProfile.classify(true, true, false, false, 17000L)
                        == CanvasFeelProfile.Phase.HOME_NIGHT,
                "HOME should have a night phase");
        context.assertTrue(
                CanvasFeelProfile.classify(true, true, true, false, 6000L)
                        == CanvasFeelProfile.Phase.HOME_STORM,
                "Sheltered rain at HOME should select the storm phase");
        context.assertTrue(
                CanvasFeelProfile.classify(false, true, true, true, 17000L)
                        == CanvasFeelProfile.Phase.AWAY,
                "HOME FEEL phases must never leak outside HOME");
        context.succeed();
    }

    @GameTest
    public void villageLifeRhythmTracksNaturalSettlementActivity(GameTestHelper context) {
        context.assertTrue(
                VillageLifePolicy.classify(3, 2, 500L, false) == VillageLifePolicy.Rhythm.WAKE,
                "Nearby villagers around dawn should produce Village Wakes Up");
        context.assertTrue(
                VillageLifePolicy.classify(3, 2, 12000L, false) == VillageLifePolicy.Rhythm.WIND_DOWN,
                "Nearby villagers around dusk should produce Village Winds Down");
        context.assertTrue(
                VillageLifePolicy.classify(5, 4, 7000L, false) == VillageLifePolicy.Rhythm.GATHERING,
                "A naturally clustered daytime group should be eligible for community gathering");
        context.assertTrue(
                VillageLifePolicy.classify(1, 1, 500L, false) == VillageLifePolicy.Rhythm.NONE,
                "A lone villager must not manufacture settlement rhythm");
        context.succeed();
    }

    @GameTest
    public void contextualMusicOnlyPresentsMeaningfulVillageTransitions(GameTestHelper context) {
        context.assertTrue(
                ContextualMusicPolicy.eventFor(VillageLifePolicy.Rhythm.WAKE).equals("music.village_wake"),
                "Wake rhythm must map to wake music");
        context.assertTrue(
                ContextualMusicPolicy.eventFor(VillageLifePolicy.Rhythm.GATHERING).equals("music.community_gathering"),
                "Gathering must map to the community music moment");
        context.assertFalse(
                ContextualMusicPolicy.shouldPresent(
                        VillageLifePolicy.Rhythm.ACTIVE,
                        VillageLifePolicy.Rhythm.ACTIVE,
                        ContextualMusicPolicy.GENERAL_COOLDOWN_TICKS),
                "Stable ordinary village activity must remain musically quiet");
        context.assertTrue(
                ContextualMusicPolicy.shouldPresent(
                        VillageLifePolicy.Rhythm.ACTIVE,
                        VillageLifePolicy.Rhythm.WIND_DOWN,
                        ContextualMusicPolicy.GENERAL_COOLDOWN_TICKS),
                "A meaningful rhythm transition should be musically eligible");
        context.succeed();
    }

    @GameTest
    public void comingHomeV2CombinesVillageAndFamiliarContext(GameTestHelper context) {
        context.assertTrue(
                HomecomingPolicy.classify(true, VillageLifePolicy.Rhythm.GATHERING)
                        == HomecomingPolicy.Flavor.LIVED_IN,
                "Familiar mobs plus settlement life should create the richest homecoming flavor");
        context.assertTrue(
                HomecomingPolicy.classify(false, VillageLifePolicy.Rhythm.WIND_DOWN)
                        == HomecomingPolicy.Flavor.VILLAGE,
                "Settlement rhythm should enrich homecoming without requiring a familiar mob");
        context.assertTrue(
                HomecomingPolicy.classify(true, VillageLifePolicy.Rhythm.NONE)
                        == HomecomingPolicy.Flavor.FAMILIAR,
                "A familiar mob should enrich a quiet homecoming");
        context.assertTrue(
                HomecomingPolicy.classify(false, VillageLifePolicy.Rhythm.NONE)
                        == HomecomingPolicy.Flavor.QUIET,
                "A quiet home should remain restrained");
        context.succeed();
    }

    @GameTest
    public void rareSurprisesRemainRareAndContextBound(GameTestHelper context) {
        int rareDays = 0;
        for (long day = 0; day < 28; day++) {
            if (RareSurprisePolicy.rareDay(day, 0, 0, 5)) rareDays++;
        }
        context.assertTrue(rareDays >= 3 && rareDays <= 5,
                "Rare day cadence should stay sparse over four Minecraft weeks: " + rareDays);

        context.assertTrue(
                RareSurprisePolicy.classify(
                        false, true, false, false, 12000L, 7L, 0, 0, 99L)
                        == RareSurprisePolicy.Moment.NONE,
                "Rare surprises must not fire outside HOME");
        context.assertTrue(
                RareSurprisePolicy.classify(
                        true, true, false, false, 12000L, 7L, 0, 0, 1L)
                        == RareSurprisePolicy.Moment.NONE,
                "Rare surprises must respect the multi-day cooldown");
        context.succeed();
    }

    @GameTest
    public void weatherCharacterDistinguishesRoofStormAndCalm(GameTestHelper context) {
        context.assertTrue(
                WeatherCharacterPolicy.classify(true, true, true, false, Integer.MAX_VALUE)
                        == WeatherCharacterPolicy.Character.RAIN_SHELTERED,
                "Sheltered rain should create Rain on the Roof character");
        context.assertTrue(
                WeatherCharacterPolicy.classify(true, true, true, true, Integer.MAX_VALUE)
                        == WeatherCharacterPolicy.Character.THUNDER_SHELTERED,
                "Sheltered thunder should deepen the storm mood");
        context.assertTrue(
                WeatherCharacterPolicy.classify(true, true, false, false, 80)
                        == WeatherCharacterPolicy.Character.CALM_AFTER_STORM,
                "A sheltered HOME should briefly notice calm after rain ends");
        context.assertTrue(
                WeatherCharacterPolicy.classify(false, false, true, false, Integer.MAX_VALUE)
                        == WeatherCharacterPolicy.Character.RAIN_EXPOSED,
                "Exposed rain should remain owned by the mature companion acoustics stack");
        context.assertTrue(
                WeatherCharacterPolicy.usesCompanionAcousticsOnly(
                        WeatherCharacterPolicy.Character.RAIN_EXPOSED),
                "Canvas must not reinvent exposed/material rain acoustics");
        context.succeed();
    }

    @GameTest
    public void comingHomeV3CoordinatesWeatherMusicAndReturnHistory(GameTestHelper context) {
        HomecomingPolicy.Plan first = HomecomingPolicy.compose(
                true,
                VillageLifePolicy.Rhythm.GATHERING,
                WeatherCharacterPolicy.Character.CLEAR,
                0);
        context.assertTrue(first.flavor() == HomecomingPolicy.Flavor.LIVED_IN,
                "Familiar faces and village life should still drive the arrival flavor");
        context.assertTrue(first.musicEvent().isBlank(),
                "The first rich return should remain restrained rather than always forcing music");

        HomecomingPolicy.Plan remembered = HomecomingPolicy.compose(
                true,
                VillageLifePolicy.Rhythm.GATHERING,
                WeatherCharacterPolicy.Character.CLEAR,
                3);
        context.assertTrue(remembered.musicEvent().equals("music.coming_home"),
                "Repeated meaningful returns may earn a restrained contextual music moment");
        context.assertTrue(remembered.pulseTicks() > first.pulseTicks(),
                "Return history should subtly deepen a familiar homecoming");

        HomecomingPolicy.Plan storm = HomecomingPolicy.compose(
                false,
                VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.THUNDER_SHELTERED,
                1);
        context.assertTrue(storm.musicEvent().equals("music.coming_home_storm"),
                "Returning into a sheltered thunderstorm should coordinate weather and music");
        context.assertTrue(storm.cuePitch() < 1.0f,
                "Storm homecoming should have a lower, calmer cue profile");
        context.succeed();
    }

    @GameTest
    public void rareSurpriseV2AddsAThirdDeterministicMoment(GameTestHelper context) {
        boolean foundStarlit = false;
        int totalRareNights = 0;
        for (long day = 0; day < 28; day++) {
            RareSurprisePolicy.Moment moment = RareSurprisePolicy.classify(
                    true, false, false, false, 19000L, day, 0, 0, 99L);
            if (moment == RareSurprisePolicy.Moment.STARLIT_STILLNESS) {
                foundStarlit = true;
                totalRareNights++;
            }
        }
        context.assertTrue(foundStarlit,
                "The rare library should include a deterministic starlit-stillness moment");
        context.assertTrue(totalRareNights >= 3 && totalRareNights <= 5,
                "Starlit stillness must remain sparse over four Minecraft weeks: " + totalRareNights);

        RareSurprisePolicy.Moment first = RareSurprisePolicy.classify(
                true, false, false, false, 19000L, 7L, 12, -9, 99L);
        RareSurprisePolicy.Moment second = RareSurprisePolicy.classify(
                true, false, false, false, 19000L, 7L, 12, -9, 99L);
        context.assertTrue(first == second,
                "Rare-surprise selection must be deterministic for the same HOME/day context");
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
