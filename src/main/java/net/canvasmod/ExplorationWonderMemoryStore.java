package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ExplorationWonderMemoryStore {
    public record Snapshot(long lastWorldDay, int moments) {
        public static Snapshot empty() {
            return new Snapshot(Long.MIN_VALUE / 4L, 0);
        }
    }

    private final Path file;
    private final Map<String, Snapshot> memory = new HashMap<>();

    public ExplorationWonderMemoryStore(Path file) {
        this.file = file;
        load();
    }

    public Snapshot snapshot(String contextKey) {
        if (contextKey == null || contextKey.isBlank()) return Snapshot.empty();
        return memory.getOrDefault(contextKey, Snapshot.empty());
    }

    public void note(String contextKey, long worldDay) {
        if (contextKey == null || contextKey.isBlank()) return;
        Snapshot current = snapshot(contextKey);
        memory.put(contextKey, new Snapshot(
                Math.max(current.lastWorldDay(), worldDay),
                current.moments() + 1));
        save();
    }

    private void load() {
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (var in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
            return;
        }

        int count;
        try {
            count = Math.max(0, Integer.parseInt(properties.getProperty("count", "0")));
        } catch (NumberFormatException ignored) {
            return;
        }
        for (int i = 0; i < count; i++) {
            String prefix = "wonder." + i + ".";
            String key = properties.getProperty(prefix + "key", "");
            if (key.isBlank()) continue;
            try {
                long day = Long.parseLong(properties.getProperty(prefix + "lastDay", Long.toString(Long.MIN_VALUE / 4L)));
                int moments = Math.max(0, Integer.parseInt(properties.getProperty(prefix + "moments", "0")));
                memory.put(key, new Snapshot(day, moments));
            } catch (NumberFormatException ignored) { }
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(memory.size()));
        int index = 0;
        for (var entry : memory.entrySet()) {
            String prefix = "wonder." + index + ".";
            properties.setProperty(prefix + "key", entry.getKey());
            properties.setProperty(prefix + "lastDay", Long.toString(entry.getValue().lastWorldDay()));
            properties.setProperty(prefix + "moments", Integer.toString(entry.getValue().moments()));
            index++;
        }

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas exploration wonder memory v1");
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
}
