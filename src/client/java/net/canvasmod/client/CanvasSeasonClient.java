package net.canvasmod.client;

import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.SeasonPolicy;
import net.canvasmod.SeasonalPresentationPolicy;
import net.canvasmod.SeasonalHomeProfile;
import net.canvasmod.SeasonalRareMomentPolicy;
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
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasSeasonObserver observer = new CanvasSeasonObserver();
    private final CanvasFeelClient feel;
    private final CanvasExperienceDirector director;
    private long tick;
    private SeasonPolicy.Observation observation =
            new SeasonPolicy.Observation(SeasonPolicy.Season.UNKNOWN, "none", "");
    private final SeasonalPresentationPolicy transitions = new SeasonalPresentationPolicy();
    private Object observedLevel;
    private CanvasSeasonTransition activeTransition;
    private CanvasSeasonCue activeCue;
    private CanvasSeasonLoop fadingAmbience;
    private CanvasSeasonLoop ambience;
    private String ambienceKey = "";
    private int rareMomentTicks;
    private SeasonalRareMomentPolicy.Moment rareMoment = SeasonalRareMomentPolicy.Moment.NONE;
    private boolean ciSeasonalRareAnnounced;

    CanvasSeasonClient(CanvasFeelClient feel, CanvasExperienceDirector director) {
        this.feel = feel;
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            // Tick-level cleanup avoids audible bleed during the sampling gap.
            if (observedLevel != client.level) {
                resetForWorld();
                observedLevel = client.level;
            }
            if (tick % SAMPLE_INTERVAL == 0) update();
            if (rareMomentTicks > 0) rareMomentTicks--;
        });
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("canvas", "seasonal_home"),
                (graphics, tickCounter) -> render(graphics));
    }

    void resetForWorld() {
        transitions.reset();
        observation = new SeasonPolicy.Observation(SeasonPolicy.Season.UNKNOWN, "none", "");
        rareMoment = SeasonalRareMomentPolicy.Moment.NONE;
        rareMomentTicks = 0;
        if (ambience != null) ambience.endImmediately();
        if (fadingAmbience != null) fadingAmbience.endImmediately();
        if (activeTransition != null) activeTransition.endImmediately();
        if (activeCue != null) activeCue.endImmediately();
        ambience = null;
        fadingAmbience = null;
        activeTransition = null;
        activeCue = null;
        ambienceKey = "";
        observedLevel = client.level;
    }

    private void update() {
        observation = observer.observe(client);
        director.setSeason(observation.season());

        if (!feel.isAtHome() || !observation.known()) {
            stopAmbience(35);
            ambienceKey = "";
            if (observation.known()) transitions.seed(observation.season());
            return;
        }

        setAmbience(
                SeasonalHomeProfile.ambienceEvent(observation.season()),
                SeasonalHomeProfile.ambienceVolume(observation.season()),
                SeasonalHomeProfile.ambiencePitch(observation.season()));

        long clock = client.level == null ? 0L : client.level.getOverworldClockTime();
        long worldDay = Math.floorDiv(clock, 24000L);

        if (transitions.observe(observation.season())
                && director.allowMoment(MomentDensityPolicy.Kind.SEASON_SHIFT, tick, true)) {
            String event = SeasonalHomeProfile.transitionMusicEvent(observation.season());
            if (!event.isBlank()) {
                if (activeTransition != null) activeTransition.endImmediately();
                activeTransition = new CanvasSeasonTransition(
                        event, 0.18f,
                        SeasonalHomeProfile.homecomingMusicPitchMultiplier(observation.season()));
                client.getSoundManager().play(activeTransition);
            }
        }
        boolean snowing = observer.isSnowingAt(client);
        long lastDay = director.lastSeasonalMomentDay();
        long daysSinceLast = lastDay <= Long.MIN_VALUE / 8L
                ? Long.MAX_VALUE / 4L
                : worldDay - lastDay;

        SeasonalRareMomentPolicy.Moment candidate = SeasonalRareMomentPolicy.classify(
                observation.season(),
                true,
                snowing,
                worldDay,
                daysSinceLast,
                director.firstSnowSeen());

        if (CI_VISUAL_TEST
                && observation.season() == SeasonPolicy.Season.WINTER
                && snowing
                && !ciSeasonalRareAnnounced) {
            // The visual witness must prove the presentation path itself. Earlier CI-only
            // moments deliberately exercise the shared density guardrail, so requiring this
            // witness to reacquire the same budget would make seasonal coverage order-dependent.
            // Production seasonal moments below still use the normal density gate.
            presentRareMoment(SeasonalRareMomentPolicy.Moment.FIRST_SNOW, worldDay);
            System.out.println("CANVAS_CI_SEASONAL_RARE_ACTIVE");
            ciSeasonalRareAnnounced = true;
            return;
        }

        if (candidate != SeasonalRareMomentPolicy.Moment.NONE
                && director.allowMoment(MomentDensityPolicy.Kind.SEASONAL_RARE, tick, false)) {
            presentRareMoment(candidate, worldDay);
        }
    }

    private void presentRareMoment(SeasonalRareMomentPolicy.Moment moment, long worldDay) {
        String event = SeasonalRareMomentPolicy.cueEvent(moment);
        if (event.isBlank()) return;
        if (activeCue != null) activeCue.endImmediately();
        activeCue = new CanvasSeasonCue(
                event, 0.28f, SeasonalRareMomentPolicy.cuePitch(moment));
        client.getSoundManager().play(activeCue);
        rareMoment = moment;
        rareMomentTicks = 140;
        director.noteSeasonalMoment(moment, worldDay);
    }

    private void render(GuiGraphicsExtractor graphics) {
        if (!feel.isAtHome() || !observation.known()) return;
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        int color = SeasonalHomeProfile.washArgb(observation.season());
        if (color != 0) graphics.fill(0, 0, width, height, color);

        if (rareMomentTicks > 0) {
            int rareWash = SeasonalRareMomentPolicy.washArgb(rareMoment);
            if (rareWash != 0) graphics.fill(0, 0, width, height, rareWash);
            int band = Math.max(2, Math.min(width, height) / 80);
            graphics.fill(0, 0, width, band, 0x42E4F4FF);
            graphics.fill(0, height - band, width, height, 0x42E4F4FF);
        }
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
        if (fadingAmbience != null) fadingAmbience.endImmediately();
        fadingAmbience = null;
        if (ambience != null && !ambience.isStopped()) {
            ambience.fadeOut(ticks);
            fadingAmbience = ambience;
        }
        ambience = null;
        ambienceKey = "";
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
        void endImmediately() { volume = 0.0f; stop(); }
        @Override public void tick() { if (++age >= 20 * 14) stop(); }
    }

    private static final class CanvasSeasonCue extends AbstractTickableSoundInstance {
        private int age;
        CanvasSeasonCue(String path, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", path)),
                    SoundSource.AMBIENT, RandomSource.create());
            this.volume = volume;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }
        void endImmediately() { volume = 0.0f; stop(); }
        @Override public void tick() { if (++age >= 20 * 10) stop(); }
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
        void endImmediately() { targetVolume = 0.0f; volume = 0.0f; stop(); }

        @Override public void tick() {
            pitch += (targetPitch - pitch) * 0.08f;
            volume += (targetVolume - volume) * 0.08f;
            if (fadeRemaining > 0 && (--fadeRemaining == 0 || volume < 0.001f)) stop();
        }
    }
}
