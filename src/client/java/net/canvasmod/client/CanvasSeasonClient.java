package net.canvasmod.client;

import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.SeasonPolicy;
import net.canvasmod.SeasonalHomeProfile;
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

final class CanvasSeasonClient {
    private static final int SAMPLE_INTERVAL = 200;

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasSeasonObserver observer = new CanvasSeasonObserver();
    private final CanvasFeelClient feel;
    private final CanvasExperienceDirector director;
    private long tick;
    private SeasonPolicy.Observation observation =
            new SeasonPolicy.Observation(SeasonPolicy.Season.UNKNOWN, "none", "");
    private SeasonPolicy.Season previousKnown = SeasonPolicy.Season.UNKNOWN;
    private CanvasSeasonLoop ambience;
    private String ambienceKey = "";

    CanvasSeasonClient(CanvasFeelClient feel, CanvasExperienceDirector director) {
        this.feel = feel;
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (tick % SAMPLE_INTERVAL == 0) update();
        });
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("canvas", "seasonal_home"),
                (graphics, tickCounter) -> render(graphics));
    }

    private void update() {
        observation = observer.observe(client);
        if (!feel.isAtHome() || !observation.known()) {
            stopAmbience(35);
            ambienceKey = "";
            if (observation.known()) previousKnown = observation.season();
            return;
        }

        setAmbience(
                SeasonalHomeProfile.ambienceEvent(observation.season()),
                SeasonalHomeProfile.ambienceVolume(observation.season()),
                SeasonalHomeProfile.ambiencePitch(observation.season()));

        if (previousKnown != SeasonPolicy.Season.UNKNOWN
                && previousKnown != observation.season()
                && director.allowMoment(MomentDensityPolicy.Kind.SEASON_SHIFT, tick, true)) {
            String event = SeasonalHomeProfile.transitionMusicEvent(observation.season());
            if (!event.isBlank()) {
                client.getSoundManager().play(new CanvasSeasonTransition(
                        event, 0.18f,
                        SeasonalHomeProfile.homecomingMusicPitchMultiplier(observation.season())));
            }
        }
        previousKnown = observation.season();
    }

    private void render(GuiGraphicsExtractor graphics) {
        if (!feel.isAtHome() || !observation.known()) return;
        int color = SeasonalHomeProfile.washArgb(observation.season());
        if (color == 0) return;
        graphics.fill(
                0, 0,
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                color);
    }

    private void setAmbience(String key, float volume, float pitch) {
        if (key.isBlank()) return;
        if (ambience != null && !ambience.isStopped() && ambienceKey.equals(key)) {
            ambience.setTarget(volume, pitch);
            return;
        }
        stopAmbience(25);
        ambience = new CanvasSeasonLoop(key, volume, pitch);
        ambienceKey = key;
        client.getSoundManager().play(ambience);
    }

    private void stopAmbience(int ticks) {
        if (ambience != null && !ambience.isStopped()) ambience.fadeOut(ticks);
    }

    private static final class CanvasSeasonTransition extends AbstractTickableSoundInstance {
        private int age;
        CanvasSeasonTransition(String path, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", path)),
                    SoundSource.MUSIC, RandomSource.create());
            this.volume = volume;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }
        @Override public void tick() { if (++age >= 20 * 14) stop(); }
    }

    private static final class CanvasSeasonLoop extends AbstractTickableSoundInstance {
        private float targetVolume;
        private float targetPitch;
        private int fadeRemaining;

        CanvasSeasonLoop(String path, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", path)),
                    SoundSource.AMBIENT, RandomSource.create());
            targetVolume = volume;
            targetPitch = pitch;
            this.pitch = pitch;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            looping = true;
        }

        void setTarget(float volume, float pitch) {
            targetVolume = Math.max(0.0f, Math.min(0.12f, volume));
            targetPitch = Math.max(0.85f, Math.min(1.15f, pitch));
        }
        void fadeOut(int ticks) { targetVolume = 0.0f; fadeRemaining = Math.max(1, ticks); }

        @Override public void tick() {
            pitch += (targetPitch - pitch) * 0.08f;
            volume += (targetVolume - volume) * 0.08f;
            if (fadeRemaining > 0 && (--fadeRemaining == 0 || volume < 0.001f)) stop();
        }
    }
}
