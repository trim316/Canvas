package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public final class CanvasWorldIdentityStore {
    private final Path file;
    private final String worldId;

    public CanvasWorldIdentityStore(Path file) {
        this.file = file;
        this.worldId = loadOrCreate();
    }

    public String worldId() {
        return worldId;
    }

    private String loadOrCreate() {
        if (Files.exists(file)) {
            Properties properties = new Properties();
            try (var in = Files.newInputStream(file)) {
                properties.load(in);
                String existing = properties.getProperty("worldId", "").trim();
                if (!existing.isBlank()) {
                    UUID.fromString(existing);
                    return existing;
                }
            } catch (IOException | IllegalArgumentException ignored) { }
        }

        String created = UUID.randomUUID().toString();
        Properties properties = new Properties();
        properties.setProperty("worldId", created);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas stable world identity v1");
            }
            try {
                Files.move(tmp, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) { }
        return created;
    }
}
