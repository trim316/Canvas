package net.canvasmod;

public final class PlaceFamiliarityPolicy {
    public enum Kind {
        NONE,
        PATH,
        DOCK,
        FARM,
        VIEWPOINT,
        GATHERING_SPOT
    }

    public static final int REQUIRED_GOOD_SAMPLES = 6;
    public static final double CANDIDATE_RADIUS_SQ = 12.0 * 12.0;
    public static final double DUPLICATE_RADIUS_SQ = 24.0 * 24.0;

    private PlaceFamiliarityPolicy() { }

    public record Evidence(
            int pathBlocks,
            int woodBlocks,
            int waterBlocks,
            int farmBlocks,
            int comfortBlocks,
            int socialAnchors,
            boolean openSky,
            int edgeDrops) {
        public Kind classify() {
            if (farmBlocks >= 12) return Kind.FARM;
            if (woodBlocks >= 10 && waterBlocks >= 12) return Kind.DOCK;
            if (socialAnchors >= 1 && comfortBlocks >= 4) return Kind.GATHERING_SPOT;
            if (pathBlocks >= 10) return Kind.PATH;
            if (openSky && edgeDrops >= 2) return Kind.VIEWPOINT;
            return Kind.NONE;
        }

        public String summary() {
            return "kind=" + classify()
                    + ",path=" + pathBlocks
                    + ",wood=" + woodBlocks
                    + ",water=" + waterBlocks
                    + ",farm=" + farmBlocks
                    + ",comfort=" + comfortBlocks
                    + ",social=" + socialAnchors
                    + ",openSky=" + openSky
                    + ",edgeDrops=" + edgeDrops;
        }
    }

    public static boolean samePlace(
            Kind aKind, String aDimension, double ax, double ay, double az,
            Kind bKind, String bDimension, double bx, double by, double bz,
            double radiusSq) {
        if (aKind == null || bKind == null || aKind != bKind) return false;
        if (aDimension == null || bDimension == null || !aDimension.equals(bDimension)) return false;
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz <= radiusSq;
    }
}
