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
        Path name = root.getFileName();
        String saveKey = name == null ? "default" : name.toString();
        store = new CanvasWorldIdentityStore(
                root.resolve("data").resolve("canvas-world-identity-v1.properties"),
                saveKey);
    }

    void syncPlayer(ServerPlayer player) {
        if (store == null) return;
        ServerPlayNetworking.send(player, new WorldIdentityPayload(store.scopeId()));
    }

    String worldId() {
        return store == null ? "" : store.worldId();
    }

    String branchId() {
        return store == null ? "" : store.branchId();
    }

    String scopeId() {
        return store == null ? "" : store.scopeId();
    }
}
