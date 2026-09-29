package net.canvasmod.client;

import net.fabricmc.api.ClientModInitializer;

public final class CanvasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new CanvasFamiliarityClient().register();
        new CanvasFeelClient().register();
    }
}
