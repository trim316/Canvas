package net.canvasmod.client;

import java.util.ArrayList;
import java.util.List;
import net.canvasmod.ContextualMusicPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.VillageLifePolicy;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

final class CanvasVillageLifeClient {
    private static final double OBSERVATION_RADIUS = 28.0;
    private static final double CLUSTER_RADIUS_SQ = 8.0 * 8.0;
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasExperienceDirector director;
    private long tick;
    private long lastMusicMoment = Long.MIN_VALUE / 4L;
    private VillageLifePolicy.Rhythm rhythm = VillageLifePolicy.Rhythm.NONE;
    private int presentationTicks;
    private boolean ciAnnounced;

    CanvasVillageLifeClient(CanvasExperienceDirector director) {
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (tick % 20L == 0L) update();
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("canvas", "village_rhythm"),
                (graphics, tickCounter) -> render(graphics));
    }

    private void update() {
        if (client.level == null || client.player == null) {
            rhythm = VillageLifePolicy.Rhythm.NONE;
            director.setVillageRhythm(rhythm);
            presentationTicks = 0;
            return;
        }

        VillageLifePolicy.Rhythm next;
        if (CI_VISUAL_TEST && tick >= 100L && tick < 180L) {
            next = VillageLifePolicy.Rhythm.GATHERING;
            if (!ciAnnounced) {
                System.out.println("CANVAS_CI_VILLAGE_RHYTHM_ACTIVE");
                ciAnnounced = true;
            }
        } else {
            List<Entity> villagers = loadedVillagers();
            int clustered = maxCluster(villagers);
            boolean storming = client.level.isRaining() || client.level.isThundering();
            next = VillageLifePolicy.classify(
                    villagers.size(),
                    clustered,
                    client.level.getOverworldClockTime(),
                    storming);
        }

        long sinceLast = tick - lastMusicMoment;
        if (ContextualMusicPolicy.shouldPresent(rhythm, next, sinceLast)) {
            String event = ContextualMusicPolicy.eventFor(next);
            if (!event.isBlank()
                    && director.allowMoment(MomentDensityPolicy.Kind.VILLAGE_RHYTHM, tick, true)) {
                playMusicMoment(event, musicVolume(next), musicPitch(next));
                presentationTicks = next == VillageLifePolicy.Rhythm.GATHERING ? 120 : 80;
                lastMusicMoment = tick;
            }
        }

        rhythm = next;
        director.setVillageRhythm(rhythm);
        if (presentationTicks > 0) presentationTicks = Math.max(0, presentationTicks - 20);
    }

    private List<Entity> loadedVillagers() {
        var box = client.player.getBoundingBox().inflate(OBSERVATION_RADIUS);
        List<Entity> villagers = new ArrayList<>();
        for (Entity entity : client.level.getEntities(client.player, box, e -> e instanceof Mob)) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            if (mob.getClass().getSimpleName().equals("Villager")) villagers.add(mob);
        }
        return villagers;
    }

    private static int maxCluster(List<Entity> villagers) {
        int best = 0;
        for (Entity anchor : villagers) {
            int count = 0;
            for (Entity other : villagers) {
                if (anchor.distanceToSqr(other) <= CLUSTER_RADIUS_SQ) count++;
            }
            best = Math.max(best, count);
        }
        return best;
    }

    private static float musicVolume(VillageLifePolicy.Rhythm value) {
        return switch (value) {
            case WAKE -> 0.20f;
            case WIND_DOWN -> 0.18f;
            case GATHERING -> 0.23f;
            default -> 0.0f;
        };
    }

    private static float musicPitch(VillageLifePolicy.Rhythm value) {
        return switch (value) {
            case WAKE -> 1.04f;
            case WIND_DOWN -> 0.96f;
            case GATHERING -> 1.0f;
            default -> 1.0f;
        };
    }

    private void playMusicMoment(String eventPath, float volume, float pitch) {
        client.getSoundManager().play(new CanvasMusicMoment(eventPath, volume, pitch));
    }

    private void render(GuiGraphicsExtractor graphics) {
        if (presentationTicks <= 0 || client.player == null) return;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int band = Math.max(2, Math.min(width, height) / 90);
        int color = switch (rhythm) {
            case WAKE -> 0x38FFE4A6;
            case WIND_DOWN -> 0x38FFB26B;
            case GATHERING -> 0x44FFD28A;
            default -> 0;
        };

        if (color == 0) return;
        graphics.fill(0, 0, width, band, color);
        graphics.fill(0, height - band, width, height, color);
    }

    private static final class CanvasMusicMoment extends AbstractTickableSoundInstance {
        private int age;

        CanvasMusicMoment(String eventPath, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", eventPath)),
                    SoundSource.MUSIC,
                    RandomSource.create());
            this.volume = volume;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.looping = false;
        }

        @Override
        public void tick() {
            age++;
            if (age >= 20 * 18) stop();
        }
    }
}
