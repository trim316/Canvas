package net.canvasmod;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RouteFamiliarityTracker {
    public record Observation(String segmentKey, int traversals, boolean becameFamiliar) {
        public static Observation none() {
            return new Observation("", 0, false);
        }
    }

    private final Map<String, Integer> traversals = new LinkedHashMap<>();
    private PlaceFamiliarityPolicy.Kind previousKind = PlaceFamiliarityPolicy.Kind.NONE;
    private String previousDimension;
    private double previousX;
    private double previousZ;

    public Observation observe(
            PlaceFamiliarityPolicy.Kind kind,
            String dimension,
            double x,
            double z) {
        Observation result = Observation.none();

        if (RouteFamiliarityPolicy.eligibleSegment(
                previousKind,
                kind,
                previousDimension,
                dimension,
                previousX,
                previousZ,
                x,
                z)) {
            String key = RouteFamiliarityPolicy.segmentKey(
                    dimension,
                    previousX,
                    previousZ,
                    x,
                    z);
            Integer existing = traversals.get(key);
            if (existing != null || traversals.size() < RouteFamiliarityPolicy.MAX_SEGMENTS) {
                int before = existing == null ? 0 : existing;
                int after = Math.min(RouteFamiliarityPolicy.REQUIRED_TRAVERSALS, before + 1);
                traversals.put(key, after);
                result = new Observation(
                        key,
                        after,
                        before < RouteFamiliarityPolicy.REQUIRED_TRAVERSALS
                                && after >= RouteFamiliarityPolicy.REQUIRED_TRAVERSALS);
            }
        }

        previousKind = kind == null ? PlaceFamiliarityPolicy.Kind.NONE : kind;
        previousDimension = dimension;
        previousX = x;
        previousZ = z;
        return result;
    }

    public boolean isFamiliar(String key) {
        return traversals.getOrDefault(key, 0) >= RouteFamiliarityPolicy.REQUIRED_TRAVERSALS;
    }

    public Map<String, Integer> entries() {
        return Collections.unmodifiableMap(traversals);
    }

    public void restore(String key, int count) {
        if (key == null || key.isBlank() || traversals.size() >= RouteFamiliarityPolicy.MAX_SEGMENTS) return;
        traversals.put(
                key,
                Math.max(0, Math.min(RouteFamiliarityPolicy.REQUIRED_TRAVERSALS, count)));
    }
}
