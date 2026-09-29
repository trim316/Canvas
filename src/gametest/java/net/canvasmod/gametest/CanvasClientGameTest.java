package net.canvasmod.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

@SuppressWarnings({"UnstableApiUsage", "try"})
public final class CanvasClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitTicks(20);
            context.takeScreenshot("canvas-before-feel");
            context.waitTicks(80);
            context.takeScreenshot("canvas-after-feel");
            context.waitTicks(100);
            context.takeScreenshot("canvas-rare-surprise");
        }
    }
}
