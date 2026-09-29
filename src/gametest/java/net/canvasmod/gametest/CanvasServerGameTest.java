package net.canvasmod.gametest;

import java.lang.reflect.Method;
import net.canvasmod.FamiliarityPolicy;
import net.canvasmod.HomeEvidenceDetector;
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

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
