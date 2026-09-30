package net.canvasmod;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LandmarkFamiliarityTracker {
    public record Observation(String key, int visits, boolean becameLandmark) {
        public static Observation none() {
            return new Observation("", 0, false);
        }
    }

    private static final class Entry {
        int visits;
        long lastVisitTick = Long.MIN_VALUE / 4L;
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private String activeKey = "";

    public Observation observe(
            String key,
            PlaceFamiliarityPolicy.Kind kind,
            long tick) {
        if (key == null || key.isBlank() || !LandmarkRecognitionPolicy.eligibleKind(kind)) {
            activeKey = "";
            return Observation.none();
        }
        if (key.equals(activeKey)) return Observation.none();
        activeKey = key;

        Entry entry = entries.get(key);
        if (entry == null) {
            if (entries.size() >= LandmarkRecognitionPolicy.MAX_LANDMARKS) {
                return Observation.none();
            }
            entry = new Entry();
            entries.put(key, entry);
        }

        if (entry.lastVisitTick > Long.MIN_VALUE / 8L
                && tick - entry.lastVisitTick < LandmarkRecognitionPolicy.MIN_REVISIT_GAP_TICKS) {
            return Observation.none();
        }

        int before = entry.visits;
        entry.visits = Math.min(LandmarkRecognitionPolicy.REQUIRED_VISITS, entry.visits + 1);
        entry.lastVisitTick = tick;
        return new Observation(
                key,
                entry.visits,
                before < LandmarkRecognitionPolicy.REQUIRED_VISITS
                        && entry.visits >= LandmarkRecognitionPolicy.REQUIRED_VISITS);
    }

    public boolean isLandmark(String key) {
        Entry entry = entries.get(key);
        return entry != null && entry.visits >= LandmarkRecognitionPolicy.REQUIRED_VISITS;
    }

    public Map<String, Integer> visits() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : entries.entrySet()) {
            result.put(entry.getKey(), entry.getValue().visits);
        }
        return Collections.unmodifiableMap(result);
    }

    public void restore(String key, int visits) {
        if (key == null || key.isBlank() || entries.size() >= LandmarkRecognitionPolicy.MAX_LANDMARKS) return;
        Entry entry = new Entry();
        entry.visits = Math.max(0, Math.min(LandmarkRecognitionPolicy.REQUIRED_VISITS, visits));
        entries.put(key, entry);
    }
}
