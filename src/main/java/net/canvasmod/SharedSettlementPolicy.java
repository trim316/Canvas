package net.canvasmod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SharedSettlementPolicy {
    public static final double HOME_LINK_RADIUS = 96.0;
    public static final double HOME_LINK_RADIUS_SQ = HOME_LINK_RADIUS * HOME_LINK_RADIUS;
    public static final double MAX_VERTICAL_SEPARATION = 48.0;
    public static final int MIN_HOMES = 2;

    private SharedSettlementPolicy() { }

    public record HomeAnchor(
            UUID playerId,
            String dimension,
            double x,
            double y,
            double z) { }

    public record Candidate(
            String dimension,
            double x,
            double y,
            double z,
            List<UUID> members) { }

    public static List<Candidate> detect(List<HomeAnchor> homes) {
        if (homes == null || homes.size() < MIN_HOMES) return List.of();

        List<HomeAnchor> ordered = new ArrayList<>(homes);
        ordered.sort(Comparator
                .comparing(HomeAnchor::dimension)
                .thenComparing(home -> home.playerId().toString()));

        boolean[] visited = new boolean[ordered.size()];
        List<Candidate> candidates = new ArrayList<>();

        for (int start = 0; start < ordered.size(); start++) {
            if (visited[start]) continue;

            ArrayDeque<Integer> queue = new ArrayDeque<>();
            List<HomeAnchor> component = new ArrayList<>();
            queue.add(start);
            visited[start] = true;

            while (!queue.isEmpty()) {
                int current = queue.removeFirst();
                HomeAnchor anchor = ordered.get(current);
                component.add(anchor);

                for (int other = 0; other < ordered.size(); other++) {
                    if (visited[other]) continue;
                    HomeAnchor candidate = ordered.get(other);
                    if (linked(anchor, candidate)) {
                        visited[other] = true;
                        queue.addLast(other);
                    }
                }
            }

            if (component.size() < MIN_HOMES) continue;

            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            Set<UUID> uniqueMembers = new HashSet<>();
            for (HomeAnchor anchor : component) {
                x += anchor.x();
                y += anchor.y();
                z += anchor.z();
                uniqueMembers.add(anchor.playerId());
            }
            if (uniqueMembers.size() < MIN_HOMES) continue;

            List<UUID> members = new ArrayList<>(uniqueMembers);
            members.sort(Comparator.comparing(UUID::toString));
            candidates.add(new Candidate(
                    component.get(0).dimension(),
                    x / component.size(),
                    y / component.size(),
                    z / component.size(),
                    List.copyOf(members)));
        }

        return List.copyOf(candidates);
    }

    public static boolean linked(HomeAnchor a, HomeAnchor b) {
        if (a == null || b == null) return false;
        if (a.playerId().equals(b.playerId())) return false;
        if (a.dimension() == null || !a.dimension().equals(b.dimension())) return false;
        if (Math.abs(a.y() - b.y()) > MAX_VERTICAL_SEPARATION) return false;
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return dx * dx + dz * dz <= HOME_LINK_RADIUS_SQ;
    }
}
