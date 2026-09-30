package net.canvasmod.client;

import net.canvasmod.HomeStatePayload;
import net.canvasmod.PlaceStatePayload;
import net.canvasmod.LandmarkStatePayload;
import net.canvasmod.WorldIdentityPayload;
import net.canvasmod.CommunityGatheringPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CanvasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CanvasExperienceDirector director = new CanvasExperienceDirector();
        CanvasFeelClient feel = new CanvasFeelClient(director);
        CanvasExplorationClient exploration = new CanvasExplorationClient(feel, director);
        CanvasExplorationWeatherClient explorationWeather =
                new CanvasExplorationWeatherClient(feel, director);
        CanvasRareWonderClient rareWonder = new CanvasRareWonderClient(feel, director);
        CanvasFamiliarityClient familiarity = new CanvasFamiliarityClient(feel, director);
        CanvasCommunityClient community = new CanvasCommunityClient(director);

        ClientPlayNetworking.registerGlobalReceiver(WorldIdentityPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    feel.acceptWorldIdentity(payload.worldId());
                    familiarity.acceptWorldIdentity(payload.worldId());
                    if (Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"))) {
                        System.out.println("CANVAS_CI_WORLD_IDENTITY_ACTIVE");
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(CommunityGatheringPayload.TYPE, (payload, context) ->
                context.client().execute(() -> community.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HomeStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> feel.acceptServerHome(payload)));
        ClientPlayNetworking.registerGlobalReceiver(PlaceStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    exploration.accept(payload);
                    explorationWeather.accept(payload);
                }));
        ClientPlayNetworking.registerGlobalReceiver(LandmarkStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> rareWonder.accept(payload)));

        community.register();
        exploration.register();
        explorationWeather.register();
        rareWonder.register();
        familiarity.register();
        new CanvasVillageLifeClient(director).register();
        new CanvasSeasonClient(feel, director).register();
        feel.register();
    }
}
