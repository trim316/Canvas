package net.canvasmod.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.canvasmod.FamiliarityPolicy;
import net.canvasmod.FamiliarBondPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.LowEndPerformanceBudgetPolicy;
import net.canvasmod.WorldMemoryScopePolicy;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.EntityHitResult;

final class CanvasFamiliarityClient {
    private static final int SAVE_INTERVAL_TICKS = 600;
    private static final int MAX_REMEMBERED_MOBS = 512;
    private static final int MAX_OBSERVED_TICKS = 20 * 60 * 60;

    private final CanvasFeelClient feel;
    private final CanvasExperienceDirector director;
    private final Map<UUID, Integer> familiarity = new HashMap<>();
    private final Map<UUID, Long> lastCue = new HashMap<>();
    private final java.util.Set<UUID> greetedThisSession = new java.util.HashSet<>();
    private final Path stateFile = FabricLoader.getInstance().getConfigDir()
            .resolve("canvas-familiarity-v1.properties");
    private long tick;
    private String worldIdentity = "";

    CanvasFamiliarityClient(CanvasFeelClient feel, CanvasExperienceDirector director) {
        this.feel = feel;
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            if (tick % FamiliarityPolicy.SAMPLE_INTERVAL_TICKS == 0) sample(client);
            cueTargetedFamiliarMob(client);
            if (tick % SAVE_INTERVAL_TICKS == 0) save();
        });
    }

    void acceptWorldIdentity(String worldId) {
        String next = worldId == null ? "" : worldId.trim();
        if (next.equals(worldIdentity)) return;
        if (!worldIdentity.isBlank()) save();
        worldIdentity = next;
        familiarity.clear();
        lastCue.clear();
        greetedThisSession.clear();
        director.setFamiliarNearby(false);
        if (!worldIdentity.isBlank()) load();
    }

    private void sample(Minecraft client) {
        if (worldIdentity.isBlank() || client.level == null || client.player == null) {
            director.setFamiliarNearby(false);
            return;
        }

        var box = client.player.getBoundingBox().inflate(FamiliarityPolicy.OBSERVATION_RADIUS);
        boolean familiarNearby = false;
        int processed = 0;

        for (Entity entity : client.level.getEntities(client.player, box, e -> e instanceof Mob)) {
            if (processed++ >= LowEndPerformanceBudgetPolicy.FAMILIARITY_MAX_MOBS_PER_SAMPLE) break;
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            if (!FamiliarBondPolicy.eligibleVanillaSubject(
                    BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
                    mob.getType().getCategory() == MobCategory.MONSTER)) continue;
            int observed = familiarity.merge(
                    mob.getUUID(),
                    FamiliarityPolicy.SAMPLE_INTERVAL_TICKS,
                    (oldValue, increment) -> Math.min(MAX_OBSERVED_TICKS, oldValue + increment));
            if (observed >= FamiliarityPolicy.REQUIRED_OBSERVATION_TICKS) familiarNearby = true;
        }

        director.setFamiliarNearby(familiarNearby);
        prune();
    }

    private void cueTargetedFamiliarMob(Minecraft client) {
        if (client.player == null || !(client.hitResult instanceof EntityHitResult hit)) return;
        Entity entity = hit.getEntity();
        if (!(entity instanceof Mob mob) || !mob.isAlive()) return;
        if (!FamiliarBondPolicy.eligibleVanillaSubject(
                BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
                mob.getType().getCategory() == MobCategory.MONSTER)) return;

        UUID id = mob.getUUID();
        int observed = familiarity.getOrDefault(id, 0);
        long previous = lastCue.getOrDefault(id, Long.MIN_VALUE / 4);
        long sinceLast = tick - previous;

        if (!FamiliarityPolicy.cueEligible(observed, true, sinceLast)) return;
        if (!director.allowMoment(MomentDensityPolicy.Kind.FAMILIAR_FACE, tick, false)) return;

        FamiliarBondPolicy.Greeting greeting = FamiliarBondPolicy.greeting(
                observed, greetedThisSession.contains(id), director.season());
        if (greeting.bond() == FamiliarBondPolicy.Bond.UNKNOWN) return;
        if (greeting.showText()) {
            client.player.sendSystemMessage(Component.literal(
                    greeting.bond() == FamiliarBondPolicy.Bond.OLD_FRIEND
                            ? "Canvas · An old friend" : "Canvas · A familiar face"));
        }
        feel.presentFamiliarFace(
                greeting.volume(), greeting.pitchMultiplier(), greeting.pulseArgb());
        director.noteFamiliarMoment();
        greetedThisSession.add(id);
        lastCue.put(id, tick);
        save();
    }

    private void prune() {
        if (familiarity.size() <= MAX_REMEMBERED_MOBS) return;
        ArrayList<Map.Entry<UUID, Integer>> entries = new ArrayList<>(familiarity.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        int remove = familiarity.size() - MAX_REMEMBERED_MOBS;
        for (int i = 0; i < remove; i++) {
            UUID id = entries.get(i).getKey();
            familiarity.remove(id);
            lastCue.remove(id);
            greetedThisSession.remove(id);
        }
    }

    private void load() {
        if (worldIdentity.isBlank() || !Files.exists(stateFile)) return;
        Properties properties = new Properties();
        try (var in = Files.newInputStream(stateFile)) {
            properties.load(in);
        } catch (IOException ignored) {
            return;
        }

        String prefix = WorldMemoryScopePolicy.mobPrefix(worldIdentity);
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(prefix) || !key.endsWith(".ticks")) continue;
            String rawId = key.substring(prefix.length(), key.length() - ".ticks".length());
            try {
                UUID id = UUID.fromString(rawId);
                int observed = Integer.parseInt(properties.getProperty(key, "0"));
                if (observed > 0) familiarity.put(id, Math.min(MAX_OBSERVED_TICKS, observed));
            } catch (IllegalArgumentException ignored) { }
        }
        prune();
    }

    private void save() {
        if (worldIdentity.isBlank()) return;
        Properties properties = new Properties();
        if (Files.exists(stateFile)) {
            try (var in = Files.newInputStream(stateFile)) {
                properties.load(in);
            } catch (IOException ignored) { }
        }

        String prefix = WorldMemoryScopePolicy.mobPrefix(worldIdentity);
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefix))
                .toList()
                .forEach(properties::remove);
        for (var entry : familiarity.entrySet()) {
            properties.setProperty(
                    prefix + entry.getKey() + ".ticks",
                    Integer.toString(entry.getValue()));
        }

        try {
            Files.createDirectories(stateFile.getParent());
            Path tmp = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas persistent world-scoped all-mob familiarity");
            }
            try {
                Files.move(tmp, stateFile,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, stateFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) { }
    }
}
