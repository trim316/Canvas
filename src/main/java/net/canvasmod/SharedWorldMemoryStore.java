package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class SharedWorldMemoryStore {
    public record Snapshot(
            int recognizedSettlements,
            int communityGatherings,
            long lastGatheringTick) { }

    private final Path file;
    private final Map<String, Snapshot> memories = new LinkedHashMap<>();

    public SharedWorldMemoryStore(Path file) {
        this.file = file;
        load();
    }

    public Snapshot snapshot(String settlementId) {
        return memories.getOrDefault(settlementId, new Snapshot(0, 0, Long.MIN_VALUE / 4L));
    }

    public void noteSettlement(String settlementId) {
        if (settlementId == null || settlementId.isBlank()) return;
        Snapshot current = snapshot(settlementId);
        memories.put(settlementId, new Snapshot(
                Math.max(1, current.recognizedSettlements()),
                current.communityGatherings(),
                current.lastGatheringTick()));
        save();
    }

    public void noteGathering(String settlementId, long tick) {
        if (settlementId == null || settlementId.isBlank()) return;
        Snapshot current = snapshot(settlementId);
        memories.put(settlementId, new Snapshot(
                Math.max(1, current.recognizedSettlements()),
                current.communityGatherings() + 1,
                tick));
        save();
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(memories.size()));
        int index = 0;
        for (var entry : memories.entrySet()) {
            String prefix = "memory." + index + ".";
            Snapshot value = entry.getValue();
            properties.setProperty(prefix + "settlementId", entry.getKey());
            properties.setProperty(prefix + "recognized", Integer.toString(value.recognizedSettlements()));
            properties.setProperty(prefix + "gatherings", Integer.toString(value.communityGatherings()));
            properties.setProperty(prefix + "lastGatheringTick", Long.toString(value.lastGatheringTick()));
            index++;
        }

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas shared observational world memory v1");
            }
            try {
                Files.move(tmp, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) { }
    }

    private void load() {
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (var in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
            return;
        }

        try {
            int count = Math.max(0, Integer.parseInt(properties.getProperty("count", "0")));
            for (int i = 0; i < count; i++) {
                String prefix = "memory." + i + ".";
                String settlementId = properties.getProperty(prefix + "settlementId", "");
                if (settlementId.isBlank()) continue;
                int recognized = Math.max(0, Integer.parseInt(properties.getProperty(prefix + "recognized", "0")));
                int gatherings = Math.max(0, Integer.parseInt(properties.getProperty(prefix + "gatherings", "0")));
                long last = Long.parseLong(properties.getProperty(
                        prefix + "lastGatheringTick",
                        Long.toString(Long.MIN_VALUE / 4L)));
                memories.put(settlementId, new Snapshot(recognized, gatherings, last));
            }
        } catch (NumberFormatException ignored) {
            memories.clear();
        }
    }
}
