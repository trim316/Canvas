package net.canvasmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class CanvasMod implements ModInitializer {
    private final CanvasHomeRuntime home = new CanvasHomeRuntime();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(HomeStatePayload.TYPE, HomeStatePayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(home::onServerStarting);
        ServerTickEvents.END_SERVER_TICK.register(home::onServerTick);
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> home.save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> home.onServerStopped());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                home.syncPlayer(handler.getPlayer()));
    }
}
