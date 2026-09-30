package net.canvasmod;

import java.nio.file.Path;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

final class CanvasWorldIdentityRuntime {
    private CanvasWorldIdentityStore store;

    void onServerStarting(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        store = new CanvasWorldIdentityStore(
                root.resolve("data").resolve("canvas-world-identity-v1.properties"));
    }

    void syncPlayer(ServerPlayer player) {
        if (store == null) return;
        ServerPlayNetworking.send(player, new WorldIdentityPayload(store.worldId()));
    }

    String worldId() {
        return store == null ? "" : store.worldId();
    }
}
