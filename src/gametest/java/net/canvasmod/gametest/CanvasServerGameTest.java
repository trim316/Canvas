package net.canvasmod.gametest;

import java.lang.reflect.Method;
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

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
