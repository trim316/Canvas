package net.canvasmod.client;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.canvasmod.SeasonPolicy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

final class CanvasSeasonObserver {
    SeasonPolicy.Observation observe(Minecraft client) {
        String override = System.getenv().getOrDefault("CANVAS_SEASON_TEST", "").trim();
        if (!override.isBlank()) {
            return new SeasonPolicy.Observation(
                    SeasonPolicy.fromToken(override), "ci-override", override);
        }
        if (client.level == null) {
            return new SeasonPolicy.Observation(SeasonPolicy.Season.UNKNOWN, "none", "");
        }
        if (FabricLoader.getInstance().isModLoaded("sereneseasons")) {
            return observeSereneSeasons(client);
        }
        return new SeasonPolicy.Observation(SeasonPolicy.Season.UNKNOWN, "none", "");
    }

    private SeasonPolicy.Observation observeSereneSeasons(Minecraft client) {
        try {
            Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
            for (Method method : helper.getMethods()) {
                if (!method.getName().equals("getSeasonState")
                        || !Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 1
                        || !method.getParameterTypes()[0].isAssignableFrom(client.level.getClass())) continue;
                Object state = method.invoke(null, client.level);
                if (state == null) break;
                Object raw = state.getClass().getMethod("getSubSeason").invoke(state);
                String token = raw == null ? "" : raw.toString();
                return new SeasonPolicy.Observation(
                        SeasonPolicy.fromToken(token), "serene-seasons", token);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return new SeasonPolicy.Observation(
                SeasonPolicy.Season.UNKNOWN, "serene-seasons", "unavailable");
    }
}
