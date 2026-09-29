package net.canvasmod.gametest;

import java.lang.reflect.Method;
import net.canvasmod.FamiliarityPolicy;
import net.canvasmod.HomeEvidencePolicy;
import net.canvasmod.HomeRecognitionAccumulator;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
