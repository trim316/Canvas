package net.canvasmod;

/**
 * Loaded-world presentation decisions for time spent away from an authored HOME.
 * Never inspects chunks, alters biomes/weather or claims unfamiliar dimensions
 * have Overworld-like climate. The empty profile deliberately leaves vanilla
 * and companion-mod ambience alone.
 */
public final class TravelAtmospherePolicy {
    public record Ambience(String event, float volume, float pitch) {
        public boolean audible() { return !event.isBlank() && volume > 0.0f; }
    }

    private static final Ambience QUIET = new Ambience("", 0.0f, 1.0f);
    private static final Ambience END = new Ambience("presence.void_stillness", 0.038f, 0.965f);

    private TravelAtmospherePolicy() { }

    public static Ambience choose(String dimension, long dayTime, boolean sheltered) {
        if (dimension == null) return QUIET;
        if ("minecraft:the_end".equals(dimension)) return END;
        // Nether and unrecognized modded dimensions retain their authored/companion
        // soundscapes; never paint their surroundings with Overworld dawn/dusk air.
        if (!"minecraft:overworld".equals(dimension)) return QUIET;

        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= 13500L && time < 22500L;
        boolean morning = time >= 22500L || time < 1700L;
        boolean evening = time >= 11500L && time < 14000L;

        if (night && sheltered) {
            return new Ambience("presence.void_stillness", 0.045f, 0.985f);
        }
        if (morning || evening) {
            return new Ambience("presence.harbor_air", 0.028f,
                    morning ? 1.015f : 0.985f);
        }
        return QUIET;
    }
}
