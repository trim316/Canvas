package net.canvasmod.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;

final class CanvasFamiliarityClient {
    private static final int SAMPLE_INTERVAL = 20;
    private static final int REQUIRED_SEEN_TICKS = 20 * 45;
    private static final int CUE_COOLDOWN = 20 * 60 * 10;

    private final Map<UUID, Integer> familiarity = new HashMap<>();
    private final Map<UUID, Long> lastCue = new HashMap<>();
    private long tick;

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            if (tick % SAMPLE_INTERVAL == 0) sample(client);
            cueTargetedFamiliarMob(client);
        });
    }

    private void sample(Minecraft client) {
        if (client.level == null || client.player == null) return;
        var box = client.player.getBoundingBox().inflate(16.0);
        for (Entity entity : client.level.getEntities(client.player, box, e -> e instanceof Mob)) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            familiarity.merge(mob.getUUID(), SAMPLE_INTERVAL, Integer::sum);
        }
    }

    private void cueTargetedFamiliarMob(Minecraft client) {
        if (client.player == null || !(client.hitResult instanceof EntityHitResult hit)) return;
        Entity entity = hit.getEntity();
        if (!(entity instanceof Mob mob) || !mob.isAlive()) return;

        UUID id = mob.getUUID();
        if (familiarity.getOrDefault(id, 0) < REQUIRED_SEEN_TICKS) return;
        long previous = lastCue.getOrDefault(id, Long.MIN_VALUE / 4);
        if (tick - previous < CUE_COOLDOWN) return;

        client.player.sendSystemMessage(Component.literal("Canvas · A familiar face"));
        lastCue.put(id, tick);
    }
}
