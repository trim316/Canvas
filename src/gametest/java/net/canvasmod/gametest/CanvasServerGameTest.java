package net.canvasmod.gametest;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.EnumSet;
import net.canvasmod.CanvasFeelProfile;
import net.canvasmod.TravelAtmospherePolicy;
import net.canvasmod.CanvasWorldMemoryStore;
import net.canvasmod.MilestoneZeroScenarioPolicy;
import net.canvasmod.ObservationBudgetPolicy;
import net.canvasmod.ContextualMusicPolicy;
import net.canvasmod.ExplorationMusicPolicy;
import net.canvasmod.ExplorationWeatherPolicy;
import net.canvasmod.RouteFamiliarityPolicy;
import net.canvasmod.RouteFamiliarityTracker;
import net.canvasmod.LandmarkRecognitionPolicy;
import net.canvasmod.LandmarkFamiliarityTracker;
import net.canvasmod.RareWonderPolicy;
import net.canvasmod.ExplorationWonderMemoryStore;
import net.canvasmod.SharedSettlementPolicy;
import net.canvasmod.SharedSettlementStore;
import net.canvasmod.CanvasWorldIdentityStore;
import net.canvasmod.WorldMemoryScopePolicy;
import net.canvasmod.SharedGatheringPolicy;
import net.canvasmod.SharedWorldMemoryStore;
import net.canvasmod.P3MultiplayerScenarioPolicy;
import net.canvasmod.CanvasFeatureConfig;
import net.canvasmod.LongSessionSoakPolicy;
import net.canvasmod.LowEndPerformanceBudgetPolicy;
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.VillageAfterRainPolicy;
import net.canvasmod.VillageContinuityPolicy;
import net.canvasmod.WeatherCharacterPolicy;
import net.canvasmod.FamiliarityPolicy;
import net.canvasmod.FamiliarBondPolicy;
import net.canvasmod.FamiliarGreetingHistoryPolicy;
import net.canvasmod.HomeEvidenceDetector;
import net.canvasmod.HomecomingPolicy;
import net.canvasmod.LongJourneyHomecomingPolicy;
import net.canvasmod.AmbienceHandoffPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.RareSurprisePolicy;
import net.canvasmod.SeasonPolicy;
import net.canvasmod.SeasonalPresentationPolicy;
import net.canvasmod.SeasonalHomecomingPolicy;
import net.canvasmod.SeasonalFamiliarityProfile;
import net.canvasmod.SeasonalRareMomentPolicy;
import net.canvasmod.SeasonalVillageProfile;
import net.canvasmod.CanvasSeasonMemoryStore;
import net.canvasmod.SeasonsOfHomeScenarioPolicy;
import net.canvasmod.SeasonalHomeProfile;
import net.canvasmod.HomeEvidencePolicy;
import net.canvasmod.HomeRecognitionAccumulator;
import net.canvasmod.PlaceEvidenceDetector;
import net.canvasmod.PlaceFamiliarityPolicy;
import net.canvasmod.PlaceRecognitionAccumulator;
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

    @GameTest
    public void nothingHappensGuardrailEnforcesQuietTime(GameTestHelper context) {
        MomentDensityPolicy.Budget budget = new MomentDensityPolicy.Budget();
        long tick = 1000L;
        context.assertTrue(
                budget.tryAcquire(MomentDensityPolicy.Kind.HOMECOMING, tick, false),
                "The first meaningful moment should be allowed");
        context.assertFalse(
                budget.tryAcquire(MomentDensityPolicy.Kind.FAMILIAR_FACE, tick + 20L, false),
                "Back-to-back presentations should be suppressed");

        int accepted = 1;
        for (int i = 1; i < 10; i++) {
            if (budget.tryAcquire(
                    MomentDensityPolicy.Kind.PHASE_SHIFT,
                    tick + i * MomentDensityPolicy.GLOBAL_MIN_GAP_TICKS,
                    false)) {
                accepted++;
            }
        }
        context.assertTrue(
                accepted == MomentDensityPolicy.MAX_PRESENTATIONS_PER_WINDOW,
                "A five-minute window must have a hard presentation ceiling");
        context.assertTrue(
                budget.suppressedCount() > 0,
                "The guardrail must record suppressed candidate moments");
        context.succeed();
    }

    @GameTest
    public void nothingHappensGuardrailLimitsMusicDensity(GameTestHelper context) {
        MomentDensityPolicy.Budget budget = new MomentDensityPolicy.Budget();
        long tick = 2000L;
        context.assertTrue(
                budget.tryAcquire(MomentDensityPolicy.Kind.VILLAGE_RHYTHM, tick, true),
                "First music moment should be allowed");
        context.assertTrue(
                budget.tryAcquire(
                        MomentDensityPolicy.Kind.HOMECOMING,
                        tick + MomentDensityPolicy.GLOBAL_MIN_GAP_TICKS,
                        true),
                "Second spaced music moment should be allowed");
        context.assertFalse(
                budget.tryAcquire(
                        MomentDensityPolicy.Kind.VILLAGE_RHYTHM,
                        tick + MomentDensityPolicy.GLOBAL_MIN_GAP_TICKS * 2L,
                        true),
                "Music must stay below its stricter density ceiling");
        context.succeed();
    }

    @GameTest
    public void rareSurpriseV2IncludesThreeSparsePresentationMoments(GameTestHelper context) {
        boolean foundStorm = false;
        boolean foundGolden = false;
        boolean foundStar = false;
        for (long day = 0; day < 64; day++) {
            long since = 99L;
            if (RareSurprisePolicy.classify(true, true, false, false, 12000L, day, 3, 9, since)
                    == RareSurprisePolicy.Moment.STORM_BREAK) foundStorm = true;
            if (RareSurprisePolicy.classify(true, false, false, false, 12000L, day, 3, 9, since)
                    == RareSurprisePolicy.Moment.GOLDEN_HUSH) foundGolden = true;
            if (RareSurprisePolicy.classify(true, false, false, false, 18000L, day, 3, 9, since)
                    == RareSurprisePolicy.Moment.STARLIT_STILLNESS) foundStar = true;
        }
        context.assertTrue(foundStorm && foundGolden && foundStar,
                "All three rare surprise families must be reachable over a long deterministic horizon");
        context.succeed();
    }

    @GameTest
    public void nothingHappensDensityGuardrailsEnforceQuietSpace(GameTestHelper context) {
        context.assertTrue(
                MomentDensityPolicy.allowMajor(10000L, 0L, 0),
                "A major moment should be allowed after a sufficiently long quiet gap");
        context.assertFalse(
                MomentDensityPolicy.allowMajor(
                        100L,
                        80L,
                        0),
                "Back-to-back major moments must be suppressed");
        context.assertFalse(
                MomentDensityPolicy.allowMajor(
                        10000L,
                        0L,
                        MomentDensityPolicy.MAX_MAJOR_MOMENTS_PER_DAY),
                "Daily density ceiling must create Nothing Happens space");
        context.succeed();
    }

    @GameTest
    public void worldMemoryPersistsHomeVillageFamiliarAndRareHistory(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-world-memory-test");
        var file = dir.resolve("memory.properties");

        CanvasWorldMemoryStore first = new CanvasWorldMemoryStore(file);
        first.bind("minecraft:overworld|10|64|20");
        first.noteHomecoming();
        first.noteHomecoming();
        first.noteFamiliarMoment();
        first.noteVillageMoment();
        first.noteRareMoment(42L);

        CanvasWorldMemoryStore reloaded = new CanvasWorldMemoryStore(file);
        reloaded.bind("minecraft:overworld|10|64|20");
        var snapshot = reloaded.snapshot();

        context.assertTrue(snapshot.homecomings() == 2,
                "Meaningful returns must survive reload");
        context.assertTrue(snapshot.familiarMoments() == 1,
                "Familiar-face history must survive reload");
        context.assertTrue(snapshot.villageMoments() == 1,
                "Village moment history must survive reload");
        context.assertTrue(snapshot.rareMoments() == 1 && snapshot.lastSurpriseDay() == 42L,
                "Rare surprise history and cooldown anchor must survive reload");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void milestoneZeroScenarioCampaignCoversEightMoments(GameTestHelper context) {
        EnumSet<MilestoneZeroScenarioPolicy.Moment> seen =
                EnumSet.noneOf(MilestoneZeroScenarioPolicy.Moment.class);

        seen.addAll(MilestoneZeroScenarioPolicy.observe(new MilestoneZeroScenarioPolicy.Frame(
                true, true, true, false, VillageLifePolicy.Rhythm.WAKE,
                false, RareSurprisePolicy.Moment.NONE, false)));
        seen.addAll(MilestoneZeroScenarioPolicy.observe(new MilestoneZeroScenarioPolicy.Frame(
                false, true, true, true, VillageLifePolicy.Rhythm.ACTIVE,
                false, RareSurprisePolicy.Moment.NONE, false)));
        seen.addAll(MilestoneZeroScenarioPolicy.observe(new MilestoneZeroScenarioPolicy.Frame(
                false, true, true, false, VillageLifePolicy.Rhythm.WIND_DOWN,
                true, RareSurprisePolicy.Moment.NONE, false)));
        seen.addAll(MilestoneZeroScenarioPolicy.observe(new MilestoneZeroScenarioPolicy.Frame(
                false, true, false, false, VillageLifePolicy.Rhythm.GATHERING,
                false, RareSurprisePolicy.Moment.GOLDEN_HUSH, false)));
        seen.addAll(MilestoneZeroScenarioPolicy.observe(new MilestoneZeroScenarioPolicy.Frame(
                false, true, true, false, VillageLifePolicy.Rhythm.ACTIVE,
                false, RareSurprisePolicy.Moment.NONE, true)));

        context.assertTrue(
                seen.equals(EnumSet.allOf(MilestoneZeroScenarioPolicy.Moment.class)),
                "Milestone-0 campaign must cover all eight experience moments: " + seen);
        context.succeed();
    }

    @GameTest
    public void observationBudgetStaysBoundedAndLoadedOnly(GameTestHelper context) {
        context.assertTrue(
                ObservationBudgetPolicy.HOME_MAX_BLOCK_PROBES == 2312,
                "HOME scan must remain explicitly bounded");
        context.assertTrue(
                ObservationBudgetPolicy.withinBudget(),
                "HOME observation equivalent probe rate must stay within budget: "
                        + ObservationBudgetPolicy.equivalentBlockProbesPerSecond());
        context.succeed();
    }


    @GameTest
    public void seasonalVillageAndFamiliarPresentationRemainInterpretive(GameTestHelper context) {
        context.assertTrue(
                SeasonalVillageProfile.musicPitchMultiplier(SeasonPolicy.Season.SPRING)
                        > SeasonalVillageProfile.musicPitchMultiplier(SeasonPolicy.Season.WINTER),
                "Village presentation should read spring and winter differently without changing villager AI");
        context.assertTrue(
                SeasonalFamiliarityProfile.cuePitch(SeasonPolicy.Season.SPRING)
                        > SeasonalFamiliarityProfile.cuePitch(SeasonPolicy.Season.WINTER),
                "Familiar faces should carry a restrained seasonal tone");
        context.assertTrue(
                SeasonalVillageProfile.accentArgb(
                        SeasonPolicy.Season.AUTUMN, VillageLifePolicy.Rhythm.GATHERING) != 0,
                "Seasonal gathering interpretation must remain presentation-only and visible");
        context.succeed();
    }

    @GameTest
    public void firstSnowAndSeasonalRareMomentsStaySparse(GameTestHelper context) {
        context.assertTrue(
                SeasonalRareMomentPolicy.classify(
                        SeasonPolicy.Season.WINTER, true, true, 2L, 99L, false)
                        == SeasonalRareMomentPolicy.Moment.FIRST_SNOW,
                "First observed winter snow at HOME should be eligible exactly once");
        context.assertTrue(
                SeasonalRareMomentPolicy.classify(
                        SeasonPolicy.Season.WINTER, true, true, 2L, 99L, true)
                        != SeasonalRareMomentPolicy.Moment.FIRST_SNOW,
                "First snow must not repeat after it has been remembered");
        context.assertTrue(
                SeasonalRareMomentPolicy.classify(
                        SeasonPolicy.Season.SPRING, true, false, 13L, 1L, false)
                        == SeasonalRareMomentPolicy.Moment.NONE,
                "Seasonal rare moments must respect multi-day quiet space");
        context.assertTrue(
                SeasonalRareMomentPolicy.classify(
                        SeasonPolicy.Season.SPRING, false, false, 13L, 99L, false)
                        == SeasonalRareMomentPolicy.Moment.NONE,
                "Seasonal HOME moments must not leak into unrelated exploration");
        context.succeed();
    }

    @GameTest
    public void seasonalMemoryPersistsTransitionsAndFirstSnow(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-season-memory-test");
        var file = dir.resolve("season-memory.properties");

        CanvasSeasonMemoryStore first = new CanvasSeasonMemoryStore(file);
        first.bind("minecraft:overworld|10|64|20");
        first.observeSeason(SeasonPolicy.Season.AUTUMN);
        first.observeSeason(SeasonPolicy.Season.WINTER);
        first.noteSeasonalMoment(SeasonalRareMomentPolicy.Moment.FIRST_SNOW, 42L);

        CanvasSeasonMemoryStore reloaded = new CanvasSeasonMemoryStore(file);
        reloaded.bind("minecraft:overworld|10|64|20");
        var snapshot = reloaded.snapshot();

        context.assertTrue(snapshot.lastSeason() == SeasonPolicy.Season.WINTER,
                "Last observed season must survive reload");
        context.assertTrue(snapshot.seasonTransitions() == 1,
                "Season transition history must survive reload");
        context.assertTrue(snapshot.seasonalMoments() == 1
                        && snapshot.lastSeasonalMomentDay() == 42L,
                "Seasonal moment history must survive reload");
        context.assertTrue(snapshot.firstSnowSeen(),
                "First-snow memory must survive reload");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void fourSeasonScenarioCampaignCoversSeasonsOfHome(GameTestHelper context) {
        EnumSet<SeasonsOfHomeScenarioPolicy.Experience> seen =
                EnumSet.noneOf(SeasonsOfHomeScenarioPolicy.Experience.class);

        seen.addAll(SeasonsOfHomeScenarioPolicy.observe(new SeasonsOfHomeScenarioPolicy.Frame(
                SeasonPolicy.Season.SPRING, true, VillageLifePolicy.Rhythm.WAKE,
                true, SeasonalRareMomentPolicy.Moment.SPRING_CHORUS)));
        seen.addAll(SeasonsOfHomeScenarioPolicy.observe(new SeasonsOfHomeScenarioPolicy.Frame(
                SeasonPolicy.Season.SUMMER, true, VillageLifePolicy.Rhythm.GATHERING,
                false, SeasonalRareMomentPolicy.Moment.SUMMER_AFTERGLOW)));
        seen.addAll(SeasonsOfHomeScenarioPolicy.observe(new SeasonsOfHomeScenarioPolicy.Frame(
                SeasonPolicy.Season.AUTUMN, true, VillageLifePolicy.Rhythm.WIND_DOWN,
                true, SeasonalRareMomentPolicy.Moment.AUTUMN_HUSH)));
        seen.addAll(SeasonsOfHomeScenarioPolicy.observe(new SeasonsOfHomeScenarioPolicy.Frame(
                SeasonPolicy.Season.WINTER, true, VillageLifePolicy.Rhythm.QUIET_NIGHT,
                true, SeasonalRareMomentPolicy.Moment.FIRST_SNOW)));

        context.assertTrue(
                seen.equals(EnumSet.allOf(SeasonsOfHomeScenarioPolicy.Experience.class)),
                "Four-season campaign must cover every Seasons of Home experience: " + seen);
        context.succeed();
    }

    @GameTest
    public void placeFamiliarityClassifiesPlayerAuthoredContexts(GameTestHelper context) {
        context.assertTrue(
                new PlaceFamiliarityPolicy.Evidence(12, 0, 0, 0, 0, 0, false, 0).classify()
                        == PlaceFamiliarityPolicy.Kind.PATH,
                "A repeated authored path surface should classify as PATH");
        context.assertTrue(
                new PlaceFamiliarityPolicy.Evidence(0, 12, 16, 0, 0, 0, false, 0).classify()
                        == PlaceFamiliarityPolicy.Kind.DOCK,
                "Wood beside substantial water should classify as DOCK");
        context.assertTrue(
                new PlaceFamiliarityPolicy.Evidence(0, 0, 0, 16, 0, 0, false, 0).classify()
                        == PlaceFamiliarityPolicy.Kind.FARM,
                "Dense farmland/crops should classify as FARM");
        context.assertTrue(
                new PlaceFamiliarityPolicy.Evidence(0, 0, 0, 0, 5, 1, false, 0).classify()
                        == PlaceFamiliarityPolicy.Kind.GATHERING_SPOT,
                "A social anchor with several comfort details should classify as GATHERING_SPOT");
        context.assertTrue(
                new PlaceFamiliarityPolicy.Evidence(0, 0, 0, 0, 0, 0, true, 2).classify()
                        == PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                "An open-sky edge with multiple loaded drop directions should classify as VIEWPOINT");
        context.succeed();
    }

    @GameTest
    public void placeRecognitionRequiresStableRepeatedEvidence(GameTestHelper context) {
        PlaceRecognitionAccumulator accumulator = new PlaceRecognitionAccumulator();
        boolean recognized = false;
        for (int i = 0; i < PlaceFamiliarityPolicy.REQUIRED_GOOD_SAMPLES; i++) {
            recognized = accumulator.observe(
                    PlaceFamiliarityPolicy.Kind.FARM,
                    "minecraft:overworld",
                    10.0 + i * 0.5,
                    64.0,
                    20.0,
                    PlaceFamiliarityPolicy.REQUIRED_GOOD_SAMPLES);
        }
        context.assertTrue(recognized,
                "Repeated evidence for the same authored place should become familiar");

        accumulator.observe(
                PlaceFamiliarityPolicy.Kind.PATH,
                "minecraft:overworld",
                100.0,
                64.0,
                100.0,
                PlaceFamiliarityPolicy.REQUIRED_GOOD_SAMPLES);
        context.assertTrue(accumulator.goodSamples() == 1,
                "A different distant place must start a new familiarity candidate");
        context.succeed();
    }

    @GameTest
    public void loadedWorldPlaceDetectorRecognizesAuthoredFarm(GameTestHelper context) {
        BlockPos center = new BlockPos(3, 2, 3);
        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                context.setBlock(x, 1, z, Blocks.FARMLAND);
            }
        }

        PlaceFamiliarityPolicy.Evidence evidence = PlaceEvidenceDetector.scan(
                context.getLevel(), context.absolutePos(center));

        context.assertTrue(evidence.farmBlocks() >= 12,
                "Loaded-only place detector should observe authored farmland");
        context.assertTrue(evidence.classify() == PlaceFamiliarityPolicy.Kind.FARM,
                "Authored farmland should be interpreted as a FARM place");
        context.succeed();
    }

    @GameTest
    public void explorationMusicRequiresFamiliarMeaningfulTransitions(GameTestHelper context) {
        context.assertTrue(
                ExplorationMusicPolicy.shouldPresent(
                        PlaceFamiliarityPolicy.Kind.NONE,
                        PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                        true,
                        false,
                        ExplorationMusicPolicy.VIEWPOINT_COOLDOWN_TICKS),
                "Entering a familiar viewpoint after quiet space should be eligible for exploration music");
        context.assertFalse(
                ExplorationMusicPolicy.shouldPresent(
                        PlaceFamiliarityPolicy.Kind.PATH,
                        PlaceFamiliarityPolicy.Kind.PATH,
                        true,
                        false,
                        ExplorationMusicPolicy.GENERAL_COOLDOWN_TICKS),
                "Remaining in the same familiar context must stay quiet");
        context.assertFalse(
                ExplorationMusicPolicy.shouldPresent(
                        PlaceFamiliarityPolicy.Kind.NONE,
                        PlaceFamiliarityPolicy.Kind.DOCK,
                        false,
                        false,
                        ExplorationMusicPolicy.GENERAL_COOLDOWN_TICKS),
                "Unfamiliar places must not immediately earn contextual music");
        context.assertFalse(
                ExplorationMusicPolicy.shouldPresent(
                        PlaceFamiliarityPolicy.Kind.NONE,
                        PlaceFamiliarityPolicy.Kind.FARM,
                        true,
                        true,
                        ExplorationMusicPolicy.GENERAL_COOLDOWN_TICKS),
                "Exploration music must not compete with HOME presentation");
        context.assertTrue(
                ExplorationMusicPolicy.volumeFor(PlaceFamiliarityPolicy.Kind.PATH)
                        < ExplorationMusicPolicy.volumeFor(PlaceFamiliarityPolicy.Kind.VIEWPOINT),
                "Ordinary routes should remain quieter than rare viewpoint transitions");
        context.succeed();
    }

    @GameTest
    public void explorationWeatherCombinesFamiliarPlaceAndWeatherSemantics(GameTestHelper context) {
        context.assertTrue(
                ExplorationWeatherPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.DOCK, true, false, true, false, false)
                        == ExplorationWeatherPolicy.Moment.RAIN_ON_DOCK,
                "Rain at a familiar dock should be interpreted as a dock-weather moment");
        context.assertTrue(
                ExplorationWeatherPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.VIEWPOINT, true, false, true, true, false)
                        == ExplorationWeatherPolicy.Moment.STORM_OVERLOOK,
                "Thunder at a familiar viewpoint should become a restrained overlook moment");
        context.assertTrue(
                ExplorationWeatherPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.FARM, true, false, false, false, true)
                        == ExplorationWeatherPolicy.Moment.FIELD_AFTER_RAIN,
                "A familiar farm may notice the calm immediately after rain");
        context.assertTrue(
                ExplorationWeatherPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.DOCK, false, false, true, false, false)
                        == ExplorationWeatherPolicy.Moment.NONE,
                "Unfamiliar terrain must remain ordinary");
        context.assertTrue(
                ExplorationWeatherPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.VIEWPOINT, true, true, true, true, false)
                        == ExplorationWeatherPolicy.Moment.NONE,
                "Exploration weather presentation must not compete inside HOME");
        context.succeed();
    }

    @GameTest
    public void repeatedRouteFamiliarityRequiresActualPathTraversal(GameTestHelper context) {
        context.assertTrue(
                RouteFamiliarityPolicy.eligibleSegment(
                        PlaceFamiliarityPolicy.Kind.PATH,
                        PlaceFamiliarityPolicy.Kind.PATH,
                        "minecraft:overworld",
                        "minecraft:overworld",
                        2.0, 2.0, 20.0, 2.0),
                "Movement across loaded path cells should be eligible as a route segment");
        context.assertFalse(
                RouteFamiliarityPolicy.eligibleSegment(
                        PlaceFamiliarityPolicy.Kind.PATH,
                        PlaceFamiliarityPolicy.Kind.PATH,
                        "minecraft:overworld",
                        "minecraft:overworld",
                        2.0, 2.0, 8.0, 2.0),
                "Movement that remains inside one coarse cell must not become a route");
        context.assertFalse(
                RouteFamiliarityPolicy.eligibleSegment(
                        PlaceFamiliarityPolicy.Kind.FARM,
                        PlaceFamiliarityPolicy.Kind.PATH,
                        "minecraft:overworld",
                        "minecraft:overworld",
                        2.0, 2.0, 20.0, 2.0),
                "Route familiarity must not infer corridors from unrelated place semantics");
        context.succeed();
    }

    @GameTest
    public void repeatedRouteFamiliarityIsDirectionNeutralAndEarned(GameTestHelper context) {
        String forward = RouteFamiliarityPolicy.segmentKey(
                "minecraft:overworld", 2.0, 2.0, 20.0, 2.0);
        String reverse = RouteFamiliarityPolicy.segmentKey(
                "minecraft:overworld", 20.0, 2.0, 2.0, 2.0);
        context.assertTrue(forward.equals(reverse),
                "Traveling the same corridor in reverse must address the same route memory");

        RouteFamiliarityTracker tracker = new RouteFamiliarityTracker();
        tracker.observe(PlaceFamiliarityPolicy.Kind.PATH, "minecraft:overworld", 2.0, 2.0);
        var first = tracker.observe(
                PlaceFamiliarityPolicy.Kind.PATH, "minecraft:overworld", 20.0, 2.0);
        var second = tracker.observe(
                PlaceFamiliarityPolicy.Kind.PATH, "minecraft:overworld", 2.0, 2.0);
        var third = tracker.observe(
                PlaceFamiliarityPolicy.Kind.PATH, "minecraft:overworld", 20.0, 2.0);

        context.assertFalse(first.becameFamiliar() || second.becameFamiliar(),
                "A route should remain ordinary during its first two traversals");
        context.assertTrue(third.becameFamiliar(),
                "The configured repeated traversal threshold should earn route familiarity");
        context.assertTrue(tracker.isFamiliar(forward),
                "Earned route familiarity must be queryable by its direction-neutral key");
        context.succeed();
    }

    @GameTest
    public void routeFamiliarityRestoreIsBounded(GameTestHelper context) {
        RouteFamiliarityTracker tracker = new RouteFamiliarityTracker();
        String key = RouteFamiliarityPolicy.segmentKey(
                "minecraft:overworld", 2.0, 2.0, 20.0, 2.0);
        tracker.restore(key, 99);
        context.assertTrue(tracker.isFamiliar(key),
                "Persisted familiar route state must survive reload");
        context.assertTrue(
                tracker.entries().get(key) == RouteFamiliarityPolicy.REQUIRED_TRAVERSALS,
                "Restored traversal counts must clamp at the familiarity threshold");
        context.succeed();
    }

    @GameTest
    public void quietLandmarksRequireMeaningfulFamiliarPlaceKinds(GameTestHelper context) {
        context.assertTrue(
                LandmarkRecognitionPolicy.eligibleKind(PlaceFamiliarityPolicy.Kind.VIEWPOINT),
                "Viewpoints should be eligible to become quiet landmarks");
        context.assertTrue(
                LandmarkRecognitionPolicy.eligibleKind(PlaceFamiliarityPolicy.Kind.DOCK),
                "Docks should be eligible to become quiet landmarks");
        context.assertFalse(
                LandmarkRecognitionPolicy.eligibleKind(PlaceFamiliarityPolicy.Kind.PATH),
                "Ordinary path segments must not become landmarks");
        context.assertFalse(
                LandmarkRecognitionPolicy.eligibleKind(PlaceFamiliarityPolicy.Kind.NONE),
                "Unknown context must remain unknown");
        context.succeed();
    }

    @GameTest
    public void quietLandmarksRequireSeparatedReturns(GameTestHelper context) {
        LandmarkFamiliarityTracker tracker = new LandmarkFamiliarityTracker();
        String key = LandmarkRecognitionPolicy.landmarkKey(
                PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                "minecraft:overworld",
                32.0, 80.0, 48.0);

        long tick = 1000L;
        for (int visit = 1; visit < LandmarkRecognitionPolicy.REQUIRED_VISITS; visit++) {
            var observation = tracker.observe(
                    key,
                    PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                    tick);
            context.assertFalse(observation.becameLandmark(),
                    "A landmark must not be recognized before the configured visit threshold");
            tracker.observe("", PlaceFamiliarityPolicy.Kind.NONE, tick + 20L);
            tick += LandmarkRecognitionPolicy.MIN_REVISIT_GAP_TICKS;
        }

        var finalObservation = tracker.observe(
                key,
                PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                tick);
        context.assertTrue(finalObservation.becameLandmark(),
                "Repeated separated returns should quietly promote a familiar place to landmark");
        context.assertTrue(tracker.isLandmark(key),
                "Recognized landmark state must remain queryable");
        context.succeed();
    }

    @GameTest
    public void quietLandmarkStandingStillDoesNotFarmVisits(GameTestHelper context) {
        LandmarkFamiliarityTracker tracker = new LandmarkFamiliarityTracker();
        String key = LandmarkRecognitionPolicy.landmarkKey(
                PlaceFamiliarityPolicy.Kind.DOCK,
                "minecraft:overworld",
                16.0, 64.0, 16.0);

        tracker.observe(key, PlaceFamiliarityPolicy.Kind.DOCK, 1000L);
        var repeated = tracker.observe(
                key,
                PlaceFamiliarityPolicy.Kind.DOCK,
                1000L + LandmarkRecognitionPolicy.MIN_REVISIT_GAP_TICKS * 2L);

        context.assertTrue(repeated.visits() == 0,
                "Remaining continuously inside the same place must not count as a return");
        context.assertFalse(tracker.isLandmark(key),
                "Standing still must never manufacture landmark familiarity");
        context.succeed();
    }

    @GameTest
    public void quietLandmarkRestoreClampsPersistedVisits(GameTestHelper context) {
        LandmarkFamiliarityTracker tracker = new LandmarkFamiliarityTracker();
        String key = LandmarkRecognitionPolicy.landmarkKey(
                PlaceFamiliarityPolicy.Kind.GATHERING_SPOT,
                "minecraft:overworld",
                64.0, 70.0, 64.0);
        tracker.restore(key, 999);
        context.assertTrue(tracker.isLandmark(key),
                "Persisted landmark recognition must survive reload");
        context.assertTrue(
                tracker.visits().get(key) == LandmarkRecognitionPolicy.REQUIRED_VISITS,
                "Restored landmark visits must clamp at the recognition threshold");
        context.succeed();
    }

    @GameTest
    public void rareWonderLibraryRequiresEarnedLandmarksAndQuietWeather(GameTestHelper context) {
        String viewpoint = "VIEWPOINT|minecraft:overworld|2,10,3";
        long rareDay = -1L;
        for (long day = 0; day < 128; day++) {
            if (RareWonderPolicy.rareDay(viewpoint, day)) {
                rareDay = day;
                break;
            }
        }
        context.assertTrue(rareDay >= 0L,
                "A deterministic landmark should have sparse eligible wonder days");

        context.assertTrue(
                RareWonderPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                        true,
                        false,
                        false,
                        false,
                        12000L,
                        rareDay,
                        viewpoint,
                        99L) == RareWonderPolicy.Moment.HORIZON_GLOW,
                "A rare clear sunset at an earned viewpoint should be eligible for horizon glow");
        context.assertTrue(
                RareWonderPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.DOCK,
                        false,
                        false,
                        false,
                        false,
                        19000L,
                        rareDay,
                        "DOCK|minecraft:overworld|1,8,1",
                        99L) == RareWonderPolicy.Moment.NONE,
                "Unrecognized landmarks must never trigger rare wonders");
        context.assertTrue(
                RareWonderPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                        true,
                        false,
                        true,
                        false,
                        12000L,
                        rareDay,
                        viewpoint,
                        99L) == RareWonderPolicy.Moment.NONE,
                "Rain should suppress the clear-weather wonder library");
        context.assertTrue(
                RareWonderPolicy.classify(
                        PlaceFamiliarityPolicy.Kind.VIEWPOINT,
                        true,
                        true,
                        false,
                        false,
                        12000L,
                        rareDay,
                        viewpoint,
                        99L) == RareWonderPolicy.Moment.NONE,
                "Exploration wonders must not compete inside HOME");
        context.succeed();
    }

    @GameTest
    public void rareWonderDaysRemainSparsePerLandmark(GameTestHelper context) {
        String key = "FARM|minecraft:overworld|4,8,4";
        int eligible = 0;
        for (long day = 0; day < 68; day++) {
            if (RareWonderPolicy.rareDay(key, day)) eligible++;
        }
        context.assertTrue(eligible >= 3 && eligible <= 5,
                "Rare wonder cadence should remain sparse across 68 Minecraft days: " + eligible);
        context.succeed();
    }

    @GameTest
    public void explorationWonderMemoryPersistsPerLandmarkCooldown(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-wonder-memory-test");
        var file = dir.resolve("wonders.properties");
        String key = "DOCK|minecraft:overworld|1,8,1";

        ExplorationWonderMemoryStore first = new ExplorationWonderMemoryStore(file);
        first.note(key, 42L);
        first.note(key, 43L);

        ExplorationWonderMemoryStore reloaded = new ExplorationWonderMemoryStore(file);
        var snapshot = reloaded.snapshot(key);
        context.assertTrue(snapshot.lastWorldDay() == 43L,
                "Wonder cooldown day must survive reload");
        context.assertTrue(snapshot.moments() == 2,
                "Per-landmark wonder history must survive reload");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void sharedSettlementRecognitionClustersNearbySemanticHomes(GameTestHelper context) {
        var homes = java.util.List.of(
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "minecraft:overworld", 0.0, 64.0, 0.0),
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "minecraft:overworld", 60.0, 66.0, 20.0));

        var settlements = SharedSettlementPolicy.detect(homes);
        context.assertTrue(settlements.size() == 1,
                "Two nearby durable HOME anchors should form one shared settlement");
        context.assertTrue(settlements.get(0).members().size() == 2,
                "Shared settlement membership should include both distinct players");
        context.succeed();
    }

    @GameTest
    public void sharedSettlementRecognitionKeepsDimensionsAndDistanceSeparate(GameTestHelper context) {
        var homes = java.util.List.of(
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                        "minecraft:overworld", 0.0, 64.0, 0.0),
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000012"),
                        "minecraft:the_nether", 10.0, 64.0, 10.0),
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000013"),
                        "minecraft:overworld", 300.0, 64.0, 0.0));

        context.assertTrue(SharedSettlementPolicy.detect(homes).isEmpty(),
                "Different dimensions or distant homes must not be merged into a settlement");
        context.succeed();
    }

    @GameTest
    public void sharedSettlementRecognitionSupportsConnectedNeighborhoods(GameTestHelper context) {
        var homes = java.util.List.of(
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000021"),
                        "minecraft:overworld", 0.0, 64.0, 0.0),
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000022"),
                        "minecraft:overworld", 80.0, 64.0, 0.0),
                new SharedSettlementPolicy.HomeAnchor(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000023"),
                        "minecraft:overworld", 160.0, 64.0, 0.0));

        var settlements = SharedSettlementPolicy.detect(homes);
        context.assertTrue(settlements.size() == 1,
                "Connected nearby HOME anchors should form one settlement component");
        context.assertTrue(settlements.get(0).members().size() == 3,
                "Transitive settlement clustering should preserve all three members");
        context.succeed();
    }

    @GameTest
    public void sharedSettlementStorePreservesStableIdentityAcrossReloadAndDrift(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-settlement-store-test");
        var file = dir.resolve("settlements.properties");
        var members = java.util.List.of(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000031"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000032"));

        SharedSettlementStore first = new SharedSettlementStore(file);
        var created = first.reconcile(java.util.List.of(
                new SharedSettlementPolicy.Candidate(
                        "minecraft:overworld", 40.0, 65.0, 40.0, members)));
        context.assertTrue(created.size() == 1,
                "First shared settlement observation should allocate a stable ID");
        String id = created.get(0).id();

        SharedSettlementStore reloaded = new SharedSettlementStore(file);
        var recreated = reloaded.reconcile(java.util.List.of(
                new SharedSettlementPolicy.Candidate(
                        "minecraft:overworld", 52.0, 65.0, 48.0, members)));
        context.assertTrue(recreated.isEmpty(),
                "Small settlement centroid drift should reuse the persisted identity");
        context.assertTrue(reloaded.settlements().size() == 1
                        && reloaded.settlements().get(0).id().equals(id),
                "Settlement identity must survive reload and modest spatial drift");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void worldIdentityPersistsAcrossReload(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-world-identity-test");
        var file = dir.resolve("world.properties");

        CanvasWorldIdentityStore first = new CanvasWorldIdentityStore(file);
        String id = first.worldId();
        CanvasWorldIdentityStore reloaded = new CanvasWorldIdentityStore(file);

        context.assertTrue(!id.isBlank(),
                "World identity must never be blank");
        context.assertTrue(id.equals(reloaded.worldId()),
                "World identity must remain stable across reload");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void worldMemoryScopesPreventCrossWorldLeakage(GameTestHelper context) {
        String localHome = "minecraft:overworld|10|64|20";
        String worldA = "11111111-1111-1111-1111-111111111111";
        String worldB = "22222222-2222-2222-2222-222222222222";

        String aHome = WorldMemoryScopePolicy.scope(worldA, localHome);
        String bHome = WorldMemoryScopePolicy.scope(worldB, localHome);
        context.assertFalse(aHome.equals(bHome),
                "Identical HOME coordinates in different worlds must not share memory");

        String aMob = WorldMemoryScopePolicy.mobPrefix(worldA)
                + "00000000-0000-0000-0000-000000000001.ticks";
        String bMob = WorldMemoryScopePolicy.mobPrefix(worldB)
                + "00000000-0000-0000-0000-000000000001.ticks";
        context.assertFalse(aMob.equals(bMob),
                "Identical mob UUIDs in different worlds must not share familiarity");

        context.assertTrue(WorldMemoryScopePolicy.scope("", localHome).isBlank(),
                "Memory must fail closed until a stable world identity is known");
        context.succeed();
    }

    @GameTest
    public void communityGatheringRequiresCoLocatedSettlementMembers(GameTestHelper context) {
        var a = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101");
        var b = java.util.UUID.fromString("00000000-0000-0000-0000-000000000102");
        var settlement = new SharedSettlementPolicy.Candidate(
                "minecraft:overworld", 20.0, 64.0, 20.0, java.util.List.of(a, b));

        var gathering = SharedGatheringPolicy.detect(
                settlement,
                java.util.List.of(
                        new SharedGatheringPolicy.PlayerPresence(a, "minecraft:overworld", 18.0, 64.0, 20.0),
                        new SharedGatheringPolicy.PlayerPresence(b, "minecraft:overworld", 24.0, 64.0, 21.0)));

        context.assertTrue(gathering != null && gathering.participants().size() == 2,
                "Two nearby members of the same settlement should form a community gathering");
        context.succeed();
    }

    @GameTest
    public void communityGatheringRejectsDistantOrForeignPlayers(GameTestHelper context) {
        var a = java.util.UUID.fromString("00000000-0000-0000-0000-000000000111");
        var b = java.util.UUID.fromString("00000000-0000-0000-0000-000000000112");
        var outsider = java.util.UUID.fromString("00000000-0000-0000-0000-000000000113");
        var settlement = new SharedSettlementPolicy.Candidate(
                "minecraft:overworld", 0.0, 64.0, 0.0, java.util.List.of(a, b));

        var gathering = SharedGatheringPolicy.detect(
                settlement,
                java.util.List.of(
                        new SharedGatheringPolicy.PlayerPresence(a, "minecraft:overworld", 0.0, 64.0, 0.0),
                        new SharedGatheringPolicy.PlayerPresence(b, "minecraft:overworld", 40.0, 64.0, 0.0),
                        new SharedGatheringPolicy.PlayerPresence(outsider, "minecraft:overworld", 1.0, 64.0, 1.0)));

        context.assertTrue(gathering == null,
                "Distant members and non-members must not manufacture a shared gathering");
        context.succeed();
    }

    @GameTest
    public void sharedWorldMemoryPersistsObservationsWithoutOverwritingPersonalMemory(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-shared-world-memory-test");
        var file = dir.resolve("shared.properties");

        SharedWorldMemoryStore first = new SharedWorldMemoryStore(file);
        first.noteSettlement("canvas-settlement-7");
        first.noteGathering("canvas-settlement-7", 1200L);
        first.noteGathering("canvas-settlement-7", 2400L);

        SharedWorldMemoryStore reloaded = new SharedWorldMemoryStore(file);
        var snapshot = reloaded.snapshot("canvas-settlement-7");

        context.assertTrue(snapshot.recognizedSettlements() == 1,
                "Shared memory should remember the settlement as an observed fact");
        context.assertTrue(snapshot.communityGatherings() == 2,
                "Shared gathering history should accumulate rather than replace player memory");
        context.assertTrue(snapshot.lastGatheringTick() == 2400L,
                "Latest shared gathering tick must survive reload");
        context.assertTrue(reloaded.snapshot("unknown").communityGatherings() == 0,
                "Unknown settlements must remain unknown rather than inheriting another settlement's history");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void p3MultiplayerScenarioCampaignCoversSettlementIsolationGatheringAndSharedMemory(GameTestHelper context) throws Exception {
        var a = java.util.UUID.fromString("00000000-0000-0000-0000-000000000201");
        var b = java.util.UUID.fromString("00000000-0000-0000-0000-000000000202");
        var homes = java.util.List.of(
                new SharedSettlementPolicy.HomeAnchor(a, "minecraft:overworld", 0.0, 64.0, 0.0),
                new SharedSettlementPolicy.HomeAnchor(b, "minecraft:overworld", 48.0, 64.0, 16.0));
        var online = java.util.List.of(
                new SharedGatheringPolicy.PlayerPresence(a, "minecraft:overworld", 22.0, 64.0, 8.0),
                new SharedGatheringPolicy.PlayerPresence(b, "minecraft:overworld", 28.0, 64.0, 9.0));

        var result = P3MultiplayerScenarioPolicy.evaluate(
                homes,
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                "minecraft:overworld|24|64|8",
                online);
        context.assertTrue(result.passed(),
                "P3 multiplayer campaign must prove shared settlement, cross-world isolation, and gathering interpretation");

        var dir = Files.createTempDirectory("canvas-p3-multiplayer-campaign");
        var file = dir.resolve("shared-memory.properties");
        SharedWorldMemoryStore first = new SharedWorldMemoryStore(file);
        first.noteSettlement("canvas-settlement-campaign");
        first.noteGathering("canvas-settlement-campaign", 3600L);
        SharedWorldMemoryStore reloaded = new SharedWorldMemoryStore(file);
        var memory = reloaded.snapshot("canvas-settlement-campaign");
        context.assertTrue(memory.recognizedSettlements() == 1
                        && memory.communityGatherings() == 1
                        && memory.lastGatheringTick() == 3600L,
                "P3 multiplayer campaign must prove shared observational history survives reload");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void featureConfigDefaultsEveryExperienceFamilyOn(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-feature-config-defaults");
        var file = dir.resolve("canvas-features.properties");
        CanvasFeatureConfig config = CanvasFeatureConfig.load(file);

        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            context.assertTrue(config.enabled(family),
                    "Canvas feature family should default on: " + family);
        }

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void featureConfigCanDisableEveryExperienceFamily(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-feature-config-disable");
        var file = dir.resolve("canvas-features.properties");
        java.util.Properties properties = new java.util.Properties();
        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            properties.setProperty(
                    "family." + family.name().toLowerCase(java.util.Locale.ROOT),
                    "false");
        }
        try (var out = Files.newOutputStream(file)) {
            properties.store(out, "Canvas test");
        }

        CanvasFeatureConfig config = CanvasFeatureConfig.load(file);
        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            context.assertFalse(config.enabled(family),
                    "Canvas feature family should respect explicit off: " + family);
        }

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void featureDisableReenableRoundTripPreservesExistingWorldMemory(GameTestHelper context) throws Exception {
        var dir = Files.createTempDirectory("canvas-feature-reversibility");
        var configFile = dir.resolve("canvas-features.properties");
        var memoryFile = dir.resolve("world-memory.properties");

        CanvasWorldMemoryStore memory = new CanvasWorldMemoryStore(memoryFile);
        memory.bind("world|test|minecraft:overworld|10|64|10");
        memory.noteHomecoming();
        memory.noteFamiliarMoment();
        var before = memory.snapshot();

        java.util.Properties off = new java.util.Properties();
        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            off.setProperty("family." + family.name().toLowerCase(java.util.Locale.ROOT), "false");
        }
        try (var out = Files.newOutputStream(configFile)) {
            off.store(out, "off");
        }
        CanvasFeatureConfig disabled = CanvasFeatureConfig.load(configFile);
        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            context.assertFalse(disabled.enabled(family),
                    "Every family should be disabled during reversibility test: " + family);
        }

        java.util.Properties on = new java.util.Properties();
        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            on.setProperty("family." + family.name().toLowerCase(java.util.Locale.ROOT), "true");
        }
        try (var out = Files.newOutputStream(configFile)) {
            on.store(out, "on");
        }
        CanvasFeatureConfig reenabled = CanvasFeatureConfig.load(configFile);
        for (CanvasFeatureConfig.Family family : CanvasFeatureConfig.Family.values()) {
            context.assertTrue(reenabled.enabled(family),
                    "Every family should re-enable cleanly: " + family);
        }

        CanvasWorldMemoryStore reloaded = new CanvasWorldMemoryStore(memoryFile);
        reloaded.bind("world|test|minecraft:overworld|10|64|10");
        var after = reloaded.snapshot();
        context.assertTrue(before.equals(after),
                "Disabling and re-enabling Canvas families must not erase or rewrite persisted world memory");

        Files.deleteIfExists(configFile);
        Files.deleteIfExists(memoryFile);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void longSessionSoakKeepsDensityBudgetsBounded(GameTestHelper context) {
        LongSessionSoakPolicy.Result result =
                LongSessionSoakPolicy.run(LongSessionSoakPolicy.DEFAULT_DAYS);

        context.assertTrue(result.passed(),
                "Thirty-day deterministic soak must preserve presentation/music ceilings: " + result);
        context.assertTrue(result.days() == LongSessionSoakPolicy.DEFAULT_DAYS,
                "Soak must cover the configured long-session horizon");
        context.assertTrue(result.accepted() < result.attempts(),
                "Nothing Happens guardrails must continue suppressing excess moments during long sessions");
        context.succeed();
    }

    @GameTest
    public void saveReloadBackupAndForkIdentityRemainSafe(GameTestHelper context) throws Exception {
        var root = Files.createTempDirectory("canvas-lineage-safety");
        var originalDir = root.resolve("WorldA").resolve("data");
        var originalFile = originalDir.resolve("canvas-world-identity-v1.properties");

        CanvasWorldIdentityStore original =
                new CanvasWorldIdentityStore(originalFile, "WorldA");
        String worldId = original.worldId();
        String branchId = original.branchId();
        String scopeId = original.scopeId();

        CanvasWorldIdentityStore reloaded =
                new CanvasWorldIdentityStore(originalFile, "WorldA");
        context.assertTrue(scopeId.equals(reloaded.scopeId()),
                "Ordinary save/reload must preserve the complete branch scope");

        var backupDir = root.resolve("BackupRestore").resolve("data");
        Files.createDirectories(backupDir);
        var backupFile = backupDir.resolve("canvas-world-identity-v1.properties");
        Files.copy(originalFile, backupFile);
        CanvasWorldIdentityStore restoredBackup =
                new CanvasWorldIdentityStore(backupFile, "WorldA");
        context.assertTrue(scopeId.equals(restoredBackup.scopeId()),
                "A backup restored under the same save key must retain continuity");

        var forkDir = root.resolve("WorldB").resolve("data");
        Files.createDirectories(forkDir);
        var forkFile = forkDir.resolve("canvas-world-identity-v1.properties");
        Files.copy(originalFile, forkFile);
        CanvasWorldIdentityStore fork =
                new CanvasWorldIdentityStore(forkFile, "WorldB");
        context.assertTrue(worldId.equals(fork.worldId()),
                "A fork should retain world lineage");
        context.assertFalse(branchId.equals(fork.branchId()),
                "A fork under a different save key must receive a new branch identity");
        context.assertFalse(scopeId.equals(fork.scopeId()),
                "Forked worlds must not share client-memory scope");

        var legacyDir = root.resolve("Legacy").resolve("data");
        Files.createDirectories(legacyDir);
        var legacyFile = legacyDir.resolve("canvas-world-identity-v1.properties");
        java.util.Properties legacy = new java.util.Properties();
        legacy.setProperty("worldId", worldId);
        try (var out = Files.newOutputStream(legacyFile)) {
            legacy.store(out, "legacy v1");
        }
        CanvasWorldIdentityStore migrated =
                new CanvasWorldIdentityStore(legacyFile, "Legacy");
        context.assertTrue(worldId.equals(migrated.worldId()),
                "Legacy v1 identity migration must preserve world lineage");
        context.assertTrue(!migrated.branchId().isBlank(),
                "Legacy v1 identity migration must allocate a branch identity");

        Files.walk(root)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (java.io.IOException ignored) { }
                });
        context.succeed();
    }

    @GameTest
    public void lowEndPerformanceBudgetIsExplicitAndBounded(GameTestHelper context) {
        context.assertTrue(LowEndPerformanceBudgetPolicy.withinBudget(),
                "Canvas low-end budget must stay within all declared operation-rate ceilings");
        context.assertTrue(
                ObservationBudgetPolicy.equivalentBlockProbesPerSecond()
                        <= ObservationBudgetPolicy.MAX_EQUIVALENT_BLOCK_PROBES_PER_SECOND,
                "Loaded-only block observation must remain under its low-end probe ceiling");
        context.assertTrue(
                LowEndPerformanceBudgetPolicy.broadEntityQueriesPerSecond()
                        <= LowEndPerformanceBudgetPolicy.MAX_BROAD_ENTITY_QUERIES_PER_SECOND,
                "Broad client entity queries must remain under the low-end cadence ceiling");
        context.assertTrue(
                LowEndPerformanceBudgetPolicy.VILLAGE_MAX_CLUSTER_COMPARISONS <= 4096,
                "Village clustering work must remain hard-bounded");
        context.succeed();
    }

    @GameTest
    public void travellingDimensionsKeepTheirOwnSoundscape(GameTestHelper context) {
        var dawn = TravelAtmospherePolicy.choose("minecraft:overworld", 1000L, false);
        context.assertTrue("presence.harbor_air".equals(dawn.event())
                        && dawn.audible() && dawn.volume() <= 0.03f,
                "Overworld dawn away from home should be subtle and warm");

        var dusk = TravelAtmospherePolicy.choose("minecraft:overworld", 12000L, false);
        context.assertTrue("presence.harbor_air".equals(dusk.event())
                        && dusk.pitch() < dawn.pitch(),
                "Overworld dusk should have a distinct quieter pitch");

        var shelteredNight = TravelAtmospherePolicy.choose(
                "minecraft:overworld", 18000L, true);
        context.assertTrue("presence.void_stillness".equals(shelteredNight.event()),
                "An Overworld night shelter may have restrained stillness");

        var openDay = TravelAtmospherePolicy.choose("minecraft:overworld", 6000L, false);
        context.assertFalse(openDay.audible(),
                "An open daytime Overworld must leave enough silence for vanilla");

        var end = TravelAtmospherePolicy.choose("minecraft:the_end", 1000L, false);
        context.assertTrue("presence.void_stillness".equals(end.event())
                        && end.volume() < shelteredNight.volume(),
                "End travel should have faint dimension-appropriate stillness");

        var nether = TravelAtmospherePolicy.choose("minecraft:the_nether", 1000L, true);
        context.assertFalse(nether.audible(),
                "Nether must never borrow Overworld morning or End stillness");

        var modded = TravelAtmospherePolicy.choose("anothermod:dreamscape", 18000L, true);
        context.assertFalse(modded.audible(),
                "Unknown dimensions retain companion and vanilla ambience");

        context.assertTrue(
                TravelAtmospherePolicy.choose("minecraft:overworld", -23000L, false)
                        .event().equals(dawn.event()),
                "Time wrap must not alter the Overworld dawn presentation");
        context.succeed();
    }

    @GameTest
    public void seasonalHomecomingsAreEarnedAndRespectStormAndSilence(GameTestHelper context) {
        var familiar = HomecomingPolicy.compose(
                true, VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.CLEAR, 2);
        var unknown = SeasonalHomecomingPolicy.adapt(familiar, SeasonPolicy.Season.UNKNOWN, 2, 3);
        context.assertTrue(unknown.equals(familiar),
                "Unknown season must preserve ordinary homecoming exactly");

        var firstVisit = SeasonalHomecomingPolicy.adapt(familiar, SeasonPolicy.Season.WINTER, 0, 2);
        context.assertTrue(firstVisit.equals(familiar),
                "Seasonal music is earned through recurring visits, not first arrival");

        var winter = SeasonalHomecomingPolicy.adapt(familiar, SeasonPolicy.Season.WINTER, 2, 1);
        var spring = SeasonalHomecomingPolicy.adapt(familiar, SeasonPolicy.Season.SPRING, 2, 1);
        context.assertTrue("music.season_home_shift".equals(winter.musicEvent()),
                "Recognized, repeatedly visited HOME should acquire restrained seasonal music");
        context.assertTrue(winter.cuePitch() < familiar.cuePitch()
                        && spring.cuePitch() > familiar.cuePitch(),
                "Winter and spring should color the same earned homecoming differently");
        context.assertTrue(winter.cueEvent().equals(familiar.cueEvent())
                        && winter.cueVolume() == familiar.cueVolume(),
                "Season must not raise cue loudness or override earned homecoming identity");

        var unchangedSeason = SeasonalHomecomingPolicy.adapt(
                familiar, SeasonPolicy.Season.WINTER, 2, 0);
        context.assertTrue(unchangedSeason.musicEvent().equals(familiar.musicEvent()),
                "Before seasonal history is observed, retain normal homecoming music");

        var storm = HomecomingPolicy.compose(
                true, VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.THUNDER_SHELTERED, 3);
        var winterStorm = SeasonalHomecomingPolicy.adapt(
                storm, SeasonPolicy.Season.WINTER, 3, 3);
        context.assertTrue("music.coming_home_storm".equals(winterStorm.musicEvent()),
                "Seasonal accents must never displace authored shelter-in-thunder music");

        var quiet = HomecomingPolicy.compose(
                false, VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.CLEAR, 3);
        context.assertTrue(SeasonalHomecomingPolicy.adapt(
                        quiet, SeasonPolicy.Season.WINTER, 3, 3).equals(quiet),
                "Quiet HOME must remain quiet: no seasonal musical reward without context");
        context.succeed();
    }

    @GameTest
    public void villageMusicRequiresSustainedLoadedPresence(GameTestHelper context) {
        VillageContinuityPolicy policy = new VillageContinuityPolicy();
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.WAKE) == VillageLifePolicy.Rhythm.NONE,
                "A passing morning villager crowd must not create a village moment");
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.WAKE) == VillageLifePolicy.Rhythm.NONE,
                "Two samples are still insufficient for sustained village identity");
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.WAKE) == VillageLifePolicy.Rhythm.WAKE,
                "Repeated loaded village presence should earn a morning presentation");

        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.GATHERING) == VillageLifePolicy.Rhythm.WAKE,
                "A single fleeting gathering must not replace an established village rhythm");
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.WIND_DOWN) == VillageLifePolicy.Rhythm.WAKE,
                "A contradicted gathering must not mature");
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.WIND_DOWN) == VillageLifePolicy.Rhythm.WAKE,
                "Changing rhythm needs multiple consistent observations");
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.WIND_DOWN) == VillageLifePolicy.Rhythm.WIND_DOWN,
                "Sustained evening presence should create a genuine wind-down");

        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.NONE) == VillageLifePolicy.Rhythm.NONE,
                "Villagers leaving loaded range must stop the previous village identity immediately");
        context.assertTrue(policy.observe(VillageLifePolicy.Rhythm.GATHERING) == VillageLifePolicy.Rhythm.NONE,
                "A reconnect or absent village cannot inherit former presentation history");
        policy.reset();
        context.assertTrue(policy.observe(null) == VillageLifePolicy.Rhythm.NONE,
                "Unknown observations must fail closed into silence");
        context.succeed();
    }

    @GameTest
    public void longKnownVanillaMobsEarnGentlerGreetings(GameTestHelper context) {
        var stranger = FamiliarBondPolicy.greeting(0, false, SeasonPolicy.Season.SUMMER);
        context.assertTrue(stranger.bond() == FamiliarBondPolicy.Bond.UNKNOWN
                        && !stranger.showText() && stranger.volume() == 0.0f,
                "Unknown mobs must not receive fabricated familiarity presentations");

        var newlyFamiliar = FamiliarBondPolicy.greeting(
                FamiliarityPolicy.REQUIRED_OBSERVATION_TICKS, false, SeasonPolicy.Season.SPRING);
        context.assertTrue(newlyFamiliar.bond() == FamiliarBondPolicy.Bond.RECOGNIZED
                        && newlyFamiliar.showText(),
                "A deliberately observed mob should receive its first familiar greeting");

        var longKnown = FamiliarBondPolicy.greeting(
                FamiliarBondPolicy.OLD_FRIEND_TICKS, false, SeasonPolicy.Season.WINTER);
        context.assertTrue(longKnown.bond() == FamiliarBondPolicy.Bond.OLD_FRIEND
                        && longKnown.showText()
                        && longKnown.volume() < newlyFamiliar.volume()
                        && longKnown.pitchMultiplier() < 1.0f,
                "Old friends should feel quieter and earned, not add loud repeated effects");

        var repeat = FamiliarBondPolicy.greeting(
                FamiliarBondPolicy.OLD_FRIEND_TICKS, true, SeasonPolicy.Season.WINTER);
        context.assertFalse(repeat.showText(),
                "Repeated glances in a session must not repeatedly post chat messages");
        context.assertTrue(repeat.volume() == longKnown.volume(),
                "Suppressing repeat text must retain the same low-key musical identity");

        var unknownSeason = FamiliarBondPolicy.greeting(
                FamiliarBondPolicy.OLD_FRIEND_TICKS, true, SeasonPolicy.Season.UNKNOWN);
        context.assertTrue(unknownSeason.bond() == FamiliarBondPolicy.Bond.OLD_FRIEND
                        && unknownSeason.volume() <= 0.28f,
                "Familiar vanilla mobs must remain recognizable without any season mod");
        context.succeed();
    }

    @GameTest
    public void familiarComfortRequiresVanillaNonhostileEntity(GameTestHelper context) {
        context.assertTrue(FamiliarBondPolicy.eligibleVanillaSubject("minecraft:cat", false),
                "A harmless vanilla cat should be able to become familiar");
        context.assertTrue(FamiliarBondPolicy.eligibleVanillaSubject("minecraft:wolf", false),
                "A harmless vanilla wolf should be eligible for earned familiarity");
        context.assertFalse(FamiliarBondPolicy.eligibleVanillaSubject("minecraft:creeper", true),
                "Hostile mobs must never provide comforting home or old-friend cues");
        context.assertFalse(FamiliarBondPolicy.eligibleVanillaSubject("mod:unknown_creature", false),
                "Do not invent a familiarity classification for an unreviewed modded mob");
        context.assertFalse(FamiliarBondPolicy.eligibleVanillaSubject(null, false),
                "Unknown registry identity must fail closed");
        context.succeed();
    }

    @GameTest
    public void rareExplorationMemoriesStayInsideTheirOwnWorldBranch(GameTestHelper context)
            throws Exception {
        var dir = Files.createTempDirectory("canvas-scoped-wonder-test");
        var file = dir.resolve("wonders.properties");
        String landmark = "VIEWPOINT|minecraft:overworld|11,8,11";
        String first = WorldMemoryScopePolicy.scope("world-one|branch-a", landmark);
        String fork = WorldMemoryScopePolicy.scope("world-one|branch-b", landmark);
        String other = WorldMemoryScopePolicy.scope("world-two|branch-a", landmark);

        context.assertTrue(!first.isBlank() && !fork.isBlank() && !other.isBlank()
                        && !first.equals(fork) && !first.equals(other),
                "Same landmark coordinates must have distinct branch-qualified identities");
        context.assertTrue(WorldMemoryScopePolicy.scope("", landmark).isBlank(),
                "An unknown world identity must not create a writeable wonder key");

        ExplorationWonderMemoryStore store = new ExplorationWonderMemoryStore(file);
        store.note(first, 42L);
        store.note(first, 43L);
        store.note(fork, 8L);
        // The legacy unscoped record is never treated as belonging to an
        // arbitrary new branch; guessing would leak privacy across worlds.
        store.note(landmark, 3L);
        ExplorationWonderMemoryStore reloaded = new ExplorationWonderMemoryStore(file);
        context.assertTrue(reloaded.snapshot(first).lastWorldDay() == 43L
                        && reloaded.snapshot(first).moments() == 2,
                "Actual world-scoped wonder history must survive save/reload");
        context.assertTrue(reloaded.snapshot(fork).moments() == 1
                        && reloaded.snapshot(fork).lastWorldDay() == 8L,
                "A copied world with a different branch must have separate wonder history");
        context.assertTrue(reloaded.snapshot(other).moments() == 0,
                "Unrelated worlds cannot inherit matching landmark cooldowns");
        context.assertTrue(reloaded.snapshot("").moments() == 0,
                "Before world identity arrives, exploring must not read global wonder history");

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        context.succeed();
    }

    @GameTest
    public void homeAmbienceCrossfadeNeverStacksOldLoops(GameTestHelper context) {
        context.assertTrue(AmbienceHandoffPolicy.oldestLoopsToRetire(0) == 0,
                "A scene with no fading ambience must remain silent except its current sound");
        context.assertTrue(AmbienceHandoffPolicy.oldestLoopsToRetire(1) == 0,
                "One prior ambience may finish a restrained crossfade");
        context.assertTrue(AmbienceHandoffPolicy.oldestLoopsToRetire(2) == 1,
                "Two old loops require immediately retiring the oldest");
        context.assertTrue(AmbienceHandoffPolicy.oldestLoopsToRetire(20) == 19,
                "Rapid repeated phase or rain changes may not stack twenty audible loops");
        context.succeed();
    }

    @GameTest
    public void genuinelyLongTripsEarnQuietDistinctHomecomings(GameTestHelper context) {
        var established = HomecomingPolicy.compose(
                true, VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.CLEAR, 3);
        long sufficientTime = LongJourneyHomecomingPolicy.MIN_AWAY_TICKS;
        double farEnough = LongJourneyHomecomingPolicy.MIN_FURTHEST_DISTANCE_SQ;

        context.assertFalse(LongJourneyHomecomingPolicy.isLongJourney(
                sufficientTime * 3L, 54.0 * 54.0, false),
                "AFK time near the doorstep cannot pretend to be distant exploration");
        context.assertFalse(LongJourneyHomecomingPolicy.isLongJourney(
                sufficientTime - 1, farEnough * 4, true),
                "Rapid portal hops or a short excursion must retain ordinary homecoming");
        context.assertTrue(LongJourneyHomecomingPolicy.isLongJourney(
                sufficientTime, farEnough, false),
                "Loaded position evidence of a genuine long walk must count");
        context.assertTrue(LongJourneyHomecomingPolicy.isLongJourney(
                sufficientTime, 0.0, true),
                "A sustained journey through another dimension qualifies without overworld distance");
        context.assertFalse(LongJourneyHomecomingPolicy.isLongJourney(
                sufficientTime, Double.NaN, false),
                "Unknown travel distance must never be treated as evidence");

        var ordinary = LongJourneyHomecomingPolicy.adapt(
                established, sufficientTime * 3, 54.0 * 54.0, false, 3);
        context.assertTrue(ordinary.equals(established),
                "Circling the house cannot manufacture a special homecoming");

        var unearned = LongJourneyHomecomingPolicy.adapt(
                established, sufficientTime, farEnough, false, 0);
        context.assertTrue(unearned.equals(established),
                "First-time visitors cannot receive an earned long-term welcome");

        var earned = LongJourneyHomecomingPolicy.adapt(
                established, sufficientTime, farEnough, false, 3);
        context.assertTrue(earned.cueEvent().equals(established.cueEvent())
                        && earned.musicEvent().equals(established.musicEvent()),
                "A longer journey must reuse existing earned sound assets and music budget");
        context.assertTrue(earned.cueVolume() < established.cueVolume()
                        && earned.cuePitch() < established.cuePitch()
                        && earned.pulseTicks() > established.pulseTicks(),
                "A genuine long trip receives a softer, slightly longer welcome");

        var quiet = HomecomingPolicy.compose(
                false, VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.CLEAR, 5);
        context.assertTrue(LongJourneyHomecomingPolicy.adapt(
                        quiet, sufficientTime, farEnough, false, 5).equals(quiet),
                "Unknown, quiet homes should not invent a familiar welcome");

        var storm = HomecomingPolicy.compose(
                true, VillageLifePolicy.Rhythm.NONE,
                WeatherCharacterPolicy.Character.THUNDER_SHELTERED, 5);
        context.assertTrue(LongJourneyHomecomingPolicy.adapt(
                        storm, sufficientTime, farEnough, false, 5).equals(storm),
                "Shelter in thunder keeps its existing distinctive storm treatment");
        context.succeed();
    }

    @GameTest
    public void villagesFeelNewAfterActualSustainedRain(GameTestHelper context) {
        VillageAfterRainPolicy isolated = new VillageAfterRainPolicy();
        context.assertFalse(isolated.observe(true, VillageLifePolicy.Rhythm.NONE, 100),
                "Rain outside an actual loaded village is not a village memory");
        context.assertFalse(isolated.observe(false, VillageLifePolicy.Rhythm.ACTIVE, 140),
                "Simply arriving as rain ends cannot fabricate sustained village history");

        VillageAfterRainPolicy village = new VillageAfterRainPolicy();
        context.assertFalse(village.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 100),
                "A first rainy village sample is observation, not a surprise");
        context.assertFalse(village.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 140),
                "Two rainy samples are insufficient");
        context.assertFalse(village.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 180),
                "The rain must actually stop before an event is eligible");
        context.assertTrue(village.observe(false, VillageLifePolicy.Rhythm.ACTIVE, 220),
                "A real village observed during rain and clearing earns a restrained cue");
        context.assertFalse(village.observe(false, VillageLifePolicy.Rhythm.ACTIVE, 260),
                "Stable dry weather must not replay the same transition");
        context.assertFalse(village.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 300),
                "Further rain starts never trigger an after-rain cue");
        context.assertFalse(village.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 340),
                "A second storm still needs sustained evidence");
        context.assertFalse(village.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 380),
                "Rain itself must stay silent");
        context.assertFalse(village.observe(false, VillageLifePolicy.Rhythm.ACTIVE, 420),
                "Second storm within cooldown cannot spam village atmosphere");
        village.reset();
        context.assertFalse(village.observe(false, VillageLifePolicy.Rhythm.ACTIVE, 500),
                "Changing worlds cannot leak rain history into another village");

        VillageAfterRainPolicy departed = new VillageAfterRainPolicy();
        departed.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 100);
        departed.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 140);
        departed.observe(true, VillageLifePolicy.Rhythm.ACTIVE, 180);
        context.assertFalse(departed.observe(true, VillageLifePolicy.Rhythm.NONE, 220),
                "Leaving loaded village range must clear its rainy evidence");
        context.assertFalse(departed.observe(false, VillageLifePolicy.Rhythm.ACTIVE, 260),
                "Returning after rain ended cannot fabricate an observed clearing");
        context.succeed();
    }

    @GameTest
    public void seasonTransitionMustBelongToTheCurrentObservedWorld(GameTestHelper context) {
        SeasonalPresentationPolicy transitions = new SeasonalPresentationPolicy();
        context.assertFalse(transitions.observe(SeasonPolicy.Season.WINTER),
                "First season seen in a newly joined world must not trigger music");
        context.assertFalse(transitions.observe(SeasonPolicy.Season.WINTER),
                "An unchanged winter must remain quiet");
        context.assertTrue(transitions.observe(SeasonPolicy.Season.SPRING),
                "A genuine observed winter-to-spring transition should earn a moment");
        transitions.reset();
        context.assertFalse(transitions.observe(SeasonPolicy.Season.AUTUMN),
                "After disconnect, another world's autumn is not a spring-to-autumn event");
        context.assertFalse(transitions.observe(SeasonPolicy.Season.UNKNOWN),
                "Unknown provider responses cannot create fabricated transitions");
        context.assertFalse(transitions.observe(SeasonPolicy.Season.AUTUMN),
                "Unknown provider observation must not clear a genuine known season");
        context.assertTrue(transitions.observe(SeasonPolicy.Season.WINTER),
                "A genuine autumn-to-winter change within the new world should work");
        transitions.reset();
        transitions.seed(SeasonPolicy.Season.SUMMER);
        context.assertFalse(transitions.observe(SeasonPolicy.Season.SUMMER),
                "Returning home after observing summer away must not fake a season shift");
        context.assertTrue(transitions.observe(SeasonPolicy.Season.AUTUMN),
                "A real summer-to-autumn transition after a known observation is valid");
        context.succeed();
    }

    @GameTest
    public void familiarGreetingMilestonesStayQuietAcrossReconnects(GameTestHelper context) {
        int fresh = 0;
        context.assertFalse(FamiliarGreetingHistoryPolicy.shouldAnnounce(
                FamiliarBondPolicy.Bond.UNKNOWN, fresh),
                "Unknown animals must not be announced");
        context.assertTrue(FamiliarGreetingHistoryPolicy.shouldAnnounce(
                FamiliarBondPolicy.Bond.RECOGNIZED, fresh),
                "First genuine recognition should still feel rewarding");
        int recognized = FamiliarGreetingHistoryPolicy.acknowledge(
                FamiliarBondPolicy.Bond.RECOGNIZED, fresh);
        context.assertTrue(recognized == 1,
                "Recognition acknowledgement must be persisted as one simple tier");
        context.assertFalse(FamiliarGreetingHistoryPolicy.shouldAnnounce(
                FamiliarBondPolicy.Bond.RECOGNIZED, recognized),
                "A reconnect must not cause repetitive familiar-face narration");
        context.assertTrue(FamiliarGreetingHistoryPolicy.shouldAnnounce(
                FamiliarBondPolicy.Bond.OLD_FRIEND, recognized),
                "An established animal deserves its one genuinely new old-friend greeting");
        int oldFriend = FamiliarGreetingHistoryPolicy.acknowledge(
                FamiliarBondPolicy.Bond.OLD_FRIEND, recognized);
        context.assertTrue(oldFriend == 2,
                "Old-friend relationship milestone should persist");
        context.assertFalse(FamiliarGreetingHistoryPolicy.shouldAnnounce(
                FamiliarBondPolicy.Bond.OLD_FRIEND, oldFriend),
                "Repeating old-friend text on every reconnection creates spam");
        context.assertTrue(FamiliarGreetingHistoryPolicy.acknowledge(
                FamiliarBondPolicy.Bond.RECOGNIZED, oldFriend) == 2,
                "Older observations must never downgrade acknowledged relationship history");
        context.assertFalse(FamiliarGreetingHistoryPolicy.shouldAnnounce(
                null, oldFriend),
                "Unknown relationship states must fail closed");
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
