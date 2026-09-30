package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public final class CanvasWorldIdentityStore {
    private final Path file;
    private final String saveKey;
    private final String worldId;
    private final String branchId;

    public CanvasWorldIdentityStore(Path file) {
        this(file, defaultSaveKey(file));
    }

    public CanvasWorldIdentityStore(Path file, String saveKey) {
        this.file = file;
        this.saveKey = normalizeSaveKey(saveKey);
        Identity identity = loadOrCreate();
        this.worldId = identity.worldId();
        this.branchId = identity.branchId();
    }

    public String worldId() {
        return worldId;
    }

    public String branchId() {
        return branchId;
    }

    public String scopeId() {
        return worldId + ":" + branchId;
    }

    private Identity loadOrCreate() {
        Properties properties = load();
        String existingWorld = validUuid(properties.getProperty("worldId", ""));
        String existingBranch = validUuid(properties.getProperty("branchId", ""));
        String existingSaveKey = normalizeSaveKey(properties.getProperty("saveKey", ""));

        if (existingWorld.isBlank()) {
            Identity created = new Identity(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString());
            persist(created);
            return created;
        }

        if (existingBranch.isBlank()) {
            Identity migrated = new Identity(existingWorld, UUID.randomUUID().toString());
            persist(migrated);
            return migrated;
        }

        if (!existingSaveKey.isBlank() && !existingSaveKey.equals(saveKey)) {
            Identity fork = new Identity(existingWorld, UUID.randomUUID().toString());
            persist(fork);
            return fork;
        }

        Identity stable = new Identity(existingWorld, existingBranch);
        if (existingSaveKey.isBlank()) persist(stable);
        return stable;
    }

    private Properties load() {
        Properties properties = new Properties();
        if (!Files.exists(file)) return properties;
        try (var in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) { }
        return properties;
    }

    private void persist(Identity identity) {
        Properties properties = new Properties();
        properties.setProperty("worldId", identity.worldId());
        properties.setProperty("branchId", identity.branchId());
        properties.setProperty("saveKey", saveKey);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas world identity and branch lineage v2");
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

    private static String validUuid(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isBlank()) return "";
        try {
            UUID.fromString(candidate);
            return candidate;
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String normalizeSaveKey(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? "default" : normalized;
    }

    private static String defaultSaveKey(Path file) {
        Path parent = file == null ? null : file.getParent();
        Path root = parent == null ? null : parent.getParent();
        Path name = root == null ? null : root.getFileName();
        return name == null ? "default" : name.toString();
    }

    private record Identity(String worldId, String branchId) { }
}
