package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

final class CanvasHomeRuntime {
    private static final int SAMPLE_INTERVAL = 100;
    private static final int REQUIRED_GOOD_SAMPLES = 12;
    private static final int RADIUS = 7;
    private static final double CANDIDATE_RADIUS_SQ = 14.0 * 14.0;

    private final Map<UUID, State> states = new HashMap<>();
    private Path file;
    private long tick;

    void onServerStarting(MinecraftServer server) {
        states.clear();
        tick = 0;
        file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("canvas-home-v1.properties");
        load();
    }

    void onServerTick(MinecraftServer server) {
        tick++;
        if (tick % SAMPLE_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            observe(player);
        }
    }

    void onServerStopped() {
        save();
        states.clear();
        tick = 0;
    }

    private void observe(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos center = player.blockPosition();
        if (level.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4) == null) return;

        State state = states.computeIfAbsent(player.getUUID(), ignored -> new State());
        if (state.homeDimension != null) return;

        Evidence evidence = scan(level, center);
        if (!evidence.qualifies()) {
            state.resetCandidate();
            return;
        }

        String dimension = level.dimension().identifier().toString();
        if (!dimension.equals(state.candidateDimension)
                || state.goodSamples == 0
                || distanceSq(player.getX(), player.getY(), player.getZ(),
                    state.candidateX, state.candidateY, state.candidateZ) > CANDIDATE_RADIUS_SQ) {
            state.candidateDimension = dimension;
            state.candidateX = player.getX();
            state.candidateY = player.getY();
            state.candidateZ = player.getZ();
            state.goodSamples = 1;
            state.bestScore = evidence.score();
            return;
        }

        state.goodSamples++;
        state.bestScore = Math.max(state.bestScore, evidence.score());
        if (state.goodSamples < REQUIRED_GOOD_SAMPLES) return;

        state.homeDimension = dimension;
        state.homeX = (int)Math.floor(state.candidateX);
        state.homeY = (int)Math.floor(state.candidateY);
        state.homeZ = (int)Math.floor(state.candidateZ);
        state.homeScore = state.bestScore;
        player.sendSystemMessage(Component.literal(
            "Canvas · Home recognized (" + evidence.summary() + ")"));
        save();
    }

    private static Evidence scan(ServerLevel level, BlockPos center) {
        boolean sheltered = !level.canSeeSky(center.above());
        int beds = 0;
        int storage = 0;
        int work = 0;
        int comfort = 0;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos pos = new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
                    String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString()
                        .toLowerCase(Locale.ROOT);

                    if (id.endsWith("_bed")) beds++;
                    if (containsAny(id, "chest", "barrel", "shulker_box")) storage++;
                    if (containsAny(id, "crafting_table", "furnace", "smoker", "blast_furnace",
                            "stonecutter", "anvil", "loom", "cartography_table", "smithing_table",
                            "grindstone", "brewing_stand", "enchanting_table")) work++;
                    if (containsAny(id, "bookshelf", "lantern", "campfire", "flower_pot", "carpet")) comfort++;
                }
            }
        }
        return new Evidence(sheltered, beds, storage, work, comfort);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static double distanceSq(double ax,double ay,double az,double bx,double by,double bz) {
        double dx=ax-bx, dy=ay-by, dz=az-bz;
        return dx*dx + dy*dy + dz*dz;
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
            try (var out = Files.newOutputStream(file)) {
                p.store(out, "Canvas semantic home memory");
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

    private record Evidence(boolean sheltered, int beds, int storage, int work, int comfort) {
        int score() {
            int s = sheltered ? 2 : 0;
            if (beds > 0) s += 4;
            if (storage > 0) s += 2;
            if (work > 0) s += 2;
            if (comfort >= 2) s += 1;
            return s;
        }

        boolean qualifies() {
            return sheltered && beds > 0 && (storage > 0 || work > 0) && score() >= 8;
        }

        String summary() {
            return "bed=" + beds + ", storage=" + storage + ", work=" + work + ", shelter=" + sheltered;
        }
    }

    private static final class State {
        String candidateDimension;
        double candidateX;
        double candidateY;
        double candidateZ;
        int goodSamples;
        int bestScore;
        String homeDimension;
        int homeX;
        int homeY;
        int homeZ;
        int homeScore;

        void resetCandidate() {
            candidateDimension = null;
            goodSamples = 0;
            bestScore = 0;
        }
    }
}
