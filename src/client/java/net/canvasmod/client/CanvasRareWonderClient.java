package net.canvasmod.client;

import java.nio.file.Path;
import net.canvasmod.ExplorationWonderMemoryStore;
import net.canvasmod.LandmarkStatePayload;
import net.canvasmod.MomentDensityPolicy;
import net.canvasmod.PlaceFamiliarityPolicy;
import net.canvasmod.RareWonderPolicy;
import net.canvasmod.WorldMemoryScopePolicy;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class CanvasRareWonderClient {
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasFeelClient feel;
    private final CanvasExperienceDirector director;
    private final ExplorationWonderMemoryStore memory;

    private long tick;
    private String worldIdentity = "";
    private CanvasWonderCue activeCue;
    private long lastWonderTick = Long.MIN_VALUE / 4L;
    private PlaceFamiliarityPolicy.Kind kind = PlaceFamiliarityPolicy.Kind.NONE;
    private String contextKey = "";
    private boolean landmark;
    private int presentationTicks;
    private RareWonderPolicy.Moment presentation = RareWonderPolicy.Moment.NONE;
    private boolean ciAnnounced;

    CanvasRareWonderClient(CanvasFeelClient feel, CanvasExperienceDirector director) {
        this.feel = feel;
        this.director = director;
        Path file = FabricLoader.getInstance().getConfigDir()
                .resolve("canvas-exploration-wonders-v1.properties");
        this.memory = new ExplorationWonderMemoryStore(file);
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (tick % 20L == 0L) update();
            if (CI_VISUAL_TEST && tick >= 248L && !ciAnnounced) {
                present(RareWonderPolicy.Moment.HORIZON_GLOW, "ci:wonder", 0L);
                System.out.println("CANVAS_CI_EXPLORATION_WONDER_ACTIVE");
                ciAnnounced = true;
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("canvas", "rare_wonder"),
                (graphics, tickCounter) -> render(graphics));
    }

    void acceptWorldIdentity(String worldId) {
        // Also clear on reconnect to the same branch. A new session must never
        // inherit the prior session's presentation or pending landmark payload.
        worldIdentity = worldId == null ? "" : worldId.trim();
        lastWonderTick = Long.MIN_VALUE / 4L;
        clearPresentation();
    }

    private void clearPresentation() {
        kind = PlaceFamiliarityPolicy.Kind.NONE;
        contextKey = "";
        landmark = false;
        presentation = RareWonderPolicy.Moment.NONE;
        presentationTicks = 0;
        if (activeCue != null) {
            activeCue.endImmediately();
            activeCue = null;
        }
    }

    void accept(LandmarkStatePayload payload) {
        kind = parse(payload.kind());
        contextKey = payload.contextKey();
        landmark = payload.isLandmark();
    }

    private void update() {
        if (client.level == null || client.player == null) {
            clearPresentation();
            return;
        }
        if (presentationTicks > 0) presentationTicks = Math.max(0, presentationTicks - 20);

        long clock = client.level.getOverworldClockTime();
        long dayTime = Math.floorMod(clock, 24000L);
        long worldDay = Math.floorDiv(clock, 24000L);
        // Landmark coordinates alone are not globally unique: unrelated worlds
        // can have matching player-authored places. Never read or write their
        // private wonder history without the server-provided branch identity.
        String scopedContext = WorldMemoryScopePolicy.scope(worldIdentity, contextKey);
        var snapshot = memory.snapshot(scopedContext);
        long daysSinceLast = snapshot.lastWorldDay() <= Long.MIN_VALUE / 8L
                ? Long.MAX_VALUE / 4L
                : worldDay - snapshot.lastWorldDay();

        RareWonderPolicy.Moment candidate = RareWonderPolicy.classify(
                kind,
                landmark,
                feel.isAtHome(),
                client.level.isRaining(),
                client.level.isThundering(),
                dayTime,
                worldDay,
                scopedContext,
                daysSinceLast);

        if (candidate == RareWonderPolicy.Moment.NONE) return;
        if (tick - lastWonderTick < RareWonderPolicy.MIN_TICKS_BETWEEN_WONDERS) return;
        if (!director.allowMoment(MomentDensityPolicy.Kind.EXPLORATION_WONDER, tick, false)) return;

        present(candidate, scopedContext, worldDay);
        lastWonderTick = tick;
    }

    private void present(RareWonderPolicy.Moment moment, String key, long worldDay) {
        String event = RareWonderPolicy.cueEvent(moment);
        if (event.isBlank()) return;
        if (activeCue != null) activeCue.endImmediately();
        activeCue = new CanvasWonderCue(event, 0.24f, RareWonderPolicy.pitch(moment));
        client.getSoundManager().play(activeCue);
        presentation = moment;
        presentationTicks = 140;
        if (!key.startsWith("ci:")) memory.note(key, worldDay);
    }

    private void render(GuiGraphicsExtractor graphics) {
        if (presentationTicks <= 0) return;
        int color = RareWonderPolicy.washArgb(presentation);
        if (color == 0) return;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, color);
        int band = Math.max(2, Math.min(width, height) / 90);
        graphics.fill(0, 0, width, band, color | 0x18000000);
        graphics.fill(0, height - band, width, height, color | 0x18000000);
    }

    private static PlaceFamiliarityPolicy.Kind parse(String raw) {
        try {
            return PlaceFamiliarityPolicy.Kind.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return PlaceFamiliarityPolicy.Kind.NONE;
        }
    }

    private static final class CanvasWonderCue extends AbstractTickableSoundInstance {
        private int age;

        CanvasWonderCue(String eventPath, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", eventPath)),
                    SoundSource.AMBIENT,
                    RandomSource.create());
            this.volume = volume;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void endImmediately() {
            volume = 0.0f;
            stop();
        }

        @Override
        public void tick() {
            age++;
            if (age >= 20 * 10) stop();
        }
    }
}
