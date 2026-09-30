package net.canvasmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

final class CanvasHomeRuntime {
    private static final int SAMPLE_INTERVAL = 100;
    private static final int REQUIRED_GOOD_SAMPLES = HomeRecognitionAccumulator.DEFAULT_REQUIRED_GOOD_SAMPLES;

    private final Map<UUID, State> states = new HashMap<>();
    private Path file;
    private Path evidenceFile;
    private long tick;

    void onServerStarting(MinecraftServer server) {
        states.clear();
        tick = 0;
        Path root = server.getWorldPath(LevelResource.ROOT);
        file = root.resolve("data").resolve("canvas-home-v2.properties");
        evidenceFile = root.resolve("canvas-runtime-evidence").resolve("home-recognition.log");
        load();
        evidence("session_start", "-", "home_schema=v2");
    }

    void onServerTick(MinecraftServer server) {
        tick++;
        if (tick % SAMPLE_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) observe(player);
    }

    void onServerStopped() {
        save();
        evidence("session_stop", "-", "players=" + states.size());
        states.clear();
        tick = 0;
    }

    void syncPlayer(ServerPlayer player) {
        State state = states.get(player.getUUID());
        if (state == null || state.homeDimension == null) return;
        sendHome(player, state);
        evidence("home_sync", player.getUUID().toString(),
                "tick=" + tick + ",dimension=" + state.homeDimension + ",score=" + state.homeScore);
    }

    private void observe(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        if (level.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4) == null) return;

        State state = states.computeIfAbsent(player.getUUID(), ignored -> new State());
        if (state.homeDimension != null) return;

        HomeEvidencePolicy.Evidence homeEvidence = HomeEvidenceDetector.scan(level, center);
        String dimension = level.dimension().identifier().toString();
        boolean recognized = state.accumulator.observe(
                dimension,
                player.getX(),
                player.getY(),
                player.getZ(),
                homeEvidence,
                REQUIRED_GOOD_SAMPLES);

        if (!homeEvidence.qualifies() || state.accumulator.goodSamples() == 1) {
            evidence("home_sample", player.getUUID().toString(),
                    "tick=" + tick + "," + homeEvidence.summary()
                    + ",goodSamples=" + state.accumulator.goodSamples());
        }

        if (!recognized) return;

        state.homeDimension = state.accumulator.dimension();
        state.homeX = (int)Math.floor(state.accumulator.x());
        state.homeY = (int)Math.floor(state.accumulator.y());
        state.homeZ = (int)Math.floor(state.accumulator.z());
        state.homeScore = state.accumulator.bestScore();

        String summary = homeEvidence.summary();
        evidence("home_recognized", player.getUUID().toString(),
                "tick=" + tick + "," + summary);
        player.sendSystemMessage(Component.literal("Canvas · Home recognized"));
        sendHome(player, state);
        save();
    }

    private void sendHome(ServerPlayer player, State state) {
        ServerPlayNetworking.send(player, new HomeStatePayload(
                state.homeDimension,
                new BlockPos(state.homeX, state.homeY, state.homeZ),
                state.homeScore));
    }

    List<SharedSettlementPolicy.HomeAnchor> recognizedHomes() {
        List<SharedSettlementPolicy.HomeAnchor> result = new ArrayList<>();
        for (var entry : states.entrySet()) {
            State state = entry.getValue();
            if (state.homeDimension == null) continue;
            result.add(new SharedSettlementPolicy.HomeAnchor(
                    entry.getKey(),
                    state.homeDimension,
                    state.homeX + 0.5,
                    state.homeY + 0.5,
                    state.homeZ + 0.5));
        }
        return List.copyOf(result);
    }

    void save() {
        if (file == null) return;
        Properties p = new Properties();
        for (var entry : states.entrySet()) {
            State s = entry.getValue();
            if (s.homeDimension == null) continue;
            String k = entry.getKey().toString();
            p.setProperty(k + ".dimension", s.homeDimension);
            p.setProperty(k + ".x", Integer.toString(s.homeX));
            p.setProperty(k + ".y", Integer.toString(s.homeY));
            p.setProperty(k + ".z", Integer.toString(s.homeZ));
            p.setProperty(k + ".score", Integer.toString(s.homeScore));
        }

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                p.store(out, "Canvas semantic home memory v2");
            }
            try {
                Files.move(tmp, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) { }
    }

    private void load() {
        if (file == null || !Files.exists(file)) return;
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException ignored) {
            return;
        }

        for (String key : p.stringPropertyNames()) {
            if (!key.endsWith(".dimension")) continue;
            String prefix = key.substring(0, key.length() - ".dimension".length());
            try {
                UUID id = UUID.fromString(prefix);
                State s = states.computeIfAbsent(id, ignored -> new State());
                s.homeDimension = p.getProperty(prefix + ".dimension");
                s.homeX = Integer.parseInt(p.getProperty(prefix + ".x", "0"));
                s.homeY = Integer.parseInt(p.getProperty(prefix + ".y", "64"));
                s.homeZ = Integer.parseInt(p.getProperty(prefix + ".z", "0"));
                s.homeScore = Integer.parseInt(p.getProperty(prefix + ".score", "0"));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void evidence(String type, String player, String detail) {
        if (evidenceFile == null) return;
        String line = Instant.now() + " type=" + type + " player=" + player + " " + detail
                + System.lineSeparator();
        try {
            Files.createDirectories(evidenceFile.getParent());
            Files.writeString(evidenceFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }

    private static final class State {
        final HomeRecognitionAccumulator accumulator = new HomeRecognitionAccumulator();
        String homeDimension;
        int homeX;
        int homeY;
        int homeZ;
        int homeScore;
    }
}
