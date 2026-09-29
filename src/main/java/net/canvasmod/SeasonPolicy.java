package net.canvasmod;

import java.util.Locale;

public final class SeasonPolicy {
    public enum Season { UNKNOWN, SPRING, SUMMER, AUTUMN, WINTER }

    public record Observation(Season season, String provider, String rawValue) {
        public Observation {
            season = season == null ? Season.UNKNOWN : season;
            provider = provider == null ? "none" : provider;
            rawValue = rawValue == null ? "" : rawValue;
        }

        public boolean known() { return season != Season.UNKNOWN; }
    }

    private SeasonPolicy() { }

    public static Season fromToken(String token) {
        if (token == null || token.isBlank()) return Season.UNKNOWN;
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("spring")) return Season.SPRING;
        if (normalized.contains("summer")) return Season.SUMMER;
        if (normalized.contains("autumn") || normalized.contains("fall")) return Season.AUTUMN;
        if (normalized.contains("winter")) return Season.WINTER;
        return Season.UNKNOWN;
    }
}
