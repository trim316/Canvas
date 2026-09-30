package net.canvasmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class CanvasSettlementRuntime {
    private static final int SAMPLE_INTERVAL = 100;
    private static final long GATHERING_COOLDOWN_TICKS = 20L * 60L * 5L;

    private final CanvasHomeRuntime home;
    private SharedSettlementStore store;
    private SharedWorldMemoryStore sharedMemory;
    private Path evidenceFile;
    private final Set<String> activeGatherings = new HashSet<>();
    private final Map<String, Long> lastGatheringTick = new HashMap<>();
    private long tick;

    CanvasSettlementRuntime(CanvasHomeRuntime home) {
        this.home = home;
    }

    void onServerStarting(MinecraftServer server) {
        tick = 0L;
        activeGatherings.clear();
        lastGatheringTick.clear();
        Path root = server.getWorldPath(LevelResource.ROOT);
        store = new SharedSettlementStore(
                root.resolve("data").resolve("canvas-settlements-v1.properties"));
        sharedMemory = new SharedWorldMemoryStore(
                root.resolve("data").resolve("canvas-shared-memory-v1.properties"));
        evidenceFile = root.resolve("canvas-runtime-evidence").resolve("shared-settlements.log");
        evidence("session_start", "known=" + store.settlements().size());
    }

    void onServerTick(MinecraftServer server) {
        tick++;
        if (tick % SAMPLE_INTERVAL != 0L || store == null) return;

        var candidates = SharedSettlementPolicy.detect(home.recognizedHomes());
        var created = store.reconcile(candidates);
        for (var settlement : created) {
            sharedMemory.noteSettlement(settlement.id());
            evidence("settlement_recognized",
                    "tick=" + tick
                            + ",id=" + settlement.id()
                            + ",dimension=" + settlement.dimension()
                            + ",members=" + settlement.memberCount()
                            + ",x=" + Math.round(settlement.x())
                            + ",y=" + Math.round(settlement.y())
                            + ",z=" + Math.round(settlement.z()));
        }
        observeGatherings(server, candidates);
    }

    private void observeGatherings(
            MinecraftServer server,
            List<SharedSettlementPolicy.Candidate> settlements) {
        List<SharedGatheringPolicy.PlayerPresence> online = new ArrayList<>();
        Map<UUID, ServerPlayer> players = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            players.put(id, player);
            online.add(new SharedGatheringPolicy.PlayerPresence(
                    id,
                    player.level().dimension().identifier().toString(),
                    player.getX(),
                    player.getY(),
                    player.getZ()));
        }

        Set<String> nowActive = new HashSet<>();
        for (SharedSettlementPolicy.Candidate settlement : settlements) {
            SharedGatheringPolicy.Gathering gathering =
                    SharedGatheringPolicy.detect(settlement, online);
            if (gathering == null) continue;
            nowActive.add(gathering.key());
            if (activeGatherings.contains(gathering.key())) continue;

            long previous = lastGatheringTick.getOrDefault(gathering.key(), Long.MIN_VALUE / 4L);
            if (tick - previous < GATHERING_COOLDOWN_TICKS) continue;

            CommunityGatheringPayload payload =
                    new CommunityGatheringPayload(gathering.participants().size());
            for (UUID participant : gathering.participants()) {
                ServerPlayer player = players.get(participant);
                if (player != null) ServerPlayNetworking.send(player, payload);
            }
            lastGatheringTick.put(gathering.key(), tick);
            SharedSettlementStore.Settlement stableSettlement = store.match(settlement);
            if (stableSettlement != null) {
                sharedMemory.noteGathering(stableSettlement.id(), tick);
            }
            evidence("community_gathering",
                    "tick=" + tick
                            + ",participants=" + gathering.participants().size()
                            + ",dimension=" + gathering.dimension()
                            + ",x=" + Math.round(gathering.x())
                            + ",y=" + Math.round(gathering.y())
                            + ",z=" + Math.round(gathering.z()));
        }

        activeGatherings.clear();
        activeGatherings.addAll(nowActive);
    }

    void save() {
        if (store != null) store.save();
        if (sharedMemory != null) sharedMemory.save();
    }

    void onServerStopped() {
        save();
        if (store != null) {
            evidence("session_stop", "known=" + store.settlements().size());
        }
        store = null;
        sharedMemory = null;
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
