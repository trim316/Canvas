package net.canvasmod.client;

import java.util.Locale;
import net.canvasmod.CanvasFeelProfile;
import net.canvasmod.HomeEvidencePolicy;
import net.canvasmod.HomeRecognitionAccumulator;
import net.canvasmod.HomeStatePayload;
import net.canvasmod.HomecomingPolicy;
import net.canvasmod.RareSurprisePolicy;
import net.canvasmod.WeatherCharacterPolicy;
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
    private static final int PHASE_CUE_COOLDOWN = 500;
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final HomeRecognitionAccumulator accumulator = new HomeRecognitionAccumulator();
    private final CanvasExperienceDirector director;

    private long tick;
    private boolean hasHome;
    private String homeDimension = "";
    private int homeX;
    private int homeY;
    private int homeZ;
    private long awaySince = -1;
    private long lastReturnCue = Long.MIN_VALUE / 4;
    private long lastPhaseCue = Long.MIN_VALUE / 4;
    private long lastSurpriseDay = Long.MIN_VALUE / 4;
    private CanvasLoopingSound ambience;
    private String ambienceKey = "";
    private boolean atHome;
    private boolean previousRaining;
    private long rainEndedTick = Long.MIN_VALUE / 4;
    private boolean ciVisualAnnounced;
    private boolean ciRareAnnounced;
    private int familiarPulseTicks;
    private int homeTransitionPulseTicks;
    private int rareSurpriseTicks;
    private RareSurprisePolicy.Moment rareMoment = RareSurprisePolicy.Moment.NONE;
    private WeatherCharacterPolicy.Character weatherCharacter = WeatherCharacterPolicy.Character.CLEAR;
    private boolean ciWeatherAnnounced;
    private HomecomingPolicy.Flavor homecomingFlavor = HomecomingPolicy.Flavor.QUIET;
    private CanvasFeelProfile.Phase phase = CanvasFeelProfile.Phase.AWAY;

    CanvasFeelClient(CanvasExperienceDirector director) {
        this.director = director;
    }

    void acceptServerHome(HomeStatePayload payload) {
        hasHome = true;
        homeDimension = payload.dimension();
        homeX = payload.pos().getX();
        homeY = payload.pos().getY();
        homeZ = payload.pos().getZ();
        accumulator.reset();
    }

    void presentFamiliarFace() {
        familiarPulseTicks = 60;
        playCue("feel.familiar_face", 0.30f, 1.0f);
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
            phase = CanvasFeelProfile.Phase.AWAY;
            previousRaining = false;
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
        if (familiarPulseTicks > 0) familiarPulseTicks = Math.max(0, familiarPulseTicks - 20);
        if (homeTransitionPulseTicks > 0) homeTransitionPulseTicks = Math.max(0, homeTransitionPulseTicks - 20);
        if (rareSurpriseTicks > 0) rareSurpriseTicks = Math.max(0, rareSurpriseTicks - 20);

        boolean sheltered = isSheltered(pos);
        boolean raining = client.level.isRaining();
        boolean thundering = client.level.isThundering();
        if (previousRaining && !raining) rainEndedTick = tick;
        int ticksSinceRainEnded = rainEndedTick <= Long.MIN_VALUE / 8
                ? Integer.MAX_VALUE
                : (int)Math.min(Integer.MAX_VALUE, tick - rainEndedTick);
        long clock = client.level.getOverworldClockTime();
        long dayTime = clock % 24000L;
        long worldDay = Math.floorDiv(clock, 24000L);

        CanvasFeelProfile.Phase nextPhase =
                CanvasFeelProfile.classify(atHome, sheltered, raining, thundering, dayTime);
        if (nextPhase != phase) {
            onPhaseChanged(phase, nextPhase);
            phase = nextPhase;
        }

        observeRareSurprise(raining, thundering, dayTime, worldDay);

        WeatherCharacterPolicy.Character nextWeather =
                WeatherCharacterPolicy.classify(atHome, sheltered, raining, thundering, ticksSinceRainEnded);
        if (CI_VISUAL_TEST && tick >= 220 && tick < 300) {
            nextWeather = WeatherCharacterPolicy.Character.THUNDER_SHELTERED;
            if (!ciWeatherAnnounced) {
                System.out.println("CANVAS_CI_WEATHER_CHARACTER_ACTIVE");
                ciWeatherAnnounced = true;
            }
        }
        if (nextWeather == WeatherCharacterPolicy.Character.CALM_AFTER_STORM
                && weatherCharacter != WeatherCharacterPolicy.Character.CALM_AFTER_STORM) {
            playCue("feel.calm_after_storm", 0.26f, 1.03f);
        }
        weatherCharacter = nextWeather;
        director.setWeatherCharacter(weatherCharacter);

        String weatherAmbience = WeatherCharacterPolicy.ambienceEvent(weatherCharacter);
        if (!weatherAmbience.isBlank()) {
            setAmbience(
                    weatherAmbience,
                    WeatherCharacterPolicy.ambienceVolume(weatherCharacter),
                    WeatherCharacterPolicy.ambiencePitch(weatherCharacter));
        } else if (phase != CanvasFeelProfile.Phase.AWAY) {
            setAmbience(
                    CanvasFeelProfile.ambienceEvent(phase),
                    CanvasFeelProfile.volume(phase, sheltered),
                    CanvasFeelProfile.pitch(phase));
        } else {
            setAwayAmbience(dayTime, sheltered);
        }

        previousRaining = raining;
    }

    private void observeRareSurprise(boolean raining, boolean thundering, long dayTime, long worldDay) {
        if (CI_VISUAL_TEST && tick >= 180 && !ciRareAnnounced) {
            presentRareSurprise(RareSurprisePolicy.Moment.GOLDEN_HUSH, worldDay);
            System.out.println("CANVAS_CI_RARE_SURPRISE_ACTIVE");
            ciRareAnnounced = true;
            return;
        }

        long daysSinceLast = lastSurpriseDay <= Long.MIN_VALUE / 8
                ? Long.MAX_VALUE / 4
                : worldDay - lastSurpriseDay;
        RareSurprisePolicy.Moment candidate = RareSurprisePolicy.classify(
                atHome, previousRaining, raining, thundering, dayTime,
                worldDay, homeX, homeZ, daysSinceLast);
        if (candidate != RareSurprisePolicy.Moment.NONE) presentRareSurprise(candidate, worldDay);
    }

    private void presentRareSurprise(RareSurprisePolicy.Moment moment, long worldDay) {
        String event = RareSurprisePolicy.cueEvent(moment);
        if (event.isBlank()) return;
        playCue(event, 0.34f, moment == RareSurprisePolicy.Moment.STORM_BREAK ? 1.02f : 0.98f);
        rareMoment = moment;
        rareSurpriseTicks = 140;
        lastSurpriseDay = worldDay;
    }

    private void onPhaseChanged(CanvasFeelProfile.Phase previous, CanvasFeelProfile.Phase next) {
        if (next == CanvasFeelProfile.Phase.AWAY) return;
        homeTransitionPulseTicks = 80;

        if (previous != CanvasFeelProfile.Phase.AWAY
                && tick - lastPhaseCue >= PHASE_CUE_COOLDOWN) {
            playCue("feel.home_shift", 0.24f, CanvasFeelProfile.transitionPitch(next));
            lastPhaseCue = tick;
        }
    }

    private void setAwayAmbience(long dayTime, boolean sheltered) {
        boolean evening = dayTime >= 11500L && dayTime < 14000L;
        boolean night = dayTime >= 13500L && dayTime < 22500L;
        boolean morning = dayTime >= 22500L || dayTime < 1700L;

        if (night && sheltered) {
            setAmbience("presence.void_stillness", 0.045f, 0.985f);
        } else if (morning || evening) {
            setAmbience("presence.harbor_air", 0.028f, morning ? 1.015f : 0.985f);
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
        HomecomingPolicy.Plan plan = director.previewHomecoming();
        homecomingFlavor = plan.flavor();
        playCue(plan.cueEvent(), Math.min(0.40f, plan.cueVolume()), plan.cuePitch());
        if (!plan.musicEvent().isBlank()) playMusicMoment(plan.musicEvent(), 0.16f, plan.cuePitch());
        homeTransitionPulseTicks = plan.pulseTicks();
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
                HomecomingPolicy.Plan plan = director.nextHomecoming();
                homecomingFlavor = plan.flavor();
                playCue(plan.cueEvent(), plan.cueVolume(), plan.cuePitch());
                if (!plan.musicEvent().isBlank()) {
                    playMusicMoment(plan.musicEvent(), 0.20f, plan.cuePitch());
                }
                homeTransitionPulseTicks = plan.pulseTicks();
                lastReturnCue = tick;
            }
            awaySince = -1;
        }
    }

    private void renderOverlay(GuiGraphicsExtractor graphics) {
        if (client.level == null || client.player == null) return;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        int wash = CanvasFeelProfile.washArgb(phase);
        int edge = CanvasFeelProfile.edgeArgb(phase);
        int weatherWash = WeatherCharacterPolicy.washArgb(weatherCharacter);
        int weatherEdge = WeatherCharacterPolicy.edgeArgb(weatherCharacter);

        if (wash != 0) graphics.fill(0, 0, width, height, wash);
        if (weatherWash != 0) graphics.fill(0, 0, width, height, weatherWash);
        if (edge != 0) {
            int band = Math.max(5, Math.min(width, height) / 32);
            graphics.fill(0, 0, width, band, edge);
            graphics.fill(0, height - band, width, height, edge);
            graphics.fill(0, band, band, height - band, edge);
            graphics.fill(width - band, band, width, height - band, edge);
        }

        if (weatherEdge != 0) {
            int weatherBand = Math.max(3, Math.min(width, height) / 52);
            graphics.fill(0, 0, width, weatherBand, weatherEdge);
            graphics.fill(0, height - weatherBand, width, height, weatherEdge);
        }

        if (homeTransitionPulseTicks > 0 && atHome) {
            int band = Math.max(3, Math.min(width, height) / 70);
            int pulse = switch (homecomingFlavor) {
                case QUIET -> 0x30FFD18A;
                case FAMILIAR -> 0x38FFE0A0;
                case VILLAGE -> 0x38FFC978;
                case LIVED_IN -> 0x44FFD78C;
            };
            graphics.fill(0, 0, width, band, pulse);
            graphics.fill(0, height - band, width, height, pulse);
        }

        if (rareSurpriseTicks > 0) {
            int rareWash = RareSurprisePolicy.washArgb(rareMoment);
            graphics.fill(0, 0, width, height, rareWash);
            int inset = Math.max(10, Math.min(width, height) / 18);
            int shimmer = rareMoment == RareSurprisePolicy.Moment.STORM_BREAK
                    ? 0x5CAADFFF
                    : 0x62FFE09A;
            graphics.fill(inset, inset, width - inset, inset + 2, shimmer);
            graphics.fill(inset, height - inset - 2, width - inset, height - inset, shimmer);
        }

        if (familiarPulseTicks > 0) {
            int cx = width / 2;
            int cy = height / 2;
            int radius = 14;
            int pulse = 0x66FFD28A;
            graphics.fill(cx - radius, cy - radius, cx - 4, cy - radius + 2, pulse);
            graphics.fill(cx + 4, cy - radius, cx + radius, cy - radius + 2, pulse);
            graphics.fill(cx - radius, cy + radius - 2, cx - 4, cy + radius, pulse);
            graphics.fill(cx + 4, cy + radius - 2, cx + radius, cy + radius, pulse);
            graphics.fill(cx - radius, cy - radius, cx - radius + 2, cy - 4, pulse);
            graphics.fill(cx - radius, cy + 4, cx - radius + 2, cy + radius, pulse);
            graphics.fill(cx + radius - 2, cy - radius, cx + radius, cy - 4, pulse);
            graphics.fill(cx + radius - 2, cy + 4, cx + radius, cy + radius, pulse);
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

    private void playMusicMoment(String eventPath, float volume, float pitch) {
        client.getSoundManager().play(new CanvasMusicMoment(eventPath, volume, pitch));
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
            if (age >= 180) stop();
        }
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
        }

        @Override
        public void tick() {
            age++;
            if (age >= 20 * 16) stop();
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
