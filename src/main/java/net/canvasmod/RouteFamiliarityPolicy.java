package net.canvasmod;

public final class RouteFamiliarityPolicy {
    public static final int GRID_SIZE = 16;
    public static final int REQUIRED_TRAVERSALS = 3;
    public static final int MAX_SEGMENTS = 1024;
    public static final double MIN_SAMPLE_DISTANCE_SQ = 4.0 * 4.0;
    public static final double MAX_SAMPLE_DISTANCE_SQ = 48.0 * 48.0;

    private RouteFamiliarityPolicy() { }

    public static String segmentKey(
            String dimension,
            double ax,
            double az,
            double bx,
            double bz) {
        int acx = Math.floorDiv((int)Math.floor(ax), GRID_SIZE);
        int acz = Math.floorDiv((int)Math.floor(az), GRID_SIZE);
        int bcx = Math.floorDiv((int)Math.floor(bx), GRID_SIZE);
        int bcz = Math.floorDiv((int)Math.floor(bz), GRID_SIZE);

        String a = acx + "," + acz;
        String b = bcx + "," + bcz;
        if (a.compareTo(b) <= 0) {
            return dimension + "|" + a + ">" + b;
        }
        return dimension + "|" + b + ">" + a;
    }

    public static boolean eligibleSegment(
            PlaceFamiliarityPolicy.Kind previousKind,
            PlaceFamiliarityPolicy.Kind currentKind,
            String previousDimension,
            String currentDimension,
            double previousX,
            double previousZ,
            double currentX,
            double currentZ) {
        if (previousKind != PlaceFamiliarityPolicy.Kind.PATH
                || currentKind != PlaceFamiliarityPolicy.Kind.PATH) return false;
        if (previousDimension == null || !previousDimension.equals(currentDimension)) return false;

        double dx = currentX - previousX;
        double dz = currentZ - previousZ;
        double distanceSq = dx * dx + dz * dz;

        int previousCellX = Math.floorDiv((int)Math.floor(previousX), GRID_SIZE);
        int previousCellZ = Math.floorDiv((int)Math.floor(previousZ), GRID_SIZE);
        int currentCellX = Math.floorDiv((int)Math.floor(currentX), GRID_SIZE);
        int currentCellZ = Math.floorDiv((int)Math.floor(currentZ), GRID_SIZE);
        boolean changedCell = previousCellX != currentCellX || previousCellZ != currentCellZ;

        return changedCell
                && distanceSq >= MIN_SAMPLE_DISTANCE_SQ
                && distanceSq <= MAX_SAMPLE_DISTANCE_SQ;
    }
}
