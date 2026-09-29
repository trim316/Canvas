package net.canvasmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Properties;

public final class CanvasWorldMemoryStore {
    public record Snapshot(
            int homecomings,
            int familiarMoments,
            int villageMoments,
            int rareMoments,
            long lastSurpriseDay) { }

    private final Path file;
    private String scopeKey;
    private Snapshot snapshot = new Snapshot(0, 0, 0, 0, Long.MIN_VALUE / 4);

    public CanvasWorldMemoryStore(Path file) {
        this.file = file;
    }

    public void bind(String scope) {
        scopeKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(scope.getBytes(StandardCharsets.UTF_8));
        snapshot = readScope(scopeKey);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public void noteHomecoming() {
        ensureBound();
        snapshot = new Snapshot(
                snapshot.homecomings() + 1,
                snapshot.familiarMoments(),
                snapshot.villageMoments(),
                snapshot.rareMoments(),
                snapshot.lastSurpriseDay());
        save();
    }

    public void noteFamiliarMoment() {
        ensureBound();
        snapshot = new Snapshot(
                snapshot.homecomings(),
                snapshot.familiarMoments() + 1,
                snapshot.villageMoments(),
                snapshot.rareMoments(),
                snapshot.lastSurpriseDay());
        save();
    }

    public void noteVillageMoment() {
        ensureBound();
        snapshot = new Snapshot(
                snapshot.homecomings(),
                snapshot.familiarMoments(),
                snapshot.villageMoments() + 1,
                snapshot.rareMoments(),
                snapshot.lastSurpriseDay());
        save();
    }

    public void noteRareMoment(long worldDay) {
        ensureBound();
        snapshot = new Snapshot(
                snapshot.homecomings(),
                snapshot.familiarMoments(),
                snapshot.villageMoments(),
                snapshot.rareMoments() + 1,
                worldDay);
        save();
    }

    private Snapshot readScope(String key) {
        Properties p = loadAll();
        String prefix = "scope." + key + ".";
        return new Snapshot(
                parseInt(p.getProperty(prefix + "homecomings"), 0),
                parseInt(p.getProperty(prefix + "familiar"), 0),
                parseInt(p.getProperty(prefix + "village"), 0),
                parseInt(p.getProperty(prefix + "rare"), 0),
                parseLong(p.getProperty(prefix + "lastSurpriseDay"), Long.MIN_VALUE / 4));
    }

    private void save() {
        Properties p = loadAll();
        String prefix = "scope." + scopeKey + ".";
        p.setProperty(prefix + "homecomings", Integer.toString(snapshot.homecomings()));
        p.setProperty(prefix + "familiar", Integer.toString(snapshot.familiarMoments()));
        p.setProperty(prefix + "village", Integer.toString(snapshot.villageMoments()));
        p.setProperty(prefix + "rare", Integer.toString(snapshot.rareMoments()));
        p.setProperty(prefix + "lastSurpriseDay", Long.toString(snapshot.lastSurpriseDay()));

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                p.store(out, "Canvas world-memory continuity v1");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) { }
    }

    private Properties loadAll() {
        Properties p = new Properties();
        if (!Files.exists(file)) return p;
        try (var in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException ignored) { }
        return p;
    }

    private void ensureBound() {
        if (scopeKey == null) throw new IllegalStateException("Canvas world-memory scope is not bound");
    }

    private static int parseInt(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static long parseLong(String value, long fallback) {
        try { return value == null ? fallback : Long.parseLong(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
