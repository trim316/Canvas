package net.canvasmod.client;

import net.canvasmod.ExplorationMusicPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.PlaceFamiliarityPolicy;
import net.canvasmod.PlaceStatePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class CanvasExplorationClient {
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasFeelClient feel;
    private final CanvasExperienceDirector director;
    private long tick;
    private long lastMusicMoment = Long.MIN_VALUE / 4L;
    private PlaceFamiliarityPolicy.Kind previous = PlaceFamiliarityPolicy.Kind.NONE;
    private boolean ciAnnounced;

    CanvasExplorationClient(CanvasFeelClient feel, CanvasExperienceDirector director) {
        this.feel = feel;
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (CI_VISUAL_TEST && tick >= 230L && !ciAnnounced) {
                present(PlaceFamiliarityPolicy.Kind.VIEWPOINT);
                System.out.println("CANVAS_CI_EXPLORATION_MUSIC_ACTIVE");
                ciAnnounced = true;
            }
        });
    }

    void accept(PlaceStatePayload payload) {
        PlaceFamiliarityPolicy.Kind current = parse(payload.kind());
        boolean familiar = payload.isFamiliar();
        long sinceLast = tick - lastMusicMoment;

        if (ExplorationMusicPolicy.shouldPresent(
                previous,
                current,
                familiar,
                feel.isAtHome(),
                sinceLast)) {
            String event = ExplorationMusicPolicy.eventFor(current, familiar);
            if (!event.isBlank()
                    && director.allowMoment(MomentDensityPolicy.Kind.EXPLORATION_MUSIC, tick, true)) {
                present(current);
                lastMusicMoment = tick;
            }
        }
        previous = current;
    }

    private void present(PlaceFamiliarityPolicy.Kind kind) {
        client.getSoundManager().play(new CanvasExplorationMusic(
                ExplorationMusicPolicy.eventFor(kind, true),
                ExplorationMusicPolicy.volumeFor(kind),
                ExplorationMusicPolicy.pitchFor(kind)));
    }

    private static PlaceFamiliarityPolicy.Kind parse(String raw) {
        try {
            return PlaceFamiliarityPolicy.Kind.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return PlaceFamiliarityPolicy.Kind.NONE;
        }
    }

    private static final class CanvasExplorationMusic extends AbstractTickableSoundInstance {
        private int age;

        CanvasExplorationMusic(String eventPath, float volume, float pitch) {
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
            if (age >= 20 * 16) stop();
        }
    }
}
