package net.canvasmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class CanvasMod implements ModInitializer {
    private final CanvasHomeRuntime home = new CanvasHomeRuntime();
    private final CanvasPlaceRuntime places = new CanvasPlaceRuntime();
    private final CanvasSettlementRuntime settlements = new CanvasSettlementRuntime(home);
    private final CanvasWorldIdentityRuntime worldIdentity = new CanvasWorldIdentityRuntime();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(HomeStatePayload.TYPE, HomeStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlaceStatePayload.TYPE, PlaceStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LandmarkStatePayload.TYPE, LandmarkStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WorldIdentityPayload.TYPE, WorldIdentityPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CommunityGatheringPayload.TYPE, CommunityGatheringPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            home.onServerStarting(server);
            places.onServerStarting(server);
            settlements.onServerStarting(server);
            worldIdentity.onServerStarting(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            home.onServerTick(server);
            places.onServerTick(server);
            settlements.onServerTick(server);
        });
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
            home.save();
            places.save();
            settlements.save();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            home.onServerStopped();
            places.onServerStopped();
            settlements.onServerStopped();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            worldIdentity.syncPlayer(handler.getPlayer());
            home.syncPlayer(handler.getPlayer());
        });
    }
}
