package net.canvasmod.client;

import net.canvasmod.CommunityGatheringPayload;
import net.canvasmod.MomentDensityPolicy;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class CanvasCommunityClient {
    private static final boolean CI_VISUAL_TEST =
            Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"));

    private final Minecraft client = Minecraft.getInstance();
    private final CanvasExperienceDirector director;
    private long tick;
    private boolean ciAnnounced;

    CanvasCommunityClient(CanvasExperienceDirector director) {
        this.director = director;
    }

    void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            tick++;
            if (CI_VISUAL_TEST && tick >= 260L && !ciAnnounced) {
                present(3);
                System.out.println("CANVAS_CI_COMMUNITY_GATHERING_ACTIVE");
                ciAnnounced = true;
            }
        });
    }

    void accept(CommunityGatheringPayload payload) {
        if (payload == null || payload.participants() < 2) return;
        if (!director.allowMoment(MomentDensityPolicy.Kind.COMMUNITY_GATHERING, tick, true)) return;
        present(payload.participants());
    }

    private void present(int participants) {
        float volume = Math.min(0.24f, 0.17f + Math.max(0, participants - 2) * 0.015f);
        client.getSoundManager().play(new CommunityMusic(volume));
    }

    private static final class CommunityMusic extends AbstractTickableSoundInstance {
        private int age;

        CommunityMusic(float volume) {
            super(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("canvas", "music.community_gathering")),
                    SoundSource.MUSIC,
                    RandomSource.create());
            this.volume = volume;
            this.pitch = 1.0f;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override public void tick() {
            if (++age >= 20 * 15) stop();
        }
    }
}
