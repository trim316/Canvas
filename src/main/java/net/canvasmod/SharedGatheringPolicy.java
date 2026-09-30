package net.canvasmod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SharedGatheringPolicy {
    public static final int MIN_PARTICIPANTS = 2;
    public static final double SETTLEMENT_RADIUS = 64.0;
    public static final double SETTLEMENT_RADIUS_SQ = SETTLEMENT_RADIUS * SETTLEMENT_RADIUS;
    public static final double CLUSTER_RADIUS = 12.0;
    public static final double CLUSTER_RADIUS_SQ = CLUSTER_RADIUS * CLUSTER_RADIUS;

    private SharedGatheringPolicy() { }

    public record PlayerPresence(
            UUID playerId,
            String dimension,
            double x,
            double y,
            double z) { }

    public record Gathering(
            String key,
            String dimension,
            double x,
            double y,
            double z,
            List<UUID> participants) { }

    public static Gathering detect(
            SharedSettlementPolicy.Candidate settlement,
            List<PlayerPresence> online) {
        if (settlement == null || online == null || online.size() < MIN_PARTICIPANTS) return null;

        List<PlayerPresence> eligible = new ArrayList<>();
        for (PlayerPresence presence : online) {
            if (presence == null || !settlement.members().contains(presence.playerId())) continue;
            if (!settlement.dimension().equals(presence.dimension())) continue;
            double dx = presence.x() - settlement.x();
            double dz = presence.z() - settlement.z();
            if (dx * dx + dz * dz <= SETTLEMENT_RADIUS_SQ) eligible.add(presence);
        }
        if (eligible.size() < MIN_PARTICIPANTS) return null;

        List<PlayerPresence> best = List.of();
        for (PlayerPresence anchor : eligible) {
            List<PlayerPresence> cluster = new ArrayList<>();
            for (PlayerPresence other : eligible) {
                double dx = anchor.x() - other.x();
                double dy = anchor.y() - other.y();
                double dz = anchor.z() - other.z();
                if (dx * dx + dy * dy + dz * dz <= CLUSTER_RADIUS_SQ) cluster.add(other);
            }
            if (cluster.size() > best.size()) best = cluster;
        }
        if (best.size() < MIN_PARTICIPANTS) return null;

        List<UUID> participants = best.stream()
                .map(PlayerPresence::playerId)
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        double x = best.stream().mapToDouble(PlayerPresence::x).average().orElse(settlement.x());
        double y = best.stream().mapToDouble(PlayerPresence::y).average().orElse(settlement.y());
        double z = best.stream().mapToDouble(PlayerPresence::z).average().orElse(settlement.z());
        String key = settlement.dimension() + "|" + participants.stream()
                .map(UUID::toString)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return new Gathering(key, settlement.dimension(), x, y, z, participants);
    }
}
