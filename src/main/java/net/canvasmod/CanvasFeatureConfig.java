package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class CanvasFeatureConfig {
    public enum Family {
        HOME,
        WEATHER,
        RARE_MOMENTS,
        FAMILIAR_FACES,
        VILLAGE_LIFE,
        SEASONS,
        EXPLORATION,
        COMMUNITY
    }

    private static volatile CanvasFeatureConfig CURRENT = load(
            FabricLoader.getInstance().getConfigDir().resolve("canvas-features.properties"));

    private final EnumMap<Family, Boolean> enabled;

    private CanvasFeatureConfig(EnumMap<Family, Boolean> enabled) {
        this.enabled = enabled;
    }

    public static CanvasFeatureConfig current() {
        return CURRENT;
    }

    public static void reload() {
        CURRENT = load(FabricLoader.getInstance().getConfigDir().resolve("canvas-features.properties"));
    }

    public boolean enabled(Family family) {
        return enabled.getOrDefault(family, true);
    }

    public static CanvasFeatureConfig load(Path file) {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (var in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException ignored) { }
        }

        EnumMap<Family, Boolean> values = new EnumMap<>(Family.class);
        for (Family family : Family.values()) {
            String key = "family." + family.name().toLowerCase(Locale.ROOT);
            values.put(family, Boolean.parseBoolean(properties.getProperty(key, "true")));
        }

        Properties normalized = new Properties();
        for (Family family : Family.values()) {
            String key = "family." + family.name().toLowerCase(Locale.ROOT);
            normalized.setProperty(key, Boolean.toString(values.get(family)));
        }
        try {
            Files.createDirectories(file.getParent());
            try (var out = Files.newOutputStream(file)) {
                normalized.store(out, "Canvas experience-family configuration; all families default on");
            }
        } catch (IOException ignored) { }

        return new CanvasFeatureConfig(values);
    }
}
