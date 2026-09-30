package net.canvasmod.client;

import net.canvasmod.HomeStatePayload;
import net.canvasmod.PlaceStatePayload;
import net.canvasmod.LandmarkStatePayload;
import net.canvasmod.WorldIdentityPayload;
import net.canvasmod.CommunityGatheringPayload;
import net.canvasmod.CanvasFeatureConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CanvasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CanvasFeatureConfig.reload();
        CanvasFeatureConfig config = CanvasFeatureConfig.current();

        CanvasExperienceDirector director = new CanvasExperienceDirector();
        CanvasFeelClient feel = new CanvasFeelClient(director);
        CanvasExplorationClient exploration = new CanvasExplorationClient(feel, director);
        CanvasExplorationWeatherClient explorationWeather =
                new CanvasExplorationWeatherClient(feel, director);
        CanvasRareWonderClient rareWonder = new CanvasRareWonderClient(feel, director);
        CanvasFamiliarityClient familiarity = new CanvasFamiliarityClient(feel, director);
        CanvasCommunityClient community = new CanvasCommunityClient(director);
        CanvasSeasonClient seasons = new CanvasSeasonClient(feel, director);

        ClientPlayNetworking.registerGlobalReceiver(WorldIdentityPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    feel.acceptWorldIdentity(payload.scopeId());
                    // A branch/server switch must never replay the prior world's
                    // exploration music, weather cue or landmark memory.
                    exploration.resetForWorld();
                    explorationWeather.resetForWorld();
                    rareWonder.acceptWorldIdentity(payload.scopeId());
                    seasons.resetForWorld();
                    if (config.enabled(CanvasFeatureConfig.Family.FAMILIAR_FACES)) {
                        familiarity.acceptWorldIdentity(payload.scopeId());
                    }
                    if (Boolean.parseBoolean(System.getenv().getOrDefault("CANVAS_VISUAL_TEST", "false"))) {
                        System.out.println("CANVAS_CI_WORLD_IDENTITY_ACTIVE");
                    }
                }));
        if (config.enabled(CanvasFeatureConfig.Family.COMMUNITY)) {
            ClientPlayNetworking.registerGlobalReceiver(CommunityGatheringPayload.TYPE, (payload, context) ->
                    context.client().execute(() -> community.accept(payload)));
            community.register();
        }
        if (config.enabled(CanvasFeatureConfig.Family.HOME)) {
            ClientPlayNetworking.registerGlobalReceiver(HomeStatePayload.TYPE, (payload, context) ->
                    context.client().execute(() -> feel.acceptServerHome(payload)));
        }
        if (config.enabled(CanvasFeatureConfig.Family.EXPLORATION)) {
            ClientPlayNetworking.registerGlobalReceiver(PlaceStatePayload.TYPE, (payload, context) ->
                    context.client().execute(() -> {
                        exploration.accept(payload);
                        explorationWeather.accept(payload);
                    }));
            ClientPlayNetworking.registerGlobalReceiver(LandmarkStatePayload.TYPE, (payload, context) ->
                    context.client().execute(() -> rareWonder.accept(payload)));
            exploration.register();
            explorationWeather.register();
            rareWonder.register();
        }
        if (config.enabled(CanvasFeatureConfig.Family.FAMILIAR_FACES)) familiarity.register();
        if (config.enabled(CanvasFeatureConfig.Family.VILLAGE_LIFE)) {
            new CanvasVillageLifeClient(director).register();
        }
        if (config.enabled(CanvasFeatureConfig.Family.SEASONS)) {
            seasons.register();
        }
        feel.register();
    }
}
