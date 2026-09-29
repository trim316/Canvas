package net.canvasmod.client;

import java.util.Locale;
import net.canvasmod.HomeEvidencePolicy;
import net.canvasmod.HomeRecognitionAccumulator;
import net.canvasmod.HomeStatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class CanvasFeelClient {
    private static final int SAMPLE_INTERVAL = 100;
    private static final double HOME_RADIUS_SQ = 22.0 * 22.0;
    private static final double AWAY_RADIUS_SQ = 54.0 * 54.0;
    private static final int MIN_AWAY_TICKS = 600;
    private static final int RETURN_COOLDOWN = 2400;
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final HomeRecognitionAccumulator accumulator = new HomeRecognitionAccumulator();

    private long tick;
    private boolean hasHome;
    private String homeDimension = "";
    private int homeX;
    private int homeY;
    private int homeZ;
    private long awaySince = -1;
    private long lastReturnCue = Long.MIN_VALUE / 4;
    private CanvasLoopingSound ambience;
    private String ambienceKey = "";
    private boolean atHome;
    private boolean ciVisualAnnounced;

    void acceptServerHome(HomeStatePayload payload) {
        hasHome = true;
        homeDimension = payload.dimension();
        homeX = payload.pos().getX();
        homeY = payload.pos().getY();
        homeZ = payload.pos().getZ();
        accumulator.reset();
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (tick % 20 == 0) update();
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("canvas", "atmosphere"),
                (graphics, tickCounter) -> renderOverlay(graphics));
    }

    private void update() {
        if (client.level == null || client.player == null) {
            atHome = false;
            stopAmbience(30);
            return;
        }

        BlockPos pos = client.player.blockPosition();
        if (client.level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return;

        String dimension = client.level.dimension().identifier().toString();

        if (CI_VISUAL_TEST && tick >= 60) {
            if (!hasHome) {
                hasHome = true;
                homeDimension = dimension;
                homeX = pos.getX();
                homeY = pos.getY();
                homeZ = pos.getZ();
            }
            if (!ciVisualAnnounced) {
                System.out.println("CANVAS_CI_VISUAL_ACTIVE");
                ciVisualAnnounced = true;
            }
        } else if (!hasHome && tick % SAMPLE_INTERVAL == 0) {
            learnHome(dimension, pos);
        }

        atHome = hasHome
                && homeDimension.equals(dimension)
                && distanceSq(client.player.getX(), client.player.getY(), client.player.getZ(),
                    homeX + 0.5, homeY + 0.5, homeZ + 0.5) <= HOME_RADIUS_SQ;

        observeReturn(dimension);

        long dayTime = client.level.getOverworldClockTime() % 24000L;
        boolean evening = dayTime >= 11500L && dayTime < 14000L;
        boolean night = dayTime >= 13000L && dayTime < 22500L;
        boolean morning = dayTime >= 22500L || dayTime < 1700L;
        boolean sheltered = !client.level.canSeeSky(pos.above());

        if (atHome && sheltered) {
            setAmbience("presence.hearth_air", night ? 0.22f : 0.18f, night ? 0.97f : 1.0f);
        } else if (atHome) {
            setAmbience("presence.hearth_air", 0.10f, 1.01f);
        } else if (night && sheltered) {
            setAmbience("presence.void_stillness", 0.05f, 0.985f);
        } else if (morning || evening) {
            setAmbience("presence.harbor_air", 0.035f, morning ? 1.015f : 0.985f);
        } else {
            stopAmbience(35);
            ambienceKey = "";
        }
    }

    private void learnHome(String dimension, BlockPos center) {
        HomeEvidencePolicy.Evidence evidence = scanClient(center);
        int requiredSamples = Integer.getInteger(
                "canvas.home.requiredSamples",
                HomeRecognitionAccumulator.DEFAULT_REQUIRED_GOOD_SAMPLES);

        boolean recognized = accumulator.observe(
                dimension,
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                evidence,
                Math.max(1, requiredSamples));

        if (!recognized) return;

        hasHome = true;
        homeDimension = accumulator.dimension();
        homeX = (int)Math.floor(accumulator.x());
        homeY = (int)Math.floor(accumulator.y());
        homeZ = (int)Math.floor(accumulator.z());
        client.player.sendSystemMessage(Component.literal("Canvas · Home recognized"));
        playCue("feel.coming_home", 0.32f, 1.0f);
    }

    private boolean isSheltered(BlockPos center) {
        if (!client.level.canSeeSky(center.above())) return true;
        for (int dy = 1; dy <= 8; dy++) {
            BlockPos above = center.above(dy);
            if (client.level.getChunkSource().getChunkNow(above.getX() >> 4, above.getZ() >> 4) == null) break;
            if (!client.level.getBlockState(above).isAir()) return true;
        }
        return false;
    }

    private HomeEvidencePolicy.Evidence scanClient(BlockPos center) {
        int beds = 0;
        int storage = 0;
        int work = 0;
        int comfort = 0;
        boolean sheltered = isSheltered(center);

        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos p = new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (client.level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) == null) continue;

                    String id = BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(p).getBlock())
                            .toString().toLowerCase(Locale.ROOT);
                    if (HomeEvidencePolicy.isBed(id)) beds++;
                    if (HomeEvidencePolicy.isStorage(id)) storage++;
                    if (HomeEvidencePolicy.isWork(id)) work++;
                    if (HomeEvidencePolicy.isComfort(id)) comfort++;
                }
            }
        }

        return new HomeEvidencePolicy.Evidence(sheltered, beds, storage, work, comfort);
    }

    private void observeReturn(String dimension) {
        if (!hasHome || client.player == null) return;

        if (!homeDimension.equals(dimension)) {
            if (awaySince < 0) awaySince = tick;
            return;
        }

        double distance = distanceSq(
                client.player.getX(), client.player.getY(), client.player.getZ(),
                homeX + 0.5, homeY + 0.5, homeZ + 0.5);

        if (distance >= AWAY_RADIUS_SQ) {
            if (awaySince < 0) awaySince = tick;
            return;
        }

        if (atHome && awaySince >= 0) {
            if (tick - awaySince >= MIN_AWAY_TICKS && tick - lastReturnCue >= RETURN_COOLDOWN) {
                playCue("feel.coming_home", 0.45f, 1.0f);
                lastReturnCue = tick;
            }
            awaySince = -1;
        }
    }

    private void renderOverlay(GuiGraphicsExtractor graphics) {
        if (client.level == null || client.player == null) return;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        long dayTime = client.level.getOverworldClockTime() % 24000L;

        int wash = 0;
        int edge = 0;
        if (atHome) {
            if (dayTime >= 13000L && dayTime < 22500L) {
                wash = 0x12F3A85A;
                edge = 0x22FFB35C;
            } else if (dayTime >= 11500L && dayTime < 14000L) {
                wash = 0x10FFC77A;
                edge = 0x1EFFB15D;
            } else {
                wash = 0x0CFFE0A5;
                edge = 0x18FFC77A;
            }
        } else if (dayTime >= 11500L && dayTime < 14000L) {
            wash = 0x060E2740;
        } else if (dayTime >= 13000L && dayTime < 22500L) {
            wash = 0x07040B18;
        }

        if (wash != 0) graphics.fill(0, 0, width, height, wash);
        if (edge != 0) {
            int band = Math.max(6, Math.min(width, height) / 28);
            graphics.fill(0, 0, width, band, edge);
            graphics.fill(0, height - band, width, height, edge);
            graphics.fill(0, band, band, height - band, edge);
            graphics.fill(width - band, band, width, height - band, edge);
        }
    }

    private void setAmbience(String key, float volume, float pitch) {
        if (ambience != null && !ambience.isStopped() && ambienceKey.equals(key)) {
            ambience.setTarget(volume, pitch);
            return;
        }

        stopAmbience(25);
        ambience = new CanvasLoopingSound(key, volume, pitch);
        ambienceKey = key;
        client.getSoundManager().play(ambience);
    }

    private void stopAmbience(int fadeTicks) {
        if (ambience != null && !ambience.isStopped()) ambience.fadeOut(fadeTicks);
    }

    private void playCue(String eventPath, float volume, float pitch) {
        client.getSoundManager().play(new CanvasOneShotSound(eventPath, volume, pitch));
    }

    private static double distanceSq(double ax,double ay,double az,double bx,double by,double bz) {
        double dx=ax-bx, dy=ay-by, dz=az-bz;
        return dx*dx + dy*dy + dz*dz;
    }

    private static final class CanvasOneShotSound extends AbstractTickableSoundInstance {
        private int age;

        CanvasOneShotSound(String eventPath, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", eventPath)),
                    SoundSource.AMBIENT,
                    RandomSource.create());
            this.volume = volume;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            age++;
            if (age >= 120) stop();
        }
    }

    private static final class CanvasLoopingSound extends AbstractTickableSoundInstance {
        private float targetVolume;
        private float targetPitch;
        private int fadeRemaining;

        CanvasLoopingSound(String eventPath, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", eventPath)),
                    SoundSource.AMBIENT,
                    RandomSource.create());
            this.targetVolume = volume;
            this.targetPitch = pitch;
            this.volume = 0.0f;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.looping = true;
        }

        void setTarget(float volume, float pitch) {
            targetVolume = Math.max(0.0f, Math.min(0.30f, volume));
            targetPitch = Math.max(0.8f, Math.min(1.2f, pitch));
        }

        void fadeOut(int ticks) {
            targetVolume = 0.0f;
            fadeRemaining = Math.max(1, ticks);
        }

        @Override
        public void tick() {
            pitch += (targetPitch - pitch) * 0.08f;
            volume += (targetVolume - volume) * 0.08f;
            if (fadeRemaining > 0) {
                fadeRemaining--;
                if (fadeRemaining == 0 || volume < 0.002f) {
                    volume = 0.0f;
                    stop();
                }
            }
        }
    }
}
