package net.canvasmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class SharedSettlementStore {
    public static final double MATCH_RADIUS = 128.0;
    private static final double MATCH_RADIUS_SQ = MATCH_RADIUS * MATCH_RADIUS;

    public record Settlement(
            String id,
            String dimension,
            double x,
            double y,
            double z,
            int memberCount) { }

    private final Path file;
    private final Map<String, Settlement> settlements = new LinkedHashMap<>();
    private long nextId = 1L;

    public SharedSettlementStore(Path file) {
        this.file = file;
        load();
    }

    public List<Settlement> reconcile(List<SharedSettlementPolicy.Candidate> candidates) {
        List<Settlement> created = new ArrayList<>();
        Set<String> claimed = new HashSet<>();

        for (SharedSettlementPolicy.Candidate candidate : candidates) {
            Settlement existing = nearestUnclaimed(candidate, claimed);
            if (existing == null) {
                String id = "canvas-settlement-" + nextId++;
                Settlement settlement = new Settlement(
                        id,
                        candidate.dimension(),
                        candidate.x(),
                        candidate.y(),
                        candidate.z(),
                        candidate.members().size());
                settlements.put(id, settlement);
                claimed.add(id);
                created.add(settlement);
            } else {
                Settlement updated = new Settlement(
                        existing.id(),
                        existing.dimension(),
                        candidate.x(),
                        candidate.y(),
                        candidate.z(),
                        candidate.members().size());
                settlements.put(existing.id(), updated);
                claimed.add(existing.id());
            }
        }

        if (!candidates.isEmpty() || !created.isEmpty()) save();
        return List.copyOf(created);
    }

    public Settlement match(SharedSettlementPolicy.Candidate candidate) {
        if (candidate == null) return null;
        return nearestUnclaimed(candidate, Set.of());
    }

    public List<Settlement> settlements() {
        List<Settlement> result = new ArrayList<>(settlements.values());
        result.sort(Comparator.comparing(Settlement::id));
        return List.copyOf(result);
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("nextId", Long.toString(nextId));
        properties.setProperty("count", Integer.toString(settlements.size()));

        int index = 0;
        for (Settlement settlement : settlements.values()) {
            String prefix = "settlement." + index + ".";
            properties.setProperty(prefix + "id", settlement.id());
            properties.setProperty(prefix + "dimension", settlement.dimension());
            properties.setProperty(prefix + "x", Double.toString(settlement.x()));
            properties.setProperty(prefix + "y", Double.toString(settlement.y()));
            properties.setProperty(prefix + "z", Double.toString(settlement.z()));
            properties.setProperty(prefix + "members", Integer.toString(settlement.memberCount()));
            index++;
        }

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(tmp)) {
                properties.store(out, "Canvas shared settlements v1");
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

    private Settlement nearestUnclaimed(
            SharedSettlementPolicy.Candidate candidate,
            Set<String> claimed) {
        Settlement best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Settlement settlement : settlements.values()) {
            if (claimed.contains(settlement.id())) continue;
            if (!settlement.dimension().equals(candidate.dimension())) continue;
            double dx = settlement.x() - candidate.x();
            double dz = settlement.z() - candidate.z();
            double distance = dx * dx + dz * dz;
            if (distance <= MATCH_RADIUS_SQ && distance < bestDistance) {
                best = settlement;
                bestDistance = distance;
            }
        }
        return best;
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
            nextId = Math.max(1L, Long.parseLong(properties.getProperty("nextId", "1")));
            int count = Math.max(0, Integer.parseInt(properties.getProperty("count", "0")));
            for (int i = 0; i < count; i++) {
                String prefix = "settlement." + i + ".";
                String id = properties.getProperty(prefix + "id", "");
                String dimension = properties.getProperty(prefix + "dimension", "");
                if (id.isBlank() || dimension.isBlank()) continue;
                double x = Double.parseDouble(properties.getProperty(prefix + "x", "0"));
                double y = Double.parseDouble(properties.getProperty(prefix + "y", "64"));
                double z = Double.parseDouble(properties.getProperty(prefix + "z", "0"));
                int members = Math.max(0, Integer.parseInt(properties.getProperty(prefix + "members", "0")));
                settlements.put(id, new Settlement(id, dimension, x, y, z, members));
            }
        } catch (NumberFormatException ignored) {
            settlements.clear();
            nextId = 1L;
        }
    }
}
