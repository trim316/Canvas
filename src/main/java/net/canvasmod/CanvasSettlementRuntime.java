package net.canvasmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class CanvasSettlementRuntime {
    private static final int SAMPLE_INTERVAL = 100;

    private final CanvasHomeRuntime home;
    private SharedSettlementStore store;
    private Path evidenceFile;
    private long tick;

    CanvasSettlementRuntime(CanvasHomeRuntime home) {
        this.home = home;
    }

    void onServerStarting(MinecraftServer server) {
        tick = 0L;
        Path root = server.getWorldPath(LevelResource.ROOT);
        store = new SharedSettlementStore(
                root.resolve("data").resolve("canvas-settlements-v1.properties"));
        evidenceFile = root.resolve("canvas-runtime-evidence").resolve("shared-settlements.log");
        evidence("session_start", "known=" + store.settlements().size());
    }

    void onServerTick(MinecraftServer server) {
        tick++;
        if (tick % SAMPLE_INTERVAL != 0L || store == null) return;

        var candidates = SharedSettlementPolicy.detect(home.recognizedHomes());
        var created = store.reconcile(candidates);
        for (var settlement : created) {
            evidence("settlement_recognized",
                    "tick=" + tick
                            + ",id=" + settlement.id()
                            + ",dimension=" + settlement.dimension()
                            + ",members=" + settlement.memberCount()
                            + ",x=" + Math.round(settlement.x())
                            + ",y=" + Math.round(settlement.y())
                            + ",z=" + Math.round(settlement.z()));
        }
    }

    void save() {
        if (store != null) store.save();
    }

    void onServerStopped() {
        save();
        if (store != null) {
            evidence("session_stop", "known=" + store.settlements().size());
        }
        store = null;
        tick = 0L;
    }

    private void evidence(String type, String detail) {
        if (evidenceFile == null) return;
        String line = Instant.now() + " type=" + type + " " + detail + System.lineSeparator();
        try {
            Files.createDirectories(evidenceFile.getParent());
            Files.writeString(evidenceFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }
}
