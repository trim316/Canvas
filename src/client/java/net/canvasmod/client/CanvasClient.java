package net.canvasmod.client;

import net.canvasmod.HomeStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CanvasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CanvasExperienceDirector director = new CanvasExperienceDirector();
        CanvasFeelClient feel = new CanvasFeelClient(director);

        ClientPlayNetworking.registerGlobalReceiver(HomeStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> feel.acceptServerHome(payload)));

        new CanvasFamiliarityClient(feel, director).register();
        new CanvasVillageLifeClient(director).register();
        new CanvasSeasonClient(feel, director).register();
        feel.register();
    }
}
