package net.canvasmod.client;

import net.canvasmod.ExplorationWeatherPolicy;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.PlaceFamiliarityPolicy;
import net.canvasmod.PlaceStatePayload;
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

final class CanvasExplorationWeatherClient {
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasFeelClient feel;
    private final CanvasExperienceDirector director;
    private long tick;
    private long lastMomentTick = Long.MIN_VALUE / 4L;
    private PlaceFamiliarityPolicy.Kind place = PlaceFamiliarityPolicy.Kind.NONE;
    private boolean familiar;
    private boolean previousRaining;
    private ExplorationWeatherPolicy.Moment previousCandidate = ExplorationWeatherPolicy.Moment.NONE;
    private ExplorationWeatherPolicy.Moment presentation = ExplorationWeatherPolicy.Moment.NONE;
    private int presentationTicks;
    private boolean ciAnnounced;

    CanvasExplorationWeatherClient(CanvasFeelClient feel, CanvasExperienceDirector director) {
        this.feel = feel;
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (tick % 20L == 0L) update();
            if (CI_VISUAL_TEST && tick >= 245L && !ciAnnounced) {
                present(ExplorationWeatherPolicy.Moment.STORM_OVERLOOK);
                System.out.println("CANVAS_CI_EXPLORATION_WEATHER_ACTIVE");
                ciAnnounced = true;
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("canvas", "exploration_weather"),
                (graphics, tickCounter) -> render(graphics));
    }

    void accept(PlaceStatePayload payload) {
        place = parse(payload.kind());
        familiar = payload.isFamiliar();
        evaluate();
    }

    private void update() {
        if (client.level == null || client.player == null) {
            previousRaining = false;
            previousCandidate = ExplorationWeatherPolicy.Moment.NONE;
            presentationTicks = 0;
            return;
        }
        evaluate();
        previousRaining = client.level.isRaining();
        if (presentationTicks > 0) presentationTicks = Math.max(0, presentationTicks - 20);
    }

    private void evaluate() {
        if (client.level == null || client.player == null) return;
        boolean raining = client.level.isRaining();
        boolean thundering = client.level.isThundering();
        boolean rainJustEnded = previousRaining && !raining;

        ExplorationWeatherPolicy.Moment candidate = ExplorationWeatherPolicy.classify(
                place,
                familiar,
                feel.isAtHome(),
                raining,
                thundering,
                rainJustEnded);

        long sinceLast = tick - lastMomentTick;
        if (candidate != ExplorationWeatherPolicy.Moment.NONE
                && candidate != previousCandidate
                && sinceLast >= ExplorationWeatherPolicy.COOLDOWN_TICKS
                && director.allowMoment(MomentDensityPolicy.Kind.EXPLORATION_WEATHER, tick, false)) {
            present(candidate);
            lastMomentTick = tick;
        }
        previousCandidate = candidate;
    }

    private void present(ExplorationWeatherPolicy.Moment moment) {
        String event = ExplorationWeatherPolicy.cueEvent(moment);
        if (event.isBlank()) return;
        client.getSoundManager().play(new CanvasWeatherCue(
                event,
                ExplorationWeatherPolicy.volume(moment),
                ExplorationWeatherPolicy.pitch(moment)));
        presentation = moment;
        presentationTicks = 120;
    }

    private void render(GuiGraphicsExtractor graphics) {
        if (presentationTicks <= 0) return;
        int color = ExplorationWeatherPolicy.washArgb(presentation);
        if (color == 0) return;
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, color);
    }

    private static PlaceFamiliarityPolicy.Kind parse(String raw) {
        try {
            return PlaceFamiliarityPolicy.Kind.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return PlaceFamiliarityPolicy.Kind.NONE;
        }
    }

    private static final class CanvasWeatherCue extends AbstractTickableSoundInstance {
        private int age;

        CanvasWeatherCue(String eventPath, float volume, float pitch) {
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
            if (age >= 20 * 8) stop();
        }
    }
}
