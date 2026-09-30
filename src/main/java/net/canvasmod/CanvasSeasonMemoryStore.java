package net.canvasmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Properties;

public final class CanvasSeasonMemoryStore {
    public record Snapshot(
            SeasonPolicy.Season lastSeason,
            int seasonTransitions,
            int seasonalMoments,
            long lastSeasonalMomentDay,
            boolean firstSnowSeen) { }

    private final Path file;
    private String scopeKey;
    private Snapshot snapshot =
            new Snapshot(SeasonPolicy.Season.UNKNOWN, 0, 0, Long.MIN_VALUE / 4L, false);

    public CanvasSeasonMemoryStore(Path file) {
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

    public boolean observeSeason(SeasonPolicy.Season season) {
        ensureBound();
        if (season == null || season == SeasonPolicy.Season.UNKNOWN) return false;
        SeasonPolicy.Season previous = snapshot.lastSeason();
        boolean changed = previous != SeasonPolicy.Season.UNKNOWN && previous != season;
        if (previous == season) return false;
        snapshot = new Snapshot(
                season,
                snapshot.seasonTransitions() + (changed ? 1 : 0),
                snapshot.seasonalMoments(),
                snapshot.lastSeasonalMomentDay(),
                snapshot.firstSnowSeen());
        save();
        return changed;
    }

    public void noteSeasonalMoment(SeasonalRareMomentPolicy.Moment moment, long worldDay) {
        ensureBound();
        if (moment == null || moment == SeasonalRareMomentPolicy.Moment.NONE) return;
        snapshot = new Snapshot(
                snapshot.lastSeason(),
                snapshot.seasonTransitions(),
                snapshot.seasonalMoments() + 1,
                worldDay,
                snapshot.firstSnowSeen() || moment == SeasonalRareMomentPolicy.Moment.FIRST_SNOW);
        save();
    }

    private Snapshot readScope(String key) {
        Properties p = loadAll();
        String prefix = "scope." + key + ".";
        SeasonPolicy.Season season;
        try {
            season = SeasonPolicy.Season.valueOf(
                    p.getProperty(prefix + "lastSeason", "UNKNOWN"));
        } catch (IllegalArgumentException ignored) {
            season = SeasonPolicy.Season.UNKNOWN;
        }
        return new Snapshot(
                season,
                parseInt(p.getProperty(prefix + "transitions"), 0),
                parseInt(p.getProperty(prefix + "moments"), 0),
                parseLong(p.getProperty(prefix + "lastMomentDay"), Long.MIN_VALUE / 4L),
                Boolean.parseBoolean(p.getProperty(prefix + "firstSnowSeen", "false")));
    }

    private void save() {
        Properties p = loadAll();
        String prefix = "scope." + scopeKey + ".";
        p.setProperty(prefix + "lastSeason", snapshot.lastSeason().name());
        p.setProperty(prefix + "transitions", Integer.toString(snapshot.seasonTransitions()));
        p.setProperty(prefix + "moments", Integer.toString(snapshot.seasonalMoments()));
        p.setProperty(prefix + "lastMomentDay", Long.toString(snapshot.lastSeasonalMomentDay()));
        p.setProperty(prefix + "firstSnowSeen", Boolean.toString(snapshot.firstSnowSeen()));

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                p.store(out, "Canvas Seasons of Home memory v1");
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
        if (scopeKey == null) throw new IllegalStateException("Canvas season-memory scope is not bound");
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
