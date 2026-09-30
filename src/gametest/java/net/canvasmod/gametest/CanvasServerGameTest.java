package net.canvasmod.gametest;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.EnumSet;
import net.canvasmod.CanvasFeelProfile;
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
import net.canvasmod.VillageLifePolicy;
import net.canvasmod.WeatherCharacterPolicy;
import net.canvasmod.FamiliarityPolicy;
import net.canvasmod.HomeEvidenceDetector;
import net.canvasmod.HomecomingPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.RareSurprisePolicy;
import net.canvasmod.SeasonPolicy;
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

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
