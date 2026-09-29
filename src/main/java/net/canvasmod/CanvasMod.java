package net.canvasmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class CanvasMod implements ModInitializer {
    private final CanvasHomeRuntime home = new CanvasHomeRuntime();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(home::onServerStarting);
        ServerTickEvents.END_SERVER_TICK.register(home::onServerTick);
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> home.save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> home.onServerStopped());
    }
}
