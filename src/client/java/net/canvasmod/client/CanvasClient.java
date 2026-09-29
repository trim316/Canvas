package net.canvasmod.client;

import net.canvasmod.HomeStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CanvasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CanvasFeelClient feel = new CanvasFeelClient();

        ClientPlayNetworking.registerGlobalReceiver(HomeStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> feel.acceptServerHome(payload)));

        new CanvasFamiliarityClient(feel).register();
        new CanvasVillageLifeClient().register();
        feel.register();
    }
}
